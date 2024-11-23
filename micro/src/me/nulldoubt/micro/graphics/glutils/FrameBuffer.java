package me.nulldoubt.micro.graphics.glutils;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.graphics.GL20;
import me.nulldoubt.micro.graphics.Pixmap;
import me.nulldoubt.micro.graphics.Texture;
import me.nulldoubt.micro.graphics.Texture.TextureFilter;
import me.nulldoubt.micro.graphics.Texture.TextureWrap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

public class FrameBuffer extends GLFrameBuffer<Texture> {
	
	private static final IntBuffer tempBuffer = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asIntBuffer();
	
	private final int[] viewport;
	private int previous;
	private boolean bound;
	
	FrameBuffer() {
		super();
		viewport = new int[4];
		previous = -1;
		bound = false;
	}
	
	protected FrameBuffer(GLFrameBufferBuilder<? extends GLFrameBuffer<Texture>> bufferBuilder) {
		this();
		this.bufferBuilder = bufferBuilder;
		build();
	}
	
	public FrameBuffer(final Pixmap.Format format, final int width, final int height) {
		this(format, width, height, false);
	}
	
	public FrameBuffer(final Pixmap.Format format, final int width, final int height, final boolean hasStencil) {
		this();
		FrameBufferBuilder frameBufferBuilder = new FrameBufferBuilder(width, height);
		frameBufferBuilder.addBasicColorTextureAttachment(format);
		if (hasStencil)
			frameBufferBuilder.addBasicStencilRenderBuffer();
		this.bufferBuilder = frameBufferBuilder;
		build();
	}
	
	@Override
	public void begin() {
		if (bound)
			throw new MicroRuntimeException("Buffer is already bound");
		bound = true;
		
		Micro.gl.glGetIntegerv(GL20.GL_FRAMEBUFFER_BINDING, tempBuffer);
		previous = tempBuffer.get(0);
		bind();
		
		Micro.gl.glGetIntegerv(GL20.GL_VIEWPORT, tempBuffer);
		viewport[0] = tempBuffer.get(0);
		viewport[1] = tempBuffer.get(1);
		viewport[2] = tempBuffer.get(2);
		viewport[3] = tempBuffer.get(3);
		setFrameBufferViewport();
	}
	
	@Override
	protected Texture createTexture(FrameBufferTextureAttachmentSpec attachmentSpec) {
		final GLOnlyTextureData data = new GLOnlyTextureData(bufferBuilder.width, bufferBuilder.height, 0, attachmentSpec.internalFormat, attachmentSpec.format, attachmentSpec.type);
		final Texture result = new Texture(data);
		result.setFilter(TextureFilter.Linear, TextureFilter.Linear);
		result.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
		return result;
	}
	
	@Override
	protected void disposeColorTexture(Texture colorTexture) {
		colorTexture.dispose();
	}
	
	@Override
	protected void attachFrameBufferColorTexture(Texture texture) {
		Micro.gl20.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_COLOR_ATTACHMENT0, GL20.GL_TEXTURE_2D, texture.getTextureObjectHandle(), 0);
	}
	
	@Override
	public void end() {
		end(viewport[0], viewport[1], viewport[2], viewport[3]);
	}
	
	@Override
	public void end(final int x, final int y, final int width, final int height) {
		if (!bound)
			throw new MicroRuntimeException("Buffer wasn't bound");
		bound = false;
		
		Micro.gl.glGetIntegerv(GL20.GL_FRAMEBUFFER_BINDING, tempBuffer);
		if (tempBuffer.get(0) != framebufferHandle)
			throw new MicroRuntimeException("Buffer handle doesn't match render order");
		
		Micro.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, previous);
		Micro.gl20.glViewport(x, y, width, height);
	}
	
	@Override
	protected void build() {
		Micro.gl.glGetIntegerv(GL20.GL_FRAMEBUFFER_BINDING, tempBuffer);
		super.build();
		Micro.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, tempBuffer.get(0));
	}
	
	public static void unbind() {
		GLFrameBuffer.unbind();
	}
	
}
