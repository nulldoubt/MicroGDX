
package com.badlogic.gdx.tests;

import com.badlogic.gdx.Micro;
import com.badlogic.gdx.tests.utils.GdxTest;

public class ShortSoundTest extends GdxTest {

	@Override
	public void create () {
		Micro.audio.newSound(Micro.files.internal("data/tic.ogg")).play();
	}

}
