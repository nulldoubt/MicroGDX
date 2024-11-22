package me.nulldoubt.micro.utils.collections;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static me.nulldoubt.micro.utils.collections.ObjectSet.tableSize;

public class IntMap<V> implements Iterable<IntMap.Entry<V>> {
	
	public int size;
	
	int[] keyTable;
	V[] valueTable;
	
	V zeroValue;
	boolean hasZeroValue;
	
	private final float loadFactor;
	private int threshold;
	
	protected int shift;
	
	protected int mask;
	
	public IntMap() {
		this(51, 0.8f);
	}
	
	public IntMap(int initialCapacity) {
		this(initialCapacity, 0.8f);
	}
	
	public IntMap(int initialCapacity, float loadFactor) {
		if (loadFactor <= 0f || loadFactor >= 1f)
			throw new IllegalArgumentException("loadFactor must be > 0 and < 1: " + loadFactor);
		this.loadFactor = loadFactor;
		
		int tableSize = tableSize(initialCapacity, loadFactor);
		threshold = (int) (tableSize * loadFactor);
		mask = tableSize - 1;
		shift = Long.numberOfLeadingZeros(mask);
		
		keyTable = new int[tableSize];
		valueTable = (V[]) new Object[tableSize];
	}
	
	public IntMap(IntMap<? extends V> map) {
		this((int) (map.keyTable.length * map.loadFactor), map.loadFactor);
		System.arraycopy(map.keyTable, 0, keyTable, 0, map.keyTable.length);
		System.arraycopy(map.valueTable, 0, valueTable, 0, map.valueTable.length);
		size = map.size;
		zeroValue = map.zeroValue;
		hasZeroValue = map.hasZeroValue;
	}
	
	protected int place(int item) {
		return (int) (item * 0x9E3779B97F4A7C15L >>> shift);
	}
	
	private int locateKey(int key) {
		int[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			int other = keyTable[i];
			if (other == 0)
				return -(i + 1); // Empty space is available.
			if (other == key)
				return i; // Same key was found.
		}
	}
	
	public V put(int key, V value) {
		if (key == 0) {
			V oldValue = zeroValue;
			zeroValue = value;
			if (!hasZeroValue) {
				hasZeroValue = true;
				size++;
			}
			return oldValue;
		}
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
	
	public void putAll(IntMap<? extends V> map) {
		ensureCapacity(map.size);
		if (map.hasZeroValue)
			put(0, map.zeroValue);
		int[] keyTable = map.keyTable;
		V[] valueTable = map.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			int key = keyTable[i];
			if (key != 0)
				put(key, valueTable[i]);
		}
	}
	
	private void putResize(int key, V value) {
		int[] keyTable = this.keyTable;
		for (int i = place(key); ; i = (i + 1) & mask) {
			if (keyTable[i] == 0) {
				keyTable[i] = key;
				valueTable[i] = value;
				return;
			}
		}
	}
	
	public V get(int key) {
		if (key == 0)
			return hasZeroValue ? zeroValue : null;
		int i = locateKey(key);
		return i >= 0 ? valueTable[i] : null;
	}
	
	public V get(int key, V defaultValue) {
		if (key == 0)
			return hasZeroValue ? zeroValue : defaultValue;
		int i = locateKey(key);
		return i >= 0 ? valueTable[i] : defaultValue;
	}
	
	public V remove(int key) {
		if (key == 0) {
			if (!hasZeroValue)
				return null;
			hasZeroValue = false;
			V oldValue = zeroValue;
			zeroValue = null;
			size--;
			return oldValue;
		}
		
		int i = locateKey(key);
		if (i < 0)
			return null;
		int[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		V oldValue = valueTable[i];
		int mask = this.mask, next = i + 1 & mask;
		while ((key = keyTable[next]) != 0) {
			int placement = place(key);
			if ((next - placement & mask) > (i - placement & mask)) {
				keyTable[i] = key;
				valueTable[i] = valueTable[next];
				i = next;
			}
			next = next + 1 & mask;
		}
		keyTable[i] = 0;
		valueTable[i] = null;
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
		hasZeroValue = false;
		zeroValue = null;
		resize(tableSize);
	}
	
	public void clear() {
		if (size == 0)
			return;
		size = 0;
		Arrays.fill(keyTable, 0);
		Arrays.fill(valueTable, null);
		zeroValue = null;
		hasZeroValue = false;
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
			if (hasZeroValue && zeroValue == null)
				return true;
			int[] keyTable = this.keyTable;
			for (int i = valueTable.length - 1; i >= 0; i--)
				if (keyTable[i] != 0 && valueTable[i] == null)
					return true;
		} else if (identity) {
			if (value == zeroValue)
				return true;
			for (int i = valueTable.length - 1; i >= 0; i--)
				if (valueTable[i] == value)
					return true;
		} else {
			if (hasZeroValue && value.equals(zeroValue))
				return true;
			for (int i = valueTable.length - 1; i >= 0; i--)
				if (value.equals(valueTable[i]))
					return true;
		}
		return false;
		
	}
	
