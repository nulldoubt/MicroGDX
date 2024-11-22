package me.nulldoubt.micro.backends.android;

public interface AndroidWallpaperListener {
	
	void offsetChange(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset);
	
	void previewStateChange(boolean isPreview);
	
	void iconDropped(int x, int y);
	
}
