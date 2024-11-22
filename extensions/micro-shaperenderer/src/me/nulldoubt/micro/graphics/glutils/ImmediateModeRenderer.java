package me.nulldoubt.micro.graphics.glutils;

import me.nulldoubt.micro.graphics.Color;
import me.nulldoubt.micro.math.Matrix4;

public interface ImmediateModeRenderer {
	
	void begin(Matrix4 projModelView, int primitiveType);
	
	void flush();
	
	void color(Color color);
	
	void color(float r, float g, float b, float a);
	
	void color(float colorBits);
	
	void texCoord(float u, float v);
	
	void normal(float x, float y, float z);
	
	void vertex(float x, float y, float z);
	
	void end();
	
	int getNumVertices();
	
	int getMaxVertices();
	
	void dispose();
	
}