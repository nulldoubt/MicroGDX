package me.nulldoubt.micro.graphics.glutils;

import me.nulldoubt.micro.Application.ApplicationType;
import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.graphics.GL20;
import me.nulldoubt.micro.graphics.Pixmap;
import me.nulldoubt.micro.graphics.Pixmap.Blending;
import me.nulldoubt.micro.utils.GdxRuntimeException;

public class MipMapGenerator {
	
	private MipMapGenerator() {}
	
	private static boolean useHWMipMap = true;
	
	public static void setUseHardwareMipMap(boolean useHWMipMap) {
		MipMapGenerator.useHWMipMap = useHWMipMap;
	}
	
	public static void generateMipMap(Pixmap pixmap, int textureWidth, int textureHeight) {
		generateMipMap(GL20.GL_TEXTURE_2D, pixmap, textureWidth, textureHeight);
	}
	
	public static void generateMipMap(int target, Pixmap pixmap, int textureWidth, int textureHeight) {
		if (!useHWMipMap) {
			generateMipMapCPU(target, pixmap, textureWidth, textureHeight);
			return;
		}
		
		if (Micro.app.getType() == ApplicationType.Android)
			generateMipMapGLES20(target, pixmap);
		else
			generateMipMapDesktop(target, pixmap, textureWidth, textureHeight);
	}
	
	private static void generateMipMapGLES20(int target, Pixmap pixmap) {
		Micro.gl.glTexImage2D(target, 0, pixmap.getGLInternalFormat(), pixmap.getWidth(), pixmap.getHeight(), 0, pixmap.getGLFormat(),
				pixmap.getGLType(), pixmap.getPixels());
		Micro.gl20.glGenerateMipmap(target);
	}
	
	private static void generateMipMapDesktop(int target, Pixmap pixmap, int textureWidth, int textureHeight) {
		if (Micro.graphics.supportsExtension("GL_ARB_framebuffer_object")
				|| Micro.graphics.supportsExtension("GL_EXT_framebuffer_object")
				|| Micro.gl20.getClass().getName().equals("com.nulldoubt.gdx.backends.lwjgl3.Lwjgl3GLES20") // LWJGL3ANGLE
				|| Micro.gl30 != null) {
			Micro.gl.glTexImage2D(target, 0, pixmap.getGLInternalFormat(), pixmap.getWidth(), pixmap.getHeight(), 0,
					pixmap.getGLFormat(), pixmap.getGLType(), pixmap.getPixels());
			Micro.gl20.glGenerateMipmap(target);
		} else {
			generateMipMapCPU(target, pixmap, textureWidth, textureHeight);
		}
	}
	
	private static void generateMipMapCPU(int target, Pixmap pixmap, int textureWidth, int textureHeight) {
		Micro.gl.glTexImage2D(target, 0, pixmap.getGLInternalFormat(), pixmap.getWidth(), pixmap.getHeight(), 0, pixmap.getGLFormat(),
				pixmap.getGLType(), pixmap.getPixels());
		if ((Micro.gl20 == null) && textureWidth != textureHeight)
			throw new GdxRuntimeException("texture width and height must be square when using mipmapping.");
		int width = pixmap.getWidth() / 2;
		int height = pixmap.getHeight() / 2;
		int level = 1;
		while (width > 0 && height > 0) {
			Pixmap tmp = new Pixmap(width, height, pixmap.getFormat());
			tmp.setBlending(Blending.None);
			tmp.drawPixmap(pixmap, 0, 0, pixmap.getWidth(), pixmap.getHeight(), 0, 0, width, height);
			if (level > 1)
				pixmap.dispose();
			pixmap = tmp;
			
			Micro.gl.glTexImage2D(target, level, pixmap.getGLInternalFormat(), pixmap.getWidth(), pixmap.getHeight(), 0,
					pixmap.getGLFormat(), pixmap.getGLType(), pixmap.getPixels());
			
			width = pixmap.getWidth() / 2;
			height = pixmap.getHeight() / 2;
			level++;
		}
	}
	
}
