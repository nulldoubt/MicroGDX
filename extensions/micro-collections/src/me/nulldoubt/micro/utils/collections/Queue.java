package me.nulldoubt.micro.utils.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public class Queue<T> implements Iterable<T> {
	
	protected T[] values;
	
	protected int head = 0;
	
	protected int tail = 0;
	
	public int size = 0;
	
	public Queue() {
		this(16);
	}
	
	public Queue(int initialSize) {
		this.values = (T[]) new Object[initialSize];
	}
	
	public Queue(int initialSize, Class<T> type) {
		this.values = (T[]) java.lang.reflect.Array.newInstance(type, initialSize);
	}
	
	public void addLast(T object) {
		T[] values = this.values;
		
		if (size == values.length) {
			resize(values.length << 1);// * 2
			values = this.values;
		}
		
		values[tail++] = object;
		if (tail == values.length) {
			tail = 0;
		}
		size++;
	}
	
	public void addFirst(T object) {
		T[] values = this.values;
		
		if (size == values.length) {
			resize(values.length << 1);// * 2
			values = this.values;
		}
		
		int head = this.head;
		head--;
		if (head == -1) {
			head = values.length - 1;
		}
		values[head] = object;
		
		this.head = head;
		this.size++;
	}
	
	public void ensureCapacity(int additional) {
		final int needed = size + additional;
		if (values.length < needed) {
			resize(needed);
		}
	}
	
	protected void resize(int newSize) {
		final T[] values = this.values;
		final int head = this.head;
		final int tail = this.tail;
		
		final T[] newArray = (T[]) java.lang.reflect.Array.newInstance(values.getClass().getComponentType(), newSize);
		if (head < tail) {
			// Continuous
			System.arraycopy(values, head, newArray, 0, tail - head);
		} else if (size > 0) {
			// Wrapped
			final int rest = values.length - head;
			System.arraycopy(values, head, newArray, 0, rest);
			System.arraycopy(values, 0, newArray, rest, tail);
		}
		this.values = newArray;
		this.head = 0;
		this.tail = size;
	}
	
	public T removeFirst() {
		if (size == 0) {
			// Underflow
			throw new NoSuchElementException("Queue is empty.");
		}
		
		final T[] values = this.values;
		
		final T result = values[head];
		values[head] = null;
		head++;
		if (head == values.length) {
			head = 0;
		}
		size--;
		
		return result;
	}
	
	public T removeLast() {
		if (size == 0)
			throw new NoSuchElementException("Queue is empty.");
		
		final T[] values = this.values;
		int tail = this.tail;
		tail--;
		if (tail == -1) {
			tail = values.length - 1;
		}
		final T result = values[tail];
		values[tail] = null;
		this.tail = tail;
		size--;
		
		return result;
	}
	
	public int indexOf(T value, boolean identity) {
		if (size == 0)
			return -1;
		T[] values = this.values;
		final int head = this.head, tail = this.tail;
		if (identity || value == null) {
			if (head < tail) {
				for (int i = head; i < tail; i++)
					if (values[i] == value)
						return i - head;
			} else {
				for (int i = head, n = values.length; i < n; i++)
					if (values[i] == value)
						return i - head;
				for (int i = 0; i < tail; i++)
					if (values[i] == value)
						return i + values.length - head;
			}
		} else {
			if (head < tail) {
				for (int i = head; i < tail; i++)
					if (value.equals(values[i]))
						return i - head;
			} else {
				for (int i = head, n = values.length; i < n; i++)
					if (value.equals(values[i]))
						return i - head;
				for (int i = 0; i < tail; i++)
					if (value.equals(values[i]))
						return i + values.length - head;
			}
		}
		return -1;
	}
	
	public boolean removeValue(T value, boolean identity) {
		int index = indexOf(value, identity);
		if (index == -1)
			return false;
		removeIndex(index);
		return true;
	}
	
	public T removeIndex(int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException("index can't be < 0: " + index);
		if (index >= size)
			throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		
		T[] values = this.values;
		int head = this.head, tail = this.tail;
		index += head;
		T value;
		if (head < tail) { // index is between head and tail.
			value = values[index];
			System.arraycopy(values, index + 1, values, index, tail - index);
			values[tail] = null;
			this.tail--;
		} else if (index >= values.length) { // index is between 0 and tail.
			index -= values.length;
			value = values[index];
			System.arraycopy(values, index + 1, values, index, tail - index);
			this.tail--;
		} else { // index is between head and values.length.
			value = values[index];
			System.arraycopy(values, head, values, head + 1, index - head);
			values[head] = null;
			this.head++;
			if (this.head == values.length) {
				this.head = 0;
			}
		}
		size--;
		return value;
	}
	
	public boolean notEmpty() {
		return size > 0;
	}
	
	public boolean isEmpty() {
		return size == 0;
	}
	
	public T first() {
		if (size == 0) {
			// Underflow
			throw new NoSuchElementException("Queue is empty.");
		}
		return values[head];
	}
	
	public T last() {
		if (size == 0)
			throw new NoSuchElementException("Queue is empty.");
		final T[] values = this.values;
		int tail = this.tail;
		tail--;
		if (tail == -1) {
			tail = values.length - 1;
		}
		return values[tail];
	}
	
	public T get(int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException("index can't be < 0: " + index);
		if (index >= size)
			throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		final T[] values = this.values;
		
		int i = head + index;
		if (i >= values.length) {
			i -= values.length;
		}
		return values[i];
	}
	
	public void clear() {
		if (size == 0)
			return;
		final T[] values = this.values;
		final int head = this.head;
		final int tail = this.tail;
		
		if (head < tail) {
			// Continuous
			for (int i = head; i < tail; i++) {
				values[i] = null;
			}
		} else {
			// Wrapped
			for (int i = head; i < values.length; i++) {
				values[i] = null;
			}
			for (int i = 0; i < tail; i++) {
				values[i] = null;
			}
		}
		this.head = 0;
		this.tail = 0;
		this.size = 0;
	}
	
	public Iterator<T> iterator() {
		return new QueueIterator<>(this, true);
	}
	
	public String toString() {
		if (size == 0)
			return "[]";
		final T[] values = this.values;
		final int head = this.head;
		final int tail = this.tail;
		
		StringBuilder sb = new StringBuilder(64);
		sb.append('[');
		sb.append(values[head]);
		for (int i = (head + 1) % values.length; i != tail; i = (i + 1) % values.length) {
			sb.append(", ").append(values[i]);
		}
		sb.append(']');
		return sb.toString();
	}
	
	public String toString(String separator) {
		if (size == 0)
			return "";
		final T[] values = this.values;
		final int head = this.head;
		final int tail = this.tail;
		
		StringBuilder sb = new StringBuilder(64);
		sb.append(values[head]);
		for (int i = (head + 1) % values.length; i != tail; i = (i + 1) % values.length)
			sb.append(separator).append(values[i]);
		return sb.toString();
	}
	
	public int hashCode() {
		final int size = this.size;
		final T[] values = this.values;
		final int backingLength = values.length;
		int index = this.head;
		
		int hash = size + 1;
		for (int s = 0; s < size; s++) {
			final T value = values[index];
			
			hash *= 31;
			if (value != null)
				hash += value.hashCode();
			
			index++;
			if (index == backingLength)
				index = 0;
		}
		
		return hash;
	}
	
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Queue<?> queue))
			return false;
		
		final int size = this.size;
		
		if (queue.size != size)
			return false;
		
		final T[] myValues = this.values;
		final int myBackingLength = myValues.length;
		final Object[] itsValues = queue.values;
		final int itsBackingLength = itsValues.length;
		
		int myIndex = head;
		int itsIndex = queue.head;
		for (int s = 0; s < size; s++) {
			T myValue = myValues[myIndex];
			Object itsValue = itsValues[itsIndex];
			
			if (!(Objects.equals(myValue, itsValue)))
				return false;
			myIndex++;
			itsIndex++;
			if (myIndex == myBackingLength)
				myIndex = 0;
			if (itsIndex == itsBackingLength)
				itsIndex = 0;
		}
		return true;
	}
	
	public boolean equalsIdentity(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Queue<?> queue))
			return false;
		
		final int size = this.size;
		
		if (queue.size != size)
			return false;
		
		final T[] myValues = this.values;
		final int myBackingLength = myValues.length;
		final Object[] itsValues = queue.values;
		final int itsBackingLength = itsValues.length;
		
		int myIndex = head;
		int itsIndex = queue.head;
		for (int s = 0; s < size; s++) {
			if (myValues[myIndex] != itsValues[itsIndex])
				return false;
			myIndex++;
			itsIndex++;
			if (myIndex == myBackingLength)
				myIndex = 0;
			if (itsIndex == itsBackingLength)
				itsIndex = 0;
		}
		return true;
	}
	
	public static class QueueIterator<T> implements Iterator<T>, Iterable<T> {
		
		private final Queue<T> queue;
		private final boolean allowRemove;
		int index;
		boolean valid = true;
		
		public QueueIterator(Queue<T> queue) {
			this(queue, true);
		}
		
		public QueueIterator(Queue<T> queue, boolean allowRemove) {
			this.queue = queue;
			this.allowRemove = allowRemove;
		}
		
		public boolean hasNext() {
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			return index < queue.size;
		}
		
		public T next() {
			if (index >= queue.size)
				throw new NoSuchElementException(String.valueOf(index));
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			return queue.get(index++);
		}
		
		public void remove() {
			if (!allowRemove)
				throw new RuntimeException("Remove not allowed.");
			index--;
			queue.removeIndex(index);
		}
		
		public void reset() {
			index = 0;
		}
		
		public Iterator<T> iterator() {
			return this;
		}
		
	}
	
}