package me.nulldoubt.micro.assets;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.assets.loaders.*;
import me.nulldoubt.micro.audio.Music;
import me.nulldoubt.micro.audio.Sound;
import me.nulldoubt.micro.graphics.Pixmap;
import me.nulldoubt.micro.graphics.Texture;
import me.nulldoubt.micro.graphics.g2d.BitmapFont;
import me.nulldoubt.micro.graphics.g2d.TextureAtlas;
import me.nulldoubt.micro.graphics.glutils.Shader;
import me.nulldoubt.micro.utils.*;
import me.nulldoubt.micro.utils.ObjectMap.Entry;
import me.nulldoubt.micro.utils.async.AsyncExecutor;

import java.lang.StringBuilder;

public class AssetManager implements Disposable {
	
	public static final String TAG = "AssetManager";
	
	public static final FileHandleResolver RESOLVER_ABSOLUTE = ((fileName) -> Micro.files.absolute(fileName));
	public static final FileHandleResolver RESOLVER_CLASSPATH = ((fileName) -> Micro.files.classpath(fileName));
	public static final FileHandleResolver RESOLVER_EXTERNAL = ((fileName) -> Micro.files.external(fileName));
	public static final FileHandleResolver RESOLVER_INTERNAL = ((fileName) -> Micro.files.internal(fileName));
	public static final FileHandleResolver RESOLVER_LOCAL = ((fileName) -> Micro.files.local(fileName));
	
	final ObjectMap<Class<?>, ObjectMap<String, RefCountedContainer>> assets = new ObjectMap<>();
	final ObjectMap<String, Class<?>> assetTypes = new ObjectMap<>();
	final ObjectMap<String, Array<String>> assetDependencies = new ObjectMap<>();
	final ObjectSet<String> injected = new ObjectSet<>();
	
	final ObjectMap<Class<?>, ObjectMap<String, AssetLoader<?, ?>>> loaders = new ObjectMap<>();
	final Array<AssetDescriptor<?>> loadQueue = new Array<>();
	final AsyncExecutor executor;
	
	final Array<AssetLoadingTask> tasks = new Array<>();
	AssetErrorListener listener;
	int loaded;
	int toLoad;
	int peakTasks;
	
	final FileHandleResolver resolver;
	
	public AssetManager() {
		this(RESOLVER_INTERNAL);
	}
	
	public AssetManager(FileHandleResolver resolver) {
		this(resolver, true);
	}
	
	public AssetManager(FileHandleResolver resolver, boolean defaultLoaders) {
		this.resolver = resolver;
		if (defaultLoaders) {
			setLoader(BitmapFont.class, new BitmapFontLoader(resolver));
			setLoader(Music.class, new MusicLoader(resolver));
			setLoader(Pixmap.class, new PixmapLoader(resolver));
			setLoader(Sound.class, new SoundLoader(resolver));
			setLoader(TextureAtlas.class, new TextureAtlasLoader(resolver));
			setLoader(Texture.class, new TextureLoader(resolver));
			setLoader(I18NBundle.class, new I18NBundleLoader(resolver));
			setLoader(Shader.class, new ShaderProgramLoader(resolver));
		}
		executor = new AsyncExecutor(1, "AssetManager");
	}
	
	public FileHandleResolver getFileHandleResolver() {
		return resolver;
	}
	
	public synchronized <T> T get(String fileName) {
		return get(fileName, true);
	}
	
	public synchronized <T> T get(String fileName, Class<T> type) {
		return get(fileName, type, true);
	}
	
	public synchronized <T> T get(String fileName, boolean required) {
		Class<T> type = (Class<T>) assetTypes.get(fileName);
		if (type != null) {
			ObjectMap<String, RefCountedContainer> assetsByType = assets.get(type);
			if (assetsByType != null) {
				RefCountedContainer assetContainer = assetsByType.get(fileName);
				if (assetContainer != null)
					return (T) assetContainer.object;
			}
		}
		if (required)
			throw new MicroRuntimeException("Asset not loaded: " + fileName);
		return null;
	}
	
