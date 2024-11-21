package me.nulldoubt.micro.backends.lwjgl3.audio;

import me.nulldoubt.micro.Audio;
import me.nulldoubt.micro.utils.Disposable;

public interface Lwjgl3Audio extends Audio, Disposable {
	
	void update();
	
}
