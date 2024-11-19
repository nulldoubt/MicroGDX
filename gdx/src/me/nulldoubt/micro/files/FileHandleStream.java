package me.nulldoubt.micro.files;

import me.nulldoubt.micro.Files.FileType;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class FileHandleStream extends FileHandle {
	
	public FileHandleStream(final String path) {
		super(new File(path), FileType.Absolute);
	}
	
	public boolean isDirectory() {
		return false;
	}
	
	public long length() {
		return 0;
	}
	
	public boolean exists() {
		return true;
	}
	
	public FileHandle child(final String name) {
		throw new UnsupportedOperationException();
	}
	
	public FileHandle sibling(final String name) {
		throw new UnsupportedOperationException();
	}
	
	public FileHandle parent() {
		throw new UnsupportedOperationException();
	}
	
	public InputStream read() {
		throw new UnsupportedOperationException();
	}
	
	public OutputStream write(boolean overwrite) {
		throw new UnsupportedOperationException();
	}
	
	public FileHandle[] list() {
		throw new UnsupportedOperationException();
	}
	
	public void mkdirs() {
		throw new UnsupportedOperationException();
	}
	
	public boolean delete() {
		throw new UnsupportedOperationException();
	}
	
	public boolean deleteDirectory() {
		throw new UnsupportedOperationException();
	}
	
	public void copyTo(final FileHandle dest) {
		throw new UnsupportedOperationException();
	}
	
	public void moveTo(final FileHandle dest) {
		throw new UnsupportedOperationException();
	}
	
	public void emptyDirectory() {
		throw new UnsupportedOperationException();
	}
	
	public void emptyDirectory(final boolean preserveTree) {
		throw new UnsupportedOperationException();
	}
	
}
