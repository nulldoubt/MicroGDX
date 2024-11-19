package me.nulldoubt.micro.assets.loaders;

import me.nulldoubt.micro.assets.AssetDescriptor;
import me.nulldoubt.micro.assets.AssetLoaderParameters;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.utils.collections.Array;

public abstract class AssetLoader<T, P extends AssetLoaderParameters<T>> {
	
	private final FileHandleResolver resolver;
	
	public AssetLoader(FileHandleResolver resolver) {
		this.resolver = resolver;
	}
	
	public FileHandle resolve(String fileName) {
		return resolver.resolve(fileName);
	}
	
	public abstract Array<AssetDescriptor<?>> getDependencies(String fileName, FileHandle file, P parameter);
	
}
