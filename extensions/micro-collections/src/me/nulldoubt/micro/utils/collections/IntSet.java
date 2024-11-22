package me.nulldoubt.micro.utils.collections;


import java.util.Arrays;
import java.util.NoSuchElementException;

import static me.nulldoubt.micro.utils.collections.ObjectSet.tableSize;

public class IntSet {
	
	public int size;
	
	int[] keyTable;
	boolean hasZeroValue;
	
	private final float loadFactor;
	private int threshold;
	
	protected int shift;
	
	/**
	 * A bitmask used to confine hashcodes to the size of the table. Must be all 1 bits in its low positions, ie a power of two
	 * minus 1. If {@link #place(int)} is overriden, this can be used instead of {@link #shift} to isolate usable bits of a
	 * hash.
	 */
	protected int mask;
	
	private transient IntSetIterator iterator1, iterator2;
	
	/**
	 * Creates a new set with an initial capacity of 51 and a load factor of 0.8.
	 */
	public IntSet() {
		this(51, 0.8f);
	}
	
	/**
	 * Creates a new set with a load factor of 0.8.
	 *
	 * @param initialCapacity The backing array size is initialCapacity / loadFactor, increased to the next power of two.
	 */
	public IntSet(int initialCapacity) {
		this(initialCapacity, 0.8f);
	}
	
	/**
	 * Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity The backing array size is initialCapacity / loadFactor, increased to the next power of two.
	 */
	public IntSet(int initialCapacity, float loadFactor) {
		if (loadFactor <= 0f || loadFactor >= 1f)
			throw new IllegalArgumentException("loadFactor must be > 0 and < 1: " + loadFactor);
		this.loadFactor = loadFactor;
		
		int tableSize = tableSize(initialCapacity, loadFactor);
		threshold = (int) (tableSize * loadFactor);
		mask = tableSize - 1;
		shift = Long.numberOfLeadingZeros(mask);
		
		keyTable = new int[tableSize];
	}
	
	/**
	 * Creates a new set identical to the specified set.
	 */
	public IntSet(IntSet set) {
		this((int) (set.keyTable.length * set.loadFactor), set.loadFactor);
		System.arraycopy(set.keyTable, 0, keyTable, 0, set.keyTable.length);
		size = set.size;
		hasZeroValue = set.hasZeroValue;
	}
	
	protected int place(int item) {
		return (int) (item * 0x9E3779B97F4A7C15L >>> shift);
	}
	
	/**
	 * Returns the index of the key if already present, else -(index + 1) for the next empty index. This can be overridden in this
	 * pacakge to compare for equality differently than {@link Object#equals(Object)}.
	 */
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
	
	/**
	 * Returns true if the key was added to the set or false if it was already in the set.
	 */
	public boolean add(int key) {
		if (key == 0) {
			if (hasZeroValue)
				return false;
			hasZeroValue = true;
			size++;
			return true;
		}
		int i = locateKey(key);
		if (i >= 0)
			return false; // Existing key was found.
		i = -(i + 1); // Empty space was found.
		keyTable[i] = key;
		if (++size >= threshold)
			resize(keyTable.length << 1);
		return true;
	}
	
	public void addAll(IntArray array) {
		addAll(array.items, 0, array.size);
	}
	
	public void addAll(IntArray array, int offset, int length) {
		if (offset + length > array.size)
			throw new IllegalArgumentException("offset + length must be <= size: " + offset + " + " + length + " <= " + array.size);
		addAll(array.items, offset, length);
	}
	
	public void addAll(int... array) {
		addAll(array, 0, array.length);
	}
	
	public void addAll(int[] array, int offset, int length) {
		ensureCapacity(length);
		for (int i = offset, n = i + length; i < n; i++)
			add(array[i]);
	}
	
