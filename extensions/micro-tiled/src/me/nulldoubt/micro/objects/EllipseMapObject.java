package me.nulldoubt.micro.objects;

import me.nulldoubt.micro.maps.MapObject;
import me.nulldoubt.micro.math.shapes.Ellipse;

public class EllipseMapObject extends MapObject {
	
	public final Ellipse ellipse;
	
	public EllipseMapObject() {
		this(0.0f, 0.0f, 1.0f, 1.0f);
	}
	
	public EllipseMapObject(final float x, final float y, final float width, final float height) {
		super();
		ellipse = new Ellipse(x, y, width, height);
	}
	
}
