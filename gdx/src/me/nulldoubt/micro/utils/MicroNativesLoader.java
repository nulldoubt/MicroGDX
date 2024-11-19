package me.nulldoubt.micro.utils;

import com.badlogic.gdx.utils.SharedLibraryLoader;

public class MicroNativesLoader {
	
	public static boolean disableNativesLoading = false;
	
	private static boolean nativesLoaded;
	
	public static synchronized void load() {
		if (nativesLoaded)
			return;
		
		if (disableNativesLoading)
			return;
		
		new SharedLibraryLoader().load("gdx");
		nativesLoaded = true;
	}
	
}
