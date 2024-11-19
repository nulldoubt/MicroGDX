/*******************************************************************************
 * Copyright 2022 See AUTHORS file.
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

package com.badlogic.gdx.tests.gles3;

import com.badlogic.gdx.Micro;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.Shader;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.tests.utils.GdxTest;
import com.badlogic.gdx.tests.utils.GdxTestConfig;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/** Added during WebGL2 implementation but also applicable to Desktop. UBO's were added in WebGL2, this test uses a UBO to send
 * color and position data to the shader using a buffer.
 * @author JamesTKhan */
@GdxTestConfig(requireGL30 = true)
public class UniformBufferObjectsTest extends GdxTest {

	Skin skin;
	Stage stage;
	Table table;

	RandomXS128 random;
	SpriteBatch batch;
	Texture texture;
	Shader shader;
	FloatBuffer uniformBuffer = BufferUtils.newFloatBuffer(16);

	float lerpToR = 1.0f;
	float lerpToG = 1.0f;
	float lerpToB = 1.0f;
	float elapsedTime = 0;

	@Override
	public void create () {
		random = new RandomXS128();
		batch = new SpriteBatch();
		texture = new Texture(Micro.files.internal("data/badlogic.jpg"));
		texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

		shader = new Shader(Micro.files.internal("data/shaders/ubo.vert"), Micro.files.internal("data/shaders/ubo.frag"));

		Micro.app.log("UniformBufferObjectsTest", shader.getLog());
		if (shader.isCompiled()) {
			Micro.app.log("UniformBufferObjectsTest", "Shader compiled");
			batch.setShader(shader);
		}

		IntBuffer tmpBuffer = BufferUtils.newIntBuffer(16);

		// Get the block index for the uniform block
		int blockIndex = Micro.gl30.glGetUniformBlockIndex(shader.getHandle(), "u_bufferBlock");

		// Use the index to get the active block uniform count
		Micro.gl30.glGetActiveUniformBlockiv(shader.getHandle(), blockIndex, GL30.GL_UNIFORM_BLOCK_ACTIVE_UNIFORMS, tmpBuffer);
		int activeUniforms = tmpBuffer.get(0);

		tmpBuffer.clear();
		Micro.gl30.glGenBuffers(1, tmpBuffer);
		int bufferHandle = tmpBuffer.get(0);

		Micro.gl.glBindBuffer(GL30.GL_UNIFORM_BUFFER, bufferHandle);
		Micro.gl.glBufferData(GL30.GL_UNIFORM_BUFFER, 16, uniformBuffer, GL30.GL_STATIC_DRAW);

		int bindingPoint = 0;
		// Use the index to bind to a binding point, then bind the buffer
		Micro.gl30.glUniformBlockBinding(shader.getHandle(), blockIndex, bindingPoint);
		Micro.gl30.glBindBufferBase(GL30.GL_UNIFORM_BUFFER, bindingPoint, bufferHandle);

		// UI
		skin = new Skin(Micro.files.internal("data/uiskin.json"));
		stage = new Stage(new ScreenViewport());
		Micro.input.setInputProcessor(stage);

		table = new Table();
		table.add(new Label("Block Uniforms (2 is expected):" + activeUniforms, skin)).row();
		table.add(new Label("Block Index (-1 is invalid): " + blockIndex, skin));
		stage.addActor(table);
	}

	@Override
	public void render () {
		ScreenUtils.clear(0, 0, 0, 1);
		elapsedTime += Micro.graphics.getDeltaTime();

		if (elapsedTime > 2f) {
			elapsedTime = 0;
			lerpToR = random.nextFloat();
			lerpToG = random.nextFloat();
			lerpToB = random.nextFloat();
		}

		// Update the colors
		uniformBuffer.put(0, Interpolation.smooth.apply(uniformBuffer.get(0), lerpToR, Micro.graphics.getDeltaTime() * 2));// ColorBuffer.R
		uniformBuffer.put(1, Interpolation.smooth.apply(uniformBuffer.get(1), lerpToG, Micro.graphics.getDeltaTime() * 2));// ColorBuffer.G
		uniformBuffer.put(2, Interpolation.smooth.apply(uniformBuffer.get(2), lerpToB, Micro.graphics.getDeltaTime() * 2));// ColorBuffer.B

		// Update the positions
		uniformBuffer.put(4, Interpolation.smooth.apply(uniformBuffer.get(4), lerpToR, Micro.graphics.getDeltaTime() * 2));// Position.X
		uniformBuffer.put(5, Interpolation.smooth.apply(uniformBuffer.get(5), lerpToG, Micro.graphics.getDeltaTime() * 2));// Position.Y

		// Update the buffer data store
		Micro.gl30.glBufferSubData(GL30.GL_UNIFORM_BUFFER, 0, uniformBuffer.capacity() * 4, uniformBuffer);

		batch.begin();
		batch.draw(texture, 0, 0, Micro.graphics.getWidth() / 2f, Micro.graphics.getHeight() / 2f);
		batch.end();

		stage.act();
		stage.draw();
	}

	@Override
	public void resize (int width, int height) {
		stage.getViewport().update(width, height, true);
		batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);

		table.setPosition(stage.getViewport().getScreenWidth() * .25f, stage.getViewport().getScreenHeight() * .95f);
	}

	@Override
	public void dispose () {
		texture.dispose();
		batch.dispose();
		shader.dispose();
	}
}