	public void addAll(IntSet set) {
		ensureCapacity(set.size);
		if (set.hasZeroValue)
			add(0);
		int[] keyTable = set.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			int key = keyTable[i];
			if (key != 0)
				add(key);
		}
	}
	
	/**
	 * Skips checks for existing keys, doesn't increment size, doesn't need to handle key 0.
	 */
	private void addResize(int key) {
		int[] keyTable = this.keyTable;
		for (int i = place(key); ; i = (i + 1) & mask) {
			if (keyTable[i] == 0) {
				keyTable[i] = key;
				return;
			}
		}
	}
	
	/**
	 * Returns true if the key was removed.
	 */
	public boolean remove(int key) {
		if (key == 0) {
			if (!hasZeroValue)
				return false;
			hasZeroValue = false;
			size--;
			return true;
		}
		
		int i = locateKey(key);
		if (i < 0)
			return false;
		int[] keyTable = this.keyTable;
		int mask = this.mask, next = i + 1 & mask;
		while ((key = keyTable[next]) != 0) {
			int placement = place(key);
			if ((next - placement & mask) > (i - placement & mask)) {
				keyTable[i] = key;
				i = next;
			}
			next = next + 1 & mask;
		}
		keyTable[i] = 0;
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
	 */
	public void clear(int maximumCapacity) {
		int tableSize = tableSize(maximumCapacity, loadFactor);
		if (keyTable.length <= tableSize) {
			clear();
			return;
		}
		size = 0;
		hasZeroValue = false;
		resize(tableSize);
	}
	
	public void clear() {
		if (size == 0)
			return;
		size = 0;
		Arrays.fill(keyTable, 0);
		hasZeroValue = false;
	}
	
	public boolean contains(int key) {
		if (key == 0)
			return hasZeroValue;
		return locateKey(key) >= 0;
	}
	
	public int first() {
		if (hasZeroValue)
			return 0;
		int[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++)
			if (keyTable[i] != 0)
				return keyTable[i];
		throw new IllegalStateException("IntSet is empty.");
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
		
		keyTable = new int[newSize];
		
		if (size > 0) {
			for (int i = 0; i < oldCapacity; i++) {
				int key = oldKeyTable[i];
				if (key != 0)
					addResize(key);
			}
		}
	}
	
	public int hashCode() {
		int h = size;
		int[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			int key = keyTable[i];
			if (key != 0)
				h += key;
		}
		return h;
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof IntSet))
			return false;
		IntSet other = (IntSet) obj;
		if (other.size != size)
			return false;
		if (other.hasZeroValue != hasZeroValue)
			return false;
		int[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++)
			if (keyTable[i] != 0 && !other.contains(keyTable[i]))
				return false;
		return true;
	}
	
	public String toString() {
		if (size == 0)
			return "[]";
		java.lang.StringBuilder buffer = new java.lang.StringBuilder(32);
		buffer.append('[');
		int[] keyTable = this.keyTable;
		int i = keyTable.length;
		if (hasZeroValue)
			buffer.append("0");
		else {
			while (i-- > 0) {
				int key = keyTable[i];
				if (key == 0)
					continue;
				buffer.append(key);
				break;
			}
		}
		while (i-- > 0) {
			int key = keyTable[i];
			if (key == 0)
				continue;
			buffer.append(", ");
			buffer.append(key);
		}
		buffer.append(']');
		return buffer.toString();
	}
	
	public IntSetIterator iterator() {
		return new IntSetIterator(this);
	}
	
	public static IntSet with(int... array) {
		IntSet set = new IntSet();
		set.addAll(array);
		return set;
	}
	
	public static class IntSetIterator {
		
		private static final int INDEX_ILLEGAL = -2, INDEX_ZERO = -1;
		
		public boolean hasNext;
		
		final IntSet set;
		int nextIndex, currentIndex;
		boolean valid = true;
		
		public IntSetIterator(IntSet set) {
			this.set = set;
			reset();
		}
		
		public void reset() {
			currentIndex = INDEX_ILLEGAL;
			nextIndex = INDEX_ZERO;
			if (set.hasZeroValue)
				hasNext = true;
			else
				findNextIndex();
		}
		
		void findNextIndex() {
			int[] keyTable = set.keyTable;
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
			if (i == INDEX_ZERO && set.hasZeroValue) {
				set.hasZeroValue = false;
			} else if (i < 0) {
				throw new IllegalStateException("next must be called before remove.");
			} else {
				int[] keyTable = set.keyTable;
				int mask = set.mask, next = i + 1 & mask, key;
				while ((key = keyTable[next]) != 0) {
					int placement = set.place(key);
					if ((next - placement & mask) > (i - placement & mask)) {
						keyTable[i] = key;
						i = next;
					}
					next = next + 1 & mask;
				}
				keyTable[i] = 0;
				if (i != currentIndex)
					--nextIndex;
			}
			currentIndex = INDEX_ILLEGAL;
			set.size--;
		}
		
		public int next() {
			if (!hasNext)
				throw new NoSuchElementException();
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			int key = nextIndex == INDEX_ZERO ? 0 : set.keyTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return key;
		}
		
		/**
		 * Returns a new array containing the remaining keys.
		 */
		public IntArray toArray() {
			IntArray array = new IntArray(true, set.size);
			while (hasNext)
				array.add(next());
			return array;
		}
		
	}
	
}
