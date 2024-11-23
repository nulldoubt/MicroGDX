package me.nulldoubt.micro.graphics;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.graphics.Texture.TextureFilter;
import me.nulldoubt.micro.graphics.Texture.TextureWrap;
import me.nulldoubt.micro.math.MathUtils;
import me.nulldoubt.micro.utils.Buffers;
import me.nulldoubt.micro.utils.Disposable;

import java.nio.Buffer;
import java.nio.FloatBuffer;

public abstract class GLTexture implements Disposable {
	
	public final int glTarget;
	protected int glHandle;
	protected TextureFilter minFilter = TextureFilter.Nearest;
	protected TextureFilter magFilter = TextureFilter.Nearest;
	protected TextureWrap uWrap = TextureWrap.ClampToEdge;
	protected TextureWrap vWrap = TextureWrap.ClampToEdge;
	protected float anisotropicFilterLevel = 1.0f;
	private static float maxAnisotropicFilterLevel = 0;
	
	public abstract int getWidth();
	
	public abstract int getHeight();
	
	public abstract int getDepth();
	
	public GLTexture(int glTarget) {
		this(glTarget, Micro.gl.glGenTexture());
	}
	
	public GLTexture(int glTarget, int glHandle) {
		this.glTarget = glTarget;
		this.glHandle = glHandle;
	}
	
	public abstract boolean isManaged();
	
	protected abstract void reload();
	
	public void bind() {
		Micro.gl.glBindTexture(glTarget, glHandle);
	}
	
	public void bind(int unit) {
		Micro.gl.glActiveTexture(GL20.GL_TEXTURE0 + unit);
		Micro.gl.glBindTexture(glTarget, glHandle);
	}
	
	public TextureFilter getMinFilter() {
		return minFilter;
	}
	
	public TextureFilter getMagFilter() {
		return magFilter;
	}
	
	public TextureWrap getUWrap() {
		return uWrap;
	}
	
	public TextureWrap getVWrap() {
		return vWrap;
	}
	
	public int getTextureObjectHandle() {
		return glHandle;
	}
	
	public void unsafeSetWrap(TextureWrap u, TextureWrap v) {
		unsafeSetWrap(u, v, false);
	}
	
	public void unsafeSetWrap(TextureWrap u, TextureWrap v, boolean force) {
		if (u != null && (force || uWrap != u)) {
			Micro.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_WRAP_S, u.getGLEnum());
			uWrap = u;
		}
		if (v != null && (force || vWrap != v)) {
			Micro.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_WRAP_T, v.getGLEnum());
			vWrap = v;
		}
	}
	
	public void setWrap(TextureWrap u, TextureWrap v) {
		this.uWrap = u;
		this.vWrap = v;
		bind();
		Micro.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_WRAP_S, u.getGLEnum());
		Micro.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_WRAP_T, v.getGLEnum());
	}
	
	public void unsafeSetFilter(TextureFilter minFilter, TextureFilter magFilter) {
		unsafeSetFilter(minFilter, magFilter, false);
	}
	
	public void unsafeSetFilter(TextureFilter minFilter, TextureFilter magFilter, boolean force) {
		if (minFilter != null && (force || this.minFilter != minFilter)) {
			Micro.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_MIN_FILTER, minFilter.getGLEnum());
			this.minFilter = minFilter;
		}
		if (magFilter != null && (force || this.magFilter != magFilter)) {
			Micro.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_MAG_FILTER, magFilter.getGLEnum());
			this.magFilter = magFilter;
		}
	}
	
	public void setFilter(TextureFilter minFilter, TextureFilter magFilter) {
		this.minFilter = minFilter;
		this.magFilter = magFilter;
		bind();
		Micro.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_MIN_FILTER, minFilter.getGLEnum());
		Micro.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_MAG_FILTER, magFilter.getGLEnum());
	}
	
	public float unsafeSetAnisotropicFilter(float level) {
		return unsafeSetAnisotropicFilter(level, false);
	}
	
	public float unsafeSetAnisotropicFilter(float level, boolean force) {
		float max = getMaxAnisotropicFilterLevel();
		if (max == 1f)
			return 1f;
		level = Math.min(level, max);
		if (!force && MathUtils.isEqual(level, anisotropicFilterLevel, 0.1f))
			return anisotropicFilterLevel;
		Micro.gl20.glTexParameterf(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAX_ANISOTROPY_EXT, level);
		return anisotropicFilterLevel = level;
	}
	
	public float setAnisotropicFilter(float level) {
		float max = getMaxAnisotropicFilterLevel();
		if (max == 1f)
			return 1f;
		level = Math.min(level, max);
		if (MathUtils.isEqual(level, anisotropicFilterLevel, 0.1f))
			return level;
		bind();
		Micro.gl20.glTexParameterf(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAX_ANISOTROPY_EXT, level);
		return anisotropicFilterLevel = level;
	}
	
	public float getAnisotropicFilter() {
		return anisotropicFilterLevel;
	}
	
	public static float getMaxAnisotropicFilterLevel() {
		if (maxAnisotropicFilterLevel > 0)
			return maxAnisotropicFilterLevel;
		if (Micro.graphics.supportsExtension("GL_EXT_texture_filter_anisotropic")) {
			FloatBuffer buffer = Buffers.newFloatBuffer(16);
			((Buffer) buffer).position(0);
			((Buffer) buffer).limit(buffer.capacity());
			Micro.gl20.glGetFloatv(GL20.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, buffer);
			return maxAnisotropicFilterLevel = buffer.get(0);
		}
		return maxAnisotropicFilterLevel = 1f;
	}
	
	protected void delete() {
		if (glHandle != 0) {
			Micro.gl.glDeleteTexture(glHandle);
			glHandle = 0;
		}
	}
	
	@Override
	public void dispose() {
		delete();
	}
	
	protected static void uploadImageData(final int target, final TextureData data) {
		uploadImageData(target, data, 0);
	}
	
	public static void uploadImageData(final int target, final TextureData data, final int mipMapLevel) {
		if (data == null)
			return;
		if (!data.isPrepared())
			data.prepare();
		data.consume(target, mipMapLevel);
	}
	
}
