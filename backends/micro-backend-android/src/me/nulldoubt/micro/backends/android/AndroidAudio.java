package me.nulldoubt.micro.backends.android;

import me.nulldoubt.micro.Audio;
import me.nulldoubt.micro.utils.Disposable;

public interface AndroidAudio extends Audio, Disposable {
	
	void pause();
	
	void resume();
	
	void notifyMusicDisposed(final AndroidMusic music);
	
}
