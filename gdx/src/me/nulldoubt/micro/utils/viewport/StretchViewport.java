package me.nulldoubt.micro.utils.viewport;

import me.nulldoubt.micro.graphics.Camera;
import me.nulldoubt.micro.utils.Scaling;

public class StretchViewport extends ScalingViewport {
	
	public StretchViewport(final float worldWidth, final float worldHeight) {
		super(Scaling.stretch, worldWidth, worldHeight);
	}
	
	public StretchViewport(final float worldWidth, final float worldHeight, final Camera camera) {
		super(Scaling.stretch, worldWidth, worldHeight, camera);
	}
	
}
