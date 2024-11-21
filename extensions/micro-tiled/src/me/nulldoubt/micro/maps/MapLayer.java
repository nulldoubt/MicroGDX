package me.nulldoubt.micro.maps;

import me.nulldoubt.micro.exceptions.MicroRuntimeException;

public class MapLayer {
	
	public String name = "";
	public float opacity = 1.0f;
	public boolean visible = true;
	public float offsetX;
	public float offsetY;
	private float renderOffsetX;
	private float renderOffsetY;
	public float parallaxX = 1;
	public float parallaxY = 1;
	
	private boolean renderOffsetDirty = true;
	
	private MapLayer parent;
	public final MapObjects objects;
	public final MapProperties properties;
	
	public MapLayer() {
		objects = new MapObjects();
		properties = new MapProperties();
	}
	
	public float getRenderOffsetX() {
		if (renderOffsetDirty)
			calculateRenderOffsets();
		return renderOffsetX;
	}
	
	public float getRenderOffsetY() {
		if (renderOffsetDirty)
			calculateRenderOffsets();
		return renderOffsetY;
	}
	
	public void invalidateRenderOffset() {
		renderOffsetDirty = true;
	}
	
	public MapLayer getParent() {
		return parent;
	}
	
	public void setParent(final MapLayer parent) {
		if (parent == this)
			throw new MicroRuntimeException("Can't set self as the parent");
		this.parent = parent;
	}
	
	protected void calculateRenderOffsets() {
		if (parent != null) {
			parent.calculateRenderOffsets();
			renderOffsetX = parent.getRenderOffsetX() + offsetX;
			renderOffsetY = parent.getRenderOffsetY() + offsetY;
		} else {
			renderOffsetX = offsetX;
			renderOffsetY = offsetY;
		}
		renderOffsetDirty = false;
	}
	
}
