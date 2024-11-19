/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.graphics;

import com.badlogic.gdx.Micro;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;

public class OrthographicCamera extends Camera {
	
	public float zoom = 1;
	
	public OrthographicCamera() {
		this.near = 0;
	}
	
	public OrthographicCamera(float viewportWidth, float viewportHeight) {
		this.viewportWidth = viewportWidth;
		this.viewportHeight = viewportHeight;
		this.near = 0;
		update();
	}
	
	@Override
	public void update() {
		update(true);
	}
	
	@Override
	public void update(boolean updateFrustum) {
		projection.setToOrtho(zoom * -viewportWidth / 2, zoom * (viewportWidth / 2), zoom * -(viewportHeight / 2), zoom * viewportHeight / 2, near, far);
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
	
}
