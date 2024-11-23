package me.nulldoubt.micro.graphics.glutils;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.graphics.Pixmap;
import me.nulldoubt.micro.graphics.Pixmap.Format;
import me.nulldoubt.micro.graphics.TextureData;

public class GLOnlyTextureData implements TextureData {
	
	int width;
	int height;
	boolean isPrepared = false;
	
	int mipLevel;
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
			throw new MicroRuntimeException("Already prepared");
		isPrepared = true;
	}
	
	@Override
	public void consumeCustomData(int target) {
		Micro.gl.glTexImage2D(target, mipLevel, internalFormat, width, height, 0, format, type, null);
	}
	
	@Override
	public Pixmap consumePixmap() {
		throw new MicroRuntimeException("This TextureData implementation does not return a Pixmap");
	}
	
	@Override
	public boolean disposePixmap() {
		throw new MicroRuntimeException("This TextureData implementation does not return a Pixmap");
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
