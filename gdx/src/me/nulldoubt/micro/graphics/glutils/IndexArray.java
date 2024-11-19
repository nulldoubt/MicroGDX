package me.nulldoubt.micro.graphics.glutils;

import me.nulldoubt.micro.utils.BufferUtils;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public class IndexArray implements IndexData {
	
	final ShortBuffer buffer;
	final ByteBuffer byteBuffer;
	
	private final boolean empty;
	
	public IndexArray(int maxIndices) {
		
		empty = maxIndices == 0;
		if (empty)
			maxIndices = 1;
		
		byteBuffer = BufferUtils.newUnsafeByteBuffer(maxIndices * 2);
		buffer = byteBuffer.asShortBuffer();
		((Buffer) buffer).flip();
		((Buffer) byteBuffer).flip();
	}
	
	public int getNumIndices() {
		return empty ? 0 : buffer.limit();
	}
	
	public int getNumMaxIndices() {
		return empty ? 0 : buffer.capacity();
	}
	
	public void setIndices(short[] indices, int offset, int count) {
		((Buffer) buffer).clear();
		buffer.put(indices, offset, count);
		((Buffer) buffer).flip();
		((Buffer) byteBuffer).position(0);
		((Buffer) byteBuffer).limit(count << 1);
	}
	
	public void setIndices(ShortBuffer indices) {
		int pos = indices.position();
		((Buffer) buffer).clear();
		((Buffer) buffer).limit(indices.remaining());
		buffer.put(indices);
		((Buffer) buffer).flip();
		((Buffer) indices).position(pos);
		((Buffer) byteBuffer).position(0);
		((Buffer) byteBuffer).limit(buffer.limit() << 1);
	}
	
	@Override
	public void updateIndices(int targetOffset, short[] indices, int offset, int count) {
		final int pos = byteBuffer.position();
		((Buffer) byteBuffer).position(targetOffset * 2);
		BufferUtils.copy(indices, offset, byteBuffer, count);
		((Buffer) byteBuffer).position(pos);
	}
	
	@Override
	public ShortBuffer getBuffer(boolean writing) {
		return buffer;
	}
	
	public void bind() {}
	
	public void unbind() {}
	
	public void invalidate() {}
	
	public void dispose() {
		BufferUtils.disposeUnsafeByteBuffer(byteBuffer);
	}
	
}
