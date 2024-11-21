package me.nulldoubt.micro.graphics.glutils;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.graphics.GL20;
import me.nulldoubt.micro.graphics.VertexAttribute;
import me.nulldoubt.micro.graphics.VertexAttributes;
import me.nulldoubt.micro.utils.Buffers;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class VertexBufferObject implements VertexData {
	
	private VertexAttributes attributes;
	private FloatBuffer buffer;
	private ByteBuffer byteBuffer;
	private boolean ownsBuffer;
	private int bufferHandle;
	private int usage;
	boolean isDirty = false;
	boolean isBound = false;
	
	/**
	 * Constructs a new interleaved VertexBufferObject.
	 *
	 * @param isStatic    whether the vertex data is static.
	 * @param numVertices the maximum number of vertices
	 * @param attributes  the {@link VertexAttribute}s.
	 */
	public VertexBufferObject(boolean isStatic, int numVertices, VertexAttribute... attributes) {
		this(isStatic, numVertices, new VertexAttributes(attributes));
	}
	
	/**
	 * Constructs a new interleaved VertexBufferObject.
	 *
	 * @param isStatic    whether the vertex data is static.
	 * @param numVertices the maximum number of vertices
	 * @param attributes  the {@link VertexAttributes}.
	 */
	public VertexBufferObject(boolean isStatic, int numVertices, VertexAttributes attributes) {
		bufferHandle = Micro.gl20.glGenBuffer();
		
		ByteBuffer data = Buffers.newUnsafeByteBuffer(attributes.vertexSize * numVertices);
		((Buffer) data).limit(0);
		setBuffer(data, true, attributes);
		setUsage(isStatic ? GL20.GL_STATIC_DRAW : GL20.GL_DYNAMIC_DRAW);
	}
	
	protected VertexBufferObject(int usage, ByteBuffer data, boolean ownsBuffer, VertexAttributes attributes) {
		bufferHandle = Micro.gl20.glGenBuffer();
		
		setBuffer(data, ownsBuffer, attributes);
		setUsage(usage);
	}
	
	@Override
	public VertexAttributes getAttributes() {
		return attributes;
	}
	
	@Override
	public int getNumVertices() {
		return buffer.limit() * 4 / attributes.vertexSize;
	}
	
	@Override
	public int getNumMaxVertices() {
		return byteBuffer.capacity() / attributes.vertexSize;
	}
	
	@Override
	public FloatBuffer getBuffer(boolean forWriting) {
		isDirty |= forWriting;
		return buffer;
	}
	
	/**
	 * Low level method to reset the buffer and attributes to the specified values. Use with care!
	 *
	 * @param data
	 * @param ownsBuffer
	 * @param value
	 */
	protected void setBuffer(Buffer data, boolean ownsBuffer, VertexAttributes value) {
		if (isBound)
			throw new MicroRuntimeException("Cannot change attributes while VBO is bound");
		if (this.ownsBuffer && byteBuffer != null)
			Buffers.disposeUnsafeByteBuffer(byteBuffer);
		attributes = value;
		if (data instanceof ByteBuffer)
			byteBuffer = (ByteBuffer) data;
		else
			throw new MicroRuntimeException("Only ByteBuffer is currently supported");
		this.ownsBuffer = ownsBuffer;
		
		final int l = byteBuffer.limit();
		((Buffer) byteBuffer).limit(byteBuffer.capacity());
		buffer = byteBuffer.asFloatBuffer();
		((Buffer) byteBuffer).limit(l);
		((Buffer) buffer).limit(l / 4);
	}
	
	private void bufferChanged() {
		if (isBound) {
			Micro.gl20.glBufferData(GL20.GL_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage);
			isDirty = false;
		}
	}
	
	@Override
	public void setVertices(float[] vertices, int offset, int count) {
		isDirty = true;
		Buffers.copy(vertices, byteBuffer, count, offset);
		((Buffer) buffer).position(0);
		((Buffer) buffer).limit(count);
		bufferChanged();
	}
	
	@Override
	public void updateVertices(int targetOffset, float[] vertices, int sourceOffset, int count) {
		isDirty = true;
		final int pos = byteBuffer.position();
		((Buffer) byteBuffer).position(targetOffset * 4);
		Buffers.copy(vertices, sourceOffset, count, byteBuffer);
		((Buffer) byteBuffer).position(pos);
		((Buffer) buffer).position(0);
		bufferChanged();
	}
	
	/**
	 * @return The GL enum used in the call to {@link GL20#glBufferData(int, int, java.nio.Buffer, int)}, e.g. GL_STATIC_DRAW or
	 * GL_DYNAMIC_DRAW
	 */
	protected int getUsage() {
		return usage;
	}
	
	/**
	 * Set the GL enum used in the call to {@link GL20#glBufferData(int, int, java.nio.Buffer, int)}, can only be called when the
	 * VBO is not bound.
	 */
	protected void setUsage(int value) {
		if (isBound)
			throw new MicroRuntimeException("Cannot change usage while VBO is bound");
		usage = value;
	}
	
	/**
	 * Binds this VertexBufferObject for rendering via glDrawArrays or glDrawElements
	 *
	 * @param shader the shader
	 */
	@Override
	public void bind(Shader shader) {
		bind(shader, null);
	}
	
	@Override
	public void bind(Shader shader, int[] locations) {
		final GL20 gl = Micro.gl20;
		
		gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, bufferHandle);
		if (isDirty) {
			((Buffer) byteBuffer).limit(buffer.limit() * 4);
			gl.glBufferData(GL20.GL_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage);
			isDirty = false;
		}
		
		final int numAttributes = attributes.size();
		if (locations == null) {
			for (int i = 0; i < numAttributes; i++) {
				final VertexAttribute attribute = attributes.get(i);
				final int location = shader.getAttributeLocation(attribute.alias);
				if (location < 0)
					continue;
				shader.enableVertexAttribute(location);
				
				shader.setVertexAttribute(location, attribute.numComponents, attribute.type, attribute.normalized,
						attributes.vertexSize, attribute.offset);
			}
			
		} else {
			for (int i = 0; i < numAttributes; i++) {
				final VertexAttribute attribute = attributes.get(i);
				final int location = locations[i];
				if (location < 0)
					continue;
				shader.enableVertexAttribute(location);
				
				shader.setVertexAttribute(location, attribute.numComponents, attribute.type, attribute.normalized,
						attributes.vertexSize, attribute.offset);
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
	public void unbind(final Shader shader) {
		unbind(shader, null);
	}
	
	@Override
	public void unbind(final Shader shader, final int[] locations) {
		final GL20 gl = Micro.gl20;
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
		gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
		isBound = false;
	}
	
	/**
	 * Invalidates the VertexBufferObject so a new OpenGL buffer handle is created. Use this in case of a context loss.
	 */
	@Override
	public void invalidate() {
		bufferHandle = Micro.gl20.glGenBuffer();
		isDirty = true;
	}
	
	/**
	 * Disposes of all resources this VertexBufferObject uses.
	 */
	@Override
	public void dispose() {
		GL20 gl = Micro.gl20;
		gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
		gl.glDeleteBuffer(bufferHandle);
		bufferHandle = 0;
		if (ownsBuffer)
			Buffers.disposeUnsafeByteBuffer(byteBuffer);
	}
	
}
