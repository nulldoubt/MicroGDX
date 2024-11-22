package me.nulldoubt.micro.utils.collections;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static me.nulldoubt.micro.utils.collections.ObjectSet.tableSize;

public class ObjectMap<K, V> implements Iterable<ObjectMap.Entry<K, V>> {
	
	static final Object dummy = new Object();
	
	public int size;
	
	K[] keyTable;
	V[] valueTable;
	
	float loadFactor;
	int threshold;
	
	protected int shift;
	
	protected int mask;
	
	public ObjectMap() {
		this(51, 0.8f);
	}
	
	public ObjectMap(int initialCapacity) {
		this(initialCapacity, 0.8f);
	}
	
	public ObjectMap(int initialCapacity, float loadFactor) {
		if (loadFactor <= 0f || loadFactor >= 1f)
			throw new IllegalArgumentException("loadFactor must be > 0 and < 1: " + loadFactor);
		this.loadFactor = loadFactor;
		
		int tableSize = tableSize(initialCapacity, loadFactor);
		threshold = (int) (tableSize * loadFactor);
		mask = tableSize - 1;
		shift = Long.numberOfLeadingZeros(mask);
		
		keyTable = (K[]) new Object[tableSize];
		valueTable = (V[]) new Object[tableSize];
	}
	
	public ObjectMap(ObjectMap<? extends K, ? extends V> map) {
		this((int) (map.keyTable.length * map.loadFactor), map.loadFactor);
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
	
	/**
	 * Returns the old value associated with the specified key, or null.
	 */
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
		if (++size >= threshold)
			resize(keyTable.length << 1);
		return null;
	}
	
