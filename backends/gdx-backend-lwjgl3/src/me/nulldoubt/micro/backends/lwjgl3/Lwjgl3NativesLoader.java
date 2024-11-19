package me.nulldoubt.micro.backends.lwjgl3;

import me.nulldoubt.micro.utils.MicroNativesLoader;

public final class Lwjgl3NativesLoader {
	
	static {
		System.setProperty("org.lwjgl.input.Mouse.allowNegativeMouseCoords", "true");
	}
	
	public static void load() {
		MicroNativesLoader.load();
	}
	
}
