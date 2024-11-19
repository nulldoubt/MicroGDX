package me.nulldoubt.micro.math.shapes;

import me.nulldoubt.micro.math.Vector2;

// FIXME: Add the getPosition method here.
public interface Shape2D {
	
	default boolean contains(final Vector2 point) {
		return contains(point.x, point.y);
	}
	
	boolean contains(float x, float y);
	
}
