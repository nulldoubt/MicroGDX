package me.nulldoubt.micro.graphics;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.math.Matrix4;
import me.nulldoubt.micro.math.Quaternion;
import me.nulldoubt.micro.math.Vector2;
import me.nulldoubt.micro.math.Vector3;

public class Camera {
	
	public final Vector3 position = new Vector3();
	public final Vector3 direction = new Vector3(0, 0, -1);
	public final Vector3 up = new Vector3(0, 1, 0);
	
	public final Matrix4 projection = new Matrix4();
	public final Matrix4 view = new Matrix4();
	
	public final Matrix4 combined = new Matrix4();
	public final Matrix4 invProjectionView = new Matrix4();
	
	public float viewportWidth = 0;
	public float viewportHeight = 0;
	
	private final Vector3 tmpVec = new Vector3();
	
	public float zoom = 1f;
	
	public Camera() {}
	
	public Camera(final float viewportWidth, final float viewportHeight) {
		this.viewportWidth = viewportWidth;
		this.viewportHeight = viewportHeight;
		update();
	}
	
	public void update() {
		update(true);
	}
	
	public void update(boolean updateFrustum) {
		projection.setToOrtho(zoom * -viewportWidth / 2, zoom * (viewportWidth / 2), zoom * -(viewportHeight / 2), zoom * viewportHeight / 2, 0, (float) 100);
		view.setToLookAt(direction, up);
		view.translate(-position.x, -position.y, -position.z);
		combined.set(projection);
		Matrix4.mul(combined.val, view.val);
		
		if (updateFrustum) {
			invProjectionView.set(combined);
			Matrix4.inv(invProjectionView.val);
		}
	}
	
	public void setToOrtho(boolean yDown) {
		setToOrtho(yDown, Micro.graphics.getWidth(), Micro.graphics.getHeight());
	}
	
	public void setToOrtho(boolean yDown, float viewportWidth, float viewportHeight) {
		if (yDown) {
			up.set(0, -1, 0);
			direction.set(0, 0, 1);
		} else {
			up.set(0, 1, 0);
			direction.set(0, 0, -1);
		}
		position.set(zoom * viewportWidth / 2.0f, zoom * viewportHeight / 2.0f, 0);
		this.viewportWidth = viewportWidth;
		this.viewportHeight = viewportHeight;
		update();
	}
	
	public void rotate(float angle) {
		rotate(direction, angle);
	}
	
	public void translate(float x, float y) {
		translate(x, y, 0);
	}
	
	public void translate(Vector2 vec) {
		translate(vec.x, vec.y, 0);
	}
	
	public void lookAt(float x, float y, float z) {
		tmpVec.set(x, y, z).sub(position).nor();
		if (!tmpVec.isZero()) {
			float dot = tmpVec.dot(up); // up and direction must ALWAYS be orthonormal vectors
			if (Math.abs(dot - 1) < 0.000000001f) {
				// Collinear
				up.set(direction).scl(-1);
			} else if (Math.abs(dot + 1) < 0.000000001f) {
				// Collinear opposite
				up.set(direction);
			}
			direction.set(tmpVec);
			normalizeUp();
		}
	}
	
	public void lookAt(Vector3 target) {
		lookAt(target.x, target.y, target.z);
	}
	
	public void normalizeUp() {
		tmpVec.set(direction).crs(up);
		up.set(tmpVec).crs(direction).nor();
	}
	
	public void rotate(float angle, float axisX, float axisY, float axisZ) {
		direction.rotate(angle, axisX, axisY, axisZ);
		up.rotate(angle, axisX, axisY, axisZ);
	}
	
	public void rotate(Vector3 axis, float angle) {
		direction.rotate(axis, angle);
		up.rotate(axis, angle);
	}
	
	public void rotate(final Matrix4 transform) {
		direction.rot(transform);
		up.rot(transform);
	}
	
	public void rotate(final Quaternion quat) {
		quat.transform(direction);
		quat.transform(up);
	}
	
	public void rotateAround(Vector3 point, Vector3 axis, float angle) {
		tmpVec.set(point);
		tmpVec.sub(position);
		translate(tmpVec);
		rotate(axis, angle);
		tmpVec.rotate(axis, angle);
		translate(-tmpVec.x, -tmpVec.y, -tmpVec.z);
	}
	
	public void transform(final Matrix4 transform) {
		position.mul(transform);
		rotate(transform);
	}
	
	public void translate(float x, float y, float z) {
		position.add(x, y, z);
	}
	
	public void translate(Vector3 vec) {
		position.add(vec);
	}
	
	public Vector3 unproject(Vector3 touchCoords, float viewportX, float viewportY, float viewportWidth, float viewportHeight) {
		float x = touchCoords.x - viewportX, y = Micro.graphics.getHeight() - touchCoords.y - viewportY;
		touchCoords.x = (2 * x) / viewportWidth - 1;
		touchCoords.y = (2 * y) / viewportHeight - 1;
		touchCoords.z = 2 * touchCoords.z - 1;
		touchCoords.prj(invProjectionView);
		return touchCoords;
	}
	
	public Vector3 unproject(Vector3 touchCoords) {
		unproject(touchCoords, 0, 0, Micro.graphics.getWidth(), Micro.graphics.getHeight());
		return touchCoords;
	}
	
	public Vector3 project(Vector3 worldCoords) {
		project(worldCoords, 0, 0, Micro.graphics.getWidth(), Micro.graphics.getHeight());
		return worldCoords;
	}
	
	public Vector3 project(Vector3 worldCoords, float viewportX, float viewportY, float viewportWidth, float viewportHeight) {
		worldCoords.prj(combined);
		worldCoords.x = viewportWidth * (worldCoords.x + 1) / 2 + viewportX;
		worldCoords.y = viewportHeight * (worldCoords.y + 1) / 2 + viewportY;
		worldCoords.z = (worldCoords.z + 1) / 2;
		return worldCoords;
	}
	
}
