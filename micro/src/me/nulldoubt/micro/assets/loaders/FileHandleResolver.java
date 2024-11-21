package me.nulldoubt.micro.assets.loaders;

import me.nulldoubt.micro.files.FileHandle;

public interface FileHandleResolver {
	
	FileHandle resolve(final String fileName);
	
}
