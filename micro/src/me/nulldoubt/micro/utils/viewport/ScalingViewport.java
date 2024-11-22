package me.nulldoubt.micro.utils.viewport;

import me.nulldoubt.micro.graphics.Camera;
import me.nulldoubt.micro.math.Vector2;
import me.nulldoubt.micro.utils.Scaling;

public class ScalingViewport extends Viewport {
	
	public Scaling scaling;
	
	public ScalingViewport(final Scaling scaling, final float worldWidth, final float worldHeight) {
		this(scaling, worldWidth, worldHeight, new Camera());
	}
	
	public ScalingViewport(final Scaling scaling, final float worldWidth, final float worldHeight, final Camera camera) {
		this.scaling = scaling;
		setWorldSize(worldWidth, worldHeight);
		this.camera = camera;
	}
	
	public void update(final int screenWidth, final int screenHeight, final boolean centerCamera) {
		final Vector2 scaled = scaling.apply(worldWidth, worldHeight, screenWidth, screenHeight);
		final int viewportWidth = Math.round(scaled.x);
		final int viewportHeight = Math.round(scaled.y);
		
		// Center.
		setScreenBounds((screenWidth - viewportWidth) / 2, (screenHeight - viewportHeight) / 2, viewportWidth, viewportHeight);
		
		apply(centerCamera);
	}
	
}
