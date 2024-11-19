
package me.nulldoubt.micro.backends.android;

import me.nulldoubt.micro.Audio;
import me.nulldoubt.micro.utils.Disposable;

public interface AndroidAudio extends Audio, Disposable {

	/** Pauses all playing sounds and musics **/
	void pause ();

	/** Resumes all playing sounds and musics **/
	void resume ();

	/** Notifies the AndroidAudio if an AndroidMusic is disposed **/
	void notifyMusicDisposed (AndroidMusic music);
}
