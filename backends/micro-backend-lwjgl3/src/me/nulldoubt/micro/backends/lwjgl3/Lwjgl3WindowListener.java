package me.nulldoubt.micro.backends.lwjgl3;

public interface Lwjgl3WindowListener {
	
	default void created(Lwjgl3Window window) {}
	
	default void iconified(boolean isIconified) {}
	
	default void maximized(boolean isMaximized) {}
	
	default void focusLost() {}
	
	default void focusGained() {}
	
	default boolean closeRequested() {
		return false;
	}
	
	default void filesDropped(String[] files) {}
	
	default void refreshRequested() {}
	
}
