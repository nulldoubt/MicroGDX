package me.nulldoubt.micro.audio;

import me.nulldoubt.micro.utils.Disposable;

public interface AudioDevice extends Disposable {
	
	boolean isMono();
	
	void writeSamples(short[] samples, int offset, int numSamples);
	
	void writeSamples(float[] samples, int offset, int numSamples);
	
	int getLatency();
	
	void dispose();
	
	void setVolume(float volume);
	
	void pause();
	
	void resume();
	
}
