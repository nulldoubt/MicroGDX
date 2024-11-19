package com.badlogic.gdx.graphics;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.utils.ByteArray;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class PixmapIO {
	
	public static void writePNG(final FileHandle file, final Pixmap pixmap, final int compression, final boolean flipY) {
		try {
			PNG writer = new PNG((int) (pixmap.getWidth() * pixmap.getHeight() * 1.5f)); // Guess at deflated size.
			try {
				writer.setFlipY(flipY);
				writer.setCompression(compression);
				writer.write(file, pixmap);
			} finally {
				writer.dispose();
			}
		} catch (IOException ex) {
			throw new GdxRuntimeException("Error writing PNG: " + file, ex);
		}
	}
	
	public static void writePNG(final FileHandle file, final Pixmap pixmap) {
		writePNG(file, pixmap, Deflater.DEFAULT_COMPRESSION, false);
	}
	
	public static class PNG implements Disposable {
		
		private static final byte[] SIGNATURE = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
		private static final int IHDR = 0x49484452, IDAT = 0x49444154, IEND = 0x49454E44;
		private static final byte COLOR_ARGB = 6;
		private static final byte COMPRESSION_DEFLATE = 0;
		private static final byte FILTER_NONE = 0;
		private static final byte INTERLACE_NONE = 0;
		private static final byte PAETH = 4;
		
		private final ChunkBuffer buffer;
		private final Deflater deflater;
		private ByteArray lineOutBytes, curLineBytes, prevLineBytes;
		private boolean flipY = true;
		private int lastLineLen;
		
		public PNG() {
			this(128 * 128);
		}
		
		public PNG(int initialBufferSize) {
			buffer = new ChunkBuffer(initialBufferSize);
			deflater = new Deflater();
		}
		
		public void setFlipY(boolean flipY) {
			this.flipY = flipY;
		}
		
		public void setCompression(int level) {
			deflater.setLevel(level);
		}
		
		public void write(FileHandle file, Pixmap pixmap) throws IOException {
			OutputStream output = file.write(false);
			try {
				write(output, pixmap);
			} finally {
				StreamUtils.closeQuietly(output);
			}
		}
		
		public void write(OutputStream output, Pixmap pixmap) throws IOException {
			DeflaterOutputStream deflaterOutput = new DeflaterOutputStream(buffer, deflater);
			DataOutputStream dataOutput = new DataOutputStream(output);
			dataOutput.write(SIGNATURE);
			
			buffer.writeInt(IHDR);
			buffer.writeInt(pixmap.getWidth());
			buffer.writeInt(pixmap.getHeight());
			buffer.writeByte(8); // 8 bits per component.
			buffer.writeByte(COLOR_ARGB);
			buffer.writeByte(COMPRESSION_DEFLATE);
			buffer.writeByte(FILTER_NONE);
			buffer.writeByte(INTERLACE_NONE);
			buffer.endChunk(dataOutput);
			
			buffer.writeInt(IDAT);
			deflater.reset();
			
			int lineLen = pixmap.getWidth() * 4;
			byte[] lineOut, curLine, prevLine;
			if (lineOutBytes == null) {
				lineOut = (lineOutBytes = new ByteArray(lineLen)).items;
				curLine = (curLineBytes = new ByteArray(lineLen)).items;
				prevLine = (prevLineBytes = new ByteArray(lineLen)).items;
			} else {
				lineOut = lineOutBytes.ensureCapacity(lineLen);
				curLine = curLineBytes.ensureCapacity(lineLen);
				prevLine = prevLineBytes.ensureCapacity(lineLen);
				for (int i = 0, n = lastLineLen; i < n; i++)
					prevLine[i] = 0;
			}
			lastLineLen = lineLen;
			
			ByteBuffer pixels = pixmap.getPixels();
			int oldPosition = pixels.position();
			boolean rgba8888 = pixmap.getFormat() == Format.RGBA8888;
			for (int y = 0, h = pixmap.getHeight(); y < h; y++) {
				int py = flipY ? (h - y - 1) : y;
				if (rgba8888) {
					((Buffer) pixels).position(py * lineLen);
					pixels.get(curLine, 0, lineLen);
				} else {
					for (int px = 0, x = 0; px < pixmap.getWidth(); px++) {
						int pixel = pixmap.getPixel(px, py);
						curLine[x++] = (byte) ((pixel >> 24) & 0xff);
						curLine[x++] = (byte) ((pixel >> 16) & 0xff);
						curLine[x++] = (byte) ((pixel >> 8) & 0xff);
						curLine[x++] = (byte) (pixel & 0xff);
					}
				}
				
				lineOut[0] = (byte) (curLine[0] - prevLine[0]);
				lineOut[1] = (byte) (curLine[1] - prevLine[1]);
				lineOut[2] = (byte) (curLine[2] - prevLine[2]);
				lineOut[3] = (byte) (curLine[3] - prevLine[3]);
				
				for (int x = 4; x < lineLen; x++) {
					int a = curLine[x - 4] & 0xff;
					int b = prevLine[x] & 0xff;
					int c = prevLine[x - 4] & 0xff;
					int p = a + b - c;
					int pa = p - a;
					if (pa < 0)
						pa = -pa;
					int pb = p - b;
					if (pb < 0)
						pb = -pb;
					int pc = p - c;
					if (pc < 0)
						pc = -pc;
					if (pa <= pb && pa <= pc)
						c = a;
					else if (pb <= pc) //
						c = b;
					lineOut[x] = (byte) (curLine[x] - c);
				}
				
				deflaterOutput.write(PAETH);
				deflaterOutput.write(lineOut, 0, lineLen);
				
				byte[] temp = curLine;
				curLine = prevLine;
				prevLine = temp;
			}
			((Buffer) pixels).position(oldPosition);
			deflaterOutput.finish();
			buffer.endChunk(dataOutput);
			
			buffer.writeInt(IEND);
			buffer.endChunk(dataOutput);
			
			output.flush();
		}
		
		public void dispose() {
			deflater.end();
		}
		
		static class ChunkBuffer extends DataOutputStream {
			
			final ByteArrayOutputStream buffer;
			final CRC32 crc;
			
			ChunkBuffer(int initialSize) {
				this(new ByteArrayOutputStream(initialSize), new CRC32());
			}
			
			private ChunkBuffer(ByteArrayOutputStream buffer, CRC32 crc) {
				super(new CheckedOutputStream(buffer, crc));
				this.buffer = buffer;
				this.crc = crc;
			}
			
			public void endChunk(DataOutputStream target) throws IOException {
				flush();
				target.writeInt(buffer.size() - 4);
				buffer.writeTo(target);
				target.writeInt((int) crc.getValue());
				buffer.reset();
				crc.reset();
			}
			
		}
		
	}
	
}
