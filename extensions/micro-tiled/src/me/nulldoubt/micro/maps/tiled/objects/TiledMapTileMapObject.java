package me.nulldoubt.micro.maps.tiled.objects;

import me.nulldoubt.micro.graphics.g2d.TextureRegion;
import me.nulldoubt.micro.maps.tiled.TiledMapTile;
import me.nulldoubt.micro.objects.TextureMapObject;

public class TiledMapTileMapObject extends TextureMapObject {
	
	public boolean flipHorizontally;
	public boolean flipVertically;
	
	public TiledMapTile tile;
	
	public TiledMapTileMapObject(TiledMapTile tile, boolean flipHorizontally, boolean flipVertically) {
		this.flipHorizontally = flipHorizontally;
		this.flipVertically = flipVertically;
		this.tile = tile;
		
		textureRegion = new TextureRegion(tile.getTextureRegion());
		textureRegion.flip(flipHorizontally, flipVertically);
	}
	
}
