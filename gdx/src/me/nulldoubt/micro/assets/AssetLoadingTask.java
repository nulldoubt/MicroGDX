package me.nulldoubt.micro.assets;

import me.nulldoubt.micro.Application;
import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.assets.loaders.AssetLoader;
import me.nulldoubt.micro.assets.loaders.AsynchronousAssetLoader;
import me.nulldoubt.micro.assets.loaders.SynchronousAssetLoader;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.utils.collections.Array;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.utils.async.AsyncExecutor;
import me.nulldoubt.micro.utils.async.AsyncResult;
import me.nulldoubt.micro.utils.async.AsyncTask;

public class AssetLoadingTask implements AsyncTask<Void> {
	
	AssetManager manager;
	final AssetDescriptor assetDesc;
	final AssetLoader loader;
	final AsyncExecutor executor;
	final long startTime;
	
	volatile boolean asyncDone;
	volatile boolean dependenciesLoaded;
	volatile Array<AssetDescriptor<?>> dependencies;
	volatile AsyncResult<Void> depsFuture;
	volatile AsyncResult<Void> loadFuture;
	volatile Object asset;
	
	volatile boolean cancel;
	
	public AssetLoadingTask(AssetManager manager, AssetDescriptor assetDesc, AssetLoader loader, AsyncExecutor threadPool) {
		this.manager = manager;
		this.assetDesc = assetDesc;
		this.loader = loader;
		this.executor = threadPool;
		startTime = Micro.app.getLogLevel() == Application.LOG_DEBUG ? System.nanoTime() : 0;
	}
	
	@Override
	public Void call() throws Exception {
		if (cancel)
			return null;
		AsynchronousAssetLoader asyncLoader = (AsynchronousAssetLoader) loader;
		if (!dependenciesLoaded) {
			dependencies = asyncLoader.getDependencies(assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
			if (dependencies != null) {
				removeDuplicates(dependencies);
				manager.injectDependencies(assetDesc.fileName, dependencies);
			} else {
				// if we have no dependencies, we load the async part of the task immediately.
				asyncLoader.loadAsync(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
				asyncDone = true;
			}
		} else {
			asyncLoader.loadAsync(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
			asyncDone = true;
		}
		return null;
	}
	
	public boolean update() {
		if (loader instanceof SynchronousAssetLoader)
			handleSyncLoader();
		else
			handleAsyncLoader();
		return asset != null;
	}
	
	private void handleSyncLoader() {
		SynchronousAssetLoader syncLoader = (SynchronousAssetLoader) loader;
		if (!dependenciesLoaded) {
			dependenciesLoaded = true;
			dependencies = syncLoader.getDependencies(assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
			if (dependencies == null) {
				asset = syncLoader.load(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
				return;
			}
			removeDuplicates(dependencies);
			manager.injectDependencies(assetDesc.fileName, dependencies);
		} else
			asset = syncLoader.load(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
	}
	
	private void handleAsyncLoader() {
		AsynchronousAssetLoader asyncLoader = (AsynchronousAssetLoader) loader;
		if (!dependenciesLoaded) {
			if (depsFuture == null)
				depsFuture = executor.submit(this);
			else if (depsFuture.isDone()) {
				try {
					depsFuture.get();
				} catch (Exception e) {
					throw new MicroRuntimeException("Couldn't load dependencies of asset: " + assetDesc.fileName, e);
				}
				dependenciesLoaded = true;
				if (asyncDone)
					asset = asyncLoader.loadSync(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
			}
		} else if (loadFuture == null && !asyncDone)
			loadFuture = executor.submit(this);
		else if (asyncDone)
			asset = asyncLoader.loadSync(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
		else if (loadFuture.isDone()) {
			try {
				loadFuture.get();
			} catch (Exception e) {
				throw new MicroRuntimeException("Couldn't load asset: " + assetDesc.fileName, e);
			}
			asset = asyncLoader.loadSync(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
		}
	}
	
	public void unload() {
		if (loader instanceof AsynchronousAssetLoader<?, ?> asynchronousAssetLoader)
			asynchronousAssetLoader.unloadAsync(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
	}
	
	private FileHandle resolve(AssetLoader<?, ?> loader, AssetDescriptor<?> assetDesc) {
		if (assetDesc.file == null)
			assetDesc.file = loader.resolve(assetDesc.fileName);
		return assetDesc.file;
	}
	
	private void removeDuplicates(Array<AssetDescriptor<?>> array) {
		boolean ordered = array.ordered;
		array.ordered = true;
		for (int i = 0; i < array.size; ++i) {
			final String fn = array.get(i).fileName;
			final Class<?> type = array.get(i).type;
			for (int j = array.size - 1; j > i; --j)
				if (type == array.get(j).type && fn.equals(array.get(j).fileName))
					array.removeIndex(j);
		}
		array.ordered = ordered;
	}
	
}
