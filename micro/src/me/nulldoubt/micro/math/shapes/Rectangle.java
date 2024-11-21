package me.nulldoubt.micro.math.shapes;

import me.nulldoubt.micro.math.Vector2;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.utils.Numbers;

import java.io.Serializable;

public class Rectangle implements Serializable, Shape2D {
	
	// FIXME: Remove
	public static final Rectangle tmp = new Rectangle();
	
	public float x, y;
	public float width, height;
	
	public Rectangle() {}
	
	public Rectangle(float x, float y, float width, float height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
	
	public Rectangle(Rectangle rect) {
		x = rect.x;
		y = rect.y;
		width = rect.width;
		height = rect.height;
	}
	
	public Rectangle set(float x, float y, float width, float height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		
		return this;
	}
	
	public float getX() {
		return x;
	}
	
	public Rectangle setX(float x) {
		this.x = x;
		return this;
	}
	
	public float getY() {
		return y;
	}
	
	public Rectangle setY(float y) {
		this.y = y;
		return this;
	}
	
	public float getWidth() {
		return width;
	}
	
	public Rectangle setWidth(float width) {
		this.width = width;
		return this;
	}
	
	public float getHeight() {
		return height;
	}
	
	public Rectangle setHeight(float height) {
		this.height = height;
		return this;
	}
	
	public Vector2 getPosition(Vector2 position) {
		return position.set(x, y);
	}
	
	public Rectangle setPosition(Vector2 position) {
		this.x = position.x;
		this.y = position.y;
		return this;
	}
	
	public Rectangle setPosition(float x, float y) {
		this.x = x;
		this.y = y;
		return this;
	}
	
	public Rectangle setSize(float width, float height) {
		this.width = width;
		this.height = height;
		return this;
	}
	
	public Rectangle setSize(float sizeXY) {
		this.width = sizeXY;
		this.height = sizeXY;
		return this;
	}
	
	public Vector2 getSize(Vector2 size) {
		return size.set(width, height);
	}
	
	public boolean contains(float x, float y) {
		return this.x <= x && this.x + this.width >= x && this.y <= y && this.y + this.height >= y;
	}
	
	public boolean contains(Circle circle) {
		return (circle.x - circle.radius >= x) && (circle.x + circle.radius <= x + width) && (circle.y - circle.radius >= y) && (circle.y + circle.radius <= y + height);
	}
	
	public boolean contains(Rectangle rectangle) {
		float minX = rectangle.x;
		float maxX = minX + rectangle.width;
		
		float minY = rectangle.y;
		float maxY = minY + rectangle.height;
		
		return ((minX > x && minX < x + width) && (maxX > x && maxX < x + width)) && ((minY > y && minY < y + height) && (maxY > y && maxY < y + height));
	}
	
	public boolean overlaps(Rectangle r) {
		return x < r.x + r.width && x + width > r.x && y < r.y + r.height && y + height > r.y;
	}
	
	public Rectangle set(Rectangle rect) {
		this.x = rect.x;
		this.y = rect.y;
		this.width = rect.width;
		this.height = rect.height;
		return this;
	}
	
	public Rectangle merge(Rectangle rect) {
		float minX = Math.min(x, rect.x);
		float maxX = Math.max(x + width, rect.x + rect.width);
		this.x = minX;
		this.width = maxX - minX;
		
		float minY = Math.min(y, rect.y);
		float maxY = Math.max(y + height, rect.y + rect.height);
		this.y = minY;
		this.height = maxY - minY;
		
		return this;
	}
	
	public Rectangle merge(float x, float y) {
		float minX = Math.min(this.x, x);
		float maxX = Math.max(this.x + width, x);
		this.x = minX;
		this.width = maxX - minX;
		
		float minY = Math.min(this.y, y);
		float maxY = Math.max(this.y + height, y);
		this.y = minY;
		this.height = maxY - minY;
		
		return this;
	}
	
	public Rectangle merge(Vector2 vec) {
		return merge(vec.x, vec.y);
	}
	
	public Rectangle merge(Vector2[] vecs) {
		float minX = x;
		float maxX = x + width;
		float minY = y;
		float maxY = y + height;
		for (Vector2 v : vecs) {
			minX = Math.min(minX, v.x);
			maxX = Math.max(maxX, v.x);
			minY = Math.min(minY, v.y);
			maxY = Math.max(maxY, v.y);
		}
		x = minX;
		width = maxX - minX;
		y = minY;
		height = maxY - minY;
		return this;
	}
	
	public float getAspectRatio() {
		return (height == 0) ? Float.NaN : width / height;
	}
	
	public Vector2 getCenter(Vector2 vector) {
		vector.x = x + width / 2;
		vector.y = y + height / 2;
		return vector;
	}
	
	public Rectangle setCenter(float x, float y) {
		setPosition(x - width / 2, y - height / 2);
		return this;
	}
	
	public Rectangle setCenter(Vector2 position) {
		setPosition(position.x - width / 2, position.y - height / 2);
		return this;
	}
	
	public Rectangle fitOutside(Rectangle rect) {
		float ratio = getAspectRatio();
		
		if (ratio > rect.getAspectRatio())
			setSize(rect.height * ratio, rect.height);
		else
			setSize(rect.width, rect.width / ratio);
		
		setPosition((rect.x + rect.width / 2) - width / 2, (rect.y + rect.height / 2) - height / 2);
		return this;
	}
	
	public Rectangle fitInside(Rectangle rect) {
		float ratio = getAspectRatio();
		
		if (ratio < rect.getAspectRatio())
			setSize(rect.height * ratio, rect.height);
		else
			setSize(rect.width, rect.width / ratio);
		
		setPosition((rect.x + rect.width / 2) - width / 2, (rect.y + rect.height / 2) - height / 2);
		return this;
	}
	
	public String toString() {
		return "[" + x + "," + y + "," + width + "," + height + "]";
	}
	
	public Rectangle fromString(String v) {
		int s0 = v.indexOf(',', 1);
		int s1 = v.indexOf(',', s0 + 1);
		int s2 = v.indexOf(',', s1 + 1);
		if (s0 != -1 && s1 != -1 && s2 != -1 && v.charAt(0) == '[' && v.charAt(v.length() - 1) == ']') {
			try {
				float x = Float.parseFloat(v.substring(1, s0));
				float y = Float.parseFloat(v.substring(s0 + 1, s1));
				float width = Float.parseFloat(v.substring(s1 + 1, s2));
				float height = Float.parseFloat(v.substring(s2 + 1, v.length() - 1));
				return this.set(x, y, width, height);
			} catch (NumberFormatException ex) {
				// Throw a GdxRuntimeException
			}
		}
		throw new MicroRuntimeException("Malformed Rectangle: " + v);
	}
	
	public float area() {
		return this.width * this.height;
	}
	
	public float perimeter() {
		return 2 * (this.width + this.height);
	}
	
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Numbers.floatToRawIntBits(height);
		result = prime * result + Numbers.floatToRawIntBits(width);
		result = prime * result + Numbers.floatToRawIntBits(x);
		result = prime * result + Numbers.floatToRawIntBits(y);
		return result;
	}
	
	// FIXME: What? We have to redo this.
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Rectangle other = (Rectangle) obj;
		if (Numbers.floatToRawIntBits(height) != Numbers.floatToRawIntBits(other.height))
			return false;
		if (Numbers.floatToRawIntBits(width) != Numbers.floatToRawIntBits(other.width))
			return false;
		if (Numbers.floatToRawIntBits(x) != Numbers.floatToRawIntBits(other.x))
			return false;
		return Numbers.floatToRawIntBits(y) == Numbers.floatToRawIntBits(other.y);
	}
	
}
