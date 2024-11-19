
package me.nulldoubt.micro.backends.android;

import me.nulldoubt.micro.audio.AudioDevice;
import me.nulldoubt.micro.audio.AudioRecorder;
import me.nulldoubt.micro.audio.Music;
import me.nulldoubt.micro.audio.Sound;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.utils.GdxRuntimeException;

public class DisabledAndroidAudio implements AndroidAudio {

	@Override
	public void pause () {
	}

	@Override
	public void resume () {
	}

	@Override
	public void notifyMusicDisposed (AndroidMusic music) {
	}

	@Override
	public AudioDevice newAudioDevice (int samplingRate, boolean mono) {
		throw new GdxRuntimeException("Android audio is not enabled by the application config");
	}

	@Override
	public AudioRecorder newAudioRecorder (int samplingRate, boolean mono) {
		throw new GdxRuntimeException("Android audio is not enabled by the application config");
	}

	@Override
	public Sound newSound (FileHandle file) {
		throw new GdxRuntimeException("Android audio is not enabled by the application config");
	}

	@Override
	public Music newMusic (FileHandle file) {
		throw new GdxRuntimeException("Android audio is not enabled by the application config");
	}

	@Override
	public boolean switchOutputDevice (String device) {
		return false;
	}

	@Override
	public String[] getAvailableOutputDevices () {
		return new String[0];
	}

	@Override
	public void dispose () {
	}
}
