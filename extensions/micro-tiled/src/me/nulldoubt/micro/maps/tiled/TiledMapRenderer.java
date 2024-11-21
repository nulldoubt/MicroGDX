package me.nulldoubt.micro.maps.tiled;

import me.nulldoubt.micro.maps.MapLayer;
import me.nulldoubt.micro.maps.MapObject;
import me.nulldoubt.micro.maps.MapRenderer;

public interface TiledMapRenderer extends MapRenderer {
	
	void renderObjects(final MapLayer layer);
	
	void renderObject(final MapObject object);
	
	void renderTileLayer(final TiledMapTileLayer layer);
	
	void renderImageLayer(final TiledMapImageLayer layer);
	
}
