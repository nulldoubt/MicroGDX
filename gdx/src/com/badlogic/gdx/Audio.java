package com.badlogic.gdx;

import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.audio.AudioRecorder;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;

public interface Audio {
	
	AudioDevice newAudioDevice(final int samplingRate, final boolean mono);
	
	AudioRecorder newAudioRecorder(final int samplingRate, final boolean mono);
	
	Sound newSound(final FileHandle file);
	
	Music newMusic(final FileHandle file);
	
	boolean switchOutputDevice(final String device);
	
	String[] getAvailableOutputDevices();
	
}
