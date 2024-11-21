package me.nulldoubt.micro.maps;

import me.nulldoubt.micro.graphics.OrthographicCamera;
import me.nulldoubt.micro.math.Matrix4;

public interface MapRenderer {
	
	void setView(final OrthographicCamera camera);
	
	void setView(final Matrix4 projectionMatrix, final float viewBoundsX, final float viewBoundsY, final float viewBoundsWidth, final float viewBoundsHeight);
	
	void render();
	
	void render(int[] layers);
	
}
