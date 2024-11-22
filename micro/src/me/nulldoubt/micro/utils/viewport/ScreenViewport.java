package me.nulldoubt.micro.utils.viewport;

import me.nulldoubt.micro.graphics.Camera;

public class ScreenViewport extends Viewport {
	
	public float unitsPerPixel = 1f;
	
	public ScreenViewport() {
		this(new Camera());
	}
	
	public ScreenViewport(Camera camera) {
		this.camera = camera;
	}
	
	public void update(int screenWidth, int screenHeight, boolean centerCamera) {
		setScreenBounds(0, 0, screenWidth, screenHeight);
		setWorldSize(screenWidth * unitsPerPixel, screenHeight * unitsPerPixel);
		apply(centerCamera);
	}
	
}
