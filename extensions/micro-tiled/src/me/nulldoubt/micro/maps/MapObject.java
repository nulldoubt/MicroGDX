package me.nulldoubt.micro.maps;

import me.nulldoubt.micro.graphics.Color;

public class MapObject {
	
	public String name = "";
	public float opacity = 1.0f;
	public boolean visible = true;
	
	public final MapProperties properties;
	public final Color color = Color.WHITE.cpy();
	
	public MapObject() {
		properties = new MapProperties();
	}
	
}
