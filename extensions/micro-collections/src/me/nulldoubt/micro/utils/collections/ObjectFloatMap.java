package me.nulldoubt.micro.utils.collections;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ObjectFloatMap<K> implements Iterable<ObjectFloatMap.Entry<K>> {
	
	public int size;
	
	K[] keyTable;
	float[] valueTable;
	
	float loadFactor;
	int threshold;
	
	protected int shift;
	
	protected int mask;
	
	public ObjectFloatMap() {
		this(51, 0.8f);
	}
	
	public ObjectFloatMap(int initialCapacity) {
		this(initialCapacity, 0.8f);
	}
	
	public ObjectFloatMap(int initialCapacity, float loadFactor) {
		if (loadFactor <= 0f || loadFactor >= 1f)
			throw new IllegalArgumentException("loadFactor must be > 0 and < 1: " + loadFactor);
		this.loadFactor = loadFactor;
		
		int tableSize = ObjectSet.tableSize(initialCapacity, loadFactor);
		threshold = (int) (tableSize * loadFactor);
		mask = tableSize - 1;
		shift = Long.numberOfLeadingZeros(mask);
		
		keyTable = (K[]) new Object[tableSize];
		valueTable = new float[tableSize];
	}
	
	public ObjectFloatMap(ObjectFloatMap<? extends K> map) {
		this((int) Math.floor(map.keyTable.length * map.loadFactor), map.loadFactor);
		System.arraycopy(map.keyTable, 0, keyTable, 0, map.keyTable.length);
		System.arraycopy(map.valueTable, 0, valueTable, 0, map.valueTable.length);
		size = map.size;
	}
	
	protected int place(K item) {
		return (int) (item.hashCode() * 0x9E3779B97F4A7C15L >>> shift);
	}
	
	int locateKey(K key) {
		if (key == null)
			throw new IllegalArgumentException("key cannot be null.");
		K[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			K other = keyTable[i];
			if (other == null)
				return -(i + 1); // Empty space is available.
			if (other.equals(key))
				return i; // Same key was found.
		}
	}
	
	public void put(K key, float value) {
		int i = locateKey(key);
		if (i >= 0) { // Existing key was found.
			valueTable[i] = value;
			return;
		}
		i = -(i + 1); // Empty space was found.
		keyTable[i] = key;
		valueTable[i] = value;
		if (++size >= threshold)
			resize(keyTable.length << 1);
	}
	
	/**
	 * Returns the old value associated with the specified key, or the specified default value.
	 *
	 * @param defaultValue {@link Float#NaN} can be used for a value unlikely to be in the map.
	 */
	public float put(K key, float value, float defaultValue) {
		int i = locateKey(key);
		if (i >= 0) { // Existing key was found.
			float oldValue = valueTable[i];
			valueTable[i] = value;
			return oldValue;
		}
		i = -(i + 1); // Empty space was found.
		keyTable[i] = key;
		valueTable[i] = value;
		if (++size >= threshold)
			resize(keyTable.length << 1);
		return defaultValue;
	}
	
	public void putAll(ObjectFloatMap<? extends K> map) {
		ensureCapacity(map.size);
		K[] keyTable = map.keyTable;
		float[] valueTable = map.valueTable;
		K key;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			key = keyTable[i];
			if (key != null)
				put(key, valueTable[i]);
		}
	}
	
	/**
	 * Skips checks for existing keys, doesn't increment size.
	 */
	private void putResize(K key, float value) {
		K[] keyTable = this.keyTable;
		for (int i = place(key); ; i = (i + 1) & mask) {
			if (keyTable[i] == null) {
				keyTable[i] = key;
				valueTable[i] = value;
				return;
			}
		}
	}
	
	/**
	 * Returns the value for the specified key, or the default value if the key is not in the map.
	 *
	 * @param defaultValue {@link Float#NaN} can be used for a value unlikely to be in the map.
	 */
	public float get(K key, float defaultValue) {
		int i = locateKey(key);
		return i < 0 ? defaultValue : valueTable[i];
	}
	
	/**
	 * Returns the key's current value and increments the stored value. If the key is not in the map, defaultValue + increment is
	 * put into the map and defaultValue is returned.
	 */
	public float getAndIncrement(K key, float defaultValue, float increment) {
		int i = locateKey(key);
		if (i >= 0) { // Existing key was found.
			float oldValue = valueTable[i];
			valueTable[i] += increment;
			return oldValue;
		}
		i = -(i + 1); // Empty space was found.
		keyTable[i] = key;
		valueTable[i] = defaultValue + increment;
		if (++size >= threshold)
			resize(keyTable.length << 1);
		return defaultValue;
	}
	
	/**
	 * Returns the value for the removed key, or the default value if the key is not in the map.
	 *
	 * @param defaultValue {@link Float#NaN} can be used for a value unlikely to be in the map.
	 */
	public float remove(K key, float defaultValue) {
		int i = locateKey(key);
		if (i < 0)
			return defaultValue;
		K[] keyTable = this.keyTable;
		float[] valueTable = this.valueTable;
		float oldValue = valueTable[i];
		int mask = this.mask, next = i + 1 & mask;
		while ((key = keyTable[next]) != null) {
			int placement = place(key);
			if ((next - placement & mask) > (i - placement & mask)) {
				keyTable[i] = key;
				valueTable[i] = valueTable[next];
				i = next;
			}
			next = next + 1 & mask;
		}
		keyTable[i] = null;
		size--;
		return oldValue;
	}
	
	public boolean notEmpty() {
		return size > 0;
	}
	
	public boolean isEmpty() {
		return size == 0;
	}
	
	public void shrink(int maximumCapacity) {
		if (maximumCapacity < 0)
			throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity);
		int tableSize = ObjectSet.tableSize(maximumCapacity, loadFactor);
		if (keyTable.length > tableSize)
			resize(tableSize);
	}
	
	public void clear(int maximumCapacity) {
		int tableSize = ObjectSet.tableSize(maximumCapacity, loadFactor);
		if (keyTable.length <= tableSize) {
			clear();
			return;
		}
		size = 0;
		resize(tableSize);
	}
	
	public void clear() {
		if (size == 0)
			return;
		size = 0;
		Arrays.fill(keyTable, null);
	}
	
	public boolean containsValue(float value) {
		K[] keyTable = this.keyTable;
		float[] valueTable = this.valueTable;
		for (int i = valueTable.length - 1; i >= 0; i--)
			if (keyTable[i] != null && valueTable[i] == value)
				return true;
		return false;
	}
	
	public boolean containsValue(float value, float epsilon) {
		K[] keyTable = this.keyTable;
		float[] valueTable = this.valueTable;
		for (int i = valueTable.length - 1; i >= 0; i--)
			if (keyTable[i] != null && Math.abs(valueTable[i] - value) <= epsilon)
				return true;
		return false;
	}
	
	public boolean containsKey(K key) {
		return locateKey(key) >= 0;
	}
	
	public K findKey(float value) {
		K[] keyTable = this.keyTable;
		float[] valueTable = this.valueTable;
		for (int i = valueTable.length - 1; i >= 0; i--) {
			K key = keyTable[i];
			if (key != null && valueTable[i] == value)
				return key;
		}
		return null;
	}
	
	public K findKey(float value, float epsilon) {
		K[] keyTable = this.keyTable;
		float[] valueTable = this.valueTable;
		for (int i = valueTable.length - 1; i >= 0; i--) {
			K key = keyTable[i];
			if (key != null && Math.abs(valueTable[i] - value) <= epsilon)
				return key;
		}
		return null;
	}
	
	public void ensureCapacity(int additionalCapacity) {
		int tableSize = ObjectSet.tableSize(size + additionalCapacity, loadFactor);
		if (keyTable.length < tableSize)
			resize(tableSize);
	}
	
	final void resize(int newSize) {
		int oldCapacity = keyTable.length;
		threshold = (int) (newSize * loadFactor);
		mask = newSize - 1;
		shift = Long.numberOfLeadingZeros(mask);
		
		K[] oldKeyTable = keyTable;
		float[] oldValueTable = valueTable;
		
		keyTable = (K[]) new Object[newSize];
		valueTable = new float[newSize];
		
		if (size > 0) {
			for (int i = 0; i < oldCapacity; i++) {
				K key = oldKeyTable[i];
				if (key != null)
					putResize(key, oldValueTable[i]);
			}
		}
	}
	
	public int hashCode() {
		int h = size;
		K[] keyTable = this.keyTable;
		float[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			K key = keyTable[i];
			if (key != null)
				h += key.hashCode() + Float.floatToRawIntBits(valueTable[i]);
		}
		return h;
	}
	
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof ObjectFloatMap))
			return false;
		ObjectFloatMap other = (ObjectFloatMap) obj;
		if (other.size != size)
			return false;
		K[] keyTable = this.keyTable;
		float[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			K key = keyTable[i];
			if (key != null) {
				float otherValue = other.get(key, 0);
				if (otherValue == 0 && !other.containsKey(key))
					return false;
				if (otherValue != valueTable[i])
					return false;
			}
		}
		return true;
	}
	
	public String toString(String separator) {
		return toString(separator, false);
	}
	
	public String toString() {
		return toString(", ", true);
	}
	
	private String toString(String separator, boolean braces) {
		if (size == 0)
			return braces ? "{}" : "";
		java.lang.StringBuilder buffer = new java.lang.StringBuilder(32);
		if (braces)
			buffer.append('{');
		K[] keyTable = this.keyTable;
		float[] valueTable = this.valueTable;
		int i = keyTable.length;
		while (i-- > 0) {
			K key = keyTable[i];
			if (key == null)
				continue;
			buffer.append(key);
			buffer.append('=');
			buffer.append(valueTable[i]);
			break;
		}
		while (i-- > 0) {
			K key = keyTable[i];
			if (key == null)
				continue;
			buffer.append(separator);
			buffer.append(key);
			buffer.append('=');
			buffer.append(valueTable[i]);
		}
		if (braces)
			buffer.append('}');
		return buffer.toString();
	}
	
	public Entries<K> iterator() {
		return entries();
	}
	
	public Entries<K> entries() {
		return new Entries(this);
	}
	
	public Values values() {
		return new Values(this);
	}
	
	public Keys<K> keys() {
		return new Keys(this);
	}
	
	public static class Entry<K> {
		
		public K key;
		public float value;
		
		public String toString() {
			return key + "=" + value;
		}
		
	}
	
	private static class MapIterator<K> {
		
		public boolean hasNext;
		
		final ObjectFloatMap<K> map;
		int nextIndex, currentIndex;
		boolean valid = true;
		
		public MapIterator(ObjectFloatMap<K> map) {
			this.map = map;
			reset();
		}
		
		public void reset() {
			currentIndex = -1;
			nextIndex = -1;
			findNextIndex();
		}
		
		void findNextIndex() {
			K[] keyTable = map.keyTable;
			for (int n = keyTable.length; ++nextIndex < n; ) {
				if (keyTable[nextIndex] != null) {
					hasNext = true;
					return;
				}
			}
			hasNext = false;
		}
		
		public void remove() {
			int i = currentIndex;
			if (i < 0)
				throw new IllegalStateException("next must be called before remove.");
			K[] keyTable = map.keyTable;
			float[] valueTable = map.valueTable;
			int mask = map.mask, next = i + 1 & mask;
			K key;
			while ((key = keyTable[next]) != null) {
				int placement = map.place(key);
				if ((next - placement & mask) > (i - placement & mask)) {
					keyTable[i] = key;
					valueTable[i] = valueTable[next];
					i = next;
				}
				next = next + 1 & mask;
			}
			keyTable[i] = null;
			map.size--;
			if (i != currentIndex)
				--nextIndex;
			currentIndex = -1;
		}
		
	}
	
	public static class Entries<K> extends MapIterator<K> implements Iterable<Entry<K>>, Iterator<Entry<K>> {
		
		Entry<K> entry = new Entry<K>();
		
		public Entries(ObjectFloatMap<K> map) {
			super(map);
		}
		
		public Entry<K> next() {
			if (!hasNext)
				throw new NoSuchElementException();
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			K[] keyTable = map.keyTable;
			entry.key = keyTable[nextIndex];
			entry.value = map.valueTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return entry;
		}
		
		public boolean hasNext() {
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			return hasNext;
		}
		
		public Entries<K> iterator() {
			return this;
		}
		
	}
	
	public static class Values extends MapIterator<Object> {
		
		public Values(ObjectFloatMap<?> map) {
			super((ObjectFloatMap<Object>) map);
		}
		
		public boolean hasNext() {
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			return hasNext;
		}
		
		public float next() {
			if (!hasNext)
				throw new NoSuchElementException();
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			float value = map.valueTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return value;
		}
		
		public Values iterator() {
			return this;
		}
		
		/**
		 * Returns a new array containing the remaining values.
		 */
		public FloatArray toArray() {
			FloatArray array = new FloatArray(true, map.size);
			while (hasNext)
				array.add(next());
			return array;
		}
		
		/**
		 * Adds the remaining values to the specified array.
		 */
		public FloatArray toArray(FloatArray array) {
			while (hasNext)
				array.add(next());
			return array;
		}
		
	}
	
	public static class Keys<K> extends MapIterator<K> implements Iterable<K>, Iterator<K> {
		
		public Keys(ObjectFloatMap<K> map) {
			super(map);
		}
		
		public boolean hasNext() {
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			return hasNext;
		}
		
		public K next() {
			if (!hasNext)
				throw new NoSuchElementException();
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			K key = map.keyTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return key;
		}
		
		public Keys<K> iterator() {
			return this;
		}
		
		public Array<K> toArray() {
			return toArray(new Array<K>(true, map.size));
		}
		
		public Array<K> toArray(Array<K> array) {
			while (hasNext)
				array.add(next());
			return array;
		}
		
	}
	
}