package me.nulldoubt.micro.assets.loaders;

import me.nulldoubt.micro.assets.AssetLoaderParameters;
import me.nulldoubt.micro.assets.AssetManager;
import me.nulldoubt.micro.files.FileHandle;

public abstract class AsynchronousAssetLoader<T, P extends AssetLoaderParameters<T>> extends AssetLoader<T, P> {
	
	public AsynchronousAssetLoader(FileHandleResolver resolver) {
		super(resolver);
	}
	
	public abstract void loadAsync(AssetManager manager, String fileName, FileHandle file, P parameter);
	
	public void unloadAsync(AssetManager manager, String fileName, FileHandle file, AssetLoaderParameters<P> parameter) {}
	
	public abstract T loadSync(AssetManager manager, String fileName, FileHandle file, P parameter);
	
}
