package me.nulldoubt.micro.objects;

import me.nulldoubt.micro.maps.MapObject;
import me.nulldoubt.micro.math.shapes.Polyline;

public class PolylineMapObject extends MapObject {
	
	public Polyline polyline;
	
	public PolylineMapObject() {
		this(new float[0]);
	}
	
	public PolylineMapObject(final float[] vertices) {
		polyline = new Polyline(vertices);
	}
	
	public PolylineMapObject(final Polyline polyline) {
		this.polyline = polyline;
	}
	
}
