package me.nulldoubt.micro.backends.lwjgl3.audio.mock;

import me.nulldoubt.micro.audio.AudioDevice;
import me.nulldoubt.micro.audio.AudioRecorder;
import me.nulldoubt.micro.audio.Music;
import me.nulldoubt.micro.audio.Sound;
import me.nulldoubt.micro.backends.lwjgl3.audio.Lwjgl3Audio;
import me.nulldoubt.micro.files.FileHandle;

public class MockAudio implements Lwjgl3Audio {
	
	@Override
	public AudioDevice newAudioDevice(int samplingRate, boolean mono) {
		return new MockAudioDevice();
	}
	
	@Override
	public AudioRecorder newAudioRecorder(int samplingRate, boolean mono) {
		return new MockAudioRecorder();
	}
	
	@Override
	public Sound newSound(FileHandle file) {
		return new MockSound();
	}
	
	@Override
	public Music newMusic(FileHandle file) {
		return new MockMusic();
	}
	
	@Override
	public boolean switchOutputDevice(String device) {
		return true;
	}
	
	@Override
	public String[] getAvailableOutputDevices() {
		return new String[0];
	}
	
	@Override
	public void update() {}
	
	@Override
	public void dispose() {}
	
}
