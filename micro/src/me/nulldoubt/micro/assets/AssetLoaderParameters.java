package me.nulldoubt.micro.assets;

public class AssetLoaderParameters<T> {
	
	public interface LoadedCallback {
		
		void finishedLoading(AssetManager assetManager, String fileName, Class<?> type);
		
	}
	
	public LoadedCallback loadedCallback;
	
}