	public synchronized <T> T get(String fileName, Class<T> type, boolean required) {
		ObjectMap<String, RefCountedContainer> assetsByType = assets.get(type);
		if (assetsByType != null) {
			RefCountedContainer assetContainer = assetsByType.get(fileName);
			if (assetContainer != null)
				return (T) assetContainer.object;
		}
		if (required)
			throw new MicroRuntimeException("Asset not loaded: " + fileName);
		return null;
	}
	
	public synchronized <T> T get(AssetDescriptor<T> assetDescriptor) {
		return get(assetDescriptor.fileName, assetDescriptor.type, true);
	}
	
	public synchronized <T> Array<T> getAll(Class<T> type, Array<T> out) {
		ObjectMap<String, RefCountedContainer> assetsByType = assets.get(type);
		if (assetsByType != null) {
			for (RefCountedContainer assetRef : assetsByType.values())
				out.add((T) assetRef.object);
		}
		return out;
	}
	
	public synchronized boolean contains(String fileName) {
		if (tasks.size > 0 && tasks.first().assetDesc.fileName.equals(fileName))
			return true;
		
		for (int i = 0; i < loadQueue.size; i++)
			if (loadQueue.get(i).fileName.equals(fileName))
				return true;
		
		return isLoaded(fileName);
	}
	
	public synchronized boolean contains(String fileName, Class<?> type) {
		if (tasks.size > 0) {
			final AssetDescriptor<?> assetDesc = tasks.first().assetDesc;
			if (assetDesc.type == type && assetDesc.fileName.equals(fileName))
				return true;
		}
		
		for (int i = 0; i < loadQueue.size; i++) {
			final AssetDescriptor<?> assetDesc = loadQueue.get(i);
			if (assetDesc.type == type && assetDesc.fileName.equals(fileName))
				return true;
		}
		
		return isLoaded(fileName, type);
	}
	
	public synchronized void unload(String fileName) {
		
		if (tasks.size > 0) {
			AssetLoadingTask currentTask = tasks.first();
			if (currentTask.assetDesc.fileName.equals(fileName)) {
				Micro.app.log(TAG, "Unload (from tasks): " + fileName);
				currentTask.cancel = true;
				currentTask.unload();
				return;
			}
		}
		
		Class<?> type = assetTypes.get(fileName);
		
		// check if it's in the queue
		int foundIndex = -1;
		for (int i = 0; i < loadQueue.size; i++) {
			if (loadQueue.get(i).fileName.equals(fileName)) {
				foundIndex = i;
				break;
			}
		}
		if (foundIndex != -1) {
			toLoad--;
			AssetDescriptor<?> desc = loadQueue.removeIndex(foundIndex);
			Micro.app.log(TAG, "Unload (from queue): " + fileName);
			
			// if the queued asset was already loaded, let the callback know it is available.
			if (type != null && desc.params != null && desc.params.loadedCallback != null)
				desc.params.loadedCallback.finishedLoading(this, desc.fileName, desc.type);
			return;
		}
		
		if (type == null)
			throw new MicroRuntimeException("Asset not loaded: " + fileName);
		
		RefCountedContainer assetRef = assets.get(type).get(fileName);
		
		// if it is reference counted, decrement ref count and check if we can really get rid of it.
		assetRef.refCount--;
		if (assetRef.refCount <= 0) {
			Micro.app.log(TAG, "Unload (dispose): " + fileName);
			
			// if it is disposable dispose it
			if (assetRef.object instanceof Disposable)
				((Disposable) assetRef.object).dispose();
			
			// remove the asset from the manager.
			assetTypes.remove(fileName);
			assets.get(type).remove(fileName);
		} else
			Micro.app.log(TAG, "Unload (decrement): " + fileName);
		
		// remove any dependencies (or just decrement their ref count).
		Array<String> dependencies = assetDependencies.get(fileName);
		if (dependencies != null) {
			for (String dependency : dependencies)
				if (isLoaded(dependency))
					unload(dependency);
		}
		// remove dependencies if ref count < 0
		if (assetRef.refCount <= 0)
			assetDependencies.remove(fileName);
	}
	
