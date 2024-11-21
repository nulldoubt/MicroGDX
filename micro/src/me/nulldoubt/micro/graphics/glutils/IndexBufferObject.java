package me.nulldoubt.micro.graphics.glutils;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.graphics.GL20;
import me.nulldoubt.micro.utils.Buffers;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public class IndexBufferObject implements IndexData {
	
	final ShortBuffer buffer;
	final ByteBuffer byteBuffer;
	final boolean ownsBuffer;
	int bufferHandle;
	final boolean isDirect;
	boolean isDirty = true;
	boolean isBound = false;
	final int usage;
	
	private final boolean empty;
	
	public IndexBufferObject(int maxIndices) {
		this(true, maxIndices);
	}
	
	public IndexBufferObject(boolean isStatic, int maxIndices) {
		
		empty = maxIndices == 0;
		if (empty)
			maxIndices = 1;
		
		byteBuffer = Buffers.newUnsafeByteBuffer(maxIndices * 2);
		isDirect = true;
		
		buffer = byteBuffer.asShortBuffer();
		ownsBuffer = true;
		((Buffer) buffer).flip();
		((Buffer) byteBuffer).flip();
		bufferHandle = Micro.gl20.glGenBuffer();
		usage = isStatic ? GL20.GL_STATIC_DRAW : GL20.GL_DYNAMIC_DRAW;
	}
	
	public IndexBufferObject(boolean isStatic, ByteBuffer data) {
		
		empty = data.limit() == 0;
		byteBuffer = data;
		isDirect = true;
		
		buffer = byteBuffer.asShortBuffer();
		ownsBuffer = false;
		bufferHandle = Micro.gl20.glGenBuffer();
		usage = isStatic ? GL20.GL_STATIC_DRAW : GL20.GL_DYNAMIC_DRAW;
	}
	
	public int getNumIndices() {
		return empty ? 0 : buffer.limit();
	}
	
	public int getNumMaxIndices() {
		return empty ? 0 : buffer.capacity();
	}
	
	public void setIndices(short[] indices, int offset, int count) {
		isDirty = true;
		((Buffer) buffer).clear();
		buffer.put(indices, offset, count);
		((Buffer) buffer).flip();
		((Buffer) byteBuffer).position(0);
		((Buffer) byteBuffer).limit(count << 1);
		
		if (isBound) {
			Micro.gl20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage);
			isDirty = false;
		}
	}
	
	public void setIndices(ShortBuffer indices) {
		isDirty = true;
		int pos = indices.position();
		((Buffer) buffer).clear();
		buffer.put(indices);
		((Buffer) buffer).flip();
		((Buffer) indices).position(pos);
		((Buffer) byteBuffer).position(0);
		((Buffer) byteBuffer).limit(buffer.limit() << 1);
		
		if (isBound) {
			Micro.gl20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage);
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
			Micro.gl20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage);
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
			throw new MicroRuntimeException("No buffer allocated!");
		
		Micro.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, bufferHandle);
		if (isDirty) {
			((Buffer) byteBuffer).limit(buffer.limit() * 2);
			Micro.gl20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage);
			isDirty = false;
		}
		isBound = true;
	}
	
	public void unbind() {
		Micro.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);
		isBound = false;
	}
	
	public void invalidate() {
		bufferHandle = Micro.gl20.glGenBuffer();
		isDirty = true;
	}
	
	public void dispose() {
		Micro.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);
		Micro.gl20.glDeleteBuffer(bufferHandle);
		bufferHandle = 0;
		if (ownsBuffer)
			Buffers.disposeUnsafeByteBuffer(byteBuffer);
	}
	
}
