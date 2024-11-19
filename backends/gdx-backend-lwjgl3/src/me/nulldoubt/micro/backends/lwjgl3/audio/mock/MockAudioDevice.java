package me.nulldoubt.micro.backends.lwjgl3.audio.mock;

import me.nulldoubt.micro.audio.AudioDevice;

public class MockAudioDevice implements AudioDevice {
	
	@Override
	public boolean isMono() {
		return false;
	}
	
	@Override
	public void writeSamples(short[] samples, int offset, int numSamples) {}
	
	@Override
	public void writeSamples(float[] samples, int offset, int numSamples) {}
	
	@Override
	public int getLatency() {
		return 0;
	}
	
	@Override
	public void dispose() {}
	
	@Override
	public void setVolume(float volume) {}
	
	@Override
	public void pause() {}
	
	@Override
	public void resume() {}
	
}
