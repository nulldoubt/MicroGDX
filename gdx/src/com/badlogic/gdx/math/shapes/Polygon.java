package com.badlogic.gdx.math.shapes;

import com.badlogic.gdx.math.GeometryUtils;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class Polygon implements Shape2D {
	
	private float[] localVertices;
	private float[] worldVertices;
	private float x, y;
	private float originX, originY;
	private float rotation;
	private float scaleX = 1, scaleY = 1;
	private boolean dirty = true;
	private Rectangle bounds;
	
	public Polygon() {
		this.localVertices = new float[0];
	}
	
	public Polygon(float[] vertices) {
		if (vertices.length < 6)
			throw new IllegalArgumentException("Polygon must contain at least 3 points.");
		this.localVertices = vertices;
	}
	
	public float[] getVertices() {
		return localVertices;
	}
	
	public float[] getTransformedVertices() {
		if (!dirty)
			return worldVertices;
		dirty = false;
		
		final float[] localVertices = this.localVertices;
		if (worldVertices == null || worldVertices.length != localVertices.length)
			worldVertices = new float[localVertices.length];
		
		final float[] worldVertices = this.worldVertices;
		final float positionX = x;
		final float positionY = y;
		final float originX = this.originX;
		final float originY = this.originY;
		final float scaleX = this.scaleX;
		final float scaleY = this.scaleY;
		final boolean scale = scaleX != 1 || scaleY != 1;
		final float rotation = this.rotation;
		final float cos = MathUtils.cosDeg(rotation);
		final float sin = MathUtils.sinDeg(rotation);
		
		for (int i = 0, n = localVertices.length; i < n; i += 2) {
			float x = localVertices[i] - originX;
			float y = localVertices[i + 1] - originY;
			
			// scale if needed
			if (scale) {
				x *= scaleX;
				y *= scaleY;
			}
			
			// rotate if needed
			if (rotation != 0) {
				float oldX = x;
				x = cos * x - sin * y;
				y = sin * oldX + cos * y;
			}
			
			worldVertices[i] = positionX + x + originX;
			worldVertices[i + 1] = positionY + y + originY;
		}
		return worldVertices;
	}
	
	public void setOrigin(float originX, float originY) {
		this.originX = originX;
		this.originY = originY;
		dirty = true;
	}
	
	public void setPosition(float x, float y) {
		this.x = x;
		this.y = y;
		dirty = true;
	}
	
	public void setVertices(float[] vertices) {
		if (vertices.length < 6)
			throw new IllegalArgumentException("polygons must contain at least 3 points.");
		localVertices = vertices;
		dirty = true;
	}
	
	public void setVertex(int vertexNum, float x, float y) {
		if (vertexNum < 0 || vertexNum > localVertices.length / 2 - 1)
			throw new IllegalArgumentException("the vertex " + vertexNum + " doesn't exist");
		localVertices[2 * vertexNum] = x;
		localVertices[2 * vertexNum + 1] = y;
		dirty = true;
	}
	
	public void translate(float x, float y) {
		this.x += x;
		this.y += y;
		dirty = true;
	}
	
	public void setRotation(float degrees) {
		this.rotation = degrees;
		dirty = true;
	}
	
	public void rotate(float degrees) {
		rotation += degrees;
		dirty = true;
	}
	
	public void setScale(float scaleX, float scaleY) {
		this.scaleX = scaleX;
		this.scaleY = scaleY;
		dirty = true;
	}
	
	public void scale(float amount) {
		this.scaleX += amount;
		this.scaleY += amount;
		dirty = true;
	}
	
	public float area() {
		float[] vertices = getTransformedVertices();
		return GeometryUtils.polygonArea(vertices, 0, vertices.length);
	}
	
	public int getVertexCount() {
		return this.localVertices.length / 2;
	}
	
	public Vector2 getVertex(int vertexNum, Vector2 pos) {
		if (vertexNum < 0 || vertexNum > getVertexCount())
			throw new IllegalArgumentException("the vertex " + vertexNum + " doesn't exist");
		float[] vertices = this.getTransformedVertices();
		return pos.set(vertices[2 * vertexNum], vertices[2 * vertexNum + 1]);
	}
	
	public Vector2 getCentroid(Vector2 centroid) {
		float[] vertices = getTransformedVertices();
		return GeometryUtils.polygonCentroid(vertices, 0, vertices.length, centroid);
	}
	
	public Rectangle getBoundingRectangle() {
		float[] vertices = getTransformedVertices();
		
		float minX = vertices[0];
		float minY = vertices[1];
		float maxX = vertices[0];
		float maxY = vertices[1];
		
		final int numFloats = vertices.length;
		for (int i = 2; i < numFloats; i += 2) {
			minX = Math.min(minX, vertices[i]);
			minY = Math.min(minY, vertices[i + 1]);
			maxX = Math.max(maxX, vertices[i]);
			maxY = Math.max(maxY, vertices[i + 1]);
		}
		
		if (bounds == null)
			bounds = new Rectangle();
		
		return bounds.set(minX, minY, maxX - minX, maxY - minY);
	}
	
	@Override
	public boolean contains(float x, float y) {
		final float[] vertices = getTransformedVertices();
		final int numFloats = vertices.length;
		int intersects = 0;
		
		for (int i = 0; i < numFloats; i += 2) {
			float x1 = vertices[i];
			float y1 = vertices[i + 1];
			float x2 = vertices[(i + 2) % numFloats];
			float y2 = vertices[(i + 3) % numFloats];
			if (((y1 <= y && y < y2) || (y2 <= y && y < y1)) && x < ((x2 - x1) / (y2 - y1) * (y - y1) + x1))
				intersects++;
		}
		return (intersects & 1) == 1;
	}
	
	public float getX() {
		return x;
	}
	
	public float getY() {
		return y;
	}
	
	public float getOriginX() {
		return originX;
	}
	
	public float getOriginY() {
		return originY;
	}
	
	public float getRotation() {
		return rotation;
	}
	
	public float getScaleX() {
		return scaleX;
	}
	
	public float getScaleY() {
		return scaleY;
	}
	
}
