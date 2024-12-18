package me.nulldoubt.micro.utils.collections;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ObjectSet<T> implements Iterable<T> {
	
	public int size;
	
	T[] keyTable;
	
	float loadFactor;
	int threshold;
	
	protected int shift;
	protected int mask;
	
	public ObjectSet() {
		this(51, 0.8f);
	}
	
	public ObjectSet(int initialCapacity) {
		this(initialCapacity, 0.8f);
	}
	
	public ObjectSet(int initialCapacity, float loadFactor) {
		if (loadFactor <= 0f || loadFactor >= 1f)
			throw new IllegalArgumentException("loadFactor must be > 0 and < 1: " + loadFactor);
		this.loadFactor = loadFactor;
		
		int tableSize = tableSize(initialCapacity, loadFactor);
		threshold = (int) (tableSize * loadFactor);
		mask = tableSize - 1;
		shift = Long.numberOfLeadingZeros(mask);
		
		keyTable = (T[]) new Object[tableSize];
	}
	
	public ObjectSet(ObjectSet<? extends T> set) {
		this((int) (set.keyTable.length * set.loadFactor), set.loadFactor);
		System.arraycopy(set.keyTable, 0, keyTable, 0, set.keyTable.length);
		size = set.size;
	}
	
	protected int place(T item) {
		return (int) (item.hashCode() * 0x9E3779B97F4A7C15L >>> shift);
	}
	
	int locateKey(T key) {
		if (key == null)
			throw new IllegalArgumentException("key cannot be null.");
		T[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			T other = keyTable[i];
			if (other == null)
				return -(i + 1); // Empty space is available.
			if (other.equals(key))
				return i; // Same key was found.
		}
	}
	
	public boolean add(T key) {
		int i = locateKey(key);
		if (i >= 0)
			return false; // Existing key was found.
		i = -(i + 1); // Empty space was found.
		keyTable[i] = key;
		if (++size >= threshold)
			resize(keyTable.length << 1);
		return true;
	}
	
	public void addAll(Array<? extends T> array) {
		addAll(array.items, 0, array.size);
	}
	
	public void addAll(Array<? extends T> array, int offset, int length) {
		if (offset + length > array.size)
			throw new IllegalArgumentException("offset + length must be <= size: " + offset + " + " + length + " <= " + array.size);
		addAll(array.items, offset, length);
	}
	
	public boolean addAll(T... array) {
		return addAll(array, 0, array.length);
	}
	
	public boolean addAll(T[] array, int offset, int length) {
		ensureCapacity(length);
		int oldSize = size;
		for (int i = offset, n = i + length; i < n; i++)
			add(array[i]);
		return oldSize != size;
	}
	
	public void addAll(ObjectSet<T> set) {
		ensureCapacity(set.size);
		T[] keyTable = set.keyTable;
		for (T key : keyTable) {
			if (key != null)
				add(key);
		}
	}
	
	private void addResize(T key) {
		T[] keyTable = this.keyTable;
		for (int i = place(key); ; i = (i + 1) & mask) {
			if (keyTable[i] == null) {
				keyTable[i] = key;
				return;
			}
		}
	}
	
	public boolean remove(T key) {
		int i = locateKey(key);
		if (i < 0)
			return false;
		T[] keyTable = this.keyTable;
		int mask = this.mask, next = i + 1 & mask;
		while ((key = keyTable[next]) != null) {
			int placement = place(key);
			if ((next - placement & mask) > (i - placement & mask)) {
				keyTable[i] = key;
				i = next;
			}
			next = next + 1 & mask;
		}
		keyTable[i] = null;
		size--;
		return true;
	}
	
	/**
	 * Returns true if the set has one or more items.
	 */
	public boolean notEmpty() {
		return size > 0;
	}
	
	/**
	 * Returns true if the set is empty.
	 */
	public boolean isEmpty() {
		return size == 0;
	}
	
	/**
	 * Reduces the size of the backing arrays to be the specified capacity / loadFactor, or less. If the capacity is already less,
	 * nothing is done. If the set contains more items than the specified capacity, the next highest power of two capacity is used
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
	 * Clears the set and reduces the size of the backing arrays to be the specified capacity / loadFactor, if they are larger.
	 * The reduction is done by allocating new arrays, though for large arrays this can be faster than clearing the existing
	 * array.
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
	
	/**
	 * Clears the set, leaving the backing arrays at the current capacity. When the capacity is high and the population is low,
	 * iteration can be unnecessarily slow. {@link #clear(int)} can be used to reduce the capacity.
	 */
	public void clear() {
		if (size == 0)
			return;
		size = 0;
		Arrays.fill(keyTable, null);
	}
	
	public boolean contains(T key) {
		return locateKey(key) >= 0;
	}
	
	public T get(T key) {
		int i = locateKey(key);
		return i < 0 ? null : keyTable[i];
	}
	
	public T first() {
		T[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++)
			if (keyTable[i] != null)
				return keyTable[i];
		throw new IllegalStateException("ObjectSet is empty.");
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
		T[] oldKeyTable = keyTable;
		
		keyTable = (T[]) (new Object[newSize]);
		
		if (size > 0) {
			for (int i = 0; i < oldCapacity; i++) {
				T key = oldKeyTable[i];
				if (key != null)
					addResize(key);
			}
		}
	}
	
	public int hashCode() {
		int h = size;
		T[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			T key = keyTable[i];
			if (key != null)
				h += key.hashCode();
		}
		return h;
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof ObjectSet))
			return false;
		ObjectSet other = (ObjectSet) obj;
		if (other.size != size)
			return false;
		T[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++)
			if (keyTable[i] != null && !other.contains(keyTable[i]))
				return false;
		return true;
	}
	
	public String toString() {
		return '{' + toString(", ") + '}';
	}
	
	public String toString(String separator) {
		if (size == 0)
			return "";
		java.lang.StringBuilder buffer = new java.lang.StringBuilder(32);
		T[] keyTable = this.keyTable;
		int i = keyTable.length;
		while (i-- > 0) {
			T key = keyTable[i];
			if (key == null)
				continue;
			buffer.append(key == this ? "(this)" : key);
			break;
		}
		while (i-- > 0) {
			T key = keyTable[i];
			if (key == null)
				continue;
			buffer.append(separator);
			buffer.append(key == this ? "(this)" : key);
		}
		return buffer.toString();
	}
	
	public ObjectSetIterator<T> iterator() {
		return new ObjectSetIterator(this);
	}
	
	public static <T> ObjectSet<T> with(T... array) {
		ObjectSet<T> set = new ObjectSet<T>();
		set.addAll(array);
		return set;
	}
	
	static int tableSize(int capacity, float loadFactor) {
		if (capacity < 0)
			throw new IllegalArgumentException("capacity must be >= 0: " + capacity);
		int tableSize = CollectionUtils.nextPowerOfTwo(Math.max(2, (int) Math.ceil(capacity / loadFactor)));
		if (tableSize > 1 << 30)
			throw new IllegalArgumentException("The required capacity is too large: " + capacity);
		return tableSize;
	}
	
	public static class ObjectSetIterator<K> implements Iterable<K>, Iterator<K> {
		
		public boolean hasNext;
		
		final ObjectSet<K> set;
		int nextIndex, currentIndex;
		boolean valid = true;
		
		public ObjectSetIterator(ObjectSet<K> set) {
			this.set = set;
			reset();
		}
		
		public void reset() {
			currentIndex = -1;
			nextIndex = -1;
			findNextIndex();
		}
		
		private void findNextIndex() {
			K[] keyTable = set.keyTable;
			for (int n = set.keyTable.length; ++nextIndex < n; ) {
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
			K[] keyTable = set.keyTable;
			int mask = set.mask, next = i + 1 & mask;
			K key;
			while ((key = keyTable[next]) != null) {
				int placement = set.place(key);
				if ((next - placement & mask) > (i - placement & mask)) {
					keyTable[i] = key;
					i = next;
				}
				next = next + 1 & mask;
			}
			keyTable[i] = null;
			set.size--;
			if (i != currentIndex)
				--nextIndex;
			currentIndex = -1;
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
			K key = set.keyTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return key;
		}
		
		public ObjectSetIterator<K> iterator() {
			return this;
		}
		
		/**
		 * Adds the remaining values to the array.
		 */
		public Array<K> toArray(Array<K> array) {
			while (hasNext)
				array.add(next());
			return array;
		}
		
		/**
		 * Returns a new array containing the remaining values.
		 */
		public Array<K> toArray() {
			return toArray(new Array<K>(true, set.size));
		}
		
	}
	
}
