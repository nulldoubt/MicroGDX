package me.nulldoubt.micro.maps.tiled;

import me.nulldoubt.micro.utils.collections.Array;

import java.util.Iterator;

public class TiledMapTileSets implements Iterable<TiledMapTileSet> {
	
	private final Array<TiledMapTileSet> tileSets;
	
	public TiledMapTileSets() {
		tileSets = new Array<>();
	}
	
	public TiledMapTileSet getTileSet(int index) {
		return tileSets.get(index);
	}
	
	public TiledMapTileSet getTileSet(String name) {
		for (TiledMapTileSet tileset : tileSets)
			if (name.equals(tileset.name))
				return tileset;
		return null;
	}
	
	public void addTileSet(TiledMapTileSet tileset) {
		tileSets.add(tileset);
	}
	
	public void removeTileSet(int index) {
		tileSets.removeIndex(index);
	}
	
	public void removeTileSet(TiledMapTileSet tileset) {
		tileSets.removeValue(tileset, true);
	}
	
	public TiledMapTile getTile(int id) {
		for (int i = tileSets.size - 1; i >= 0; i--) {
			TiledMapTileSet tileset = tileSets.get(i);
			TiledMapTile tile = tileset.getTile(id);
			if (tile != null)
				return tile;
		}
		return null;
	}
	
	@Override
	public Iterator<TiledMapTileSet> iterator() {
		return tileSets.iterator();
	}
	
}