	public boolean containsKey(int key) {
		if (key == 0)
			return hasZeroValue;
		return locateKey(key) >= 0;
	}
	
	/**
	 * Returns the key for the specified value, or <tt>notFound</tt> if it is not in the map. Note this traverses the entire map
	 * and compares every value, which may be an expensive operation.
	 *
	 * @param identity If true, uses == to compare the specified value with values in the map. If false, uses
	 *                 {@link #equals(Object)}.
	 */
	public int findKey(Object value, boolean identity, int notFound) {
		V[] valueTable = this.valueTable;
		if (value == null) {
			if (hasZeroValue && zeroValue == null)
				return 0;
			int[] keyTable = this.keyTable;
			for (int i = valueTable.length - 1; i >= 0; i--)
				if (keyTable[i] != 0 && valueTable[i] == null)
					return keyTable[i];
		} else if (identity) {
			if (value == zeroValue)
				return 0;
			for (int i = valueTable.length - 1; i >= 0; i--)
				if (valueTable[i] == value)
					return keyTable[i];
		} else {
			if (hasZeroValue && value.equals(zeroValue))
				return 0;
			for (int i = valueTable.length - 1; i >= 0; i--)
				if (value.equals(valueTable[i]))
					return keyTable[i];
		}
		return notFound;
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
	
	private void resize(int newSize) {
		int oldCapacity = keyTable.length;
		threshold = (int) (newSize * loadFactor);
		mask = newSize - 1;
		shift = Long.numberOfLeadingZeros(mask);
		
		int[] oldKeyTable = keyTable;
		V[] oldValueTable = valueTable;
		
		keyTable = new int[newSize];
		valueTable = (V[]) new Object[newSize];
		
		if (size > 0) {
			for (int i = 0; i < oldCapacity; i++) {
				int key = oldKeyTable[i];
				if (key != 0)
					putResize(key, oldValueTable[i]);
			}
		}
	}
	
	public int hashCode() {
		int h = size;
		if (hasZeroValue && zeroValue != null)
			h += zeroValue.hashCode();
		int[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			int key = keyTable[i];
			if (key != 0) {
				h += key * 31;
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
		if (!(obj instanceof IntMap))
			return false;
		IntMap other = (IntMap) obj;
		if (other.size != size)
			return false;
		if (other.hasZeroValue != hasZeroValue)
			return false;
		if (hasZeroValue) {
			if (other.zeroValue == null) {
				if (zeroValue != null)
					return false;
			} else {
				if (!other.zeroValue.equals(zeroValue))
					return false;
			}
		}
		int[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			int key = keyTable[i];
			if (key != 0) {
				V value = valueTable[i];
				if (value == null) {
					if (other.get(key, ObjectMap.dummy) != null)
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
		if (!(obj instanceof IntMap))
			return false;
		IntMap other = (IntMap) obj;
		if (other.size != size)
			return false;
		if (other.hasZeroValue != hasZeroValue)
			return false;
		if (hasZeroValue && zeroValue != other.zeroValue)
			return false;
		int[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			int key = keyTable[i];
			if (key != 0 && valueTable[i] != other.get(key, ObjectMap.dummy))
				return false;
		}
		return true;
	}
	
	public String toString() {
		if (size == 0)
			return "[]";
		java.lang.StringBuilder buffer = new java.lang.StringBuilder(32);
		buffer.append('[');
		int[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		int i = keyTable.length;
		if (hasZeroValue) {
			buffer.append("0=");
			buffer.append(zeroValue);
		} else {
			while (i-- > 0) {
				int key = keyTable[i];
				if (key == 0)
					continue;
				buffer.append(key);
				buffer.append('=');
				buffer.append(valueTable[i]);
				break;
			}
		}
		while (i-- > 0) {
			int key = keyTable[i];
			if (key == 0)
				continue;
			buffer.append(", ");
			buffer.append(key);
			buffer.append('=');
			buffer.append(valueTable[i]);
		}
		buffer.append(']');
		return buffer.toString();
	}
	
	public Iterator<Entry<V>> iterator() {
		return entries();
	}
	
	public Entries<V> entries() {
		return new Entries(this);
	}
	
	public Values<V> values() {
		return new Values(this);
	}
	
	public Keys keys() {
		return new Keys(this);
	}
	
	public static class Entry<V> {
		
		public int key;
		public V value;
		
		public String toString() {
			return key + "=" + value;
		}
		
	}
	
	private static class MapIterator<V> {
		
		private static final int INDEX_ILLEGAL = -2;
		static final int INDEX_ZERO = -1;
		
		public boolean hasNext;
		
		final IntMap<V> map;
		int nextIndex, currentIndex;
		boolean valid = true;
		
		public MapIterator(IntMap<V> map) {
			this.map = map;
			reset();
		}
		
		public void reset() {
			currentIndex = INDEX_ILLEGAL;
			nextIndex = INDEX_ZERO;
			if (map.hasZeroValue)
				hasNext = true;
			else
				findNextIndex();
		}
		
		void findNextIndex() {
			int[] keyTable = map.keyTable;
			for (int n = keyTable.length; ++nextIndex < n; ) {
				if (keyTable[nextIndex] != 0) {
					hasNext = true;
					return;
				}
			}
			hasNext = false;
		}
		
		public void remove() {
			int i = currentIndex;
			if (i == INDEX_ZERO && map.hasZeroValue) {
				map.hasZeroValue = false;
				map.zeroValue = null;
			} else if (i < 0) {
				throw new IllegalStateException("next must be called before remove.");
			} else {
				int[] keyTable = map.keyTable;
				V[] valueTable = map.valueTable;
				int mask = map.mask, next = i + 1 & mask, key;
				while ((key = keyTable[next]) != 0) {
					int placement = map.place(key);
					if ((next - placement & mask) > (i - placement & mask)) {
						keyTable[i] = key;
						valueTable[i] = valueTable[next];
						i = next;
					}
					next = next + 1 & mask;
				}
				keyTable[i] = 0;
				valueTable[i] = null;
				if (i != currentIndex)
					--nextIndex;
			}
			currentIndex = INDEX_ILLEGAL;
			map.size--;
		}
		
	}
	
	public static class Entries<V> extends MapIterator<V> implements Iterable<Entry<V>>, Iterator<Entry<V>> {
		
		private final Entry<V> entry = new Entry();
		
		public Entries(IntMap map) {
			super(map);
		}
		
		public Entry<V> next() {
			if (!hasNext)
				throw new NoSuchElementException();
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			int[] keyTable = map.keyTable;
			if (nextIndex == INDEX_ZERO) {
				entry.key = 0;
				entry.value = map.zeroValue;
			} else {
				entry.key = keyTable[nextIndex];
				entry.value = map.valueTable[nextIndex];
			}
			currentIndex = nextIndex;
			findNextIndex();
			return entry;
		}
		
		public boolean hasNext() {
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			return hasNext;
		}
		
		public Iterator<Entry<V>> iterator() {
			return this;
		}
		
	}
	
	public static class Values<V> extends MapIterator<V> implements Iterable<V>, Iterator<V> {
		
		public Values(IntMap<V> map) {
			super(map);
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
			V value;
			if (nextIndex == INDEX_ZERO)
				value = map.zeroValue;
			else
				value = map.valueTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return value;
		}
		
		public Iterator<V> iterator() {
			return this;
		}
		
		/**
		 * Returns a new array containing the remaining values.
		 */
		public Array<V> toArray() {
			Array array = new Array(true, map.size);
			while (hasNext)
				array.add(next());
			return array;
		}
		
	}
	
	public static class Keys extends MapIterator {
		
		public Keys(IntMap map) {
			super(map);
		}
		
		public int next() {
			if (!hasNext)
				throw new NoSuchElementException();
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			int key = nextIndex == INDEX_ZERO ? 0 : map.keyTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return key;
		}
		
		/**
		 * Returns a new array containing the remaining keys.
		 */
		public IntArray toArray() {
			IntArray array = new IntArray(true, map.size);
			while (hasNext)
				array.add(next());
			return array;
		}
		
		/**
		 * Adds the remaining values to the specified array.
		 */
		public IntArray toArray(IntArray array) {
			while (hasNext)
				array.add(next());
			return array;
		}
		
	}
	
}
