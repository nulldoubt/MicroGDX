package me.nulldoubt.micro.files;

import me.nulldoubt.micro.Files.FileType;
import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.utils.Streams;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

public class FileHandle {
	
	protected File file;
	protected FileType type;
	
	protected FileHandle() {}
	
	public FileHandle(String fileName) {
		this.file = new File(fileName);
		this.type = FileType.Absolute;
	}
	
	public FileHandle(File file) {
		this.file = file;
		this.type = FileType.Absolute;
	}
	
	protected FileHandle(String fileName, FileType type) {
		this.type = type;
		file = new File(fileName);
	}
	
	protected FileHandle(File file, FileType type) {
		this.file = file;
		this.type = type;
	}
	
	public String path() {
		return file.getPath().replace('\\', '/');
	}
	
	public String name() {
		return file.getName();
	}
	
	public String extension() {
		String name = file.getName();
		int dotIndex = name.lastIndexOf('.');
		if (dotIndex == -1)
			return "";
		return name.substring(dotIndex + 1);
	}
	
	public String nameWithoutExtension() {
		String name = file.getName();
		int dotIndex = name.lastIndexOf('.');
		if (dotIndex == -1)
			return name;
		return name.substring(0, dotIndex);
	}
	
	public String pathWithoutExtension() {
		String path = file.getPath().replace('\\', '/');
		int dotIndex = path.lastIndexOf('.');
		if (dotIndex == -1)
			return path;
		return path.substring(0, dotIndex);
	}
	
	public FileType type() {
		return type;
	}
	
	public File file() {
		if (type == FileType.External)
			return new File(Micro.files.getExternalStoragePath(), file.getPath());
		return file;
	}
	
	public InputStream read() {
		if (type == FileType.Classpath || (type == FileType.Internal && !file().exists()) || (type == FileType.Local && !file().exists())) {
			InputStream input = FileHandle.class.getResourceAsStream("/" + file.getPath().replace('\\', '/'));
			if (input == null)
				throw new MicroRuntimeException("File not found: " + file + " (" + type + ")");
			return input;
		}
		try {
			return new FileInputStream(file());
		} catch (Exception ex) {
			if (file().isDirectory())
				throw new MicroRuntimeException("Cannot open a stream to a directory: " + file + " (" + type + ")", ex);
			throw new MicroRuntimeException("Error reading file: " + file + " (" + type + ")", ex);
		}
	}
	
	public BufferedInputStream read(int bufferSize) {
		return new BufferedInputStream(read(), bufferSize);
	}
	
	public Reader reader() {
		return new InputStreamReader(read());
	}
	
	public Reader reader(String charset) {
		InputStream stream = read();
		try {
			return new InputStreamReader(stream, charset);
		} catch (UnsupportedEncodingException ex) {
			Streams.closeQuietly(stream);
			throw new MicroRuntimeException("Error reading file: " + this, ex);
		}
	}
	
	public BufferedReader reader(int bufferSize) {
		return new BufferedReader(new InputStreamReader(read()), bufferSize);
	}
	
	public BufferedReader reader(int bufferSize, String charset) {
		try {
			return new BufferedReader(new InputStreamReader(read(), charset), bufferSize);
		} catch (UnsupportedEncodingException ex) {
			throw new MicroRuntimeException("Error reading file: " + this, ex);
		}
	}
	
	public String readString() {
		return readString(null);
	}
	
	public String readString(String charset) {
		StringBuilder output = new StringBuilder(estimateLength());
		InputStreamReader reader = null;
		try {
			if (charset == null)
				reader = new InputStreamReader(read());
			else
				reader = new InputStreamReader(read(), charset);
			char[] buffer = new char[256];
			while (true) {
				int length = reader.read(buffer);
				if (length == -1)
					break;
				output.append(buffer, 0, length);
			}
		} catch (IOException ex) {
			throw new MicroRuntimeException("Error reading layout file: " + this, ex);
		} finally {
			Streams.closeQuietly(reader);
		}
		return output.toString();
	}
	
	public byte[] readBytes() {
		InputStream input = read();
		try {
			return Streams.copyStreamToByteArray(input, estimateLength());
		} catch (IOException ex) {
			throw new MicroRuntimeException("Error reading file: " + this, ex);
		} finally {
			Streams.closeQuietly(input);
		}
	}
	
