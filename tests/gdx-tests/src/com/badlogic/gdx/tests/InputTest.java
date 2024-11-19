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
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.tests.utils.GdxTest;

public class InputTest extends GdxTest implements InputProcessor {

	@Override
	public void create () {
// Gdx.input = new RemoteInput();
		Micro.input.setInputProcessor(this);
// Gdx.input.setCursorCatched(true);
//
// Gdx.input.getTextInput(new Input.TextInputListener() {
// @Override
// public void input(String text) {
// Gdx.app.log("Input test", "Input value: " + text);
// }
//
// @Override
// public void canceled() {
// Gdx.app.log("Input test", "Canceled input text");
// }
// }, "Title", "Text", "Placeholder");
	}

	@Override
	public void render () {
		if (Micro.input.justTouched()) {
			Micro.app.log("Input Test", "just touched, button: " + (Micro.input.isButtonPressed(Buttons.LEFT) ? "left " : "")
				+ (Micro.input.isButtonPressed(Buttons.MIDDLE) ? "middle " : "")
				+ (Micro.input.isButtonPressed(Buttons.RIGHT) ? "right" : "") + (Micro.input.isButtonPressed(Buttons.BACK) ? "back" : "")
				+ (Micro.input.isButtonPressed(Buttons.FORWARD) ? "forward" : ""));
		}

		for (int i = 0; i < 10; i++) {
			if (Micro.input.getDeltaX(i) != 0 || Micro.input.getDeltaY(i) != 0) {
				Micro.app.log("Input Test", "delta[" + i + "]: " + Micro.input.getDeltaX(i) + ", " + Micro.input.getDeltaY(i));
			}
		}
// Gdx.input.setCursorPosition(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
// if(Gdx.input.isTouched()) {
// Gdx.app.log("Input Test", "is touched");
// }
	}

	@Override
	public boolean keyDown (int keycode) {
		Micro.app.log("Input Test", "key down: " + keycode);
		if (keycode == Keys.G) Micro.input.setCursorCatched(!Micro.input.isCursorCatched());
		return false;
	}

	@Override
	public boolean keyTyped (char character) {
		Micro.app.log("Input Test", "key typed: '" + character + "'");
		return false;
	}

	@Override
	public boolean keyUp (int keycode) {
		Micro.app.log("Input Test", "key up: " + keycode);
		return false;
	}

	@Override
	public boolean touchDown (int x, int y, int pointer, int button) {
		Micro.app.log("Input Test", "touch down: " + x + ", " + y + ", button: " + getButtonString(button));
		return false;
	}

	@Override
	public boolean touchDragged (int x, int y, int pointer) {
		Micro.app.log("Input Test", "touch dragged: " + x + ", " + y + ", pointer: " + pointer);
		return false;
	}

	@Override
	public boolean touchUp (int x, int y, int pointer, int button) {
		Micro.app.log("Input Test", "touch up: " + x + ", " + y + ", button: " + getButtonString(button));
		return false;
	}

	@Override
	public boolean mouseMoved (int x, int y) {
		Micro.app.log("Input Test", "touch moved: " + x + ", " + y);
		return false;
	}

	@Override
	public boolean scrolled (float amountX, float amountY) {
		Micro.app.log("Input Test", "scrolled: " + amountY);
		return false;
	}

	private String getButtonString (int button) {
		if (button == Buttons.LEFT) return "left";
		if (button == Buttons.RIGHT) return "right";
		if (button == Buttons.MIDDLE) return "middle";
		if (button == Buttons.BACK) return "back";
		if (button == Buttons.FORWARD) return "forward";
		return "unknown";
	}
}
