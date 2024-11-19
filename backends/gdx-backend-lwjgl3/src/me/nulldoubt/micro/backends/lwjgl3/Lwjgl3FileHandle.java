package me.nulldoubt.micro.backends.lwjgl3;

import me.nulldoubt.micro.Files.FileType;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.files.FileHandle;

import java.io.File;

public final class Lwjgl3FileHandle extends FileHandle {
	
	public Lwjgl3FileHandle(String fileName, FileType type) {
		super(fileName, type);
	}
	
	public Lwjgl3FileHandle(File file, FileType type) {
		super(file, type);
	}
	
	public File file() {
		if (type == FileType.External)
			return new File(Lwjgl3Files.externalPath, file.getPath());
		if (type == FileType.Local)
			return new File(Lwjgl3Files.localPath, file.getPath());
		return file;
	}
	
	public FileHandle child(String name) {
		if (file.getPath().isEmpty())
			return new Lwjgl3FileHandle(new File(name), type);
		return new Lwjgl3FileHandle(new File(file, name), type);
	}
	
	public FileHandle sibling(String name) {
		if (file.getPath().isEmpty())
			throw new MicroRuntimeException("Cannot get the sibling of the root.");
		return new Lwjgl3FileHandle(new File(file.getParent(), name), type);
	}
	
	public FileHandle parent() {
		File parent = file.getParentFile();
		if (parent == null) {
			if (type == FileType.Absolute)
				parent = new File("/");
			else
				parent = new File("");
		}
		return new Lwjgl3FileHandle(parent, type);
	}
	
}
