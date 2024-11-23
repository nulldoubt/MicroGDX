package me.nulldoubt.micro.scenes.scene2d.utils;

public interface Layout {
	
	void layout();
	
	void invalidate();
	
	void invalidateHierarchy();
	
	void validate();
	
	void pack();
	
	void setFillParent(boolean fillParent);
	
	void setLayoutEnabled(boolean enabled);
	
	float getMinWidth();
	
	float getMinHeight();
	
	float getPrefWidth();
	
	float getPrefHeight();
	
	float getMaxWidth();
	
	float getMaxHeight();
	
}
