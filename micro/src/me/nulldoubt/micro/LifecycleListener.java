package me.nulldoubt.micro;

import me.nulldoubt.micro.utils.Disposable;

public interface LifecycleListener extends Disposable {
	
	default void pause() {}
	
	default void resume() {}
	
}
