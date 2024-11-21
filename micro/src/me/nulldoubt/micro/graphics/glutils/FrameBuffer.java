package me.nulldoubt.micro.graphics.glutils;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.graphics.GL20;
import me.nulldoubt.micro.graphics.Pixmap;
import me.nulldoubt.micro.graphics.Texture;
import me.nulldoubt.micro.graphics.Texture.TextureFilter;
import me.nulldoubt.micro.graphics.Texture.TextureWrap;

public class FrameBuffer extends GLFrameBuffer<Texture> {
	
	FrameBuffer() {}
	
	protected FrameBuffer(GLFrameBufferBuilder<? extends GLFrameBuffer<Texture>> bufferBuilder) {
		super(bufferBuilder);
	}
	
	public FrameBuffer(final Pixmap.Format format, final int width, final int height) {
		this(format, width, height, false);
	}
	
	public FrameBuffer(final Pixmap.Format format, final int width, final int height, final boolean hasStencil) {
		FrameBufferBuilder frameBufferBuilder = new FrameBufferBuilder(width, height);
		frameBufferBuilder.addBasicColorTextureAttachment(format);
		if (hasStencil)
			frameBufferBuilder.addBasicStencilRenderBuffer();
		this.bufferBuilder = frameBufferBuilder;
		
		build();
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
	
	public static void unbind() {
		GLFrameBuffer.unbind();
	}
	
}
