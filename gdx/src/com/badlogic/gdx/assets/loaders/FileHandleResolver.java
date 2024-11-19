package com.badlogic.gdx.assets.loaders;

import com.badlogic.gdx.files.FileHandle;

public interface FileHandleResolver {
	
	FileHandle resolve(final String fileName);
	
}
