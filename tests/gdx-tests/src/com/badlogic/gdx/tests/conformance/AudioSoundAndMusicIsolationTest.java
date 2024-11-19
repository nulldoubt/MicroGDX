
package com.badlogic.gdx.tests.conformance;

import com.badlogic.gdx.Micro;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.tests.utils.GdxTest;

/** Test case to validate an issue where soundID is in dirty state and controls a source used by a music.
 * @author mgsx */
public class AudioSoundAndMusicIsolationTest extends GdxTest {
	private Sound sound;
	private Music music;
	private float time;
	private long soundID;

	@Override
	public void create () {
		sound = Micro.audio.newSound(Micro.files.getFileHandle("data/shotgun.ogg", FileType.Internal));
		music = Micro.audio.newMusic(Micro.files.internal("data/8.12.loop.wav"));

		soundID = sound.play();
	}

	@Override
	public void render () {
		time += Micro.graphics.getDeltaTime();
		if (time > 5 && !music.isPlaying()) {
			music.play();
		}
		// after 5 seconds, sound is finished but this code affect music volume instead.
		sound.setVolume(soundID, MathUtils.sinDeg(time * 360) * .5f + .5f);
	}
}
