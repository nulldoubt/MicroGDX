package me.nulldoubt.micro.objects;

import me.nulldoubt.micro.maps.MapObject;
import me.nulldoubt.micro.math.shapes.Circle;

public class CircleMapObject extends MapObject {
	
	public final Circle circle;
	
	public CircleMapObject() {
		this(0.0f, 0.0f, 1.0f);
	}
	
	public CircleMapObject(final float x, final float y, final float radius) {
		super();
		circle = new Circle(x, y, radius);
	}
	
}
