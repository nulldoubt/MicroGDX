package me.nulldoubt.micro.maps.tiled;

import me.nulldoubt.micro.maps.Map;
import me.nulldoubt.micro.utils.Disposable;
import me.nulldoubt.micro.utils.collections.Array;

public class TiledMap extends Map {
	
	private final TiledMapTileSets tileSets;
	private Array<? extends Disposable> ownedResources;
	
	public TiledMapTileSets getTileSets() {
		return tileSets;
	}
	
	public TiledMap() {
		tileSets = new TiledMapTileSets();
	}
	
	public void setOwnedResources(Array<? extends Disposable> resources) {
		this.ownedResources = resources;
	}
	
	@Override
	public void dispose() {
		if (ownedResources != null)
			for (Disposable resource : ownedResources)
				resource.dispose();
	}
	
}
