package me.nulldoubt.micro;

import me.nulldoubt.micro.audio.AudioDevice;
import me.nulldoubt.micro.audio.AudioRecorder;
import me.nulldoubt.micro.audio.Music;
import me.nulldoubt.micro.audio.Sound;
import me.nulldoubt.micro.files.FileHandle;

public interface Audio {
	
	AudioDevice newAudioDevice(final int samplingRate, final boolean mono);
	
	AudioRecorder newAudioRecorder(final int samplingRate, final boolean mono);
	
	Sound newSound(final FileHandle file);
	
	Music newMusic(final FileHandle file);
	
	boolean switchOutputDevice(final String device);
	
	String[] getAvailableOutputDevices();
	
}
