package me.nulldoubt.micro.maps;

import me.nulldoubt.micro.utils.Disposable;

public abstract class Map implements Disposable {
	
	public final MapLayers layers;
	public final MapProperties properties;
	
	public Map() {
		layers = new MapLayers();
		properties = new MapProperties();
	}
	
}
