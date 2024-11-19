package me.nulldoubt.micro.utils.viewport;

import me.nulldoubt.micro.graphics.Camera;
import me.nulldoubt.micro.utils.Scaling;

public class FitViewport extends ScalingViewport {
	
	public FitViewport(final float worldWidth, final float worldHeight) {
		super(Scaling.fit, worldWidth, worldHeight);
	}
	
	public FitViewport(final float worldWidth, final float worldHeight, final Camera camera) {
		super(Scaling.fit, worldWidth, worldHeight, camera);
	}
	
}
