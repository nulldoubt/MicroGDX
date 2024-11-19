package me.nulldoubt.micro.graphics.glutils;

import me.nulldoubt.micro.Application;
import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.graphics.*;
import me.nulldoubt.micro.utils.*;
import me.nulldoubt.micro.utils.collections.Array;
import me.nulldoubt.micro.utils.collections.IntArray;

import java.lang.StringBuilder;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public abstract class GLFrameBuffer<T extends GLTexture> implements Disposable {
	
	protected final static Map<Application, Array<GLFrameBuffer<?>>> buffers = new HashMap<>();
	
	protected final static int GL_DEPTH24_STENCIL8_OES = 0x88F0;
	
	protected Array<T> textureAttachments = new Array<T>();
	
	protected static int defaultFramebufferHandle;
	protected static boolean defaultFramebufferHandleInitialized = false;
	
	protected int framebufferHandle;
	protected int depthbufferHandle;
	protected int stencilbufferHandle;
	protected int depthStencilPackedBufferHandle;
	protected boolean hasDepthStencilPackedBuffer;
	protected final IntArray colorBufferHandles = new IntArray();
	
	protected boolean isMRT;
	
	protected GLFrameBufferBuilder<? extends GLFrameBuffer<T>> bufferBuilder;
	
	private IntBuffer defaultDrawBuffers;
	
	GLFrameBuffer() {
	}
	
	/**
	 * Creates a GLFrameBuffer from the specifications provided by bufferBuilder
	 **/
	protected GLFrameBuffer(GLFrameBufferBuilder<? extends GLFrameBuffer<T>> bufferBuilder) {
		this.bufferBuilder = bufferBuilder;
		build();
	}
	
	/**
	 * Convenience method to return the first Texture attachment present in the fbo
	 **/
	public T getColorBufferTexture() {
		return textureAttachments.first();
	}
	
	/**
	 * Return the Texture attachments attached to the fbo
	 **/
	public Array<T> getTextureAttachments() {
		return textureAttachments;
	}
	
	/**
	 * Override this method in a derived class to set up the backing texture as you like.
	 */
	protected abstract T createTexture(FrameBufferTextureAttachmentSpec attachmentSpec);
	
	/**
	 * Override this method in a derived class to dispose the backing texture as you like.
	 */
	protected abstract void disposeColorTexture(T colorTexture);
	
	/**
	 * Override this method in a derived class to attach the backing texture to the GL framebuffer object.
	 */
	protected abstract void attachFrameBufferColorTexture(T texture);
	
	protected void build() {
		GL20 gl = Micro.gl20;
		
		checkValidBuilder();
		
		// iOS uses a different framebuffer handle! (not necessarily 0)
		if (!defaultFramebufferHandleInitialized) {
			defaultFramebufferHandleInitialized = true;
			defaultFramebufferHandle = 0;
		}
		
		framebufferHandle = gl.glGenFramebuffer();
		gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, framebufferHandle);
		
		int width = bufferBuilder.width;
		int height = bufferBuilder.height;
		
		if (bufferBuilder.hasDepthRenderBuffer) {
			depthbufferHandle = gl.glGenRenderbuffer();
			gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, depthbufferHandle);
			if (bufferBuilder.samples > 0) {
				Micro.gl31.glRenderbufferStorageMultisample(GL20.GL_RENDERBUFFER, bufferBuilder.samples,
						bufferBuilder.depthRenderBufferSpec.internalFormat, width, height);
			} else {
				gl.glRenderbufferStorage(GL20.GL_RENDERBUFFER, bufferBuilder.depthRenderBufferSpec.internalFormat, width, height);
			}
		}
		
		if (bufferBuilder.hasStencilRenderBuffer) {
			stencilbufferHandle = gl.glGenRenderbuffer();
			gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, stencilbufferHandle);
			if (bufferBuilder.samples > 0) {
				Micro.gl31.glRenderbufferStorageMultisample(GL20.GL_RENDERBUFFER, bufferBuilder.samples,
						bufferBuilder.stencilRenderBufferSpec.internalFormat, width, height);
			} else {
				gl.glRenderbufferStorage(GL20.GL_RENDERBUFFER, bufferBuilder.stencilRenderBufferSpec.internalFormat, width, height);
			}
		}
		
		if (bufferBuilder.hasPackedStencilDepthRenderBuffer) {
			depthStencilPackedBufferHandle = gl.glGenRenderbuffer();
			gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, depthStencilPackedBufferHandle);
			if (bufferBuilder.samples > 0) {
				Micro.gl31.glRenderbufferStorageMultisample(GL20.GL_RENDERBUFFER, bufferBuilder.samples,
						bufferBuilder.packedStencilDepthRenderBufferSpec.internalFormat, width, height);
			} else {
				gl.glRenderbufferStorage(GL20.GL_RENDERBUFFER, bufferBuilder.packedStencilDepthRenderBufferSpec.internalFormat, width,
						height);
			}
			hasDepthStencilPackedBuffer = true;
		}
		
		isMRT = bufferBuilder.textureAttachmentSpecs.size > 1;
		int colorAttachmentCounter = 0;
		if (isMRT) {
			for (FrameBufferTextureAttachmentSpec attachmentSpec : bufferBuilder.textureAttachmentSpecs) {
				T texture = createTexture(attachmentSpec);
				textureAttachments.add(texture);
				if (attachmentSpec.isColorTexture()) {
					gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0 + colorAttachmentCounter,
							GL30.GL_TEXTURE_2D, texture.getTextureObjectHandle(), 0);
					colorAttachmentCounter++;
				} else if (attachmentSpec.isDepth) {
					gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_DEPTH_ATTACHMENT, GL20.GL_TEXTURE_2D,
							texture.getTextureObjectHandle(), 0);
				} else if (attachmentSpec.isStencil) {
					gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_STENCIL_ATTACHMENT, GL20.GL_TEXTURE_2D,
							texture.getTextureObjectHandle(), 0);
				}
			}
		} else if (bufferBuilder.textureAttachmentSpecs.size > 0) {
			T texture = createTexture(bufferBuilder.textureAttachmentSpecs.first());
			textureAttachments.add(texture);
			gl.glBindTexture(texture.glTarget, texture.getTextureObjectHandle());
		}
		
		for (FrameBufferRenderBufferAttachmentSpec colorBufferSpec : bufferBuilder.colorRenderBufferSpecs) {
			int colorbufferHandle = gl.glGenRenderbuffer();
			gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, colorbufferHandle);
			if (bufferBuilder.samples > 0) {
				Micro.gl31.glRenderbufferStorageMultisample(GL20.GL_RENDERBUFFER, bufferBuilder.samples, colorBufferSpec.internalFormat,
						width, height);
			} else {
				gl.glRenderbufferStorage(GL20.GL_RENDERBUFFER, colorBufferSpec.internalFormat, width, height);
			}
			Micro.gl.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER, GL20.GL_COLOR_ATTACHMENT0 + colorAttachmentCounter,
					GL20.GL_RENDERBUFFER, colorbufferHandle);
			colorBufferHandles.add(colorbufferHandle);
			colorAttachmentCounter++;
		}
		
		if (isMRT || bufferBuilder.samples > 0) {
			defaultDrawBuffers = BufferUtils.newIntBuffer(colorAttachmentCounter);
			for (int i = 0; i < colorAttachmentCounter; i++) {
				defaultDrawBuffers.put(GL30.GL_COLOR_ATTACHMENT0 + i);
			}
			((Buffer) defaultDrawBuffers).position(0);
			Micro.gl30.glDrawBuffers(colorAttachmentCounter, defaultDrawBuffers);
		} else if (bufferBuilder.textureAttachmentSpecs.size > 0) {
			attachFrameBufferColorTexture(textureAttachments.first());
		}
		
		if (bufferBuilder.hasDepthRenderBuffer) {
			gl.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER, GL20.GL_DEPTH_ATTACHMENT, GL20.GL_RENDERBUFFER, depthbufferHandle);
		}
		
		if (bufferBuilder.hasStencilRenderBuffer) {
			gl.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER, GL20.GL_STENCIL_ATTACHMENT, GL20.GL_RENDERBUFFER, stencilbufferHandle);
		}
		
		if (bufferBuilder.hasPackedStencilDepthRenderBuffer) {
			gl.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL20.GL_RENDERBUFFER,
					depthStencilPackedBufferHandle);
		}
		
		gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, 0);
		for (T texture : textureAttachments) {
			gl.glBindTexture(texture.glTarget, 0);
		}
		
		int result = gl.glCheckFramebufferStatus(GL20.GL_FRAMEBUFFER);
		
		if (result == GL20.GL_FRAMEBUFFER_UNSUPPORTED && bufferBuilder.hasDepthRenderBuffer && bufferBuilder.hasStencilRenderBuffer
				&& (Micro.graphics.supportsExtension("GL_OES_packed_depth_stencil")
				|| Micro.graphics.supportsExtension("GL_EXT_packed_depth_stencil"))) {
			if (bufferBuilder.hasDepthRenderBuffer) {
				gl.glDeleteRenderbuffer(depthbufferHandle);
				depthbufferHandle = 0;
			}
			if (bufferBuilder.hasStencilRenderBuffer) {
				gl.glDeleteRenderbuffer(stencilbufferHandle);
				stencilbufferHandle = 0;
			}
			if (bufferBuilder.hasPackedStencilDepthRenderBuffer) {
				gl.glDeleteRenderbuffer(depthStencilPackedBufferHandle);
				depthStencilPackedBufferHandle = 0;
			}
			
			depthStencilPackedBufferHandle = gl.glGenRenderbuffer();
			hasDepthStencilPackedBuffer = true;
			gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, depthStencilPackedBufferHandle);
			if (bufferBuilder.samples > 0) {
				Micro.gl31.glRenderbufferStorageMultisample(GL20.GL_RENDERBUFFER, bufferBuilder.samples, GL_DEPTH24_STENCIL8_OES, width,
						height);
			} else {
				gl.glRenderbufferStorage(GL20.GL_RENDERBUFFER, GL_DEPTH24_STENCIL8_OES, width, height);
			}
			gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, 0);
			
			gl.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER, GL20.GL_DEPTH_ATTACHMENT, GL20.GL_RENDERBUFFER,
					depthStencilPackedBufferHandle);
			gl.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER, GL20.GL_STENCIL_ATTACHMENT, GL20.GL_RENDERBUFFER,
					depthStencilPackedBufferHandle);
			result = gl.glCheckFramebufferStatus(GL20.GL_FRAMEBUFFER);
		}
		
		gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, defaultFramebufferHandle);
		
		if (result != GL20.GL_FRAMEBUFFER_COMPLETE) {
			for (T texture : textureAttachments) {
				disposeColorTexture(texture);
			}
			
			if (hasDepthStencilPackedBuffer) {
				gl.glDeleteBuffer(depthStencilPackedBufferHandle);
			} else {
				if (bufferBuilder.hasDepthRenderBuffer)
					gl.glDeleteRenderbuffer(depthbufferHandle);
				if (bufferBuilder.hasStencilRenderBuffer)
					gl.glDeleteRenderbuffer(stencilbufferHandle);
			}
			
			gl.glDeleteFramebuffer(framebufferHandle);
			
			if (result == GL20.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT)
				throw new IllegalStateException("Frame buffer couldn't be constructed: incomplete attachment");
			if (result == GL20.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS)
				throw new IllegalStateException("Frame buffer couldn't be constructed: incomplete dimensions");
			if (result == GL20.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT)
				throw new IllegalStateException("Frame buffer couldn't be constructed: missing attachment");
			if (result == GL20.GL_FRAMEBUFFER_UNSUPPORTED)
				throw new IllegalStateException("Frame buffer couldn't be constructed: unsupported combination of formats");
			if (result == GL31.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE)
				throw new IllegalStateException("Frame buffer couldn't be constructed: multisample mismatch");
			throw new IllegalStateException("Frame buffer couldn't be constructed: unknown error " + result);
		}
		
		addManagedFrameBuffer(Micro.app, this);
	}
	
	private void checkValidBuilder() {
		
		if (bufferBuilder.samples > 0 && !Micro.graphics.isGL31Available()) {
			throw new MicroRuntimeException("Framebuffer multisample requires GLES 3.1+");
		}
		if (bufferBuilder.samples > 0 && bufferBuilder.textureAttachmentSpecs.size > 0) {
			throw new MicroRuntimeException("Framebuffer multisample with texture attachments not yet supported");
		}
		
		boolean runningGL30 = Micro.graphics.isGL30Available();
		
		if (!runningGL30) {
			final boolean supportsPackedDepthStencil = Micro.graphics.supportsExtension("GL_OES_packed_depth_stencil")
					|| Micro.graphics.supportsExtension("GL_EXT_packed_depth_stencil");
			
			if (bufferBuilder.hasPackedStencilDepthRenderBuffer && !supportsPackedDepthStencil) {
				throw new MicroRuntimeException("Packed Stencil/Render render buffers are not available on GLES 2.0");
			}
			if (bufferBuilder.textureAttachmentSpecs.size > 1) {
				throw new MicroRuntimeException("Multiple render targets not available on GLES 2.0");
			}
			for (FrameBufferTextureAttachmentSpec spec : bufferBuilder.textureAttachmentSpecs) {
				if (spec.isDepth)
					throw new MicroRuntimeException("Depth texture FrameBuffer Attachment not available on GLES 2.0");
				if (spec.isStencil)
					throw new MicroRuntimeException("Stencil texture FrameBuffer Attachment not available on GLES 2.0");
				if (spec.isFloat) {
					if (!Micro.graphics.supportsExtension("OES_texture_float")) {
						throw new MicroRuntimeException("Float texture FrameBuffer Attachment not available on GLES 2.0");
					}
				}
			}
		}
		
		if (bufferBuilder.hasPackedStencilDepthRenderBuffer) {
			if (bufferBuilder.hasDepthRenderBuffer || bufferBuilder.hasStencilRenderBuffer)
				throw new MicroRuntimeException(
						"Frame buffer couldn't be constructed: packed stencil depth buffer cannot be specified together with separated depth or stencil buffer");
		}
	}
	
	/**
	 * Releases all resources associated with the FrameBuffer.
	 */
	@Override
	public void dispose() {
		GL20 gl = Micro.gl20;
		
		for (T texture : textureAttachments) {
			disposeColorTexture(texture);
		}
		
		gl.glDeleteRenderbuffer(depthStencilPackedBufferHandle);
		gl.glDeleteRenderbuffer(depthbufferHandle);
		gl.glDeleteRenderbuffer(stencilbufferHandle);
		
		gl.glDeleteFramebuffer(framebufferHandle);
		
		if (buffers.get(Micro.app) != null)
			buffers.get(Micro.app).removeValue(this, true);
	}
	
	/**
	 * Makes the frame buffer current so everything gets drawn to it.
	 */
	public void bind() {
		Micro.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, framebufferHandle);
	}
	
	/**
	 * Unbinds the framebuffer, all drawing will be performed to the normal framebuffer from here on.
	 */
	public static void unbind() {
		Micro.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, defaultFramebufferHandle);
	}
	
	/**
	 * Binds the frame buffer and sets the viewport accordingly, so everything gets drawn to it.
	 */
	public void begin() {
		bind();
		setFrameBufferViewport();
	}
	
	/**
	 * Sets viewport to the dimensions of framebuffer. Called by {@link #begin()}.
	 */
	protected void setFrameBufferViewport() {
		Micro.gl20.glViewport(0, 0, bufferBuilder.width, bufferBuilder.height);
	}
	
	/**
	 * Unbinds the framebuffer, all drawing will be performed to the normal framebuffer from here on.
	 */
	public void end() {
		end(0, 0, Micro.graphics.getBackBufferWidth(), Micro.graphics.getBackBufferHeight());
	}
	
	/**
	 * Unbinds the framebuffer and sets viewport sizes, all drawing will be performed to the normal framebuffer from here on.
	 *
	 * @param x      the x-axis position of the viewport in pixels
	 * @param y      the y-asis position of the viewport in pixels
	 * @param width  the width of the viewport in pixels
	 * @param height the height of the viewport in pixels
	 */
	public void end(int x, int y, int width, int height) {
		unbind();
		Micro.gl20.glViewport(x, y, width, height);
	}
	
	static final IntBuffer singleInt = BufferUtils.newIntBuffer(1);
	
	/**
	 * Transfer pixels from this frame buffer to the destination frame buffer. Usually used when using multisample, it resolves
	 * samples from this multisample FBO to a non-multisample as destination in order to be used as textures. This is a convenient
	 * method that automatically choose which of stencil, depth, and colors buffers attachment to be copied.
	 *
	 * @param destination the destination of the copy.
	 */
	public void transfer(GLFrameBuffer<T> destination) {
		
		int copyBits = 0;
		for (FrameBufferTextureAttachmentSpec attachment : destination.bufferBuilder.textureAttachmentSpecs) {
			if (attachment.isDepth && (bufferBuilder.hasDepthRenderBuffer || bufferBuilder.hasPackedStencilDepthRenderBuffer)) {
				copyBits |= GL20.GL_DEPTH_BUFFER_BIT;
			} else if (attachment.isStencil
					&& (bufferBuilder.hasStencilRenderBuffer || bufferBuilder.hasPackedStencilDepthRenderBuffer)) {
				copyBits |= GL20.GL_STENCIL_BUFFER_BIT;
			} else if (colorBufferHandles.size > 0) {
				copyBits |= GL20.GL_COLOR_BUFFER_BIT;
			}
		}
		
		transfer(destination, copyBits);
	}
	
	/**
	 * Transfer pixels from this frame buffer to the destination frame buffer. Usually used when using multisample, it resolves
	 * samples from this multisample FBO to a non-multisample as destination in order to be used as textures.
	 *
	 * @param destination the destination of the copy (should be same size as this frame buffer).
	 * @param copyBits    combination of GL20.GL_COLOR_BUFFER_BIT, GL20.GL_STENCIL_BUFFER_BIT, and GL20.GL_DEPTH_BUFFER_BIT. When
	 *                    GL20.GL_COLOR_BUFFER_BIT is present, every color buffers will be copied to each corresponding color texture
	 *                    buffers in the destination framebuffer.
	 */
	public void transfer(GLFrameBuffer<T> destination, int copyBits) {
		
		if (destination.getWidth() != getWidth() || destination.getHeight() != getHeight()) {
			throw new IllegalArgumentException("source and destination frame buffers must have same size.");
		}
		
		Micro.gl.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, framebufferHandle);
		Micro.gl.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, destination.framebufferHandle);
		
		int colorBufferIndex = 0;
		int attachmentIndex = 0;
		for (FrameBufferTextureAttachmentSpec attachment : destination.bufferBuilder.textureAttachmentSpecs) {
			if (attachment.isColorTexture()) {
				Micro.gl30.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0 + colorBufferIndex);
				
				singleInt.clear();
				singleInt.put(GL30.GL_COLOR_ATTACHMENT0 + attachmentIndex);
				singleInt.flip();
				Micro.gl30.glDrawBuffers(1, singleInt);
				
				Micro.gl30.glBlitFramebuffer(0, 0, getWidth(), getHeight(), 0, 0, destination.getWidth(), destination.getHeight(),
						copyBits, GL20.GL_NEAREST);
				
				copyBits = GL20.GL_COLOR_BUFFER_BIT;
				colorBufferIndex++;
			}
			attachmentIndex++;
		}
		// case of depth and/or stencil only
		if (copyBits != GL20.GL_COLOR_BUFFER_BIT) {
			Micro.gl30.glBlitFramebuffer(0, 0, getWidth(), getHeight(), 0, 0, destination.getWidth(), destination.getHeight(),
					copyBits, GL20.GL_NEAREST);
		}
		
		// restore draw buffers for destination (in case of MRT only)
		if (destination.defaultDrawBuffers != null) {
			Micro.gl30.glDrawBuffers(destination.defaultDrawBuffers.limit(), destination.defaultDrawBuffers);
		}
		
		Micro.gl.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
		Micro.gl.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
	}
	
	/**
	 * @return The OpenGL handle of the framebuffer (see {@link GL20#glGenFramebuffer()})
	 */
	public int getFramebufferHandle() {
		return framebufferHandle;
	}
	
	/**
	 * @return The OpenGL handle of the (optional) depth buffer (see {@link GL20#glGenRenderbuffer()}). May return 0 even if depth
	 * buffer enabled
	 */
	public int getDepthBufferHandle() {
		return depthbufferHandle;
	}
	
	/**
	 * @param n index of the color buffer as added to the frame buffer builder.
	 * @return The OpenGL handle of a color buffer (see {@link GL20#glGenRenderbuffer()}).
	 **/
	public int getColorBufferHandle(int n) {
		return colorBufferHandles.get(n);
	}
	
	/**
	 * @return The OpenGL handle of the (optional) stencil buffer (see {@link GL20#glGenRenderbuffer()}). May return 0 even if
	 * stencil buffer enabled
	 */
	public int getStencilBufferHandle() {
		return stencilbufferHandle;
	}
	
	/**
	 * @return The OpenGL handle of the packed depth & stencil buffer (GL_DEPTH24_STENCIL8_OES) or 0 if not used.
	 **/
	protected int getDepthStencilPackedBuffer() {
		return depthStencilPackedBufferHandle;
	}
	
	public int getHeight() {
		return bufferBuilder.height;
	}
	
	public int getWidth() {
		return bufferBuilder.width;
	}
	
	private static void addManagedFrameBuffer(final Application application, final GLFrameBuffer<?> frameBuffer) {
		Array<GLFrameBuffer<?>> managedResources = buffers.get(application);
		if (managedResources == null)
			managedResources = new Array<>();
		managedResources.add(frameBuffer);
		buffers.put(application, managedResources);
	}
	
	public static void invalidateAllFrameBuffers(Application app) {
		if (Micro.gl20 == null)
			return;
		final Array<GLFrameBuffer<?>> bufferArray = buffers.get(app);
		if (bufferArray == null)
			return;
		for (int i = 0; i < bufferArray.size; i++)
			bufferArray.get(i).build();
	}
	
	public static void clearAllFrameBuffers(Application app) {
		buffers.remove(app);
	}
	
	public static StringBuilder getManagedStatus(final StringBuilder builder) {
		builder.append("Managed buffers/app: { ");
		for (Application app : buffers.keySet()) {
			builder.append(buffers.get(app).size);
			builder.append(" ");
		}
		builder.append("}");
		return builder;
	}
	
	public static String getManagedStatus() {
		return getManagedStatus(new StringBuilder()).toString();
	}
	
	protected static class FrameBufferTextureAttachmentSpec {
		
		int internalFormat, format, type;
		boolean isFloat, isGpuOnly;
		boolean isDepth;
		boolean isStencil;
		
		public FrameBufferTextureAttachmentSpec(int internalformat, int format, int type) {
			this.internalFormat = internalformat;
			this.format = format;
			this.type = type;
		}
		
		public boolean isColorTexture() {
			return !isDepth && !isStencil;
		}
		
	}
	
	protected static class FrameBufferRenderBufferAttachmentSpec {
		
		int internalFormat;
		
		public FrameBufferRenderBufferAttachmentSpec(int internalFormat) {
			this.internalFormat = internalFormat;
		}
		
	}
	
	public static abstract class GLFrameBufferBuilder<U extends GLFrameBuffer<? extends GLTexture>> {
		
		protected int width, height, samples;
		
		protected Array<FrameBufferTextureAttachmentSpec> textureAttachmentSpecs = new Array<FrameBufferTextureAttachmentSpec>();
		protected Array<FrameBufferRenderBufferAttachmentSpec> colorRenderBufferSpecs = new Array<FrameBufferRenderBufferAttachmentSpec>();
		
		protected FrameBufferRenderBufferAttachmentSpec stencilRenderBufferSpec;
		protected FrameBufferRenderBufferAttachmentSpec depthRenderBufferSpec;
		protected FrameBufferRenderBufferAttachmentSpec packedStencilDepthRenderBufferSpec;
		
		protected boolean hasStencilRenderBuffer;
		protected boolean hasDepthRenderBuffer;
		protected boolean hasPackedStencilDepthRenderBuffer;
		
		public GLFrameBufferBuilder(int width, int height) {
			this(width, height, 0);
		}
		
		public GLFrameBufferBuilder(int width, int height, int samples) {
			this.width = width;
			this.height = height;
			this.samples = samples;
		}
		
		public GLFrameBufferBuilder<U> addColorTextureAttachment(int internalFormat, int format, int type) {
			textureAttachmentSpecs.add(new FrameBufferTextureAttachmentSpec(internalFormat, format, type));
			return this;
		}
		
		public GLFrameBufferBuilder<U> addBasicColorTextureAttachment(Pixmap.Format format) {
			int glFormat = Pixmap.Format.toGlFormat(format);
			int glType = Pixmap.Format.toGlType(format);
			return addColorTextureAttachment(glFormat, glFormat, glType);
		}
		
		public GLFrameBufferBuilder<U> addFloatAttachment(int internalFormat, int format, int type, boolean gpuOnly) {
			FrameBufferTextureAttachmentSpec spec = new FrameBufferTextureAttachmentSpec(internalFormat, format, type);
			spec.isFloat = true;
			spec.isGpuOnly = gpuOnly;
			textureAttachmentSpecs.add(spec);
			return this;
		}
		
		public GLFrameBufferBuilder<U> addDepthTextureAttachment(int internalFormat, int type) {
			FrameBufferTextureAttachmentSpec spec = new FrameBufferTextureAttachmentSpec(internalFormat, GL30.GL_DEPTH_COMPONENT,
					type);
			spec.isDepth = true;
			textureAttachmentSpecs.add(spec);
			return this;
		}
		
		public GLFrameBufferBuilder<U> addStencilTextureAttachment(int internalFormat, int type) {
			FrameBufferTextureAttachmentSpec spec = new FrameBufferTextureAttachmentSpec(internalFormat, GL30.GL_STENCIL_ATTACHMENT,
					type);
			spec.isStencil = true;
			textureAttachmentSpecs.add(spec);
			return this;
		}
		
		public GLFrameBufferBuilder<U> addDepthRenderBuffer(int internalFormat) {
			depthRenderBufferSpec = new FrameBufferRenderBufferAttachmentSpec(internalFormat);
			hasDepthRenderBuffer = true;
			return this;
		}
		
		public GLFrameBufferBuilder<U> addColorRenderBuffer(int internalFormat) {
			colorRenderBufferSpecs.add(new FrameBufferRenderBufferAttachmentSpec(internalFormat));
			return this;
		}
		
		public GLFrameBufferBuilder<U> addStencilRenderBuffer(int internalFormat) {
			stencilRenderBufferSpec = new FrameBufferRenderBufferAttachmentSpec(internalFormat);
			hasStencilRenderBuffer = true;
			return this;
		}
		
		public GLFrameBufferBuilder<U> addStencilDepthPackedRenderBuffer(int internalFormat) {
			packedStencilDepthRenderBufferSpec = new FrameBufferRenderBufferAttachmentSpec(internalFormat);
			hasPackedStencilDepthRenderBuffer = true;
			return this;
		}
		
		public GLFrameBufferBuilder<U> addBasicDepthRenderBuffer() {
			return addDepthRenderBuffer(GL20.GL_DEPTH_COMPONENT16);
		}
		
		public GLFrameBufferBuilder<U> addBasicStencilRenderBuffer() {
			return addStencilRenderBuffer(GL20.GL_STENCIL_INDEX8);
		}
		
		public GLFrameBufferBuilder<U> addBasicStencilDepthPackedRenderBuffer() {
			return addStencilDepthPackedRenderBuffer(GL30.GL_DEPTH24_STENCIL8);
		}
		
		public abstract U build();
		
	}
	
	public static class FrameBufferBuilder extends GLFrameBufferBuilder<FrameBuffer> {
		
		public FrameBufferBuilder(int width, int height) {
			super(width, height);
		}
		
		public FrameBufferBuilder(int width, int height, int samples) {
			super(width, height, samples);
		}
		
		@Override
		public FrameBuffer build() {
			return new FrameBuffer(this);
		}
		
	}
	
}