	private int estimateLength() {
		int length = (int) length();
		return length != 0 ? length : 512;
	}
	
	public int readBytes(byte[] bytes, int offset, int size) {
		InputStream input = read();
		int position = 0;
		try {
			while (true) {
				int count = input.read(bytes, offset + position, size - position);
				if (count <= 0)
					break;
				position += count;
			}
		} catch (IOException ex) {
			throw new MicroRuntimeException("Error reading file: " + this, ex);
		} finally {
			Streams.closeQuietly(input);
		}
		return position - offset;
	}
	
	public ByteBuffer map() {
		return map(MapMode.READ_ONLY);
	}
	
	public ByteBuffer map(FileChannel.MapMode mode) {
		if (type == FileType.Classpath)
			throw new MicroRuntimeException("Cannot map a classpath file: " + this);
		RandomAccessFile raf = null;
		try {
			File f = file();
			raf = new RandomAccessFile(f, mode == MapMode.READ_ONLY ? "r" : "rw");
			FileChannel fileChannel = raf.getChannel();
			ByteBuffer map = fileChannel.map(mode, 0, f.length());
			map.order(ByteOrder.nativeOrder());
			return map;
		} catch (Exception ex) {
			throw new MicroRuntimeException("Error memory mapping file: " + this + " (" + type + ")", ex);
		} finally {
			Streams.closeQuietly(raf);
		}
	}
	
	public OutputStream write(boolean append) {
		if (type == FileType.Classpath)
			throw new MicroRuntimeException("Cannot write to a classpath file: " + file);
		if (type == FileType.Internal)
			throw new MicroRuntimeException("Cannot write to an internal file: " + file);
		parent().mkdirs();
		try {
			return new FileOutputStream(file(), append);
		} catch (Exception ex) {
			if (file().isDirectory())
				throw new MicroRuntimeException("Cannot open a stream to a directory: " + file + " (" + type + ")", ex);
			throw new MicroRuntimeException("Error writing file: " + file + " (" + type + ")", ex);
		}
	}
	
	public OutputStream write(boolean append, int bufferSize) {
		return new BufferedOutputStream(write(append), bufferSize);
	}
	
	public void write(InputStream input, boolean append) {
		OutputStream output = null;
		try {
			output = write(append);
			Streams.copyStream(input, output);
		} catch (Exception ex) {
			throw new MicroRuntimeException("Error stream writing to file: " + file + " (" + type + ")", ex);
		} finally {
			Streams.closeQuietly(input);
			Streams.closeQuietly(output);
		}
		
	}
	
	public Writer writer(boolean append) {
		return writer(append, null);
	}
	
	public Writer writer(boolean append, String charset) {
		if (type == FileType.Classpath)
			throw new MicroRuntimeException("Cannot write to a classpath file: " + file);
		if (type == FileType.Internal)
			throw new MicroRuntimeException("Cannot write to an internal file: " + file);
		parent().mkdirs();
		try {
			FileOutputStream output = new FileOutputStream(file(), append);
			if (charset == null)
				return new OutputStreamWriter(output);
			else
				return new OutputStreamWriter(output, charset);
		} catch (IOException ex) {
			if (file().isDirectory())
				throw new MicroRuntimeException("Cannot open a stream to a directory: " + file + " (" + type + ")", ex);
			throw new MicroRuntimeException("Error writing file: " + file + " (" + type + ")", ex);
		}
	}
	
	public void writeString(String string, boolean append) {
		writeString(string, append, null);
	}
	
	public void writeString(String string, boolean append, String charset) {
		Writer writer = null;
		try {
			writer = writer(append, charset);
			writer.write(string);
		} catch (Exception ex) {
			throw new MicroRuntimeException("Error writing file: " + file + " (" + type + ")", ex);
		} finally {
			Streams.closeQuietly(writer);
		}
	}
	
	public void writeBytes(byte[] bytes, boolean append) {
		OutputStream output = write(append);
		try {
			output.write(bytes);
		} catch (IOException ex) {
			throw new MicroRuntimeException("Error writing file: " + file + " (" + type + ")", ex);
		} finally {
			Streams.closeQuietly(output);
		}
	}
	
