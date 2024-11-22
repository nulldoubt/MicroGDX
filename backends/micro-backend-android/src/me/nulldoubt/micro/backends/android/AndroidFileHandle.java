package me.nulldoubt.micro.backends.android;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import me.nulldoubt.micro.Files.FileType;
import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.utils.Streams;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class AndroidFileHandle extends FileHandle {
	
	private final AssetManager assets;
	
	AndroidFileHandle(AssetManager assets, String fileName, FileType type) {
		super(fileName.replace('\\', '/'), type);
		this.assets = assets;
	}
	
	AndroidFileHandle(AssetManager assets, File file, FileType type) {
		super(file, type);
		this.assets = assets;
	}
	
	public FileHandle child(String name) {
		name = name.replace('\\', '/');
		if (file.getPath().isEmpty())
			return new AndroidFileHandle(assets, new File(name), type);
		return new AndroidFileHandle(assets, new File(file, name), type);
	}
	
	public FileHandle sibling(String name) {
		name = name.replace('\\', '/');
		if (file.getPath().isEmpty())
			throw new MicroRuntimeException("Cannot get the sibling of the root.");
		return Micro.files.getFileHandle(new File(file.getParent(), name).getPath(), type);
	}
	
	public FileHandle parent() {
		File parent = file.getParentFile();
		if (parent == null) {
			if (type == FileType.Absolute)
				parent = new File("/");
			else
				parent = new File("");
		}
		return new AndroidFileHandle(assets, parent, type);
	}
	
	public InputStream read() {
		if (type == FileType.Internal) {
			try {
				return assets.open(file.getPath());
			} catch (IOException ex) {
				throw new MicroRuntimeException("Error reading file: " + file + " (" + type + ")", ex);
			}
		}
		return super.read();
	}
	
	public ByteBuffer map(FileChannel.MapMode mode) {
		if (type == FileType.Internal) {
			FileInputStream input = null;
			try {
				AssetFileDescriptor fd = getAssetFileDescriptor();
				long startOffset = fd.getStartOffset();
				long declaredLength = fd.getDeclaredLength();
				input = new FileInputStream(fd.getFileDescriptor());
				ByteBuffer map = input.getChannel().map(mode, startOffset, declaredLength);
				map.order(ByteOrder.nativeOrder());
				return map;
			} catch (Exception ex) {
				throw new MicroRuntimeException("Error memory mapping file: " + this + " (" + type + ")", ex);
			} finally {
				Streams.closeQuietly(input);
			}
		}
		return super.map(mode);
	}
	
	public FileHandle[] list() {
		if (type == FileType.Internal) {
			try {
				String[] relativePaths = assets.list(file.getPath());
				FileHandle[] handles = new FileHandle[relativePaths.length];
				for (int i = 0, n = handles.length; i < n; i++)
					handles[i] = new AndroidFileHandle(assets, new File(file, relativePaths[i]), type);
				return handles;
			} catch (Exception ex) {
				throw new MicroRuntimeException("Error listing children: " + file + " (" + type + ")", ex);
			}
		}
		return super.list();
	}
	
	public FileHandle[] list(FileFilter filter) {
		if (type == FileType.Internal) {
			try {
				String[] relativePaths = assets.list(file.getPath());
				FileHandle[] handles = new FileHandle[relativePaths.length];
				int count = 0;
				for (int i = 0, n = handles.length; i < n; i++) {
					String path = relativePaths[i];
					FileHandle child = new AndroidFileHandle(assets, new File(file, path), type);
					if (!filter.accept(child.file()))
						continue;
					handles[count] = child;
					count++;
				}
				if (count < relativePaths.length) {
					FileHandle[] newHandles = new FileHandle[count];
					System.arraycopy(handles, 0, newHandles, 0, count);
					handles = newHandles;
				}
				return handles;
			} catch (Exception ex) {
				throw new MicroRuntimeException("Error listing children: " + file + " (" + type + ")", ex);
			}
		}
		return super.list(filter);
	}
	
	public FileHandle[] list(FilenameFilter filter) {
		if (type == FileType.Internal) {
			try {
				String[] relativePaths = assets.list(file.getPath());
				FileHandle[] handles = new FileHandle[relativePaths.length];
				int count = 0;
				for (int i = 0, n = handles.length; i < n; i++) {
					String path = relativePaths[i];
					if (!filter.accept(file, path))
						continue;
					handles[count] = new AndroidFileHandle(assets, new File(file, path), type);
					count++;
				}
				if (count < relativePaths.length) {
					FileHandle[] newHandles = new FileHandle[count];
					System.arraycopy(handles, 0, newHandles, 0, count);
					handles = newHandles;
				}
				return handles;
			} catch (Exception ex) {
				throw new MicroRuntimeException("Error listing children: " + file + " (" + type + ")", ex);
			}
		}
		return super.list(filter);
	}
	
	public FileHandle[] list(String suffix) {
		if (type == FileType.Internal) {
			try {
				String[] relativePaths = assets.list(file.getPath());
				FileHandle[] handles = new FileHandle[relativePaths.length];
				int count = 0;
				for (int i = 0, n = handles.length; i < n; i++) {
					String path = relativePaths[i];
					if (!path.endsWith(suffix))
						continue;
					handles[count] = new AndroidFileHandle(assets, new File(file, path), type);
					count++;
				}
				if (count < relativePaths.length) {
					FileHandle[] newHandles = new FileHandle[count];
					System.arraycopy(handles, 0, newHandles, 0, count);
					handles = newHandles;
				}
				return handles;
			} catch (Exception ex) {
				throw new MicroRuntimeException("Error listing children: " + file + " (" + type + ")", ex);
			}
		}
		return super.list(suffix);
	}
	
	public boolean isDirectory() {
		if (type == FileType.Internal) {
			try {
				return assets.list(file.getPath()).length > 0;
			} catch (IOException ex) {
				return false;
			}
		}
		return super.isDirectory();
	}
	
	public boolean exists() {
		if (type == FileType.Internal) {
			String fileName = file.getPath();
			try {
				assets.open(fileName).close(); // Check if file exists.
				return true;
			} catch (Exception ex) {
				// This is SUPER slow! but we need it for directories.
				try {
					return assets.list(fileName).length > 0;
				} catch (Exception _) {}
				return false;
			}
		}
		return super.exists();
	}
	
	public long length() {
		if (type == FileType.Internal) {
			try (AssetFileDescriptor fileDescriptor = assets.openFd(file.getPath())) {
				return fileDescriptor.getLength();
			} catch (IOException _) {}
		}
		return super.length();
	}
	
	public long lastModified() {
		return super.lastModified();
	}
	
	public File file() {
		if (type == FileType.Local)
			return new File(Micro.files.getLocalStoragePath(), file.getPath());
		return super.file();
	}
	
	public AssetFileDescriptor getAssetFileDescriptor() throws IOException {
		return assets != null ? assets.openFd(path()) : null;
	}
	
}