	public void putAll(ObjectMap<? extends K, ? extends V> map) {
		ensureCapacity(map.size);
		K[] keyTable = map.keyTable;
		V[] valueTable = map.valueTable;
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
	private void putResize(K key, V value) {
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
	 * Returns the value for the specified key, or null if the key is not in the map.
	 */
	public <T extends K> V get(T key) {
		int i = locateKey(key);
		return i < 0 ? null : valueTable[i];
	}
	
	/**
	 * Returns the value for the specified key, or the default value if the key is not in the map.
	 */
	public V get(K key, V defaultValue) {
		int i = locateKey(key);
		return i < 0 ? defaultValue : valueTable[i];
	}
	
	/**
	 * Returns the value for the removed key, or null if the key is not in the map.
	 */
	public V remove(K key) {
		int i = locateKey(key);
		if (i < 0)
			return null;
		K[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		V oldValue = valueTable[i];
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
		valueTable[i] = null;
		size--;
		return oldValue;
	}
	
	/**
	 * Returns true if the map has one or more items.
	 */
	public boolean notEmpty() {
		return size > 0;
	}
	
	/**
	 * Returns true if the map is empty.
	 */
	public boolean isEmpty() {
		return size == 0;
	}
	
	/**
	 * Reduces the size of the backing arrays to be the specified capacity / loadFactor, or less. If the capacity is already less,
	 * nothing is done. If the map contains more items than the specified capacity, the next highest power of two capacity is used
	 * instead.
	 */
	public void shrink(int maximumCapacity) {
		if (maximumCapacity < 0)
			throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity);
		int tableSize = tableSize(maximumCapacity, loadFactor);
		if (keyTable.length > tableSize)
			resize(tableSize);
	}
	
	/**
	 * Clears the map and reduces the size of the backing arrays to be the specified capacity / loadFactor, if they are larger.
	 */
	public void clear(int maximumCapacity) {
		int tableSize = tableSize(maximumCapacity, loadFactor);
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
		Arrays.fill(valueTable, null);
	}
	
	/**
	 * Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may
	 * be an expensive operation.
	 *
	 * @param identity If true, uses == to compare the specified value with values in the map. If false, uses
	 *                 {@link #equals(Object)}.
	 */
	public boolean containsValue(Object value, boolean identity) {
		V[] valueTable = this.valueTable;
		if (value == null) {
			K[] keyTable = this.keyTable;
			for (int i = valueTable.length - 1; i >= 0; i--)
				if (keyTable[i] != null && valueTable[i] == null)
					return true;
		} else if (identity) {
			for (int i = valueTable.length - 1; i >= 0; i--)
				if (valueTable[i] == value)
					return true;
		} else {
			for (int i = valueTable.length - 1; i >= 0; i--)
				if (value.equals(valueTable[i]))
					return true;
		}
		return false;
	}
	
	public boolean containsKey(K key) {
		return locateKey(key) >= 0;
	}
	
	/**
	 * Returns the key for the specified value, or null if it is not in the map. Note this traverses the entire map and compares
	 * every value, which may be an expensive operation.
	 *
	 * @param identity If true, uses == to compare the specified value with values in the map. If false, uses
	 *                 {@link #equals(Object)}.
	 */
	public K findKey(Object value, boolean identity) {
		V[] valueTable = this.valueTable;
		if (value == null) {
			K[] keyTable = this.keyTable;
			for (int i = valueTable.length - 1; i >= 0; i--)
				if (keyTable[i] != null && valueTable[i] == null)
					return keyTable[i];
		} else if (identity) {
			for (int i = valueTable.length - 1; i >= 0; i--)
				if (valueTable[i] == value)
					return keyTable[i];
		} else {
			for (int i = valueTable.length - 1; i >= 0; i--)
				if (value.equals(valueTable[i]))
					return keyTable[i];
		}
		return null;
	}
	
	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. Useful before
	 * adding many items to avoid multiple backing array resizes.
	 */
	public void ensureCapacity(int additionalCapacity) {
		int tableSize = tableSize(size + additionalCapacity, loadFactor);
		if (keyTable.length < tableSize)
			resize(tableSize);
	}
	
	final void resize(int newSize) {
		int oldCapacity = keyTable.length;
		threshold = (int) (newSize * loadFactor);
		mask = newSize - 1;
		shift = Long.numberOfLeadingZeros(mask);
		
		K[] oldKeyTable = keyTable;
		V[] oldValueTable = valueTable;
		
		keyTable = (K[]) new Object[newSize];
		valueTable = (V[]) new Object[newSize];
		
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
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			K key = keyTable[i];
			if (key != null) {
				h += key.hashCode();
				V value = valueTable[i];
				if (value != null)
					h += value.hashCode();
			}
		}
		return h;
	}
	
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof ObjectMap))
			return false;
		ObjectMap other = (ObjectMap) obj;
		if (other.size != size)
			return false;
		K[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			K key = keyTable[i];
			if (key != null) {
				V value = valueTable[i];
				if (value == null) {
					if (other.get(key, dummy) != null)
						return false;
				} else {
					if (!value.equals(other.get(key)))
						return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Uses == for comparison of each value.
	 */
	public boolean equalsIdentity(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof ObjectMap))
			return false;
		ObjectMap other = (ObjectMap) obj;
		if (other.size != size)
			return false;
		K[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			K key = keyTable[i];
			if (key != null && valueTable[i] != other.get(key, dummy))
				return false;
		}
		return true;
	}
	
	public String toString(String separator) {
		return toString(separator, false);
	}
	
	public String toString() {
		return toString(", ", true);
	}
	
	protected String toString(String separator, boolean braces) {
		if (size == 0)
			return braces ? "{}" : "";
		java.lang.StringBuilder buffer = new java.lang.StringBuilder(32);
		if (braces)
			buffer.append('{');
		K[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		int i = keyTable.length;
		while (i-- > 0) {
			K key = keyTable[i];
			if (key == null)
				continue;
			buffer.append(key == this ? "(this)" : key);
			buffer.append('=');
			V value = valueTable[i];
			buffer.append(value == this ? "(this)" : value);
			break;
		}
		while (i-- > 0) {
			K key = keyTable[i];
			if (key == null)
				continue;
			buffer.append(separator);
			buffer.append(key == this ? "(this)" : key);
			buffer.append('=');
			V value = valueTable[i];
			buffer.append(value == this ? "(this)" : value);
		}
		if (braces)
			buffer.append('}');
		return buffer.toString();
	}
	
	public Entries<K, V> iterator() {
		return entries();
	}
	
	public Entries<K, V> entries() {
		return new Entries(this);
	}
	
	public Values<V> values() {
		return new Values(this);
	}
	
	public Keys<K> keys() {
		return new Keys(this);
	}
	
	public static class Entry<K, V> {
		
		public K key;
		public V value;
		
		public String toString() {
			return key + "=" + value;
		}
		
	}
	
	private static abstract class MapIterator<K, V, I> implements Iterable<I>, Iterator<I> {
		
		public boolean hasNext;
		
		final ObjectMap<K, V> map;
		int nextIndex, currentIndex;
		boolean valid = true;
		
		public MapIterator(ObjectMap<K, V> map) {
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
			V[] valueTable = map.valueTable;
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
			valueTable[i] = null;
			map.size--;
			if (i != currentIndex)
				--nextIndex;
			currentIndex = -1;
		}
		
	}
	
	public static class Entries<K, V> extends MapIterator<K, V, Entry<K, V>> {
		
		Entry<K, V> entry = new Entry<K, V>();
		
		public Entries(ObjectMap<K, V> map) {
			super(map);
		}
		
		/**
		 * Note the same entry instance is returned each time this method is called.
		 */
		public Entry<K, V> next() {
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
		
		public Entries<K, V> iterator() {
			return this;
		}
		
	}
	
	public static class Values<V> extends MapIterator<Object, V, V> {
		
		public Values(ObjectMap<?, V> map) {
			super((ObjectMap<Object, V>) map);
		}
		
		public boolean hasNext() {
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			return hasNext;
		}
		
		public V next() {
			if (!hasNext)
				throw new NoSuchElementException();
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			V value = map.valueTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return value;
		}
		
		public Values<V> iterator() {
			return this;
		}
		
		/**
		 * Returns a new array containing the remaining values.
		 */
		public Array<V> toArray() {
			return toArray(new Array(true, map.size));
		}
		
		/**
		 * Adds the remaining values to the specified array.
		 */
		public Array<V> toArray(Array<V> array) {
			while (hasNext)
				array.add(next());
			return array;
		}
		
	}
	
	public static class Keys<K> extends MapIterator<K, Object, K> {
		
		public Keys(ObjectMap<K, ?> map) {
			super((ObjectMap<K, Object>) map);
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
		
		/**
		 * Returns a new array containing the remaining keys.
		 */
		public Array<K> toArray() {
			return toArray(new Array<K>(true, map.size));
		}
		
		/**
		 * Adds the remaining keys to the array.
		 */
		public Array<K> toArray(Array<K> array) {
			while (hasNext)
				array.add(next());
			return array;
		}
		
	}
	
}
