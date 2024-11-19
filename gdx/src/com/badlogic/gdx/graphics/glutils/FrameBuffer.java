package com.badlogic.gdx.graphics.glutils;

import com.badlogic.gdx.Micro;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;

public class FrameBuffer extends GLFrameBuffer<Texture> {
	
	FrameBuffer() {}
	
	protected FrameBuffer(GLFrameBufferBuilder<? extends GLFrameBuffer<Texture>> bufferBuilder) {
		super(bufferBuilder);
	}
	
	public FrameBuffer(final Pixmap.Format format, final int width, final int height, final boolean hasDepth) {
		this(format, width, height, hasDepth, false);
	}
	
	public FrameBuffer(final Pixmap.Format format, final int width, final int height, final boolean hasDepth, final boolean hasStencil) {
		FrameBufferBuilder frameBufferBuilder = new FrameBufferBuilder(width, height);
		frameBufferBuilder.addBasicColorTextureAttachment(format);
		if (hasDepth)
			frameBufferBuilder.addBasicDepthRenderBuffer();
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
