package me.nulldoubt.micro.maps;

import me.nulldoubt.micro.utils.collections.ObjectMap;

import java.util.Iterator;

public class MapProperties {
	
	private final ObjectMap<String, Object> properties;
	
	public MapProperties() {
		properties = new ObjectMap<>();
	}
	
	public boolean containsKey(final String key) {
		return properties.containsKey(key);
	}
	
	public Object get(final String key) {
		return properties.get(key);
	}
	
	public <T> T get(final String key, final Class<T> clazz) {
		return (T) get(key);
	}
	
	public <T> T get(final String key, final T defaultValue, final Class<T> clazz) {
		final Object object = get(key);
		return object == null ? defaultValue : (T) object;
	}
	
	public void put(final String key, final Object value) {
		properties.put(key, value);
	}
	
	public void putAll(final MapProperties properties) {
		this.properties.putAll(properties.properties);
	}
	
	public void remove(final String key) {
		properties.remove(key);
	}
	
	public void clear() {
		properties.clear();
	}
	
	public Iterator<String> getKeys() {
		return properties.keys();
	}
	
	public Iterator<Object> getValues() {
		return properties.values();
	}
	
}
