package me.nulldoubt.micro.objects;

import me.nulldoubt.micro.maps.MapObject;
import me.nulldoubt.micro.math.shapes.Rectangle;

public class RectangleMapObject extends MapObject {
	
	public final Rectangle rectangle;
	
	public RectangleMapObject() {
		this(0.0f, 0.0f, 1.0f, 1.0f);
	}
	
	public RectangleMapObject(final float x, final float y, final float width, final float height) {
		super();
		rectangle = new Rectangle(x, y, width, height);
	}
	
}
