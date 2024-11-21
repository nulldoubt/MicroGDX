package me.nulldoubt.micro.maps.tiled;

import me.nulldoubt.micro.graphics.g2d.TextureRegion;
import me.nulldoubt.micro.maps.MapLayer;

public class TiledMapImageLayer extends MapLayer {
	
	private TextureRegion region;
	
	private float x;
	private float y;
	private boolean repeatX;
	private boolean repeatY;
	
	public TiledMapImageLayer(TextureRegion region, float x, float y, boolean repeatX, boolean repeatY) {
		this.region = region;
		this.x = x;
		this.y = y;
		this.repeatX = repeatX;
		this.repeatY = repeatY;
	}
	
	public TextureRegion getTextureRegion() {
		return region;
	}
	
	public void setTextureRegion(TextureRegion region) {
		this.region = region;
	}
	
	public float getX() {
		return x;
	}
	
	public void setX(float x) {
		this.x = x;
	}
	
	public float getY() {
		return y;
	}
	
	public void setY(float y) {
		this.y = y;
	}
	
	public boolean isRepeatX() {
		return repeatX;
	}
	
	public void setRepeatX(boolean repeatX) {
		this.repeatX = repeatX;
	}
	
	public boolean isRepeatY() {
		return repeatY;
	}
	
	public void setRepeatY(boolean repeatY) {
		this.repeatY = repeatY;
	}
	
}
