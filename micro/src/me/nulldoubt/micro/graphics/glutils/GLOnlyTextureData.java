package me.nulldoubt.micro.graphics.glutils;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.graphics.TextureData;

public class GLOnlyTextureData implements TextureData {
	
	protected int width;
	protected int height;
	protected boolean prepared = false;
	
	protected int mipLevel;
	protected int internalFormat;
	protected int format;
	protected int type;
	
	public GLOnlyTextureData(int width, int height, int mipMapLevel, int internalFormat, int format, int type) {
		this.width = width;
		this.height = height;
		this.mipLevel = mipMapLevel;
		this.internalFormat = internalFormat;
		this.format = format;
		this.type = type;
	}
	
	@Override
	public boolean isPrepared() {
		return prepared;
	}
	
	@Override
	public void prepare() {
		if (prepared)
			throw new MicroRuntimeException("Already prepared");
		prepared = true;
	}
	
	@Override
	public void consume(final int target, final int mipMapLevel) {
		Micro.gl.glTexImage2D(target, mipLevel, internalFormat, width, height, 0, format, type, null);
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
	public boolean isManaged() {
		return false;
	}
	
}
