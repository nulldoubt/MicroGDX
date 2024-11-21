package me.nulldoubt.micro.objects;

import me.nulldoubt.micro.maps.MapObject;
import me.nulldoubt.micro.math.shapes.Polygon;

public class PolygonMapObject extends MapObject {
	
	public Polygon polygon;
	
	public PolygonMapObject() {
		this(new float[0]);
	}
	
	public PolygonMapObject(final float[] vertices) {
		polygon = new Polygon(vertices);
	}
	
	public PolygonMapObject(final Polygon polygon) {
		this.polygon = polygon;
	}
	
}
