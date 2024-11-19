package com.badlogic.gdx.graphics.glutils;

import com.badlogic.gdx.Micro;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class GLOnlyTextureData implements TextureData {
	
	/**
	 * width and height
	 */
	int width = 0;
	int height = 0;
	boolean isPrepared = false;
	
	int mipLevel = 0;
	int internalFormat;
	int format;
	int type;
	
	public GLOnlyTextureData(int width, int height, int mipMapLevel, int internalFormat, int format, int type) {
		this.width = width;
		this.height = height;
		this.mipLevel = mipMapLevel;
		this.internalFormat = internalFormat;
		this.format = format;
		this.type = type;
	}
	
	@Override
	public boolean isCustom() {
		return true;
	}
	
	@Override
	public boolean isPrepared() {
		return isPrepared;
	}
	
	@Override
	public void prepare() {
		if (isPrepared)
			throw new GdxRuntimeException("Already prepared");
		isPrepared = true;
	}
	
	@Override
	public void consumeCustomData(int target) {
		Micro.gl.glTexImage2D(target, mipLevel, internalFormat, width, height, 0, format, type, null);
	}
	
	@Override
	public Pixmap consumePixmap() {
		throw new GdxRuntimeException("This TextureData implementation does not return a Pixmap");
	}
	
	@Override
	public boolean disposePixmap() {
		throw new GdxRuntimeException("This TextureData implementation does not return a Pixmap");
	}
	
	@Override
	public int getWidth() {
		return width;
	}
	
	@Override
	public int getHeight() {
		return height;
	}
	
	@Override
	public Format getFormat() {
		return Format.RGBA8888;
	}
	
	@Override
	public boolean useMipMaps() {
		return false;
	}
	
	@Override
	public boolean isManaged() {
		return false;
	}
	
}