	public synchronized <T> boolean containsAsset(T asset) {
		ObjectMap<String, RefCountedContainer> assetsByType = assets.get(asset.getClass());
		if (assetsByType == null)
			return false;
		for (RefCountedContainer assetRef : assetsByType.values())
			if (assetRef.object == asset || asset.equals(assetRef.object))
				return true;
		return false;
	}
	
	public synchronized <T> String getAssetFileName(T asset) {
		for (Class<?> assetType : assets.keys()) {
			ObjectMap<String, RefCountedContainer> assetsByType = assets.get(assetType);
			for (Entry<String, RefCountedContainer> entry : assetsByType) {
				Object object = entry.value.object;
				if (object == asset || asset.equals(object))
					return entry.key;
			}
		}
		return null;
	}
	
	public synchronized boolean isLoaded(AssetDescriptor<?> assetDesc) {
		return isLoaded(assetDesc.fileName);
	}
	
	public synchronized boolean isLoaded(String fileName) {
		if (fileName == null)
			return false;
		return assetTypes.containsKey(fileName);
	}
	
	public synchronized boolean isLoaded(String fileName, Class<?> type) {
		ObjectMap<String, RefCountedContainer> assetsByType = assets.get(type);
		if (assetsByType == null)
			return false;
		return assetsByType.get(fileName) != null;
	}
	
	public <T, P extends AssetLoaderParameters<T>> AssetLoader<T, P> getLoader(final Class<T> type) {
		return getLoader(type, null);
	}
	
	public <T, P extends AssetLoaderParameters<T>> AssetLoader<T, P> getLoader(final Class<T> type, final String fileName) {
		final ObjectMap<String, AssetLoader<?, ?>> loaders = this.loaders.get(type);
		if (loaders == null || loaders.size < 1)
			return null;
		if (fileName == null)
			return (AssetLoader<T, P>) loaders.get("");
		AssetLoader<T, P> result = null;
		int length = -1;
		for (Entry<String, AssetLoader<?, ?>> entry : loaders.entries()) {
			if (entry.key.length() > length && fileName.endsWith(entry.key)) {
				result = (AssetLoader<T, P>) entry.value;
				length = entry.key.length();
			}
		}
		return result;
	}
	
	public synchronized <T> void load(String fileName, Class<T> type) {
		load(fileName, type, null);
	}
	
	public synchronized void load(String fileName, Class<?> type, AssetLoaderParameters<?> parameter) {
		final AssetLoader<?, ?> loader = getLoader(type, fileName);
		if (loader == null)
			throw new MicroRuntimeException("No loader for type: " + type.getSimpleName());
		
		if (loadQueue.size == 0) {
			loaded = 0;
			toLoad = 0;
			peakTasks = 0;
		}
		
		for (int i = 0; i < loadQueue.size; i++) {
			final AssetDescriptor<?> desc = loadQueue.get(i);
			if (desc.fileName.equals(fileName) && !desc.type.equals(type))
				throw new MicroRuntimeException("Asset with name '" + fileName + "' already in preload queue, but has different type (expected: " + type.getSimpleName() + ", found: " + desc.type.getSimpleName() + ")");
		}
		
		for (int i = 0; i < tasks.size; i++) {
			final AssetDescriptor<?> desc = tasks.get(i).assetDesc;
			if (desc.fileName.equals(fileName) && !desc.type.equals(type))
				throw new MicroRuntimeException("Asset with name '" + fileName + "' already in task list, but has different type (expected: " + type.getSimpleName() + ", found: " + desc.type.getSimpleName() + ")");
		}
		
		final Class<?> otherType = assetTypes.get(fileName);
		if (otherType != null && !otherType.equals(type))
			throw new MicroRuntimeException("Asset with name '" + fileName + "' already loaded, but has different type (expected: " + type.getSimpleName() + ", found: " + otherType.getSimpleName() + ")");
		
		toLoad++;
		final AssetDescriptor<?> assetDesc = new AssetDescriptor(fileName, type, parameter);
		loadQueue.add(assetDesc);
		Micro.app.debug(TAG, "Queued: " + assetDesc);
	}
	
