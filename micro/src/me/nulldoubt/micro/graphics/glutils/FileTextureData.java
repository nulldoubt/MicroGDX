package me.nulldoubt.micro.graphics.glutils;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.graphics.GL20;
import me.nulldoubt.micro.graphics.Pixmap;
import me.nulldoubt.micro.graphics.Pixmap.Format;
import me.nulldoubt.micro.graphics.TextureData;

public class FileTextureData implements TextureData {
	
	protected final FileHandle file;
	protected int width = 0;
	protected int height = 0;
	protected Format format;
	protected Pixmap pixmap;
	protected boolean useMipMaps;
	protected boolean prepared = false;
	
	public FileTextureData(final FileHandle file, final Pixmap preloadedPixmap, final Format format, final boolean useMipMaps) {
		this.file = file;
		this.pixmap = preloadedPixmap;
		this.format = format;
		this.useMipMaps = useMipMaps;
		if (pixmap != null) {
			width = pixmap.getWidth();
			height = pixmap.getHeight();
			if (format == null)
				this.format = pixmap.getFormat();
		}
	}
	
	@Override
	public boolean isPrepared() {
		return prepared;
	}
	
	@Override
	public void prepare() {
		if (prepared)
			throw new MicroRuntimeException("Already prepared");
		if (pixmap == null) {
			pixmap = new Pixmap(file);
			width = pixmap.getWidth();
			height = pixmap.getHeight();
			if (format == null)
				format = pixmap.getFormat();
		}
		prepared = true;
	}
	
	@Override
	public void consume(final int target, final int mipMapLevel) {
		if (!prepared)
			throw new MicroRuntimeException("Not prepared");
		prepared = false;
		
		Pixmap pixmap = this.pixmap;
		this.pixmap = null;
		
		if (format != pixmap.getFormat()) {
			final Pixmap temp = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), format);
			temp.setBlending(Pixmap.Blending.None);
			temp.drawPixmap(pixmap, 0, 0, 0, 0, pixmap.getWidth(), pixmap.getHeight());
			pixmap.dispose();
			pixmap = temp;
		}
		
		Micro.gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1);
		if (useMipMaps)
			MipMapGenerator.generateMipMap(target, pixmap, pixmap.getWidth(), pixmap.getHeight());
		else
			Micro.gl.glTexImage2D(target, mipMapLevel, pixmap.getGLInternalFormat(), pixmap.getWidth(), pixmap.getHeight(), 0, pixmap.getGLFormat(), pixmap.getGLType(), pixmap.getPixels());
		
		pixmap.dispose();
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
		return true;
	}
	
	@Override
	public boolean useMipMaps() {
		return useMipMaps;
	}
	
	public FileHandle getFileHandle() {
		return file;
	}
	
	public String toString() {
		return file.toString();
	}
	
}
