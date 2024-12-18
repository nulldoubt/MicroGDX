package me.nulldoubt.micro.graphics;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.graphics.g2d.Micro2DPixmap;
import me.nulldoubt.micro.utils.Buffers;
import me.nulldoubt.micro.utils.Disposable;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Pixmap implements Disposable {
	
	public enum Format {
		Alpha, Intensity, LuminanceAlpha, RGB565, RGBA4444, RGB888, RGBA8888;
		
		public static int toMicro2DPixmapFormat(Format format) {
			if (format == Alpha)
				return Micro2DPixmap.MICRO2D_FORMAT_ALPHA;
			if (format == Intensity)
				return Micro2DPixmap.MICRO2D_FORMAT_ALPHA;
			if (format == LuminanceAlpha)
				return Micro2DPixmap.MICRO2D_FORMAT_LUMINANCE_ALPHA;
			if (format == RGB565)
				return Micro2DPixmap.MICRO2D_FORMAT_RGB565;
			if (format == RGBA4444)
				return Micro2DPixmap.MICRO2D_FORMAT_RGBA4444;
			if (format == RGB888)
				return Micro2DPixmap.MICRO2D_FORMAT_RGB888;
			if (format == RGBA8888)
				return Micro2DPixmap.MICRO2D_FORMAT_RGBA8888;
			throw new MicroRuntimeException("Unknown Format: " + format);
		}
		
		public static Format fromMicro2DPixmapFormat(int format) {
			if (format == Micro2DPixmap.MICRO2D_FORMAT_ALPHA)
				return Alpha;
			if (format == Micro2DPixmap.MICRO2D_FORMAT_LUMINANCE_ALPHA)
				return LuminanceAlpha;
			if (format == Micro2DPixmap.MICRO2D_FORMAT_RGB565)
				return RGB565;
			if (format == Micro2DPixmap.MICRO2D_FORMAT_RGBA4444)
				return RGBA4444;
			if (format == Micro2DPixmap.MICRO2D_FORMAT_RGB888)
				return RGB888;
			if (format == Micro2DPixmap.MICRO2D_FORMAT_RGBA8888)
				return RGBA8888;
			throw new MicroRuntimeException("Unknown Micro2DPixmap Format: " + format);
		}
		
		public static int toGlFormat(Format format) {
			return Micro2DPixmap.toGlFormat(toMicro2DPixmapFormat(format));
		}
		
		public static int toGlType(Format format) {
			return Micro2DPixmap.toGlType(toMicro2DPixmapFormat(format));
		}
	}
	
	public enum Blending {
		None, SourceOver
	}
	
	public enum Filter {
		NearestNeighbour, BiLinear
	}
	
	public static Pixmap createFromFrameBuffer(int x, int y, int w, int h) {
		Micro.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1);
		
		final Pixmap pixmap = new Pixmap(w, h, Format.RGBA8888);
		ByteBuffer pixels = pixmap.getPixels();
		Micro.gl.glReadPixels(x, y, w, h, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixels);
		
		return pixmap;
	}
	
	private Blending blending = Blending.SourceOver;
	private Filter filter = Filter.BiLinear;
	
	final Micro2DPixmap pixmap;
	int color = 0;
	
	private boolean disposed;
	
	public void setBlending(Blending blending) {
		this.blending = blending;
		pixmap.setBlend(blending == Blending.None ? 0 : 1);
	}
	
	public void setFilter(Filter filter) {
		this.filter = filter;
		pixmap.setScale(filter == Filter.NearestNeighbour ? Micro2DPixmap.MICRO2D_SCALE_NEAREST : Micro2DPixmap.MICRO2D_SCALE_LINEAR);
	}
	
	public Pixmap(int width, int height, Format format) {
		pixmap = new Micro2DPixmap(width, height, Format.toMicro2DPixmapFormat(format));
		setColor(0, 0, 0, 0);
		fill();
	}
	
	public Pixmap(byte[] encodedData, int offset, int len) {
		try {
			pixmap = new Micro2DPixmap(encodedData, offset, len, 0);
		} catch (IOException e) {
			throw new MicroRuntimeException("Couldn't load pixmap from image data", e);
		}
	}
	
	public Pixmap(ByteBuffer encodedData, int offset, int len) {
		if (!encodedData.isDirect())
			throw new MicroRuntimeException("Couldn't load pixmap from non-direct ByteBuffer");
		try {
			pixmap = new Micro2DPixmap(encodedData, offset, len, 0);
		} catch (IOException e) {
			throw new MicroRuntimeException("Couldn't load pixmap from image data", e);
		}
	}
	
	public Pixmap(ByteBuffer encodedData) {
		this(encodedData, encodedData.position(), encodedData.remaining());
	}
	
	public Pixmap(FileHandle file) {
		try {
			byte[] bytes = file.readBytes();
			pixmap = new Micro2DPixmap(bytes, 0, bytes.length, 0);
		} catch (Exception e) {
			throw new MicroRuntimeException("Couldn't load file: " + file, e);
		}
	}
	
	public Pixmap(Micro2DPixmap pixmap) {
		this.pixmap = pixmap;
	}
	
	public void setColor(int color) {
		this.color = color;
	}
	
	public void setColor(float r, float g, float b, float a) {
		color = Color.rgba8888(r, g, b, a);
	}
	
	public void setColor(Color color) {
		this.color = Color.rgba8888(color.r, color.g, color.b, color.a);
	}
	
	public void fill() {
		pixmap.clear(color);
	}
	
	public void drawLine(int x, int y, int x2, int y2) {
		pixmap.drawLine(x, y, x2, y2, color);
	}
	
	public void drawRectangle(int x, int y, int width, int height) {
		pixmap.drawRect(x, y, width, height, color);
	}
	
	public void drawPixmap(Pixmap pixmap, int x, int y) {
		drawPixmap(pixmap, x, y, 0, 0, pixmap.getWidth(), pixmap.getHeight());
	}
	
	public void drawPixmap(Pixmap pixmap, int x, int y, int srcx, int srcy, int srcWidth, int srcHeight) {
		this.pixmap.drawPixmap(pixmap.pixmap, srcx, srcy, x, y, srcWidth, srcHeight);
	}
	
	public void drawPixmap(Pixmap pixmap, int srcx, int srcy, int srcWidth, int srcHeight, int dstx, int dsty, int dstWidth, int dstHeight) {
		this.pixmap.drawPixmap(pixmap.pixmap, srcx, srcy, srcWidth, srcHeight, dstx, dsty, dstWidth, dstHeight);
	}
	
	public void fillRectangle(int x, int y, int width, int height) {
		pixmap.fillRect(x, y, width, height, color);
	}
	
	public void drawCircle(int x, int y, int radius) {
		pixmap.drawCircle(x, y, radius, color);
	}
	
	public void fillCircle(int x, int y, int radius) {
		pixmap.fillCircle(x, y, radius, color);
	}
	
	public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {
		pixmap.fillTriangle(x1, y1, x2, y2, x3, y3, color);
	}
	
	public int getPixel(int x, int y) {
		return pixmap.getPixel(x, y);
	}
	
	public int getWidth() {
		return pixmap.getWidth();
	}
	
	public int getHeight() {
		return pixmap.getHeight();
	}
	
	public void dispose() {
		if (disposed) {
			Micro.app.error("Pixmap", "Pixmap already disposed!");
			return;
		}
		pixmap.dispose();
		disposed = true;
	}
	
	public boolean isDisposed() {
		return disposed;
	}
	
	public void drawPixel(int x, int y) {
		pixmap.setPixel(x, y, color);
	}
	
	public void drawPixel(int x, int y, int color) {
		pixmap.setPixel(x, y, color);
	}
	
	public int getGLFormat() {
		return pixmap.getGLFormat();
	}
	
	public int getGLInternalFormat() {
		return pixmap.getGLInternalFormat();
	}
	
	public int getGLType() {
		return pixmap.getGLType();
	}
	
	public ByteBuffer getPixels() {
		if (disposed)
			throw new MicroRuntimeException("Pixmap already disposed");
		return pixmap.getPixels();
	}
	
	public void setPixels(ByteBuffer pixels) {
		if (!pixels.isDirect())
			throw new MicroRuntimeException("Couldn't setPixels from non-direct ByteBuffer");
		ByteBuffer dst = pixmap.getPixels();
		Buffers.copy(pixels, dst, dst.limit());
	}
	
	public Format getFormat() {
		return Format.fromMicro2DPixmapFormat(pixmap.getFormat());
	}
	
	public Blending getBlending() {
		return blending;
	}
	
	public Filter getFilter() {
		return filter;
	}
	
}
