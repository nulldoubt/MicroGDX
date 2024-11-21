package me.nulldoubt.micro.utils.viewport;

import me.nulldoubt.micro.graphics.Camera;
import me.nulldoubt.micro.utils.Scaling;

public class FillViewport extends ScalingViewport {
	
	public FillViewport(final float worldWidth, final float worldHeight) {
		super(Scaling.fill, worldWidth, worldHeight);
	}
	
	public FillViewport(final float worldWidth, final float worldHeight, final Camera camera) {
		super(Scaling.fill, worldWidth, worldHeight, camera);
	}
	
}
