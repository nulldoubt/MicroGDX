package me.nulldoubt.micro.scenes.scene2d.utils;

import me.nulldoubt.micro.graphics.g2d.Batch;

public interface Drawable {
	
	void draw(Batch batch, float x, float y, float width, float height);
	
	float getLeftWidth();
	
	void setLeftWidth(float leftWidth);
	
	float getRightWidth();
	
	void setRightWidth(float rightWidth);
	
	float getTopHeight();
	
	void setTopHeight(float topHeight);
	
	float getBottomHeight();
	
	void setBottomHeight(float bottomHeight);
	
	float getMinWidth();
	
	void setMinWidth(float minWidth);
	
	float getMinHeight();
	
	void setMinHeight(float minHeight);
	
}
