package me.nulldoubt.micro.utils.collections;

import me.nulldoubt.micro.utils.collections.operations.Select;
import me.nulldoubt.micro.utils.collections.operations.Sort;

import java.util.*;

public class Array<T> implements Iterable<T> {
	
	public T[] items;
	
	public int size;
	public boolean ordered;
	
	public Array() {
		this(true, 16);
	}
	
	public Array(int capacity) {
		this(true, capacity);
	}
	
	public Array(boolean ordered, int capacity) {
		this.ordered = ordered;
		items = (T[]) new Object[capacity];
	}
	
	public Array(boolean ordered, int capacity, Class arrayType) {
		this.ordered = ordered;
		items = (T[]) java.lang.reflect.Array.newInstance(arrayType, capacity);
	}
	
	public Array(Class<?> arrayType) {
		this(true, 16, arrayType);
	}
	
	public Array(Array<? extends T> array) {
		this(array.ordered, array.size, array.items.getClass().getComponentType());
		size = array.size;
		System.arraycopy(array.items, 0, items, 0, size);
	}
	
	public Array(T[] array) {
		this(true, array, 0, array.length);
	}
	
	public Array(boolean ordered, T[] array, int start, int count) {
		this(ordered, count, array.getClass().getComponentType());
		size = count;
		System.arraycopy(array, start, items, 0, size);
	}
	
	public void add(T value) {
		T[] items = this.items;
		if (size == items.length)
			items = resize(Math.max(8, (int) (size * 1.75f)));
		items[size++] = value;
	}
	
	public void add(T value1, T value2) {
		T[] items = this.items;
		if (size + 1 >= items.length)
			items = resize(Math.max(8, (int) (size * 1.75f)));
		items[size] = value1;
		items[size + 1] = value2;
		size += 2;
	}
	
	public void add(T value1, T value2, T value3) {
		T[] items = this.items;
		if (size + 2 >= items.length)
			items = resize(Math.max(8, (int) (size * 1.75f)));
		items[size] = value1;
		items[size + 1] = value2;
		items[size + 2] = value3;
		size += 3;
	}
	
	public void add(T value1, T value2, T value3, T value4) {
		T[] items = this.items;
		if (size + 3 >= items.length)
			items = resize(Math.max(8, (int) (size * 1.8f))); // 1.75 isn't enough when size=5.
		items[size] = value1;
		items[size + 1] = value2;
		items[size + 2] = value3;
		items[size + 3] = value4;
		size += 4;
	}
	
	public void addAll(Array<? extends T> array) {
		addAll(array.items, 0, array.size);
	}
	
	public void addAll(Array<? extends T> array, int start, int count) {
		if (start + count > array.size)
			throw new IllegalArgumentException("start + count must be <= size: " + start + " + " + count + " <= " + array.size);
		addAll(array.items, start, count);
	}
	
	@SafeVarargs
	public final void addAll(T... array) {
		addAll(array, 0, array.length);
	}
	
	public void addAll(T[] array, int start, int count) {
		T[] items = this.items;
		int sizeNeeded = size + count;
		if (sizeNeeded > items.length)
			items = resize(Math.max(Math.max(8, sizeNeeded), (int) (size * 1.75f)));
		System.arraycopy(array, start, items, size, count);
		size = sizeNeeded;
	}
	
	public T get(int index) {
		if (index >= size)
			throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		return items[index];
	}
	
	public void set(int index, T value) {
		if (index >= size)
			throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		items[index] = value;
	}
	
