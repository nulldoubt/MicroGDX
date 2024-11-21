package me.nulldoubt.micro.backends.lwjgl3;

import me.nulldoubt.micro.Input;
import me.nulldoubt.micro.utils.Disposable;

public interface Lwjgl3Input extends Input, Disposable {
	
	void windowHandleChanged(long windowHandle);
	
	void update();
	
	void prepareNext();
	
	void resetPollingStates();
	
}