	public synchronized void load(AssetDescriptor<?> desc) {
		load(desc.fileName, desc.type, desc.params);
	}
	
	public synchronized boolean update() {
		try {
			if (tasks.size == 0) {
				while (loadQueue.size != 0 && tasks.size == 0)
					nextTask();
				if (tasks.size == 0)
					return true;
			}
			return updateTask() && loadQueue.size == 0 && tasks.size == 0;
		} catch (Throwable t) {
			handleTaskError(t);
			return loadQueue.size == 0;
		}
	}
	
	public boolean update(final int millis) {
		final long endTime = System.currentTimeMillis() + millis;
		while (true) {
			final boolean done = update();
			if (done || System.currentTimeMillis() > endTime)
				return done;
			Thread.yield();
		}
	}
	
	public synchronized boolean isFinished() {
		return loadQueue.size == 0 && tasks.size == 0;
	}
	
	public void finishLoading() {
		Micro.app.debug(TAG, "Waiting for loading to complete...");
		while (!update())
			Thread.yield();
		Micro.app.debug(TAG, "Loading complete.");
	}
	
	public <T> T finishLoadingAsset(AssetDescriptor assetDesc) {
		return finishLoadingAsset(assetDesc.fileName);
	}
	
	public <T> T finishLoadingAsset(String fileName) {
		Micro.app.debug(TAG, "Waiting for asset to be loaded: " + fileName);
		while (true) {
			synchronized (this) {
				Class<T> type = (Class<T>) assetTypes.get(fileName);
				if (type != null) {
					final ObjectMap<String, RefCountedContainer> assetsByType = assets.get(type);
					if (assetsByType != null) {
						RefCountedContainer assetContainer = assetsByType.get(fileName);
						if (assetContainer != null) {
							Micro.app.debug(TAG, "Asset loaded: " + fileName);
							return (T) assetContainer.object;
						}
					}
				}
				update();
			}
			Thread.yield();
		}
	}
	
	synchronized void injectDependencies(String parentAssetFilename, Array<AssetDescriptor<?>> dependendAssetDescs) {
		final ObjectSet<String> injected = this.injected;
		for (AssetDescriptor<?> desc : dependendAssetDescs) {
			if (injected.contains(desc.fileName))
				continue; // Ignore subsequent dependencies if there are duplicates.
			injected.add(desc.fileName);
			injectDependency(parentAssetFilename, desc);
		}
		injected.clear(32);
	}
	
	private synchronized void injectDependency(String parentAssetFilename, AssetDescriptor<?> dependendAssetDesc) {
		Array<String> dependencies = assetDependencies.get(parentAssetFilename);
		if (dependencies == null) {
			dependencies = new Array<>();
			assetDependencies.put(parentAssetFilename, dependencies);
		}
		dependencies.add(dependendAssetDesc.fileName);
		
		if (isLoaded(dependendAssetDesc.fileName)) {
			Micro.app.debug(TAG, "Dependency already loaded: " + dependendAssetDesc);
			Class<?> type = assetTypes.get(dependendAssetDesc.fileName);
			RefCountedContainer assetRef = assets.get(type).get(dependendAssetDesc.fileName);
			assetRef.refCount++;
			incrementRefCountedDependencies(dependendAssetDesc.fileName);
		} else {
			// else add a new task for the asset.
			Micro.app.log(TAG, "Loading dependency: " + dependendAssetDesc);
			addTask(dependendAssetDesc);
		}
	}
	
	private void nextTask() {
		AssetDescriptor<?> assetDesc = loadQueue.removeIndex(0);
		
		// if the asset not meant to be reloaded and is already loaded, increase its reference count
		if (isLoaded(assetDesc.fileName)) {
			Micro.app.debug(TAG, "Already loaded: " + assetDesc);
			Class<?> type = assetTypes.get(assetDesc.fileName);
			RefCountedContainer assetRef = assets.get(type).get(assetDesc.fileName);
			assetRef.refCount++;
			incrementRefCountedDependencies(assetDesc.fileName);
			if (assetDesc.params != null && assetDesc.params.loadedCallback != null)
				assetDesc.params.loadedCallback.finishedLoading(this, assetDesc.fileName, assetDesc.type);
			loaded++;
		} else {
			// else add a new task for the asset.
			Micro.app.log(TAG, "Loading: " + assetDesc);
			addTask(assetDesc);
		}
	}
	
