package com.badlogic.gdx.graphics.glutils;

import com.badlogic.gdx.utils.Disposable;

import java.nio.ShortBuffer;

public interface IndexData extends Disposable {
	
	int getNumIndices();
	
	int getNumMaxIndices();
	
	void setIndices(short[] indices, int offset, int count);
	
	void setIndices(ShortBuffer indices);
	
	void updateIndices(int targetOffset, short[] indices, int offset, int count);
	
	ShortBuffer getBuffer(boolean writing);
	
	void bind();
	
	void unbind();
	
	void invalidate();
	
	void dispose();
	
}
