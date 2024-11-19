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

package com.badlogic.gdx.tests;

import com.badlogic.gdx.Micro;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.tests.utils.GdxTest;
import com.badlogic.gdx.utils.ScreenUtils;

public class FullscreenTest extends GdxTest {
	SpriteBatch batch;
	Texture tex;
	boolean fullscreen = false;
	BitmapFont font;

	@Override
	public void create () {
		batch = new SpriteBatch();
		font = new BitmapFont();
		tex = new Texture(Micro.files.internal("data/badlogic.jpg"));
		DisplayMode[] modes = Micro.graphics.getDisplayModes();
		for (DisplayMode mode : modes) {
			System.out.println(mode);
		}
		Micro.app.log("FullscreenTest", Micro.graphics.getBufferFormat().toString());
	}

	@Override
	public void resume () {

	}

	@Override
	public void render () {
		ScreenUtils.clear(0, 0, 0, 1);

		batch.begin();
		batch.setColor(Micro.input.getX() < Micro.graphics.getSafeInsetLeft()
			|| Micro.input.getX() + tex.getWidth() > Micro.graphics.getWidth() - Micro.graphics.getSafeInsetRight() ? Color.RED
				: Color.WHITE);
		batch.draw(tex, Micro.input.getX(), Micro.graphics.getHeight() - Micro.input.getY());
		font.draw(batch, "" + Micro.graphics.getWidth() + ", " + Micro.graphics.getHeight(), 0, 20);
		batch.end();

		if (Micro.input.justTouched()) {
			if (fullscreen) {
				Micro.graphics.setWindowedMode(480, 320);
				batch.getProjectionMatrix().setToOrtho2D(0, 0, Micro.graphics.getWidth(), Micro.graphics.getHeight());
				Micro.gl.glViewport(0, 0, Micro.graphics.getBackBufferWidth(), Micro.graphics.getBackBufferHeight());
				fullscreen = false;
			} else {
				DisplayMode m = null;
				for (DisplayMode mode : Micro.graphics.getDisplayModes()) {
					if (m == null) {
						m = mode;
					} else {
						if (m.width < mode.width) {
							m = mode;
						}
					}
				}

				Micro.graphics.setFullscreenMode(Micro.graphics.getDisplayMode());
				batch.getProjectionMatrix().setToOrtho2D(0, 0, Micro.graphics.getWidth(), Micro.graphics.getHeight());
				Micro.gl.glViewport(0, 0, Micro.graphics.getBackBufferWidth(), Micro.graphics.getBackBufferHeight());
				fullscreen = true;
			}
		}
	}

	@Override
	public void resize (int width, int height) {
		Micro.app.log("FullscreenTest", "resized: " + width + ", " + height);
		Micro.app.log("FullscreenTest", "safe insets: " + Micro.graphics.getSafeInsetLeft() + "/" + Micro.graphics.getSafeInsetRight());
		batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
	}

	@Override
	public void pause () {
		Micro.app.log("FullscreenTest", "paused");
	}

	@Override
	public void dispose () {
		Micro.app.log("FullscreenTest", "disposed");
	}
}
