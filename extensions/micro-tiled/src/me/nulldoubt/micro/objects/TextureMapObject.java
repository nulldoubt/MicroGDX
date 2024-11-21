package me.nulldoubt.micro.objects;

import me.nulldoubt.micro.graphics.g2d.TextureRegion;
import me.nulldoubt.micro.maps.MapObject;

public class TextureMapObject extends MapObject {
	
	public float x = 0.0f;
	public float y = 0.0f;
	public float originX = 0.0f;
	public float originY = 0.0f;
	public float scaleX = 1.0f;
	public float scaleY = 1.0f;
	public float rotation = 0.0f;
	public TextureRegion textureRegion;
	
	public TextureMapObject() {
		this(null);
	}
	
	public TextureMapObject(TextureRegion textureRegion) {
		super();
		this.textureRegion = textureRegion;
	}
	
}
