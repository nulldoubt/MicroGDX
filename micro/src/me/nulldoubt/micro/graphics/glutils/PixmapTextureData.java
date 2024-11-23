package me.nulldoubt.micro.graphics.glutils;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.graphics.GL20;
import me.nulldoubt.micro.graphics.Pixmap;
import me.nulldoubt.micro.graphics.Pixmap.Format;
import me.nulldoubt.micro.graphics.TextureData;

public class PixmapTextureData implements TextureData {
	
	protected final Pixmap pixmap;
	protected final Format format;
	protected final boolean useMipMaps;
	protected final boolean disposePixmap;
	protected final boolean managed;
	
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
	public boolean isPrepared() {
		return true;
	}
	
	@Override
	public void prepare() {
		throw new MicroRuntimeException("prepare() must not be called on a PixmapTextureData instance as it is already prepared.");
	}
	
	@Override
	public void consume(final int target, final int mipMapLevel) {
		Pixmap pixmap = this.pixmap;
		if (format != pixmap.getFormat()) {
			final Pixmap temp = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), format);
			temp.setBlending(Pixmap.Blending.None);
			temp.drawPixmap(pixmap, 0, 0, 0, 0, pixmap.getWidth(), pixmap.getHeight());
			if (disposePixmap)
				pixmap.dispose();
			pixmap = temp;
		}
		
		Micro.gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1);
		if (useMipMaps)
			MipMapGenerator.generateMipMap(target, pixmap, pixmap.getWidth(), pixmap.getHeight());
		else
			Micro.gl.glTexImage2D(target, mipMapLevel, pixmap.getGLInternalFormat(), pixmap.getWidth(), pixmap.getHeight(), 0, pixmap.getGLFormat(), pixmap.getGLType(), pixmap.getPixels());
		
		if (disposePixmap)
			pixmap.dispose();
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
	public boolean isManaged() {
		return managed;
	}
	
	@Override
	public boolean useMipMaps() {
		return useMipMaps;
	}
	
}
