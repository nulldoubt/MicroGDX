package me.nulldoubt.micro;

import me.nulldoubt.micro.files.FileHandle;

public interface Files {
	
	enum FileType {
		Classpath, Internal, External, Absolute, Local;
	}
	
	FileHandle getFileHandle(final String path, final FileType type);
	
	FileHandle classpath(final String path);
	
	FileHandle internal(final String path);
	
	FileHandle external(final String path);
	
	FileHandle absolute(final String path);
	
	FileHandle local(final String path);
	
	String getExternalStoragePath();
	
	boolean isExternalStorageAvailable();
	
	String getLocalStoragePath();
	
	boolean isLocalStorageAvailable();
	
}
