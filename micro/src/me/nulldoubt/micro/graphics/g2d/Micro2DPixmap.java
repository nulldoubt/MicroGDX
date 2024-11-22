package me.nulldoubt.micro.graphics.g2d;

import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.graphics.GL20;
import me.nulldoubt.micro.utils.Disposable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class Micro2DPixmap implements Disposable {
	
	public static final int MICRO2D_FORMAT_ALPHA = 1;
	public static final int MICRO2D_FORMAT_LUMINANCE_ALPHA = 2;
	public static final int MICRO2D_FORMAT_RGB888 = 3;
	public static final int MICRO2D_FORMAT_RGBA8888 = 4;
	public static final int MICRO2D_FORMAT_RGB565 = 5;
	public static final int MICRO2D_FORMAT_RGBA4444 = 6;
	
	public static final int MICRO2D_SCALE_NEAREST = 0;
	public static final int MICRO2D_SCALE_LINEAR = 1;
	
	public static final int MICRO2D_BLEND_NONE = 0;
	public static final int MICRO2D_BLEND_SRC_OVER = 1;
	
	public static int toGlFormat(int format) {
		return switch (format) {
			case MICRO2D_FORMAT_ALPHA -> GL20.GL_ALPHA;
			case MICRO2D_FORMAT_LUMINANCE_ALPHA -> GL20.GL_LUMINANCE_ALPHA;
			case MICRO2D_FORMAT_RGB888, MICRO2D_FORMAT_RGB565 -> GL20.GL_RGB;
			case MICRO2D_FORMAT_RGBA8888, MICRO2D_FORMAT_RGBA4444 -> GL20.GL_RGBA;
			default -> throw new MicroRuntimeException("Unknown format: " + format);
		};
	}
	
	public static int toGlType(int format) {
		return switch (format) {
			case MICRO2D_FORMAT_ALPHA, MICRO2D_FORMAT_LUMINANCE_ALPHA, MICRO2D_FORMAT_RGB888, MICRO2D_FORMAT_RGBA8888 ->
					GL20.GL_UNSIGNED_BYTE;
			case MICRO2D_FORMAT_RGB565 -> GL20.GL_UNSIGNED_SHORT_5_6_5;
			case MICRO2D_FORMAT_RGBA4444 -> GL20.GL_UNSIGNED_SHORT_4_4_4_4;
			default -> throw new MicroRuntimeException("Unknown format: " + format);
		};
	}
	
	long basePtr;
	int width;
	int height;
	int format;
	ByteBuffer pixelPtr;
	long[] nativeData = new long[4];
	
	public Micro2DPixmap(byte[] encodedData, int offset, int len, int requestedFormat) throws IOException {
		pixelPtr = load(nativeData, encodedData, offset, len);
		if (pixelPtr == null)
			throw new IOException("Error loading pixmap: " + getFailureReason());
		
		basePtr = nativeData[0];
		width = (int) nativeData[1];
		height = (int) nativeData[2];
		format = (int) nativeData[3];
		
		if (requestedFormat != 0 && requestedFormat != format) {
			convert(requestedFormat);
		}
	}
	
	public Micro2DPixmap(ByteBuffer encodedData, int offset, int len, int requestedFormat) throws IOException {
		if (!encodedData.isDirect())
			throw new IOException("Couldn't load pixmap from non-direct ByteBuffer");
		pixelPtr = loadByteBuffer(nativeData, encodedData, offset, len);
		if (pixelPtr == null)
			throw new IOException("Error loading pixmap: " + getFailureReason());
		
		basePtr = nativeData[0];
		width = (int) nativeData[1];
		height = (int) nativeData[2];
		format = (int) nativeData[3];
		
		if (requestedFormat != 0 && requestedFormat != format) {
			convert(requestedFormat);
		}
	}
	
	public Micro2DPixmap(InputStream in, int requestedFormat) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream(1024);
		byte[] buffer = new byte[1024];
		int readBytes = 0;
		
		while ((readBytes = in.read(buffer)) != -1) {
			bytes.write(buffer, 0, readBytes);
		}
		
		buffer = bytes.toByteArray();
		pixelPtr = load(nativeData, buffer, 0, buffer.length);
		if (pixelPtr == null)
			throw new IOException("Error loading pixmap: " + getFailureReason());
		
		basePtr = nativeData[0];
		width = (int) nativeData[1];
		height = (int) nativeData[2];
		format = (int) nativeData[3];
		
		if (requestedFormat != 0 && requestedFormat != format) {
			convert(requestedFormat);
		}
	}
	
	public Micro2DPixmap(int width, int height, int format) throws MicroRuntimeException {
		pixelPtr = newPixmap(nativeData, width, height, format);
		if (pixelPtr == null)
			throw new MicroRuntimeException("Unable to allocate memory for pixmap: " + width + "x" + height + ", " + getFormatString(format));
		
		this.basePtr = nativeData[0];
		this.width = (int) nativeData[1];
		this.height = (int) nativeData[2];
		this.format = (int) nativeData[3];
	}
	
	public Micro2DPixmap(ByteBuffer pixelPtr, long[] nativeData) {
		this.pixelPtr = pixelPtr;
		this.basePtr = nativeData[0];
		this.width = (int) nativeData[1];
		this.height = (int) nativeData[2];
		this.format = (int) nativeData[3];
	}
	
	private void convert(int requestedFormat) {
		Micro2DPixmap pixmap = new Micro2DPixmap(width, height, requestedFormat);
		pixmap.setBlend(MICRO2D_BLEND_NONE);
		pixmap.drawPixmap(this, 0, 0, 0, 0, width, height);
		dispose();
		this.basePtr = pixmap.basePtr;
		this.format = pixmap.format;
		this.height = pixmap.height;
		this.nativeData = pixmap.nativeData;
		this.pixelPtr = pixmap.pixelPtr;
		this.width = pixmap.width;
	}
	
	@Override
	public void dispose() {
		free(basePtr);
	}
	
	public void clear(int color) {
		clear(basePtr, color);
	}
	
	public void setPixel(int x, int y, int color) {
		setPixel(basePtr, x, y, color);
	}
	
	public int getPixel(int x, int y) {
		return getPixel(basePtr, x, y);
	}
	
	public void drawLine(int x, int y, int x2, int y2, int color) {
		drawLine(basePtr, x, y, x2, y2, color);
	}
	
	public void drawRect(int x, int y, int width, int height, int color) {
		drawRect(basePtr, x, y, width, height, color);
	}
	
	public void drawCircle(int x, int y, int radius, int color) {
		drawCircle(basePtr, x, y, radius, color);
	}
	
	public void fillRect(int x, int y, int width, int height, int color) {
		fillRect(basePtr, x, y, width, height, color);
	}
	
	public void fillCircle(int x, int y, int radius, int color) {
		fillCircle(basePtr, x, y, radius, color);
	}
	
	public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int color) {
		fillTriangle(basePtr, x1, y1, x2, y2, x3, y3, color);
	}
	
	public void drawPixmap(Micro2DPixmap src, int srcX, int srcY, int dstX, int dstY, int width, int height) {
		drawPixmap(src.basePtr, basePtr, srcX, srcY, width, height, dstX, dstY, width, height);
	}
	
	public void drawPixmap(Micro2DPixmap src, int srcX, int srcY, int srcWidth, int srcHeight, int dstX, int dstY, int dstWidth,
						   int dstHeight) {
		drawPixmap(src.basePtr, basePtr, srcX, srcY, srcWidth, srcHeight, dstX, dstY, dstWidth, dstHeight);
	}
	
	public void setBlend(int blend) {
		setBlend(basePtr, blend);
	}
	
	public void setScale(int scale) {
		setScale(basePtr, scale);
	}
	
	public static Micro2DPixmap newPixmap(InputStream in, int requestedFormat) {
		try {
			return new Micro2DPixmap(in, requestedFormat);
		} catch (IOException e) {
			return null;
		}
	}
	
	public static Micro2DPixmap newPixmap(int width, int height, int format) {
		try {
			return new Micro2DPixmap(width, height, format);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
	
	public ByteBuffer getPixels() {
		return pixelPtr;
	}
	
	public int getHeight() {
		return height;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getFormat() {
		return format;
	}
	
	public int getGLInternalFormat() {
		return toGlFormat(format);
	}
	
	public int getGLFormat() {
		return getGLInternalFormat();
	}
	
	public int getGLType() {
		return toGlType(format);
	}
	
	public String getFormatString() {
		return getFormatString(format);
	}
	
	private static String getFormatString(int format) {
		switch (format) {
			case MICRO2D_FORMAT_ALPHA:
				return "alpha";
			case MICRO2D_FORMAT_LUMINANCE_ALPHA:
				return "luminance alpha";
			case MICRO2D_FORMAT_RGB888:
				return "rgb888";
			case MICRO2D_FORMAT_RGBA8888:
				return "rgba8888";
			case MICRO2D_FORMAT_RGB565:
				return "rgb565";
			case MICRO2D_FORMAT_RGBA4444:
				return "rgba4444";
			default:
				return "Unknown";
		}
	}
	
	// @off
	/*JNI
	#include <micro2d/micro2d.h>
	#include <stdlib.h>
	 */
	
	private static native ByteBuffer load(long[] nativeData, byte[] buffer, int offset, int len); /*MANUAL
		const unsigned char* p_buffer = (const unsigned char*)env->GetPrimitiveArrayCritical(buffer, 0);
		micro2d_pixmap* pixmap = micro2d_load(p_buffer + offset, len);
		env->ReleasePrimitiveArrayCritical(buffer, (char*)p_buffer, 0);

		if(pixmap==0)
			return 0;

		jobject pixel_buffer = env->NewDirectByteBuffer((void*)pixmap->pixels, pixmap->width * pixmap->height * micro2d_bytes_per_pixel(pixmap->format));
		jlong* p_native_data = (jlong*)env->GetPrimitiveArrayCritical(nativeData, 0);
		p_native_data[0] = (jlong)pixmap;
		p_native_data[1] = pixmap->width;
		p_native_data[2] = pixmap->height;
		p_native_data[3] = pixmap->format;
		env->ReleasePrimitiveArrayCritical(nativeData, p_native_data, 0);

		return pixel_buffer;
	 */
	
	private static native ByteBuffer loadByteBuffer(long[] nativeData, ByteBuffer buffer, int offset, int len); /*MANUAL
		if(buffer==0)
			return 0;

		const unsigned char* p_buffer = (const unsigned char*)env->GetDirectBufferAddress(buffer);
		micro2d_pixmap* pixmap = micro2d_load(p_buffer + offset, len);

		if(pixmap==0)
			return 0;

		jobject pixel_buffer = env->NewDirectByteBuffer((void*)pixmap->pixels, pixmap->width * pixmap->height * micro2d_bytes_per_pixel(pixmap->format));
		jlong* p_native_data = (jlong*)env->GetPrimitiveArrayCritical(nativeData, 0);
		p_native_data[0] = (jlong)pixmap;
		p_native_data[1] = pixmap->width;
		p_native_data[2] = pixmap->height;
		p_native_data[3] = pixmap->format;
		env->ReleasePrimitiveArrayCritical(nativeData, p_native_data, 0);

		return pixel_buffer;
	 */
	
	private static native ByteBuffer newPixmap(long[] nativeData, int width, int height, int format); /*MANUAL
		micro2d_pixmap* pixmap = micro2d_new(width, height, format);
		if(pixmap==0)
			return 0;

		jobject pixel_buffer = env->NewDirectByteBuffer((void*)pixmap->pixels, pixmap->width * pixmap->height * micro2d_bytes_per_pixel(pixmap->format));
		jlong* p_native_data = (jlong*)env->GetPrimitiveArrayCritical(nativeData, 0);
		p_native_data[0] = (jlong)pixmap;
		p_native_data[1] = pixmap->width;
		p_native_data[2] = pixmap->height;
		p_native_data[3] = pixmap->format;
		env->ReleasePrimitiveArrayCritical(nativeData, p_native_data, 0);

		return pixel_buffer;
	 */
	
	private static native void free(long pixmap); /*
		micro2d_free((micro2d_pixmap*)pixmap);
	 */
	
	private static native void clear(long pixmap, int color); /*
		micro2d_clear((micro2d_pixmap*)pixmap, color);
	 */
	
	private static native void setPixel(long pixmap, int x, int y, int color); /*
		micro2d_set_pixel((micro2d_pixmap*)pixmap, x, y, color);
	 */
	
	private static native int getPixel(long pixmap, int x, int y); /*
		return micro2d_get_pixel((micro2d_pixmap*)pixmap, x, y);
	 */
	
	private static native void drawLine(long pixmap, int x, int y, int x2, int y2, int color); /*
		micro2d_draw_line((micro2d_pixmap*)pixmap, x, y, x2, y2, color);
	 */
	
	private static native void drawRect(long pixmap, int x, int y, int width, int height, int color); /*
		micro2d_draw_rect((micro2d_pixmap*)pixmap, x, y, width, height, color);
	 */
	
	private static native void drawCircle(long pixmap, int x, int y, int radius, int color); /*
		micro2d_draw_circle((micro2d_pixmap*)pixmap, x, y, radius, color);
	 */
	
	private static native void fillRect(long pixmap, int x, int y, int width, int height, int color); /*
		micro2d_fill_rect((micro2d_pixmap*)pixmap, x, y, width, height, color);
	 */
	
	private static native void fillCircle(long pixmap, int x, int y, int radius, int color); /*
		micro2d_fill_circle((micro2d_pixmap*)pixmap, x, y, radius, color);
	 */
	
	private static native void fillTriangle(long pixmap, int x1, int y1, int x2, int y2, int x3, int y3, int color); /*
		micro2d_fill_triangle((micro2d_pixmap*)pixmap, x1, y1, x2, y2, x3, y3, color);
	 */
	
	private static native void drawPixmap(long src, long dst, int srcX, int srcY, int srcWidth, int srcHeight, int dstX,
										  int dstY, int dstWidth, int dstHeight); /*
		micro2d_draw_pixmap((micro2d_pixmap*)src, (micro2d_pixmap*)dst, srcX, srcY, srcWidth, srcHeight, dstX, dstY, dstWidth, dstHeight);
		 */
	
	private static native void setBlend(long src, int blend); /*
		micro2d_set_blend((micro2d_pixmap*)src, blend);
	 */
	
	private static native void setScale(long src, int scale); /*
		micro2d_set_scale((micro2d_pixmap*)src, scale);
	 */
	
	public static native String getFailureReason(); /*
     return env->NewStringUTF(micro2d_get_failure_reason());
	 */
	
}
