package me.nulldoubt.micro.utils.collections;

import java.util.NoSuchElementException;

public class OrderedMap<K, V> extends ObjectMap<K, V> {
	
	final Array<K> keys;
	
	public OrderedMap() {
		keys = new Array<>();
	}
	
	public OrderedMap(int initialCapacity) {
		super(initialCapacity);
		keys = new Array<>(initialCapacity);
	}
	
	public OrderedMap(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		keys = new Array<>(initialCapacity);
	}
	
	public OrderedMap(OrderedMap<? extends K, ? extends V> map) {
		super(map);
		keys = new Array<>(map.keys);
	}
	
	public V put(K key, V value) {
		int i = locateKey(key);
		if (i >= 0) { // Existing key was found.
			V oldValue = valueTable[i];
			valueTable[i] = value;
			return oldValue;
		}
		i = -(i + 1); // Empty space was found.
		keyTable[i] = key;
		valueTable[i] = value;
		keys.add(key);
		if (++size >= threshold)
			resize(keyTable.length << 1);
		return null;
	}
	
	public <T extends K> void putAll(OrderedMap<T, ? extends V> map) {
		ensureCapacity(map.size);
		K[] keys = map.keys.items;
		for (int i = 0, n = map.keys.size; i < n; i++) {
			K key = keys[i];
			put(key, map.get((T) key));
		}
	}
	
	public V remove(K key) {
		keys.removeValue(key, false);
		return super.remove(key);
	}
	
	public V removeIndex(int index) {
		return super.remove(keys.removeIndex(index));
	}
	
	public boolean alter(K before, K after) {
		if (containsKey(after))
			return false;
		int index = keys.indexOf(before, false);
		if (index == -1)
			return false;
		super.put(after, super.remove(before));
		keys.set(index, after);
		return true;
	}
	
	public boolean alterIndex(int index, K after) {
		if (index < 0 || index >= size || containsKey(after))
			return false;
		super.put(after, super.remove(keys.get(index)));
		keys.set(index, after);
		return true;
	}
	
	public void clear(int maximumCapacity) {
		keys.clear();
		super.clear(maximumCapacity);
	}
	
	public void clear() {
		keys.clear();
		super.clear();
	}
	
	public Array<K> orderedKeys() {
		return keys;
	}
	
	public Entries<K, V> iterator() {
		return entries();
	}
	
	public Entries<K, V> entries() {
		return new OrderedMapEntries<>(this);
	}
	
	public Values<V> values() {
		return new OrderedMapValues<>(this);
	}
	
	public Keys<K> keys() {
		return new OrderedMapKeys<>(this);
	}
	
	protected String toString(String separator, boolean braces) {
		if (size == 0)
			return braces ? "{}" : "";
		StringBuilder buffer = new StringBuilder(32);
		if (braces)
			buffer.append('{');
		Array<K> keys = this.keys;
		for (int i = 0, n = keys.size; i < n; i++) {
			K key = keys.get(i);
			if (i > 0)
				buffer.append(separator);
			buffer.append(key == this ? "(this)" : key);
			buffer.append('=');
			V value = get(key);
			buffer.append(value == this ? "(this)" : value);
		}
		if (braces)
			buffer.append('}');
		return buffer.toString();
	}
	
	public static class OrderedMapEntries<K, V> extends Entries<K, V> {
		
		private final Array<K> keys;
		
		public OrderedMapEntries(OrderedMap<K, V> map) {
			super(map);
			keys = map.keys;
		}
		
		public void reset() {
			currentIndex = -1;
			nextIndex = 0;
			hasNext = map.size > 0;
		}
		
		public Entry next() {
			if (!hasNext)
				throw new NoSuchElementException();
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			currentIndex = nextIndex;
			entry.key = keys.get(nextIndex);
			entry.value = map.get(entry.key);
			nextIndex++;
			hasNext = nextIndex < map.size;
			return entry;
		}
		
		public void remove() {
			if (currentIndex < 0)
				throw new IllegalStateException("next must be called before remove.");
			map.remove(entry.key);
			nextIndex--;
			currentIndex = -1;
		}
		
	}
	
	public static class OrderedMapKeys<K> extends Keys<K> {
		
		private Array<K> keys;
		
		public OrderedMapKeys(OrderedMap<K, ?> map) {
			super(map);
			keys = map.keys;
		}
		
		public void reset() {
			currentIndex = -1;
			nextIndex = 0;
			hasNext = map.size > 0;
		}
		
		public K next() {
			if (!hasNext)
				throw new NoSuchElementException();
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			K key = keys.get(nextIndex);
			currentIndex = nextIndex;
			nextIndex++;
			hasNext = nextIndex < map.size;
			return key;
		}
		
		public void remove() {
			if (currentIndex < 0)
				throw new IllegalStateException("next must be called before remove.");
			((OrderedMap) map).removeIndex(currentIndex);
			nextIndex = currentIndex;
			currentIndex = -1;
		}
		
		public Array<K> toArray(Array<K> array) {
			array.addAll(keys, nextIndex, keys.size - nextIndex);
			nextIndex = keys.size;
			hasNext = false;
			return array;
		}
		
		public Array<K> toArray() {
			return toArray(new Array<>(true, keys.size - nextIndex));
		}
		
	}
	
	public static class OrderedMapValues<V> extends Values<V> {
		
		private final Array keys;
		
		public OrderedMapValues(OrderedMap<?, V> map) {
			super(map);
			keys = map.keys;
		}
		
		public void reset() {
			currentIndex = -1;
			nextIndex = 0;
			hasNext = map.size > 0;
		}
		
		public V next() {
			if (!hasNext)
				throw new NoSuchElementException();
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			V value = map.get(keys.get(nextIndex));
			currentIndex = nextIndex;
			nextIndex++;
			hasNext = nextIndex < map.size;
			return value;
		}
		
		public void remove() {
			if (currentIndex < 0)
				throw new IllegalStateException("next must be called before remove.");
			((OrderedMap) map).removeIndex(currentIndex);
			nextIndex = currentIndex;
			currentIndex = -1;
		}
		
		public Array<V> toArray(Array<V> array) {
			int n = keys.size;
			array.ensureCapacity(n - nextIndex);
			Object[] keys = this.keys.items;
			for (int i = nextIndex; i < n; i++)
				array.add(map.get(keys[i]));
			currentIndex = n - 1;
			nextIndex = n;
			hasNext = false;
			return array;
		}
		
		public Array<V> toArray() {
			return toArray(new Array<>(true, keys.size - nextIndex));
		}
		
	}
	
}