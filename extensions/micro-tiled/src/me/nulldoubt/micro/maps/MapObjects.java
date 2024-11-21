package me.nulldoubt.micro.maps;

import me.nulldoubt.micro.utils.collections.Array;

import java.util.Iterator;

public class MapObjects implements Iterable<MapObject> {
	
	private final Array<MapObject> objects;
	
	public MapObjects() {
		objects = new Array<>();
	}
	
	public MapObject get(final int index) {
		return objects.get(index);
	}
	
	public MapObject get(final String name) {
		for (int i = 0, n = objects.size; i < n; i++) {
			final MapObject object = objects.get(i);
			if (name.equals(object.name))
				return object;
		}
		return null;
	}
	
	public int getIndex(final String name) {
		return getIndex(get(name));
	}
	
	public int getIndex(final MapObject object) {
		return objects.indexOf(object, true);
	}
	
	public int getCount() {
		return objects.size;
	}
	
	public void add(final MapObject object) {
		this.objects.add(object);
	}
	
	public void remove(final int index) {
		objects.removeIndex(index);
	}
	
	public void remove(final MapObject object) {
		objects.removeValue(object, true);
	}
	
	public <T extends MapObject> Array<T> getByType(final Class<T> type) {
		return getByType(type, new Array<T>());
	}
	
	public <T extends MapObject> Array<T> getByType(final Class<T> type, final Array<T> fill) {
		fill.clear();
		for (int i = 0, n = objects.size; i < n; i++) {
			final MapObject object = objects.get(i);
			if (type.isInstance(object))
				fill.add((T) object);
		}
		return fill;
	}
	
	@Override
	public Iterator<MapObject> iterator() {
		return objects.iterator();
	}
	
}
