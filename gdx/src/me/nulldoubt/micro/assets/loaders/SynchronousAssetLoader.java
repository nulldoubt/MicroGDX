package me.nulldoubt.micro.assets.loaders;

import me.nulldoubt.micro.assets.AssetLoaderParameters;
import me.nulldoubt.micro.assets.AssetManager;
import me.nulldoubt.micro.files.FileHandle;

public abstract class SynchronousAssetLoader<T, P extends AssetLoaderParameters<T>> extends AssetLoader<T, P> {
	
	public SynchronousAssetLoader(FileHandleResolver resolver) {
		super(resolver);
	}
	
	public abstract T load(AssetManager assetManager, String fileName, FileHandle file, P parameter);
	
}
