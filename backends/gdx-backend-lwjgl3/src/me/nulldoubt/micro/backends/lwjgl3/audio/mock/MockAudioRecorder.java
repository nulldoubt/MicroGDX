package me.nulldoubt.micro.backends.lwjgl3.audio.mock;

import me.nulldoubt.micro.audio.AudioRecorder;

public class MockAudioRecorder implements AudioRecorder {
	
	@Override
	public void read(short[] samples, int offset, int numSamples) {}
	
	@Override
	public void dispose() {}
	
}
