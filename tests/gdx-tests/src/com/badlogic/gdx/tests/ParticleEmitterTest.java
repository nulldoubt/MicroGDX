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
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.tests.utils.GdxTest;
import com.badlogic.gdx.utils.Array;

public class ParticleEmitterTest extends GdxTest {
	private SpriteBatch spriteBatch;
	ParticleEffect effect;
	int emitterIndex;
	Array<ParticleEmitter> emitters;
	int particleCount = 10;
	float fpsCounter;
	InputProcessor inputProcessor;

	@Override
	public void create () {
		spriteBatch = new SpriteBatch();

		effect = new ParticleEffect();
		effect.load(Micro.files.internal("data/test.p"), Micro.files.internal("data"));
		effect.setPosition(Micro.graphics.getWidth() / 2, Micro.graphics.getHeight() / 2);
		// Of course, a ParticleEffect is normally just used, without messing around with its emitters.
		emitters = new Array(effect.getEmitters());
		effect.getEmitters().clear();
		effect.getEmitters().add(emitters.get(0));

		inputProcessor = new InputAdapter() {

			public boolean touchDragged (int x, int y, int pointer) {
				effect.setPosition(x, Micro.graphics.getHeight() - y);
				return false;
			}

			public boolean touchDown (int x, int y, int pointer, int newParam) {
				// effect.setPosition(x, Gdx.graphics.getHeight() - y);
				ParticleEmitter emitter = emitters.get(emitterIndex);
				particleCount += 100;
				System.out.println(particleCount);
				particleCount = Math.max(0, particleCount);
				if (particleCount > emitter.getMaxParticleCount()) emitter.setMaxParticleCount(particleCount * 2);
				emitter.getEmission().setHigh(particleCount / emitter.getLife().getHighMax() * 1000);
				effect.getEmitters().clear();
				effect.getEmitters().add(emitter);
				return false;
			}

			public boolean keyDown (int keycode) {
				ParticleEmitter emitter = emitters.get(emitterIndex);
				if (keycode == Input.Keys.DPAD_UP)
					particleCount += 5;
				else if (keycode == Input.Keys.PLUS) {
					emitter = new ParticleEmitter(emitter);
				} else if (keycode == Input.Keys.DPAD_DOWN)
					particleCount -= 5;
				else if (keycode == Input.Keys.SPACE) {
					emitterIndex = (emitterIndex + 1) % emitters.size;
					emitter = emitters.get(emitterIndex);

					// if we've previously stopped the emitter reset it
					if (emitter.isComplete()) emitter.reset();
					particleCount = (int)(emitter.getEmission().getHighMax() * emitter.getLife().getHighMax() / 1000f);
				} else if (keycode == Input.Keys.ENTER) {
					emitter = emitters.get(emitterIndex);
					if (emitter.isComplete())
						emitter.reset();
					else
						emitter.allowCompletion();
				} else
					return false;
				particleCount = Math.max(0, particleCount);
				if (particleCount > emitter.getMaxParticleCount()) emitter.setMaxParticleCount(particleCount * 2);
				emitter.getEmission().setHigh(particleCount / emitter.getLife().getHighMax() * 1000);
				effect.getEmitters().clear();
				effect.getEmitters().add(emitter);
				return false;
			}
		};

		Micro.input.setInputProcessor(inputProcessor);
	}

	@Override
	public void dispose () {
		spriteBatch.dispose();
		effect.dispose();
	}

	public void render () {
		spriteBatch.getProjectionMatrix().setToOrtho2D(0, 0, Micro.graphics.getWidth(), Micro.graphics.getHeight());
		float delta = Micro.graphics.getDeltaTime();
		Micro.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		spriteBatch.begin();
		effect.draw(spriteBatch, delta);
		spriteBatch.end();
		fpsCounter += delta;
		if (fpsCounter > 3) {
			fpsCounter = 0;
			int activeCount = emitters.get(emitterIndex).getActiveCount();
			Micro.app.log("libGDX", activeCount + "/" + particleCount + " particles, FPS: " + Micro.graphics.getFPS());
		}
	}

	public boolean needsGL20 () {
		return false;
	}
}
