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
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.tests.utils.GdxTest;

public class ClipboardTest extends GdxTest {

	Stage stage;
	TextArea textArea;
	TextButton buttonCopy;
	TextButton buttonPaste;
	TextButton buttonHasContents;

	@Override
	public void create () {
		stage = new Stage();
		Skin skin = new Skin(Micro.files.internal("data/uiskin.json"));
		textArea = new TextArea("", skin);
		buttonCopy = new TextButton("Copy", skin);
		buttonPaste = new TextButton("Paste", skin);
		buttonHasContents = new TextButton("Has Contents: " + Micro.app.getClipboard().hasContents(), skin);

		textArea.setSize(Micro.graphics.getWidth() / 3f, Micro.graphics.getHeight() / 3f);

		textArea.setPosition(Micro.graphics.getWidth() / 2f - textArea.getWidth() / 2f,
			Micro.graphics.getHeight() / 2f - textArea.getHeight() / 2f);
		buttonCopy.setPosition(Micro.graphics.getWidth() / 4f - buttonCopy.getWidth() / 2f,
			Micro.graphics.getHeight() / 4f - buttonCopy.getHeight() / 2f);
		buttonPaste.setPosition(3 * Micro.graphics.getWidth() / 4f - buttonPaste.getWidth() / 2f,
			Micro.graphics.getHeight() / 4f - buttonPaste.getHeight() / 2f);
		buttonHasContents.setPosition(Micro.graphics.getWidth() / 2f - buttonHasContents.getWidth() / 2f,
			Micro.graphics.getHeight() / 4f - buttonHasContents.getHeight() / 2f);

		buttonCopy.addListener(new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				Micro.app.getClipboard().setContents(textArea.getText());
			}
		});
		buttonPaste.addListener(new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				textArea.setText(Micro.app.getClipboard().getContents());
			}
		});
		buttonHasContents.addListener(new ChangeListener() {
			@Override
			public void changed (ChangeEvent event, Actor actor) {
				buttonHasContents.setText("Has Contents: " + Micro.app.getClipboard().hasContents());
			}
		});

		stage.addActor(textArea);
		stage.addActor(buttonCopy);
		stage.addActor(buttonPaste);
		stage.addActor(buttonHasContents);

		Micro.input.setInputProcessor(stage);
	}

	@Override
	public void render () {
		stage.act();
		stage.draw();
	}
}
