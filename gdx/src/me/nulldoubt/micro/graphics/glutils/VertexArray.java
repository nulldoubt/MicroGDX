package me.nulldoubt.micro.graphics.glutils;

import me.nulldoubt.micro.graphics.GL20;
import me.nulldoubt.micro.graphics.VertexAttribute;
import me.nulldoubt.micro.graphics.VertexAttributes;
import me.nulldoubt.micro.utils.Buffers;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class VertexArray implements VertexData {
	
	final VertexAttributes attributes;
	final FloatBuffer buffer;
	final ByteBuffer byteBuffer;
	boolean isBound = false;
	
	/**
	 * Constructs a new interleaved VertexArray
	 *
	 * @param numVertices the maximum number of vertices
	 * @param attributes  the {@link VertexAttribute}s
	 */
	public VertexArray(int numVertices, VertexAttribute... attributes) {
		this(numVertices, new VertexAttributes(attributes));
	}
	
	/**
	 * Constructs a new interleaved VertexArray
	 *
	 * @param numVertices the maximum number of vertices
	 * @param attributes  the {@link VertexAttributes}
	 */
	public VertexArray(int numVertices, VertexAttributes attributes) {
		this.attributes = attributes;
		byteBuffer = Buffers.newUnsafeByteBuffer(this.attributes.vertexSize * numVertices);
		buffer = byteBuffer.asFloatBuffer();
		((Buffer) buffer).flip();
		((Buffer) byteBuffer).flip();
	}
	
	@Override
	public void dispose() {
		Buffers.disposeUnsafeByteBuffer(byteBuffer);
	}
	
	@Override
	public FloatBuffer getBuffer(boolean forWriting) {
		return buffer;
	}
	
	@Override
	public int getNumVertices() {
		return buffer.limit() * 4 / attributes.vertexSize;
	}
	
	public int getNumMaxVertices() {
		return byteBuffer.capacity() / attributes.vertexSize;
	}
	
	@Override
	public void setVertices(float[] vertices, int offset, int count) {
		Buffers.copy(vertices, byteBuffer, count, offset);
		((Buffer) buffer).position(0);
		((Buffer) buffer).limit(count);
	}
	
	@Override
	public void updateVertices(int targetOffset, float[] vertices, int sourceOffset, int count) {
		final int pos = byteBuffer.position();
		((Buffer) byteBuffer).position(targetOffset * 4);
		Buffers.copy(vertices, sourceOffset, count, byteBuffer);
		((Buffer) byteBuffer).position(pos);
	}
	
	@Override
	public void bind(final Shader shader) {
		bind(shader, null);
	}
	
	@Override
	public void bind(final Shader shader, final int[] locations) {
		final int numAttributes = attributes.size();
		((Buffer) byteBuffer).limit(buffer.limit() * 4);
		if (locations == null) {
			for (int i = 0; i < numAttributes; i++) {
				final VertexAttribute attribute = attributes.get(i);
				final int location = shader.getAttributeLocation(attribute.alias);
				if (location < 0)
					continue;
				shader.enableVertexAttribute(location);
				
				if (attribute.type == GL20.GL_FLOAT) {
					((Buffer) buffer).position(attribute.offset / 4);
					shader.setVertexAttribute(location, attribute.numComponents, attribute.type, attribute.normalized,
							attributes.vertexSize, buffer);
				} else {
					((Buffer) byteBuffer).position(attribute.offset);
					shader.setVertexAttribute(location, attribute.numComponents, attribute.type, attribute.normalized,
							attributes.vertexSize, byteBuffer);
				}
			}
		} else {
			for (int i = 0; i < numAttributes; i++) {
				final VertexAttribute attribute = attributes.get(i);
				final int location = locations[i];
				if (location < 0)
					continue;
				shader.enableVertexAttribute(location);
				
				if (attribute.type == GL20.GL_FLOAT) {
					((Buffer) buffer).position(attribute.offset / 4);
					shader.setVertexAttribute(location, attribute.numComponents, attribute.type, attribute.normalized,
							attributes.vertexSize, buffer);
				} else {
					((Buffer) byteBuffer).position(attribute.offset);
					shader.setVertexAttribute(location, attribute.numComponents, attribute.type, attribute.normalized,
							attributes.vertexSize, byteBuffer);
				}
			}
		}
		isBound = true;
	}
	
	/**
	 * Unbinds this VertexBufferObject.
	 *
	 * @param shader the shader
	 */
	@Override
	public void unbind(Shader shader) {
		unbind(shader, null);
	}
	
	@Override
	public void unbind(Shader shader, int[] locations) {
		final int numAttributes = attributes.size();
		if (locations == null) {
			for (int i = 0; i < numAttributes; i++) {
				shader.disableVertexAttribute(attributes.get(i).alias);
			}
		} else {
			for (int i = 0; i < numAttributes; i++) {
				final int location = locations[i];
				if (location >= 0)
					shader.disableVertexAttribute(location);
			}
		}
		isBound = false;
	}
	
	@Override
	public VertexAttributes getAttributes() {
		return attributes;
	}
	
	@Override
	public void invalidate() {
	}
	
}
