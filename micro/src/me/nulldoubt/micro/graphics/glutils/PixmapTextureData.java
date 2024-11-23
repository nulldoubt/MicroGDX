package me.nulldoubt.micro.graphics.glutils;

import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.graphics.Pixmap;
import me.nulldoubt.micro.graphics.Pixmap.Format;
import me.nulldoubt.micro.graphics.TextureData;

public class PixmapTextureData implements TextureData {
	
	final Pixmap pixmap;
	final Format format;
	final boolean useMipMaps;
	final boolean disposePixmap;
	final boolean managed;
	
	public PixmapTextureData(Pixmap pixmap, Format format, boolean useMipMaps, boolean disposePixmap) {
		this(pixmap, format, useMipMaps, disposePixmap, false);
	}
	
	public PixmapTextureData(Pixmap pixmap, Format format, boolean useMipMaps, boolean disposePixmap, boolean managed) {
		this.pixmap = pixmap;
		this.format = format == null ? pixmap.getFormat() : format;
		this.useMipMaps = useMipMaps;
		this.disposePixmap = disposePixmap;
		this.managed = managed;
	}
	
	@Override
	public boolean disposePixmap() {
		return disposePixmap;
	}
	
	@Override
	public Pixmap consumePixmap() {
		return pixmap;
	}
	
	@Override
	public int getWidth() {
		return pixmap.getWidth();
	}
	
	@Override
	public int getHeight() {
		return pixmap.getHeight();
	}
	
	@Override
	public Format getFormat() {
		return format;
	}
	
	@Override
	public boolean useMipMaps() {
		return useMipMaps;
	}
	
	@Override
	public boolean isManaged() {
		return managed;
	}
	
	@Override
	public void consumeCustomData(int target) {
		throw new MicroRuntimeException("This TextureData implementation does not upload data itself");
	}
	
	@Override
	public boolean isPrepared() {
		return true;
	}
	
	@Override
	public void prepare() {
		throw new MicroRuntimeException("prepare() must not be called on a PixmapTextureData instance as it is already prepared.");
	}
	
}