package me.nulldoubt.micro.graphics;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.math.Matrix4;
import me.nulldoubt.micro.math.Quaternion;
import me.nulldoubt.micro.math.Vector3;

public abstract class Camera {
	
	/**
	 * the position of the camera
	 **/
	public final Vector3 position = new Vector3();
	/**
	 * the unit length direction vector of the camera
	 **/
	public final Vector3 direction = new Vector3(0, 0, -1);
	/**
	 * the unit length up vector of the camera
	 **/
	public final Vector3 up = new Vector3(0, 1, 0);
	
	/**
	 * the projection matrix
	 **/
	public final Matrix4 projection = new Matrix4();
	/**
	 * the view matrix
	 **/
	public final Matrix4 view = new Matrix4();
	/**
	 * the combined projection and view matrix
	 **/
	public final Matrix4 combined = new Matrix4();
	/**
	 * the inverse combined projection and view matrix
	 **/
	public final Matrix4 invProjectionView = new Matrix4();
	
	/**
	 * the near clipping plane distance, has to be positive
	 **/
	public float near = 1;
	/**
	 * the far clipping plane distance, has to be positive
	 **/
	public float far = 100;
	
	/**
	 * the viewport width
	 **/
	public float viewportWidth = 0;
	/**
	 * the viewport height
	 **/
	public float viewportHeight = 0;
	
	/**
	 * the frustum
	 **/
	private final Vector3 tmpVec = new Vector3();
	
	public abstract void update();
	
	public abstract void update(boolean updateFrustum);
	
	/**
	 * Recalculates the direction of the camera to look at the point (x, y, z). This function assumes the up vector is normalized.
	 *
	 * @param x the x-coordinate of the point to look at
	 * @param y the y-coordinate of the point to look at
	 * @param z the z-coordinate of the point to look at
	 */
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
	
	/**
	 * Recalculates the direction of the camera to look at the point (x, y, z).
	 *
	 * @param target the point to look at
	 */
	public void lookAt(Vector3 target) {
		lookAt(target.x, target.y, target.z);
	}
	
	/**
	 * Normalizes the up vector by first calculating the right vector via a cross product between direction and up, and then
	 * recalculating the up vector via a cross product between right and direction.
	 */
	public void normalizeUp() {
		tmpVec.set(direction).crs(up);
		up.set(tmpVec).crs(direction).nor();
	}
	
	/**
	 * Rotates the direction and up vector of this camera by the given angle around the given axis. The direction and up vector
	 * will not be orthogonalized.
	 *
	 * @param angle the angle
	 * @param axisX the x-component of the axis
	 * @param axisY the y-component of the axis
	 * @param axisZ the z-component of the axis
	 */
	public void rotate(float angle, float axisX, float axisY, float axisZ) {
		direction.rotate(angle, axisX, axisY, axisZ);
		up.rotate(angle, axisX, axisY, axisZ);
	}
	
	/**
	 * Rotates the direction and up vector of this camera by the given angle around the given axis. The direction and up vector
	 * will not be orthogonalized.
	 *
	 * @param axis  the axis to rotate around
	 * @param angle the angle, in degrees
	 */
	public void rotate(Vector3 axis, float angle) {
		direction.rotate(axis, angle);
		up.rotate(axis, angle);
	}
	
	/**
	 * Rotates the direction and up vector of this camera by the given rotation matrix. The direction and up vector will not be
	 * orthogonalized.
	 *
	 * @param transform The rotation matrix
	 */
	public void rotate(final Matrix4 transform) {
		direction.rot(transform);
		up.rot(transform);
	}
	
	/**
	 * Rotates the direction and up vector of this camera by the given {@link Quaternion}. The direction and up vector will not be
	 * orthogonalized.
	 *
	 * @param quat The quaternion
	 */
	public void rotate(final Quaternion quat) {
		quat.transform(direction);
		quat.transform(up);
	}
	
	/**
	 * Rotates the direction and up vector of this camera by the given angle around the given axis, with the axis attached to
	 * given point. The direction and up vector will not be orthogonalized.
	 *
	 * @param point the point to attach the axis to
	 * @param axis  the axis to rotate around
	 * @param angle the angle, in degrees
	 */
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