	public void insert(int index, T value) {
		if (index > size)
			throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + size);
		T[] items = this.items;
		if (size == items.length)
			items = resize(Math.max(8, (int) (size * 1.75f)));
		if (ordered)
			System.arraycopy(items, index, items, index + 1, size - index);
		else
			items[size] = items[index];
		size++;
		items[index] = value;
	}
	
	/**
	 * Inserts the specified number of items at the specified index. The new items will have values equal to the values at those
	 * indices before the insertion.
	 */
	public void insertRange(int index, int count) {
		if (index > size)
			throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + size);
		int sizeNeeded = size + count;
		if (sizeNeeded > items.length)
			items = resize(Math.max(Math.max(8, sizeNeeded), (int) (size * 1.75f)));
		System.arraycopy(items, index, items, index + count, size - index);
		size = sizeNeeded;
	}
	
	public void swap(int first, int second) {
		if (first >= size)
			throw new IndexOutOfBoundsException("first can't be >= size: " + first + " >= " + size);
		if (second >= size)
			throw new IndexOutOfBoundsException("second can't be >= size: " + second + " >= " + size);
		T[] items = this.items;
		T firstValue = items[first];
		items[first] = items[second];
		items[second] = firstValue;
	}
	
	/**
	 * Returns true if this array contains the specified value.
	 *
	 * @param value    May be null.
	 * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
	 */
	public boolean contains(T value, boolean identity) {
		T[] items = this.items;
		int i = size - 1;
		if (identity || value == null) {
			while (i >= 0)
				if (items[i--] == value)
					return true;
		} else {
			while (i >= 0)
				if (value.equals(items[i--]))
					return true;
		}
		return false;
	}
	
	/**
	 * Returns true if this array contains all the specified values.
	 *
	 * @param values   May contains nulls.
	 * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
	 */
	public boolean containsAll(Array<? extends T> values, boolean identity) {
		T[] items = values.items;
		for (int i = 0, n = values.size; i < n; i++)
			if (!contains(items[i], identity))
				return false;
		return true;
	}
	
	/**
	 * Returns true if this array contains any the specified values.
	 *
	 * @param values   May contains nulls.
	 * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
	 */
	public boolean containsAny(Array<? extends T> values, boolean identity) {
		T[] items = values.items;
		for (int i = 0, n = values.size; i < n; i++)
			if (contains(items[i], identity))
				return true;
		return false;
	}
	
	/**
	 * Returns the index of first occurrence of value in the array, or -1 if no such value exists.
	 *
	 * @param value    May be null.
	 * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
	 * @return An index of first occurrence of value in array or -1 if no such value exists
	 */
	public int indexOf(T value, boolean identity) {
		T[] items = this.items;
		if (identity || value == null) {
			for (int i = 0, n = size; i < n; i++)
				if (items[i] == value)
					return i;
		} else {
			for (int i = 0, n = size; i < n; i++)
				if (value.equals(items[i]))
					return i;
		}
		return -1;
	}
	
	/**
	 * Returns an index of last occurrence of value in array or -1 if no such value exists. Search is started from the end of an
	 * array.
	 *
	 * @param value    May be null.
	 * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
	 * @return An index of last occurrence of value in array or -1 if no such value exists
	 */
	public int lastIndexOf(T value, boolean identity) {
		T[] items = this.items;
		if (identity || value == null) {
			for (int i = size - 1; i >= 0; i--)
				if (items[i] == value)
					return i;
		} else {
			for (int i = size - 1; i >= 0; i--)
				if (value.equals(items[i]))
					return i;
		}
		return -1;
	}
	
	/**
	 * Removes the first instance of the specified value in the array.
	 *
	 * @param value    May be null.
	 * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
	 * @return true if value was found and removed, false otherwise
	 */
	public boolean removeValue(T value, boolean identity) {
		T[] items = this.items;
		if (identity || value == null) {
			for (int i = 0, n = size; i < n; i++) {
				if (items[i] == value) {
					removeIndex(i);
					return true;
				}
			}
		} else {
			for (int i = 0, n = size; i < n; i++) {
				if (value.equals(items[i])) {
					removeIndex(i);
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Removes and returns the item at the specified index.
	 */
	public T removeIndex(int index) {
		if (index >= size)
			throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		T[] items = this.items;
		T value = items[index];
		size--;
		if (ordered)
			System.arraycopy(items, index + 1, items, index, size - index);
		else
			items[index] = items[size];
		items[size] = null;
		return value;
	}
	
	/**
	 * Removes the items between the specified indices, inclusive.
	 */
	public void removeRange(int start, int end) {
		int n = size;
		if (end >= n)
			throw new IndexOutOfBoundsException("end can't be >= size: " + end + " >= " + size);
		if (start > end)
			throw new IndexOutOfBoundsException("start can't be > end: " + start + " > " + end);
		T[] items = this.items;
		int count = end - start + 1, lastIndex = n - count;
		if (ordered)
			System.arraycopy(items, start + count, items, start, n - (start + count));
		else {
			int i = Math.max(lastIndex, end + 1);
			System.arraycopy(items, i, items, start, n - i);
		}
		for (int i = lastIndex; i < n; i++)
			items[i] = null;
		size = n - count;
	}
	
	/**
	 * Removes from this array all of elements contained in the specified array.
	 *
	 * @param identity True to use ==, false to use .equals().
	 * @return true if this array was modified.
	 */
	public boolean removeAll(Array<? extends T> array, boolean identity) {
		int size = this.size;
		int startSize = size;
		T[] items = this.items;
		if (identity) {
			for (int i = 0, n = array.size; i < n; i++) {
				T item = array.get(i);
				for (int ii = 0; ii < size; ii++) {
					if (item == items[ii]) {
						removeIndex(ii);
						size--;
						break;
					}
				}
			}
		} else {
			for (int i = 0, n = array.size; i < n; i++) {
				T item = array.get(i);
				for (int ii = 0; ii < size; ii++) {
					if (item.equals(items[ii])) {
						removeIndex(ii);
						size--;
						break;
					}
				}
			}
		}
		return size != startSize;
	}
	
	/**
	 * Removes and returns the last item.
	 */
	public T pop() {
		if (size == 0)
			throw new IllegalStateException("Array is empty.");
		--size;
		T item = items[size];
		items[size] = null;
		return item;
	}
	
	/**
	 * Returns the last item.
	 */
	public T peek() {
		if (size == 0)
			throw new IllegalStateException("Array is empty.");
		return items[size - 1];
	}
	
	/**
	 * Returns the first item.
	 */
	public T first() {
		if (size == 0)
			throw new IllegalStateException("Array is empty.");
		return items[0];
	}
	
	/**
	 * Returns true if the array has one or more items.
	 */
	public boolean notEmpty() {
		return size > 0;
	}
	
	/**
	 * Returns true if the array is empty.
	 */
	public boolean isEmpty() {
		return size == 0;
	}
	
	public void clear() {
		Arrays.fill(items, 0, size, null);
		size = 0;
	}
	
	/**
	 * Reduces the size of the backing array to the size of the actual items. This is useful to release memory when many items
	 * have been removed, or if it is known that more items will not be added.
	 *
	 * @return {@link #items}
	 */
	public T[] shrink() {
		if (items.length != size)
			resize(size);
		return items;
	}
	
	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items. Useful before adding many
	 * items to avoid multiple backing array resizes.
	 *
	 * @return {@link #items}
	 */
	public T[] ensureCapacity(int additionalCapacity) {
		if (additionalCapacity < 0)
			throw new IllegalArgumentException("additionalCapacity must be >= 0: " + additionalCapacity);
		int sizeNeeded = size + additionalCapacity;
		if (sizeNeeded > items.length)
			resize(Math.max(Math.max(8, sizeNeeded), (int) (size * 1.75f)));
		return items;
	}
	
	public T[] setSize(int newSize) {
		truncate(newSize);
		if (newSize > items.length)
			resize(Math.max(8, newSize));
		size = newSize;
		return items;
	}
	
	/**
	 * Creates a new backing array with the specified size containing the current items.
	 */
	protected T[] resize(int newSize) {
		T[] items = this.items;
		T[] newItems = (T[]) java.lang.reflect.Array.newInstance(items.getClass().getComponentType(), newSize);
		System.arraycopy(items, 0, newItems, 0, Math.min(size, newItems.length));
		this.items = newItems;
		return newItems;
	}
	
	/**
	 * Sorts this array. The array elements must implement {@link Comparable}. This method is not thread safe (uses
	 * {@link Sort#instance()}).
	 */
	public void sort() {
		Sort.instance().sort(items, 0, size);
	}
	
	/**
	 * Sorts the array. This method is not thread safe (uses {@link Sort#instance()}).
	 */
	public void sort(Comparator<? super T> comparator) {
		Sort.instance().sort(items, comparator, 0, size);
	}
	
	public T selectRanked(Comparator<T> comparator, int kthLowest) {
		if (kthLowest < 1)
			throw new RuntimeException("nth_lowest must be greater than 0, 1 = first, 2 = second...");
		return Select.instance().select(items, comparator, kthLowest, size);
	}
	
	public int selectRankedIndex(Comparator<T> comparator, int kthLowest) {
		if (kthLowest < 1)
			throw new RuntimeException("nth_lowest must be greater than 0, 1 = first, 2 = second...");
		return Select.instance().selectIndex(items, comparator, kthLowest, size);
	}
	
	public void reverse() {
		T[] items = this.items;
		for (int i = 0, lastIndex = size - 1, n = size / 2; i < n; i++) {
			int ii = lastIndex - i;
			T temp = items[i];
			items[i] = items[ii];
			items[ii] = temp;
		}
	}
	
	public void shuffle() {
		T[] items = this.items;
		for (int i = size - 1; i >= 0; i--) {
			int ii = CollectionUtils.random(i);
			T temp = items[i];
			items[i] = items[ii];
			items[ii] = temp;
		}
	}
	
	public ArrayIterator<T> iterator() {
		return new ArrayIterator<T>(this, true);
	}
	
	public void truncate(int newSize) {
		if (newSize < 0)
			throw new IllegalArgumentException("newSize must be >= 0: " + newSize);
		if (size <= newSize)
			return;
		for (int i = newSize; i < size; i++)
			items[i] = null;
		size = newSize;
	}
	
	public T random() {
		if (size == 0)
			return null;
		return items[CollectionUtils.random(size - 1)];
	}
	
	public T[] toArray() {
		return (T[]) toArray(items.getClass().getComponentType());
	}
	
	public <V> V[] toArray(Class<V> type) {
		V[] result = (V[]) java.lang.reflect.Array.newInstance(type, size);
		System.arraycopy(items, 0, result, 0, size);
		return result;
	}
	
	public int hashCode() {
		if (!ordered)
			return super.hashCode();
		Object[] items = this.items;
		int h = 1;
		for (int i = 0, n = size; i < n; i++) {
			h *= 31;
			Object item = items[i];
			if (item != null)
				h += item.hashCode();
		}
		return h;
	}
	
	/**
	 * Returns false if either array is unordered.
	 */
	public boolean equals(Object object) {
		if (object == this)
			return true;
		if (!ordered)
			return false;
		if (!(object instanceof Array array))
			return false;
		if (!array.ordered)
			return false;
		int n = size;
		if (n != array.size)
			return false;
		Object[] items1 = this.items, items2 = array.items;
		for (int i = 0; i < n; i++) {
			Object o1 = items1[i], o2 = items2[i];
			if (!(Objects.equals(o1, o2)))
				return false;
		}
		return true;
	}
	
	public boolean equalsIdentity(Object object) {
		if (object == this)
			return true;
		if (!ordered)
			return false;
		if (!(object instanceof Array array))
			return false;
		if (!array.ordered)
			return false;
		int n = size;
		if (n != array.size)
			return false;
		Object[] items1 = this.items, items2 = array.items;
		for (int i = 0; i < n; i++)
			if (items1[i] != items2[i])
				return false;
		return true;
	}
	
	public String toString() {
		if (size == 0)
			return "[]";
		T[] items = this.items;
		StringBuilder buffer = new StringBuilder(32);
		buffer.append('[');
		buffer.append(items[0]);
		for (int i = 1; i < size; i++) {
			buffer.append(", ");
			buffer.append(items[i]);
		}
		buffer.append(']');
		return buffer.toString();
	}
	
	public String toString(String separator) {
		if (size == 0)
			return "";
		T[] items = this.items;
		StringBuilder buffer = new StringBuilder(32);
		buffer.append(items[0]);
		for (int i = 1; i < size; i++) {
			buffer.append(separator);
			buffer.append(items[i]);
		}
		return buffer.toString();
	}
	
	public static <T> Array<T> of(Class<T> arrayType) {
		return new Array<>(arrayType);
	}
	
	public static <T> Array<T> of(boolean ordered, int capacity, Class<T> arrayType) {
		return new Array<>(ordered, capacity, arrayType);
	}
	
	@SafeVarargs
	public static <T> Array<T> with(T... array) {
		return new Array<>(array);
	}
	
	public static class ArrayIterator<T> implements Iterator<T>, Iterable<T> {
		
		private final Array<T> array;
		private final boolean allowRemove;
		int index;
		boolean valid = true;
		
		// ArrayIterable<T> iterable;
		
		public ArrayIterator(Array<T> array) {
			this(array, true);
		}
		
		public ArrayIterator(Array<T> array, boolean allowRemove) {
			this.array = array;
			this.allowRemove = allowRemove;
		}
		
		public boolean hasNext() {
			if (!valid) {
				// System.out.println(iterable.lastAcquire);
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			return index < array.size;
		}
		
		public T next() {
			if (index >= array.size)
				throw new NoSuchElementException(String.valueOf(index));
			if (!valid) {
				// System.out.println(iterable.lastAcquire);
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			return array.items[index++];
		}
		
		public void remove() {
			if (!allowRemove)
				throw new RuntimeException("Remove not allowed.");
			index--;
			array.removeIndex(index);
		}
		
		public void reset() {
			index = 0;
		}
		
		public ArrayIterator<T> iterator() {
			return this;
		}
		
	}
	
	public static class ArrayIterable<T> implements Iterable<T> {
		
		private final Array<T> array;
		private final boolean allowRemove;
		
		public ArrayIterable(Array<T> array) {
			this(array, true);
		}
		
		public ArrayIterable(Array<T> array, boolean allowRemove) {
			this.array = array;
			this.allowRemove = allowRemove;
		}
		
		public ArrayIterator<T> iterator() {
			return new ArrayIterator<T>(array, allowRemove);
		}
		
	}
	
}
