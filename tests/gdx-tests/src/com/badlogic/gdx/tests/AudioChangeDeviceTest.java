
package com.badlogic.gdx.tests;

import com.badlogic.gdx.Micro;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.tests.utils.GdxTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AudioChangeDeviceTest extends GdxTest {

	private Stage stage;
	private Skin skin;
	private Sound sound;

	@Override
	public void create () {
		stage = new Stage();
		Micro.input.setInputProcessor(stage);
		skin = new Skin(Micro.files.internal("data/uiskin.json"));
		final SelectBox<String> selectBox = new SelectBox<>(skin);
		List<String> tmp = new ArrayList<>(Arrays.asList(Micro.audio.getAvailableOutputDevices()));
		tmp.add(0, "Auto");
		selectBox.setItems(tmp.toArray(new String[0]));
		sound = Micro.audio.newSound(Micro.files.internal("data").child("bubblepop-stereo-left-only.wav"));
		sound.loop();
		selectBox.addListener(new ChangeListener() {

			@Override
			public void changed (ChangeEvent event, Actor actor) {
				if (selectBox.getSelected().equals("Auto")) {
					Micro.app.getAudio().switchOutputDevice(null);
					return;
				}
				Micro.app.getAudio().switchOutputDevice(selectBox.getSelected());
			}
		});
		selectBox.setWidth(200);
		selectBox.setPosition(200, 200);

		stage.addActor(selectBox);
	}

	@Override
	public void render () {
		Micro.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		stage.act(Micro.graphics.getDeltaTime());
		stage.draw();
	}

	@Override
	public void resize (int width, int height) {
		stage.getViewport().update(width, height, true);
	}

	@Override
	public void dispose () {
		stage.dispose();
		skin.dispose();
		sound.stop();
		sound.dispose();
	}
}
