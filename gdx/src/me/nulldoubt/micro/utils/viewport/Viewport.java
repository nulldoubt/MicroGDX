package me.nulldoubt.micro.utils.viewport;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.graphics.Camera;
import me.nulldoubt.micro.graphics.glutils.HdpiUtils;
import me.nulldoubt.micro.math.Matrix4;
import me.nulldoubt.micro.math.Vector2;
import me.nulldoubt.micro.math.Vector3;
import me.nulldoubt.micro.math.shapes.Rectangle;
import me.nulldoubt.micro.utils.Scissors;

public abstract class Viewport {
	
	public Camera camera;
	public float worldWidth, worldHeight;
	public int screenX, screenY, screenWidth, screenHeight;
	
	private final Vector3 tmp = new Vector3();
	
	public void apply() {
		apply(false);
	}
	
	public void apply(boolean centerCamera) {
		HdpiUtils.glViewport(screenX, screenY, screenWidth, screenHeight);
		camera.viewportWidth = worldWidth;
		camera.viewportHeight = worldHeight;
		if (centerCamera)
			camera.position.set(worldWidth / 2, worldHeight / 2, 0);
		camera.update();
	}
	
	public final void update(int screenWidth, int screenHeight) {
		update(screenWidth, screenHeight, false);
	}
	
	public void update(int screenWidth, int screenHeight, boolean centerCamera) {
		apply(centerCamera);
	}
	
	public Vector2 unproject(Vector2 touchCoords) {
		tmp.set(touchCoords.x, touchCoords.y, 1);
		camera.unproject(tmp, screenX, screenY, screenWidth, screenHeight);
		touchCoords.set(tmp.x, tmp.y);
		return touchCoords;
	}
	
	public Vector2 project(Vector2 worldCoords) {
		tmp.set(worldCoords.x, worldCoords.y, 1);
		camera.project(tmp, screenX, screenY, screenWidth, screenHeight);
		worldCoords.set(tmp.x, tmp.y);
		return worldCoords;
	}
	
	public Vector3 unproject(Vector3 screenCoords) {
		camera.unproject(screenCoords, screenX, screenY, screenWidth, screenHeight);
		return screenCoords;
	}
	
	public Vector3 project(Vector3 worldCoords) {
		camera.project(worldCoords, screenX, screenY, screenWidth, screenHeight);
		return worldCoords;
	}
	
	public void calculateScissors(Matrix4 batchTransform, Rectangle area, Rectangle scissor) {
		Scissors.calculateScissors(camera, screenX, screenY, screenWidth, screenHeight, batchTransform, area, scissor);
	}
	
	public Vector2 toScreenCoordinates(Vector2 worldCoords, Matrix4 transformMatrix) {
		tmp.set(worldCoords.x, worldCoords.y, 0);
		tmp.mul(transformMatrix);
		camera.project(tmp, screenX, screenY, screenWidth, screenHeight);
		tmp.y = Micro.graphics.getHeight() - tmp.y;
		worldCoords.x = tmp.x;
		worldCoords.y = tmp.y;
		return worldCoords;
	}
	
	public void setWorldSize(final float worldWidth, final float worldHeight) {
		this.worldWidth = worldWidth;
		this.worldHeight = worldHeight;
	}
	
	public void setScreenPosition(final int screenX, final int screenY) {
		this.screenX = screenX;
		this.screenY = screenY;
	}
	
	public void setScreenSize(final int screenWidth, final int screenHeight) {
		this.screenWidth = screenWidth;
		this.screenHeight = screenHeight;
	}
	
	public void setScreenBounds(final int screenX, final int screenY, final int screenWidth, final int screenHeight) {
		this.screenX = screenX;
		this.screenY = screenY;
		this.screenWidth = screenWidth;
		this.screenHeight = screenHeight;
	}
	
	public int getLeftGutterWidth() {
		return screenX;
	}
	
	public int getRightGutterX() {
		return screenX + screenWidth;
	}
	
	public int getRightGutterWidth() {
		return Micro.graphics.getWidth() - (screenX + screenWidth);
	}
	
	public int getBottomGutterHeight() {
		return screenY;
	}
	
	public int getTopGutterY() {
		return screenY + screenHeight;
	}
	
	public int getTopGutterHeight() {
		return Micro.graphics.getHeight() - (screenY + screenHeight);
	}
	
}
