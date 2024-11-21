package me.nulldoubt.micro.graphics.glutils;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.graphics.GL20;
import me.nulldoubt.micro.utils.Buffers;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public class IndexBufferObjectSubData implements IndexData {
	
	final ShortBuffer buffer;
	final ByteBuffer byteBuffer;
	int bufferHandle;
	final boolean isDirect;
	boolean isDirty = true;
	boolean isBound = false;
	final int usage;
	
	public IndexBufferObjectSubData(boolean isStatic, int maxIndices) {
		byteBuffer = Buffers.newByteBuffer(maxIndices * 2);
		isDirect = true;
		
		usage = isStatic ? GL20.GL_STATIC_DRAW : GL20.GL_DYNAMIC_DRAW;
		buffer = byteBuffer.asShortBuffer();
		((Buffer) buffer).flip();
		((Buffer) byteBuffer).flip();
		bufferHandle = createBufferObject();
	}
	
	public IndexBufferObjectSubData(int maxIndices) {
		byteBuffer = Buffers.newByteBuffer(maxIndices * 2);
		this.isDirect = true;
		
		usage = GL20.GL_STATIC_DRAW;
		buffer = byteBuffer.asShortBuffer();
		((Buffer) buffer).flip();
		((Buffer) byteBuffer).flip();
		bufferHandle = createBufferObject();
	}
	
	private int createBufferObject() {
		int result = Micro.gl20.glGenBuffer();
		Micro.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, result);
		Micro.gl20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, byteBuffer.capacity(), null, usage);
		Micro.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);
		return result;
	}
	
	public int getNumIndices() {
		return buffer.limit();
	}
	
	public int getNumMaxIndices() {
		return buffer.capacity();
	}
	
	public void setIndices(short[] indices, int offset, int count) {
		isDirty = true;
		((Buffer) buffer).clear();
		buffer.put(indices, offset, count);
		((Buffer) buffer).flip();
		((Buffer) byteBuffer).position(0);
		((Buffer) byteBuffer).limit(count << 1);
		
		if (isBound) {
			Micro.gl20.glBufferSubData(GL20.GL_ELEMENT_ARRAY_BUFFER, 0, byteBuffer.limit(), byteBuffer);
			isDirty = false;
		}
	}
	
	public void setIndices(ShortBuffer indices) {
		int pos = indices.position();
		isDirty = true;
		((Buffer) buffer).clear();
		buffer.put(indices);
		((Buffer) buffer).flip();
		((Buffer) indices).position(pos);
		((Buffer) byteBuffer).position(0);
		((Buffer) byteBuffer).limit(buffer.limit() << 1);
		
		if (isBound) {
			Micro.gl20.glBufferSubData(GL20.GL_ELEMENT_ARRAY_BUFFER, 0, byteBuffer.limit(), byteBuffer);
			isDirty = false;
		}
	}
	
	@Override
	public void updateIndices(int targetOffset, short[] indices, int offset, int count) {
		isDirty = true;
		final int pos = byteBuffer.position();
		((Buffer) byteBuffer).position(targetOffset * 2);
		Buffers.copy(indices, offset, byteBuffer, count);
		((Buffer) byteBuffer).position(pos);
		((Buffer) buffer).position(0);
		
		if (isBound) {
			Micro.gl20.glBufferSubData(GL20.GL_ELEMENT_ARRAY_BUFFER, 0, byteBuffer.limit(), byteBuffer);
			isDirty = false;
		}
	}
	
	@Override
	public ShortBuffer getBuffer(boolean writing) {
		isDirty |= writing;
		return buffer;
	}
	
	public void bind() {
		if (bufferHandle == 0)
			throw new MicroRuntimeException("IndexBufferObject cannot be used after it has been disposed.");
		
		Micro.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, bufferHandle);
		if (isDirty) {
			((Buffer) byteBuffer).limit(buffer.limit() * 2);
			Micro.gl20.glBufferSubData(GL20.GL_ELEMENT_ARRAY_BUFFER, 0, byteBuffer.limit(), byteBuffer);
			isDirty = false;
		}
		isBound = true;
	}
	
	public void unbind() {
		Micro.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);
		isBound = false;
	}
	
	public void invalidate() {
		bufferHandle = createBufferObject();
		isDirty = true;
	}
	
	public void dispose() {
		GL20 gl = Micro.gl20;
		gl.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);
		gl.glDeleteBuffer(bufferHandle);
		bufferHandle = 0;
	}
	
}