	public void writeBytes(byte[] bytes, int offset, int length, boolean append) {
		OutputStream output = write(append);
		try {
			output.write(bytes, offset, length);
		} catch (IOException ex) {
			throw new MicroRuntimeException("Error writing file: " + file + " (" + type + ")", ex);
		} finally {
			Streams.closeQuietly(output);
		}
	}
	
	public FileHandle[] list() {
		if (type == FileType.Classpath)
			throw new MicroRuntimeException("Cannot list a classpath directory: " + file);
		String[] relativePaths = file().list();
		if (relativePaths == null)
			return new FileHandle[0];
		FileHandle[] handles = new FileHandle[relativePaths.length];
		for (int i = 0, n = relativePaths.length; i < n; i++)
			handles[i] = child(relativePaths[i]);
		return handles;
	}
	
	public FileHandle[] list(FileFilter filter) {
		if (type == FileType.Classpath)
			throw new MicroRuntimeException("Cannot list a classpath directory: " + file);
		File file = file();
		String[] relativePaths = file.list();
		if (relativePaths == null)
			return new FileHandle[0];
		FileHandle[] handles = new FileHandle[relativePaths.length];
		int count = 0;
		for (String path : relativePaths) {
			FileHandle child = child(path);
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
	}
	
	public FileHandle[] list(FilenameFilter filter) {
		if (type == FileType.Classpath)
			throw new MicroRuntimeException("Cannot list a classpath directory: " + file);
		File file = file();
		String[] relativePaths = file.list();
		if (relativePaths == null)
			return new FileHandle[0];
		FileHandle[] handles = new FileHandle[relativePaths.length];
		int count = 0;
		for (String path : relativePaths) {
			if (!filter.accept(file, path))
				continue;
			handles[count] = child(path);
			count++;
		}
		if (count < relativePaths.length) {
			FileHandle[] newHandles = new FileHandle[count];
			System.arraycopy(handles, 0, newHandles, 0, count);
			handles = newHandles;
		}
		return handles;
	}
	
	public FileHandle[] list(String suffix) {
		if (type == FileType.Classpath)
			throw new MicroRuntimeException("Cannot list a classpath directory: " + file);
		String[] relativePaths = file().list();
		if (relativePaths == null)
			return new FileHandle[0];
		FileHandle[] handles = new FileHandle[relativePaths.length];
		int count = 0;
		for (String path : relativePaths) {
			if (!path.endsWith(suffix))
				continue;
			handles[count] = child(path);
			count++;
		}
		if (count < relativePaths.length) {
			FileHandle[] newHandles = new FileHandle[count];
			System.arraycopy(handles, 0, newHandles, 0, count);
			handles = newHandles;
		}
		return handles;
	}
	
	public boolean isDirectory() {
		if (type == FileType.Classpath)
			return false;
		return file().isDirectory();
	}
	
	public FileHandle child(String name) {
		if (file.getPath().isEmpty())
			return new FileHandle(new File(name), type);
		return new FileHandle(new File(file, name), type);
	}
	
	public FileHandle sibling(String name) {
		if (file.getPath().isEmpty())
			throw new MicroRuntimeException("Cannot get the sibling of the root.");
		return new FileHandle(new File(file.getParent(), name), type);
	}
	
	public FileHandle parent() {
		File parent = file.getParentFile();
		if (parent == null) {
			if (type == FileType.Absolute)
				parent = new File("/");
			else
				parent = new File("");
		}
		return new FileHandle(parent, type);
	}
	
	public boolean mkdirs() {
		if (type == FileType.Classpath)
			throw new MicroRuntimeException("Cannot mkdirs with a classpath file: " + file);
		if (type == FileType.Internal)
			throw new MicroRuntimeException("Cannot mkdirs with an internal file: " + file);
		return file().mkdirs();
	}
	
	public boolean exists() {
		switch (type) {
			case Internal:
				if (file().exists())
					return true;
			case Classpath:
				return FileHandle.class.getResource("/" + file.getPath().replace('\\', '/')) != null;
		}
		return file().exists();
	}
	
	public boolean delete() {
		if (type == FileType.Classpath)
			throw new MicroRuntimeException("Cannot delete a classpath file: " + file);
		if (type == FileType.Internal)
			throw new MicroRuntimeException("Cannot delete an internal file: " + file);
		return file().delete();
	}
	
	public boolean deleteDirectory() {
		if (type == FileType.Classpath)
			throw new MicroRuntimeException("Cannot delete a classpath file: " + file);
		if (type == FileType.Internal)
			throw new MicroRuntimeException("Cannot delete an internal file: " + file);
		return deleteDirectory(file());
	}
	
	public void emptyDirectory() {
		emptyDirectory(false);
	}
	
	public void emptyDirectory(boolean preserveTree) {
		if (type == FileType.Classpath)
			throw new MicroRuntimeException("Cannot delete a classpath file: " + file);
		if (type == FileType.Internal)
			throw new MicroRuntimeException("Cannot delete an internal file: " + file);
		emptyDirectory(file(), preserveTree);
	}
	
	public void copyTo(FileHandle dest) {
		if (!isDirectory()) {
			if (dest.isDirectory())
				dest = dest.child(name());
			copyFile(this, dest);
			return;
		}
		if (dest.exists()) {
			if (!dest.isDirectory())
				throw new MicroRuntimeException("Destination exists but is not a directory: " + dest);
		} else {
			dest.mkdirs();
			if (!dest.isDirectory())
				throw new MicroRuntimeException("Destination directory cannot be created: " + dest);
		}
		copyDirectory(this, dest.child(name()));
	}
	
	public void moveTo(FileHandle dest) {
		switch (type) {
			case Classpath:
				throw new MicroRuntimeException("Cannot move a classpath file: " + file);
			case Internal:
				throw new MicroRuntimeException("Cannot move an internal file: " + file);
			case Absolute:
			case External:
				if (file().renameTo(dest.file()))
					return;
		}
		copyTo(dest);
		delete();
		if (exists() && isDirectory())
			deleteDirectory();
	}
	
	public long length() {
		if (type == FileType.Classpath || (type == FileType.Internal && !file.exists())) {
			InputStream input = read();
			try {
				return input.available();
			} catch (Exception ignored) {
			} finally {
				Streams.closeQuietly(input);
			}
			return 0;
		}
		return file().length();
	}
	
	public long lastModified() {
		return file().lastModified();
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof FileHandle other))
			return false;
		return type == other.type && path().equals(other.path());
	}
	
	public int hashCode() {
		int hash = 1;
		hash = hash * 37 + type.hashCode();
		hash = hash * 67 + path().hashCode();
		return hash;
	}
	
	public String toString() {
		return file.getPath().replace('\\', '/');
	}
	
	public static FileHandle tempFile(String prefix) {
		try {
			return new FileHandle(File.createTempFile(prefix, null));
		} catch (IOException ex) {
			throw new MicroRuntimeException("Unable to create temp file.", ex);
		}
	}
	
	public static FileHandle tempDirectory(String prefix) {
		try {
			File file = File.createTempFile(prefix, null);
			if (!file.delete())
				throw new IOException("Unable to delete temp file: " + file);
			if (!file.mkdir())
				throw new IOException("Unable to create temp directory: " + file);
			return new FileHandle(file);
		} catch (IOException ex) {
			throw new MicroRuntimeException("Unable to create temp file.", ex);
		}
	}
	
	private static void emptyDirectory(File file, boolean preserveTree) {
		if (file.exists()) {
			File[] files = file.listFiles();
			if (files != null) {
				for (File value : files) {
					if (!value.isDirectory())
						value.delete();
					else if (preserveTree)
						emptyDirectory(value, true);
					else
						deleteDirectory(value);
				}
			}
		}
	}
	
	private static boolean deleteDirectory(File file) {
		emptyDirectory(file, false);
		return file.delete();
	}
	
	private static void copyFile(FileHandle source, FileHandle dest) {
		try {
			dest.write(source.read(), false);
		} catch (Exception ex) {
			throw new MicroRuntimeException("Error copying source file: " + source.file + " (" + source.type + ")\n" + "To destination: " + dest.file + " (" + dest.type + ")", ex);
		}
	}
	
	private static void copyDirectory(FileHandle sourceDir, FileHandle destDir) {
		destDir.mkdirs();
		FileHandle[] files = sourceDir.list();
		for (FileHandle srcFile : files) {
			FileHandle destFile = destDir.child(srcFile.name());
			if (srcFile.isDirectory())
				copyDirectory(srcFile, destFile);
			else
				copyFile(srcFile, destFile);
		}
	}
	
}
