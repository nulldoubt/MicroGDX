package me.nulldoubt.micro.backends.lwjgl3.angle;

import me.nulldoubt.micro.exceptions.MicroRuntimeException;

import java.io.*;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.UUID;
import java.util.zip.CRC32;

public class ANGLELoader {
	
	public static boolean isWindows = System.getProperty("os.name").contains("Windows");
	public static boolean isLinux = System.getProperty("os.name").contains("Linux") || System.getProperty("os.name").contains("FreeBSD");
	public static boolean isMac = System.getProperty("os.name").contains("Mac");
	public static boolean isARM = System.getProperty("os.arch").startsWith("arm") || System.getProperty("os.arch").startsWith("aarch64");
	public static boolean is64Bit = System.getProperty("os.arch").contains("64") || System.getProperty("os.arch").startsWith("armv8");
	
	private static final Random random = new Random();
	private static File egl;
	private static File gles;
	private static File lastWorkingDir;
	
	public static void closeQuietly(Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (Throwable _) {}
		}
	}
	
	static String randomUUID() {
		return new UUID(random.nextLong(), random.nextLong()).toString();
	}
	
	public static String crc(InputStream input) {
		if (input == null)
			throw new IllegalArgumentException("input cannot be null.");
		CRC32 crc = new CRC32();
		byte[] buffer = new byte[4096];
		try {
			while (true) {
				int length = input.read(buffer);
				if (length == -1)
					break;
				crc.update(buffer, 0, length);
			}
		} catch (Exception _) {
		} finally {
			closeQuietly(input);
		}
		return Long.toString(crc.getValue(), 16);
	}
	
	private static File extractFile(String sourcePath, File outFile) {
		try {
			if (!outFile.getParentFile().exists() && !outFile.getParentFile().mkdirs())
				throw new MicroRuntimeException("Couldn't create ANGLE native library output directory " + outFile.getParentFile().getAbsolutePath());
			OutputStream out = null;
			InputStream in = null;
			
			if (outFile.exists())
				return outFile;
			
			try {
				out = new FileOutputStream(outFile);
				in = ANGLELoader.class.getResourceAsStream("/" + sourcePath);
				byte[] buffer = new byte[4096];
				while (true) {
					int length = in.read(buffer);
					if (length == -1)
						break;
					out.write(buffer, 0, length);
				}
				return outFile;
			} finally {
				closeQuietly(out);
				closeQuietly(in);
			}
		} catch (Throwable t) {
			throw new MicroRuntimeException("Couldn't load ANGLE shared library " + sourcePath, t);
		}
	}
	
	private static File getExtractedFile(String dirName, String fileName) {
		File idealFile = new File(System.getProperty("java.io.tmpdir") + "/libgdx" + System.getProperty("user.name") + "/" + dirName, fileName);
		if (canWrite(idealFile))
			return idealFile;
		
		try {
			File file = File.createTempFile(dirName, null);
			if (file.delete()) {
				file = new File(file, fileName);
				if (canWrite(file))
					return file;
			}
		} catch (IOException _) {}
		
		// User home.
		File file = new File(System.getProperty("user.home") + "/.libgdx/" + dirName, fileName);
		if (canWrite(file))
			return file;
		
		// Relative directory.
		file = new File(".temp/" + dirName, fileName);
		if (canWrite(file))
			return file;
		
		// We are running in the OS X sandbox.
		if (System.getenv("APP_SANDBOX_CONTAINER_ID") != null)
			return idealFile;
		
		return null;
	}
	
	private static boolean canWrite(File file) {
		File parent = file.getParentFile();
		File testFile;
		if (file.exists()) {
			if (!file.canWrite() || !canExecute(file))
				return false;
			testFile = new File(parent, randomUUID().toString());
		} else {
			parent.mkdirs();
			if (!parent.isDirectory())
				return false;
			testFile = file;
		}
		try {
			new FileOutputStream(testFile).close();
			return canExecute(testFile);
		} catch (Throwable ex) {
			return false;
		} finally {
			testFile.delete();
		}
	}
	
	private static boolean canExecute(File file) {
		try {
			Method canExecute = File.class.getMethod("canExecute");
			if ((Boolean) canExecute.invoke(file))
				return true;
			
			Method setExecutable = File.class.getMethod("setExecutable", boolean.class, boolean.class);
			setExecutable.invoke(file, true, false);
			
			return (Boolean) canExecute.invoke(file);
		} catch (Exception _) {}
		return false;
	}
	
	public static void load() {
		if ((isARM && !isMac) || (!isWindows && !isLinux && !isMac))
			throw new MicroRuntimeException("ANGLE is only supported on x86/x86_64 Windows, x64 Linux, and x64/arm64 macOS.");
		String osDir = null;
		String ext = null;
		if (isWindows) {
			osDir = is64Bit ? "windows64" : "windows32";
			ext = ".dll";
		}
		if (isLinux) {
			osDir = "linux64";
			ext = ".so";
		}
		if (isMac) {
			osDir = isARM ? "macosxarm64" : "macosx64";
			ext = ".dylib";
		}
		
		String eglSource = osDir + "/libEGL" + ext;
		String glesSource = osDir + "/libGLESv2" + ext;
		String crc = crc(ANGLELoader.class.getResourceAsStream("/" + eglSource)) + crc(ANGLELoader.class.getResourceAsStream("/" + glesSource));
		egl = getExtractedFile(crc, new File(eglSource).getName());
		gles = getExtractedFile(crc, new File(glesSource).getName());
		
		if (!isMac) {
			extractFile(eglSource, egl);
			System.load(egl.getAbsolutePath());
			extractFile(glesSource, gles);
			System.load(gles.getAbsolutePath());
		} else {
			lastWorkingDir = new File(".");
			extractFile(eglSource, new File(lastWorkingDir, egl.getName()));
			extractFile(glesSource, new File(lastWorkingDir, gles.getName()));
		}
	}
	
	public static void postGlfwInit() {
		new File(lastWorkingDir, egl.getName()).delete();
		new File(lastWorkingDir, gles.getName()).delete();
	}
	
}
