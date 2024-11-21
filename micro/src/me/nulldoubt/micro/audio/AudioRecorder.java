package me.nulldoubt.micro.audio;

import me.nulldoubt.micro.utils.Disposable;

public interface AudioRecorder extends Disposable {
	
	void read(short[] samples, int offset, int numSamples);
	
}