	private void addTask(AssetDescriptor<?> assetDesc) {
		AssetLoader<?, ?> loader = getLoader(assetDesc.type, assetDesc.fileName);
		if (loader == null)
			throw new MicroRuntimeException("No loader for type: " + assetDesc.type.getSimpleName());
		tasks.add(new AssetLoadingTask(this, assetDesc, loader, executor));
		peakTasks++;
	}
	
	protected <T> void addAsset(final String fileName, Class<T> type, T asset) {
		assetTypes.put(fileName, type);
		
		ObjectMap<String, RefCountedContainer> typeToAssets = assets.get(type);
		if (typeToAssets == null) {
			typeToAssets = new ObjectMap<String, RefCountedContainer>();
			assets.put(type, typeToAssets);
		}
		final RefCountedContainer assetRef = new RefCountedContainer();
		assetRef.object = asset;
		typeToAssets.put(fileName, assetRef);
	}
	
	private boolean updateTask() {
		AssetLoadingTask task = tasks.peek();
		
		boolean complete = true;
		try {
			complete = task.cancel || task.update();
		} catch (RuntimeException ex) {
			task.cancel = true;
			taskFailed(task.assetDesc, ex);
		}
		
		// if the task has been cancelled or has finished loading
		if (complete) {
			// increase the number of loaded assets and pop the task from the stack
			if (tasks.size == 1) {
				loaded++;
				peakTasks = 0;
			}
			tasks.pop();
			
			if (task.cancel)
				return true;
			
			addAsset(task.assetDesc.fileName, task.assetDesc.type, task.asset);
			
			// otherwise, if a listener was found in the parameter invoke it
			if (task.assetDesc.params != null && task.assetDesc.params.loadedCallback != null)
				task.assetDesc.params.loadedCallback.finishedLoading(this, task.assetDesc.fileName, task.assetDesc.type);
			
			long endTime = System.nanoTime();
			Micro.app.debug(TAG, "Loaded: " + (endTime - task.startTime) / 1000000f + "ms " + task.assetDesc);
			
			return true;
		}
		return false;
	}
	
	protected void taskFailed(AssetDescriptor assetDesc, RuntimeException ex) {
		throw ex;
	}
	
	private void incrementRefCountedDependencies(String parent) {
		Array<String> dependencies = assetDependencies.get(parent);
		if (dependencies == null)
			return;
		
		for (String dependency : dependencies) {
			Class type = assetTypes.get(dependency);
			RefCountedContainer assetRef = assets.get(type).get(dependency);
			assetRef.refCount++;
			incrementRefCountedDependencies(dependency);
		}
	}
	
	private void handleTaskError(Throwable t) {
		Micro.app.error(TAG, "Error loading asset.", t);
		
		if (tasks.isEmpty())
			throw new MicroRuntimeException(t);
		
		// pop the faulty task from the stack
		AssetLoadingTask task = tasks.pop();
		AssetDescriptor<?> assetDesc = task.assetDesc;
		
		// remove all dependencies
		if (task.dependenciesLoaded && task.dependencies != null) {
			for (AssetDescriptor<?> desc : task.dependencies)
				unload(desc.fileName);
		}
		
		// clear the rest of the stack
		tasks.clear();
		
		// inform the listener that something bad happened
		if (listener != null)
			listener.error(assetDesc, t);
		else
			throw new MicroRuntimeException(t);
	}
	
	public synchronized <T, P extends AssetLoaderParameters<T>> void setLoader(Class<T> type, AssetLoader<T, P> loader) {
		setLoader(type, null, loader);
	}
	
