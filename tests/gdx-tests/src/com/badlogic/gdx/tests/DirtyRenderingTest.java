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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.tests.utils.GdxTest;
import com.badlogic.gdx.utils.ScreenUtils;

/** Demonstrates how to use non-continuous (aka dirty-only) rendering. The application will clear the screen with a random color
 * every frame it renders. Rendering requests are issued automatically if new input events arrive.
 * 
 * @author mzechner */
public class DirtyRenderingTest extends GdxTest {
	@Override
	public void create () {
		// disable continuous rendering
		Micro.graphics.setContinuousRendering(false);
		Micro.app.log("DirtyRenderingTest", "created");
	}

	@Override
	public void resume () {
		Micro.app.log("DirtyRenderingTest", "resumed");
	}

	@Override
	public void resize (int width, int height) {
		Micro.app.log("DirtyRenderingTest", "resized");
	}

	@Override
	public void pause () {
		Micro.app.log("DirtyRenderingTest", "paused");
	}

	@Override
	public void dispose () {
		Micro.app.log("DirtyRenderingTest", "disposed");
	}

	@Override
	public void render () {
		ScreenUtils.clear(MathUtils.random(), MathUtils.random(), MathUtils.random(), MathUtils.random());
	}
}
