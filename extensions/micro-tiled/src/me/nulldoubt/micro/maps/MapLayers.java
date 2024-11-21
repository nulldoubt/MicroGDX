package me.nulldoubt.micro.maps;

import me.nulldoubt.micro.utils.collections.Array;

import java.util.Iterator;

public class MapLayers implements Iterable<MapLayer> {
	
	private final Array<MapLayer> layers;
	
	public MapLayers() {
		layers = new Array<>();
	}
	
	public MapLayer get(final int index) {
		return layers.get(index);
	}
	
	public MapLayer get(final String name) {
		for (int i = 0, n = layers.size; i < n; i++) {
			final MapLayer layer = layers.get(i);
			if (name.equals(layer.name))
				return layer;
		}
		return null;
	}
	
	public int getIndex(final String name) {
		return getIndex(get(name));
	}
	
	public int getIndex(final MapLayer layer) {
		return layers.indexOf(layer, true);
	}
	
	public int getCount() {
		return layers.size;
	}
	
	public void add(final MapLayer layer) {
		this.layers.add(layer);
	}
	
	public void remove(final int index) {
		layers.removeIndex(index);
	}
	
	public void remove(final MapLayer layer) {
		layers.removeValue(layer, true);
	}
	
	public int size() {
		return layers.size;
	}
	
	public <T extends MapLayer> Array<T> getByType(final Class<T> type) {
		return getByType(type, new Array<>());
	}
	
	public <T extends MapLayer> Array<T> getByType(final Class<T> type, final Array<T> fill) {
		fill.clear();
		for (int i = 0, n = layers.size; i < n; i++) {
			final MapLayer layer = layers.get(i);
			if (type.isInstance(layer))
				fill.add((T) layer);
		}
		return fill;
	}
	
	@Override
	public Iterator<MapLayer> iterator() {
		return layers.iterator();
	}
	
}