	public synchronized <T, P extends AssetLoaderParameters<T>> void setLoader(Class<T> type, String suffix, AssetLoader<T, P> loader) {
		if (type == null)
			throw new IllegalArgumentException("type cannot be null.");
		if (loader == null)
			throw new IllegalArgumentException("loader cannot be null.");
		Micro.app.debug(TAG, "Loader set: " + type.getSimpleName() + " -> " + loader.getClass().getSimpleName());
		ObjectMap<String, AssetLoader<?, ?>> loaders = this.loaders.get(type);
		if (loaders == null)
			this.loaders.put(type, loaders = new ObjectMap<String, AssetLoader<?, ?>>());
		loaders.put(suffix == null ? "" : suffix, loader);
	}
	
	public synchronized int getLoadedAssets() {
		return assetTypes.size;
	}
	
	public synchronized int getQueuedAssets() {
		return loadQueue.size + tasks.size;
	}
	
	public synchronized float getProgress() {
		if (toLoad == 0)
			return 1;
		float fractionalLoaded = loaded;
		if (peakTasks > 0) {
			fractionalLoaded += ((peakTasks - tasks.size) / (float) peakTasks);
		}
		return Math.min(1, fractionalLoaded / toLoad);
	}
	
	public synchronized void setErrorListener(AssetErrorListener listener) {
		this.listener = listener;
	}
	
	@Override
	public void dispose() {
		Micro.app.debug(TAG, "Disposing.");
		clear();
		executor.dispose();
	}
	
	public void clear() {
		synchronized (this) {
			loadQueue.clear();
		}
		
		finishLoading();
		
		synchronized (this) {
			ObjectIntMap<String> dependencyCount = new ObjectIntMap<String>();
			while (assetTypes.size > 0) {
				// for each asset, figure out how often it was referenced
				dependencyCount.clear(51);
				Array<String> assets = assetTypes.keys().toArray();
				for (String asset : assets) {
					Array<String> dependencies = assetDependencies.get(asset);
					if (dependencies == null)
						continue;
					for (String dependency : dependencies)
						dependencyCount.getAndIncrement(dependency, 0, 1);
				}
				
				// only dispose of assets that are root assets (not referenced)
				for (String asset : assets)
					if (dependencyCount.get(asset, 0) == 0)
						unload(asset);
			}
			
			this.assets.clear(51);
			this.assetTypes.clear(51);
			this.assetDependencies.clear(51);
			this.loaded = 0;
			this.toLoad = 0;
			this.peakTasks = 0;
			this.loadQueue.clear();
			this.tasks.clear();
		}
	}
	
	public synchronized int getReferenceCount(String fileName) {
		Class<?> type = assetTypes.get(fileName);
		if (type == null)
			throw new MicroRuntimeException("Asset not loaded: " + fileName);
		return assets.get(type).get(fileName).refCount;
	}
	
	public synchronized void setReferenceCount(String fileName, int refCount) {
		Class<?> type = assetTypes.get(fileName);
		if (type == null)
			throw new MicroRuntimeException("Asset not loaded: " + fileName);
		assets.get(type).get(fileName).refCount = refCount;
	}
	
	public synchronized String getDiagnostics() {
		StringBuilder buffer = new StringBuilder(256);
		for (Entry<String, Class<?>> entry : assetTypes) {
			String fileName = entry.key;
			Class<?> type = entry.value;
			
			if (buffer.length() > 0)
				buffer.append('\n');
			buffer.append(fileName);
			buffer.append(", ");
			buffer.append(type.getSimpleName());
			buffer.append(", refs: ");
			buffer.append(assets.get(type).get(fileName).refCount);
			
			Array<String> dependencies = assetDependencies.get(fileName);
			if (dependencies != null) {
				buffer.append(", deps: [");
				for (String dep : dependencies) {
					buffer.append(dep);
					buffer.append(',');
				}
				buffer.append(']');
			}
		}
		return buffer.toString();
	}
	
	public synchronized Array<String> getAssetNames() {
		return assetTypes.keys().toArray();
	}
	
	public synchronized Array<String> getDependencies(String fileName) {
		return assetDependencies.get(fileName);
	}
	
	public synchronized Class<?> getAssetType(String fileName) {
		return assetTypes.get(fileName);
	}
	
	static class RefCountedContainer {
		
		Object object;
		int refCount = 1;
		
	}
	
}
