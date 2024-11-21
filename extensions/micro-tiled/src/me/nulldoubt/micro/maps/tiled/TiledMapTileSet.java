package me.nulldoubt.micro.maps.tiled;

import me.nulldoubt.micro.maps.MapProperties;
import me.nulldoubt.micro.utils.collections.IntMap;

import java.util.Iterator;

public class TiledMapTileSet implements Iterable<TiledMapTile> {
	
	public String name;
	
	public final MapProperties properties;
	private final IntMap<TiledMapTile> tiles;
	
	public TiledMapTileSet() {
		tiles = new IntMap<>();
		properties = new MapProperties();
	}
	
	public TiledMapTile getTile(int id) {
		return tiles.get(id);
	}
	
	@Override
	public Iterator<TiledMapTile> iterator() {
		return tiles.values().iterator();
	}
	
	public void putTile(final int id, final TiledMapTile tile) {
		tiles.put(id, tile);
	}
	
	public void removeTile(final int id) {
		tiles.remove(id);
	}
	
	public int size() {
		return tiles.size;
	}
	
}
