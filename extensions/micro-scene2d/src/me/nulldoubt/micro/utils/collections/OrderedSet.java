package me.nulldoubt.micro.utils.collections;

import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.utils.strings.StringBuilder;

import java.util.NoSuchElementException;

public class OrderedSet<T> extends ObjectSet<T> {
	
	final Array<T> items;
	
	public OrderedSet() {
		items = new Array<>();
	}
	
	public OrderedSet(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		items = new Array<>(initialCapacity);
	}
	
	public OrderedSet(int initialCapacity) {
		super(initialCapacity);
		items = new Array<>(initialCapacity);
	}
	
	public OrderedSet(OrderedSet<? extends T> set) {
		super(set);
		items = new Array<>(set.items);
	}
	
	public boolean add(T key) {
		if (!super.add(key))
			return false;
		items.add(key);
		return true;
	}
	
	public boolean add(T key, int index) {
		if (!super.add(key)) {
			int oldIndex = items.indexOf(key, true);
			if (oldIndex != index)
				items.insert(index, items.removeIndex(oldIndex));
			return false;
		}
		items.insert(index, key);
		return true;
	}
	
	public void addAll(OrderedSet<T> set) {
		ensureCapacity(set.size);
		T[] keys = set.items.items;
		for (int i = 0, n = set.items.size; i < n; i++)
			add(keys[i]);
	}
	
	public boolean remove(T key) {
		if (!super.remove(key))
			return false;
		items.removeValue(key, false);
		return true;
	}
	
	public T removeIndex(int index) {
		T key = items.removeIndex(index);
		super.remove(key);
		return key;
	}
	
	public boolean alter(T before, T after) {
		if (contains(after))
			return false;
		if (!super.remove(before))
			return false;
		super.add(after);
		items.set(items.indexOf(before, false), after);
		return true;
	}
	
	public boolean alterIndex(int index, T after) {
		if (index < 0 || index >= size || contains(after))
			return false;
		super.remove(items.get(index));
		super.add(after);
		items.set(index, after);
		return true;
	}
	
	public void clear(int maximumCapacity) {
		items.clear();
		super.clear(maximumCapacity);
	}
	
	public void clear() {
		items.clear();
		super.clear();
	}
	
	public Array<T> orderedItems() {
		return items;
	}
	
	public OrderedSetIterator<T> iterator() {
		return new OrderedSetIterator<>(this);
	}
	
	public String toString() {
		if (size == 0)
			return "{}";
		T[] items = this.items.items;
		StringBuilder buffer = new StringBuilder(32);
		buffer.append('{');
		buffer.append(items[0]);
		for (int i = 1; i < size; i++) {
			buffer.append(", ");
			buffer.append(items[i]);
		}
		buffer.append('}');
		return buffer.toString();
	}
	
	public String toString(String separator) {
		return items.toString(separator);
	}
	
	public static class OrderedSetIterator<K> extends ObjectSetIterator<K> {
		
		private final Array<K> items;
		
		public OrderedSetIterator(OrderedSet<K> set) {
			super(set);
			items = set.items;
		}
		
		public void reset() {
			nextIndex = 0;
			hasNext = set.size > 0;
		}
		
		public K next() {
			if (!hasNext)
				throw new NoSuchElementException();
			if (!valid)
				throw new MicroRuntimeException("#iterator() cannot be used nested.");
			K key = items.get(nextIndex);
			nextIndex++;
			hasNext = nextIndex < set.size;
			return key;
		}
		
		public void remove() {
			if (nextIndex < 0)
				throw new IllegalStateException("next must be called before remove.");
			nextIndex--;
			((OrderedSet<?>) set).removeIndex(nextIndex);
		}
		
		public Array<K> toArray(Array<K> array) {
			array.addAll(items, nextIndex, items.size - nextIndex);
			nextIndex = items.size;
			hasNext = false;
			return array;
		}
		
		public Array<K> toArray() {
			return toArray(new Array<>(true, set.size - nextIndex));
		}
		
	}
	
	@SafeVarargs
	public static <T> OrderedSet<T> with(T... array) {
		OrderedSet<T> set = new OrderedSet<T>();
		set.addAll(array);
		return set;
	}
	
}
