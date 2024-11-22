package me.nulldoubt.micro.utils.viewport;

import me.nulldoubt.micro.graphics.Camera;
import me.nulldoubt.micro.math.Vector2;
import me.nulldoubt.micro.utils.Scaling;

public class ExtendViewport extends Viewport {
	
	public float minWorldWidth, minWorldHeight;
	public float maxWorldWidth, maxWorldHeight;
	public Scaling scaling = Scaling.fit;
	
	public ExtendViewport(float minWorldWidth, float minWorldHeight) {
		this(minWorldWidth, minWorldHeight, 0, 0, new Camera());
	}
	
	public ExtendViewport(float minWorldWidth, float minWorldHeight, Camera camera) {
		this(minWorldWidth, minWorldHeight, 0, 0, camera);
	}
	
	public ExtendViewport(float minWorldWidth, float minWorldHeight, float maxWorldWidth, float maxWorldHeight) {
		this(minWorldWidth, minWorldHeight, maxWorldWidth, maxWorldHeight, new Camera());
	}
	
	public ExtendViewport(float minWorldWidth, float minWorldHeight, float maxWorldWidth, float maxWorldHeight, Camera camera) {
		this.minWorldWidth = minWorldWidth;
		this.minWorldHeight = minWorldHeight;
		this.maxWorldWidth = maxWorldWidth;
		this.maxWorldHeight = maxWorldHeight;
		this.camera = camera;
	}
	
	public void update(int screenWidth, int screenHeight, boolean centerCamera) {
		
		float worldWidth = minWorldWidth;
		float worldHeight = minWorldHeight;
		final Vector2 scaled = scaling.apply(worldWidth, worldHeight, screenWidth, screenHeight);
		
		// Extend, possibly in both directions depending on the scaling.
		int viewportWidth = Math.round(scaled.x);
		int viewportHeight = Math.round(scaled.y);
		if (viewportWidth < screenWidth) {
			float toViewportSpace = viewportHeight / worldHeight;
			float toWorldSpace = worldHeight / viewportHeight;
			float lengthen = (screenWidth - viewportWidth) * toWorldSpace;
			if (maxWorldWidth > 0)
				lengthen = Math.min(lengthen, maxWorldWidth - minWorldWidth);
			worldWidth += lengthen;
			viewportWidth += Math.round(lengthen * toViewportSpace);
		}
		if (viewportHeight < screenHeight) {
			float toViewportSpace = viewportWidth / worldWidth;
			float toWorldSpace = worldWidth / viewportWidth;
			float lengthen = (screenHeight - viewportHeight) * toWorldSpace;
			if (maxWorldHeight > 0)
				lengthen = Math.min(lengthen, maxWorldHeight - minWorldHeight);
			worldHeight += lengthen;
			viewportHeight += Math.round(lengthen * toViewportSpace);
		}
		
		setWorldSize(worldWidth, worldHeight);
		
		// Center.
		setScreenBounds((screenWidth - viewportWidth) / 2, (screenHeight - viewportHeight) / 2, viewportWidth, viewportHeight);
		
		apply(centerCamera);
	}
	
}
