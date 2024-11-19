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
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.tests.utils.GdxTest;

public class AccelerometerTest extends GdxTest {
	BitmapFont font;
	SpriteBatch batch;

	@Override
	public void create () {
		font = new BitmapFont(Micro.files.internal("data/lsans-15.fnt"), false);
		batch = new SpriteBatch();
	}

	@Override
	public void render () {
		Micro.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		batch.begin();
		font.draw(batch,
			"accel: [" + Micro.input.getAccelerometerX() + "," + Micro.input.getAccelerometerY() + "," + Micro.input.getAccelerometerZ()
				+ "]\n" + "gyros: [" + Micro.input.getGyroscopeX() + "," + Micro.input.getGyroscopeY() + "," + Micro.input.getGyroscopeZ()
				+ "]\n" + "orientation: " + Micro.input.getNativeOrientation() + "\n" + "rotation: " + Micro.input.getRotation() + "\n"
				+ "wh: " + Micro.graphics.getDisplayMode() + "\n",
			0, 100);
		batch.end();
	}

	@Override
	public void dispose () {
		font.dispose();
		batch.dispose();
	}
}
