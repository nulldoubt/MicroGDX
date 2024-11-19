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

package com.badlogic.gdx.tests.gwt;

import com.badlogic.gdx.Micro;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.tests.utils.GdxTest;

public class GwtWindowModeTest extends GdxTest {
	private Stage stage;
	TextButton changeModeButton;
	private final String windowedInstructions = "click for Full screen Mode";
	private final String fullScreenInstructions = "click for window Mode";
	private final String notSupported = "Changing the display mode is not supported";

	public void create () {
		stage = new Stage();
		Micro.input.setInputProcessor(stage);
		Skin skin = new Skin(Micro.files.internal("data/uiskin.json"));

		changeModeButton = new TextButton(windowedInstructions, skin);
		changeModeButton.setSize(Micro.graphics.getWidth() / 2, Micro.graphics.getHeight() / 2);
		changeModeButton.setPosition(Micro.graphics.getWidth() / 4, Micro.graphics.getHeight() / 4);
		stage.addActor(changeModeButton);

		changeModeButton.addListener(new ClickListener() {
			@Override
			public void clicked (InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				if (!Micro.graphics.isFullscreen()) {
					Micro.graphics.setFullscreenMode(Micro.graphics.getDisplayMode());
				} else {
					Micro.graphics.setWindowedMode(Micro.graphics.getWidth(), Micro.graphics.getHeight());
				}
			}
		});
		if (!Micro.graphics.supportsDisplayModeChange()) {
			changeModeButton.setDisabled(true);
			changeModeButton.setText(notSupported);
		}
	}

	public void render () {
		Micro.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		stage.act(Micro.graphics.getDeltaTime());
		stage.draw();
	}

	public void resize (int width, int height) {
		stage.getViewport().update(width, height, true);
		if (Micro.graphics.supportsDisplayModeChange()) {
			if (Micro.graphics.isFullscreen()) {
				changeModeButton.setText(fullScreenInstructions);
			} else {
				changeModeButton.setText(windowedInstructions);
			}
		}
	}

	public void dispose () {
		stage.dispose();
	}
}
