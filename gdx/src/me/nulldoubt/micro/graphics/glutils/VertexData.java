package me.nulldoubt.micro.graphics.glutils;

import me.nulldoubt.micro.graphics.VertexAttributes;
import me.nulldoubt.micro.utils.Disposable;

import java.nio.FloatBuffer;

public interface VertexData extends Disposable {
	
	int getNumVertices();
	
	int getNumMaxVertices();
	
	VertexAttributes getAttributes();
	
	void setVertices(float[] vertices, int offset, int count);
	
	void updateVertices(int targetOffset, float[] vertices, int sourceOffset, int count);
	
	FloatBuffer getBuffer(boolean forWriting);
	
	void bind(Shader shader);
	
	void bind(Shader shader, int[] locations);
	
	void unbind(Shader shader);
	
	void unbind(Shader shader, int[] locations);
	
	void invalidate();
	
	void dispose();
	
}
