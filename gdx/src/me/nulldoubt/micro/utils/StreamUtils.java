package me.nulldoubt.micro.utils;

import java.io.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;

public final class StreamUtils {
	
	public static final int DEFAULT_BUFFER_SIZE = 4096;
	
	public static void copyStream(InputStream input, OutputStream output) throws IOException {
		copyStream(input, output, new byte[DEFAULT_BUFFER_SIZE]);
	}
	
	public static void copyStream(InputStream input, OutputStream output, int bufferSize) throws IOException {
		copyStream(input, output, new byte[bufferSize]);
	}
	
	public static void copyStream(InputStream input, OutputStream output, byte[] buffer) throws IOException {
		int bytesRead;
		while ((bytesRead = input.read(buffer)) != -1) {
			output.write(buffer, 0, bytesRead);
		}
	}
	
	public static void copyStream(InputStream input, ByteBuffer output) throws IOException {
		copyStream(input, output, new byte[DEFAULT_BUFFER_SIZE]);
	}
	
	public static void copyStream(InputStream input, ByteBuffer output, int bufferSize) throws IOException {
		copyStream(input, output, new byte[bufferSize]);
	}
	
	public static int copyStream(InputStream input, ByteBuffer output, byte[] buffer) throws IOException {
		int startPosition = output.position(), total = 0, bytesRead;
		while ((bytesRead = input.read(buffer)) != -1) {
			BufferUtils.copy(buffer, 0, output, bytesRead);
			total += bytesRead;
			((Buffer) output).position(startPosition + total);
		}
		((Buffer) output).position(startPosition);
		return total;
	}
	
	public static byte[] copyStreamToByteArray(InputStream input) throws IOException {
		return copyStreamToByteArray(input, input.available());
	}
	
	public static byte[] copyStreamToByteArray(InputStream input, int estimatedSize) throws IOException {
		final ByteArrayOutputStream byteArrayOutputStream = new OptimizedByteArrayOutputStream(Math.max(0, estimatedSize));
		copyStream(input, byteArrayOutputStream);
		return byteArrayOutputStream.toByteArray();
	}
	
	public static String copyStreamToString(InputStream input) throws IOException {
		return copyStreamToString(input, input.available(), null);
	}
	
	public static String copyStreamToString(InputStream input, int estimatedSize) throws IOException {
		return copyStreamToString(input, estimatedSize, null);
	}
	
	public static String copyStreamToString(InputStream input, int estimatedSize, String charset) throws IOException {
		InputStreamReader reader = charset == null ? new InputStreamReader(input) : new InputStreamReader(input, charset);
		StringWriter writer = new StringWriter(Math.max(0, estimatedSize));
		char[] buffer = new char[DEFAULT_BUFFER_SIZE];
		int charsRead;
		while ((charsRead = reader.read(buffer)) != -1) {
			writer.write(buffer, 0, charsRead);
		}
		return writer.toString();
	}
	
	public static void closeQuietly(Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (Throwable ignored) {
			}
		}
	}
	
	public static class OptimizedByteArrayOutputStream extends ByteArrayOutputStream {
		
		public OptimizedByteArrayOutputStream(int initialSize) {
			super(initialSize);
		}
		
		@Override
		public synchronized byte[] toByteArray() {
			if (count == buf.length)
				return buf;
			return super.toByteArray();
		}
		
		public byte[] getBuffer() {
			return buf;
		}
		
	}
	
}
