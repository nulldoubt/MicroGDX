package me.nulldoubt.micro.utils;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.graphics.Color;
import me.nulldoubt.micro.graphics.GL20;
import me.nulldoubt.micro.graphics.Pixmap;
import me.nulldoubt.micro.graphics.Pixmap.Blending;
import me.nulldoubt.micro.graphics.Pixmap.Format;
import me.nulldoubt.micro.graphics.Texture;
import me.nulldoubt.micro.graphics.g2d.TextureRegion;
import me.nulldoubt.micro.math.MathUtils;

import java.nio.Buffer;
import java.nio.ByteBuffer;

public final class Screens {
	
	public static void clear(Color color) {
		clear(color.r, color.g, color.b, color.a, false);
	}
	
	public static void clear(float r, float g, float b, float a) {
		clear(r, g, b, a, false);
	}
	
	public static void clear(Color color, boolean clearDepth) {
		clear(color.r, color.g, color.b, color.a, clearDepth);
	}
	
	public static void clear(float r, float g, float b, float a, boolean clearDepth) {
		clear(r, g, b, a, clearDepth, false);
	}
	
	public static void clear(float r, float g, float b, float a, boolean clearDepth, boolean applyAntialiasing) {
		Micro.gl.glClearColor(r, g, b, a);
		int mask = GL20.GL_COLOR_BUFFER_BIT;
		if (clearDepth)
			mask = mask | GL20.GL_DEPTH_BUFFER_BIT;
		if (applyAntialiasing && Micro.graphics.getBufferFormat().coverageSampling())
			mask = mask | GL20.GL_COVERAGE_BUFFER_BIT_NV;
		Micro.gl.glClear(mask);
	}
	
	public static TextureRegion getFrameBufferTexture() {
		final int w = Micro.graphics.getBackBufferWidth();
		final int h = Micro.graphics.getBackBufferHeight();
		return getFrameBufferTexture(0, 0, w, h);
	}
	
	public static TextureRegion getFrameBufferTexture(int x, int y, int w, int h) {
		final int potW = MathUtils.nextPowerOfTwo(w);
		final int potH = MathUtils.nextPowerOfTwo(h);
		
		final Pixmap pixmap = Pixmap.createFromFrameBuffer(x, y, w, h);
		final Pixmap potPixmap = new Pixmap(potW, potH, Format.RGBA8888);
		potPixmap.setBlending(Blending.None);
		potPixmap.drawPixmap(pixmap, 0, 0);
		Texture texture = new Texture(potPixmap);
		TextureRegion textureRegion = new TextureRegion(texture, 0, h, w, -h);
		potPixmap.dispose();
		pixmap.dispose();
		
		return textureRegion;
	}
	
	public static byte[] getFrameBufferPixels(boolean flipY) {
		final int w = Micro.graphics.getBackBufferWidth();
		final int h = Micro.graphics.getBackBufferHeight();
		return getFrameBufferPixels(0, 0, w, h, flipY);
	}
	
	public static byte[] getFrameBufferPixels(int x, int y, int w, int h, boolean flipY) {
		Micro.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1);
		final ByteBuffer pixels = Buffers.newByteBuffer(w * h * 4);
		Micro.gl.glReadPixels(x, y, w, h, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixels);
		final int numBytes = w * h * 4;
		byte[] lines = new byte[numBytes];
		if (flipY) {
			final int numBytesPerLine = w * 4;
			for (int i = 0; i < h; i++) {
				((Buffer) pixels).position((h - i - 1) * numBytesPerLine);
				pixels.get(lines, i * numBytesPerLine, numBytesPerLine);
			}
		} else {
			((Buffer) pixels).clear();
			pixels.get(lines);
		}
		return lines;
		
	}
	
}
