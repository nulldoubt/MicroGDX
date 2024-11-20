package me.nulldoubt.micro.graphics.glutils;

import me.nulldoubt.micro.Application;
import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.graphics.*;
import me.nulldoubt.micro.utils.BufferUtils;
import me.nulldoubt.micro.utils.Disposable;
import me.nulldoubt.micro.utils.collections.Array;
import me.nulldoubt.micro.utils.collections.IntArray;

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
	protected int stencilbufferHandle;
	
	protected final IntArray colorBufferHandles = new IntArray();
	protected boolean isMRT;
	
	protected GLFrameBufferBuilder<? extends GLFrameBuffer<T>> bufferBuilder;
	private IntBuffer defaultDrawBuffers;
	
	GLFrameBuffer() {}
	
	protected GLFrameBuffer(GLFrameBufferBuilder<? extends GLFrameBuffer<T>> bufferBuilder) {
		this.bufferBuilder = bufferBuilder;
		build();
	}
	
	public T getColorBufferTexture() {
		return textureAttachments.first();
	}
	
	public Array<T> getTextureAttachments() {
		return textureAttachments;
	}
	
	protected abstract T createTexture(FrameBufferTextureAttachmentSpec attachmentSpec);
	
	protected abstract void disposeColorTexture(T colorTexture);
	
	protected abstract void attachFrameBufferColorTexture(T texture);
	
	protected void build() {
		GL20 gl = Micro.gl20;
		
		checkValidBuilder();
		
		if (!defaultFramebufferHandleInitialized) {
			defaultFramebufferHandleInitialized = true;
			defaultFramebufferHandle = 0;
		}
		
		framebufferHandle = gl.glGenFramebuffer();
		gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, framebufferHandle);
		
		int width = bufferBuilder.width;
		int height = bufferBuilder.height;
		
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
		
		isMRT = bufferBuilder.textureAttachmentSpecs.size > 1;
		int colorAttachmentCounter = 0;
		if (isMRT) {
			for (FrameBufferTextureAttachmentSpec attachmentSpec : bufferBuilder.textureAttachmentSpecs) {
				T texture = createTexture(attachmentSpec);
				textureAttachments.add(texture);
				if (attachmentSpec.isColorTexture()) {
					gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0 + colorAttachmentCounter, GL30.GL_TEXTURE_2D, texture.getTextureObjectHandle(), 0);
					colorAttachmentCounter++;
				} else if (attachmentSpec.isStencil) {
					gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_STENCIL_ATTACHMENT, GL20.GL_TEXTURE_2D, texture.getTextureObjectHandle(), 0);
				}
			}
		} else if (bufferBuilder.textureAttachmentSpecs.size > 0) {
			T texture = createTexture(bufferBuilder.textureAttachmentSpecs.first());
			textureAttachments.add(texture);
			gl.glBindTexture(texture.glTarget, texture.getTextureObjectHandle());
		}
		
		for (FrameBufferRenderBufferAttachmentSpec colorBufferSpec : bufferBuilder.colorRenderBufferSpecs) {
			final int colorBufferHandle = gl.glGenRenderbuffer();
			gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, colorBufferHandle);
			if (bufferBuilder.samples > 0)
				Micro.gl31.glRenderbufferStorageMultisample(GL20.GL_RENDERBUFFER, bufferBuilder.samples, colorBufferSpec.internalFormat, width, height);
			else
				gl.glRenderbufferStorage(GL20.GL_RENDERBUFFER, colorBufferSpec.internalFormat, width, height);
			Micro.gl.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER, GL20.GL_COLOR_ATTACHMENT0 + colorAttachmentCounter, GL20.GL_RENDERBUFFER, colorBufferHandle);
			colorBufferHandles.add(colorBufferHandle);
			colorAttachmentCounter++;
		}
		
		if (isMRT || bufferBuilder.samples > 0) {
			defaultDrawBuffers = BufferUtils.newIntBuffer(colorAttachmentCounter);
			for (int i = 0; i < colorAttachmentCounter; i++)
				defaultDrawBuffers.put(GL30.GL_COLOR_ATTACHMENT0 + i);
			((Buffer) defaultDrawBuffers).position(0);
			Micro.gl30.glDrawBuffers(colorAttachmentCounter, defaultDrawBuffers);
		} else if (bufferBuilder.textureAttachmentSpecs.size > 0) {
			attachFrameBufferColorTexture(textureAttachments.first());
		}
		
		if (bufferBuilder.hasStencilRenderBuffer) {
			gl.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER, GL20.GL_STENCIL_ATTACHMENT, GL20.GL_RENDERBUFFER, stencilbufferHandle);
		}
		
		gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, 0);
		for (T texture : textureAttachments)
			gl.glBindTexture(texture.glTarget, 0);
		
		int result = gl.glCheckFramebufferStatus(GL20.GL_FRAMEBUFFER);
		if (result == GL20.GL_FRAMEBUFFER_UNSUPPORTED && bufferBuilder.hasStencilRenderBuffer && (Micro.graphics.supportsExtension("GL_OES_packed_depth_stencil") || Micro.graphics.supportsExtension("GL_EXT_packed_depth_stencil"))) {
			if (bufferBuilder.hasStencilRenderBuffer) {
				gl.glDeleteRenderbuffer(stencilbufferHandle);
				stencilbufferHandle = 0;
			}
			
			if (bufferBuilder.samples > 0)
				Micro.gl31.glRenderbufferStorageMultisample(GL20.GL_RENDERBUFFER, bufferBuilder.samples, GL_DEPTH24_STENCIL8_OES, width, height);
			else
				gl.glRenderbufferStorage(GL20.GL_RENDERBUFFER, GL_DEPTH24_STENCIL8_OES, width, height);
			
			gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, 0);
			result = gl.glCheckFramebufferStatus(GL20.GL_FRAMEBUFFER);
		}
		
		gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, defaultFramebufferHandle);
		
		if (result != GL20.GL_FRAMEBUFFER_COMPLETE) {
			for (T texture : textureAttachments)
				disposeColorTexture(texture);
			if (bufferBuilder.hasStencilRenderBuffer)
				gl.glDeleteRenderbuffer(stencilbufferHandle);
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
		
		if (bufferBuilder.samples > 0 && !Micro.graphics.isGL31Available())
			throw new MicroRuntimeException("Framebuffer multisample requires GLES 3.1+");
		if (bufferBuilder.samples > 0 && bufferBuilder.textureAttachmentSpecs.size > 0)
			throw new MicroRuntimeException("Framebuffer multisample with texture attachments not yet supported");
		
		boolean runningGL30 = Micro.graphics.isGL30Available();
		
		if (!runningGL30) {
			if (bufferBuilder.textureAttachmentSpecs.size > 1)
				throw new MicroRuntimeException("Multiple render targets not available on GLES 2.0");
			for (FrameBufferTextureAttachmentSpec spec : bufferBuilder.textureAttachmentSpecs) {
				if (spec.isStencil)
					throw new MicroRuntimeException("Stencil texture FrameBuffer Attachment not available on GLES 2.0");
				if (spec.isFloat)
					if (!Micro.graphics.supportsExtension("OES_texture_float"))
						throw new MicroRuntimeException("Float texture FrameBuffer Attachment not available on GLES 2.0");
			}
		}
	}
	
	@Override
	public void dispose() {
		final GL20 gl = Micro.gl20;
		
		for (T texture : textureAttachments)
			disposeColorTexture(texture);
		
		gl.glDeleteRenderbuffer(stencilbufferHandle);
		gl.glDeleteFramebuffer(framebufferHandle);
		
		if (buffers.get(Micro.app) != null)
			buffers.get(Micro.app).removeValue(this, true);
	}
	
	public void bind() {
		Micro.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, framebufferHandle);
	}
	
	public static void unbind() {
		Micro.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, defaultFramebufferHandle);
	}
	
	public void begin() {
		bind();
		setFrameBufferViewport();
	}
	
	protected void setFrameBufferViewport() {
		Micro.gl20.glViewport(0, 0, bufferBuilder.width, bufferBuilder.height);
	}
	
	public void end() {
		end(0, 0, Micro.graphics.getBackBufferWidth(), Micro.graphics.getBackBufferHeight());
	}
	
	public void end(int x, int y, int width, int height) {
		unbind();
		Micro.gl20.glViewport(x, y, width, height);
	}
	
	static final IntBuffer singleInt = BufferUtils.newIntBuffer(1);
	
	public void transfer(GLFrameBuffer<T> destination) {
		int copyBits = 0;
		for (final FrameBufferTextureAttachmentSpec attachment : destination.bufferBuilder.textureAttachmentSpecs) {
			if (attachment.isStencil && bufferBuilder.hasStencilRenderBuffer)
				copyBits |= GL20.GL_STENCIL_BUFFER_BIT;
			else if (colorBufferHandles.size > 0)
				copyBits |= GL20.GL_COLOR_BUFFER_BIT;
		}
		
		transfer(destination, copyBits);
	}
	
	public void transfer(GLFrameBuffer<T> destination, int copyBits) {
		
		if (destination.getWidth() != getWidth() || destination.getHeight() != getHeight())
			throw new IllegalArgumentException("source and destination frame buffers must have same size.");
		
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
	
	public int getFramebufferHandle() {
		return framebufferHandle;
	}
	
	public int getColorBufferHandle(int n) {
		return colorBufferHandles.get(n);
	}
	
	public int getStencilBufferHandle() {
		return stencilbufferHandle;
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
		boolean isStencil;
		
		public FrameBufferTextureAttachmentSpec(int internalformat, int format, int type) {
			this.internalFormat = internalformat;
			this.format = format;
			this.type = type;
		}
		
		public boolean isColorTexture() {
			return !isStencil;
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
		
		protected Array<FrameBufferTextureAttachmentSpec> textureAttachmentSpecs = new Array<>();
		protected Array<FrameBufferRenderBufferAttachmentSpec> colorRenderBufferSpecs = new Array<>();
		
		protected FrameBufferRenderBufferAttachmentSpec stencilRenderBufferSpec;
		protected boolean hasStencilRenderBuffer;
		
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
		
		public GLFrameBufferBuilder<U> addFloatAttachment(int internalFormat, int format, int type, boolean gpuOnly) {
			FrameBufferTextureAttachmentSpec spec = new FrameBufferTextureAttachmentSpec(internalFormat, format, type);
			spec.isFloat = true;
			spec.isGpuOnly = gpuOnly;
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
		
		public GLFrameBufferBuilder<U> addColorRenderBuffer(int internalFormat) {
			colorRenderBufferSpecs.add(new FrameBufferRenderBufferAttachmentSpec(internalFormat));
			return this;
		}
		
		public GLFrameBufferBuilder<U> addStencilRenderBuffer(int internalFormat) {
			stencilRenderBufferSpec = new FrameBufferRenderBufferAttachmentSpec(internalFormat);
			hasStencilRenderBuffer = true;
			return this;
		}
		
		public GLFrameBufferBuilder<U> addBasicColorTextureAttachment(Pixmap.Format format) {
			int glFormat = Pixmap.Format.toGlFormat(format);
			int glType = Pixmap.Format.toGlType(format);
			return addColorTextureAttachment(glFormat, glFormat, glType);
		}
		
		public GLFrameBufferBuilder<U> addBasicStencilRenderBuffer() {
			return addStencilRenderBuffer(GL20.GL_STENCIL_INDEX8);
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
