package me.nulldoubt.micro.utils.pools;

import me.nulldoubt.micro.utils.Array;

public abstract class Pool<T> {
	
	public final int max;
	public int peak;
	
	private final Array<T> freeObjects;
	
	public Pool() {
		this(16, Integer.MAX_VALUE);
	}
	
	public Pool(final int initialCapacity) {
		this(initialCapacity, Integer.MAX_VALUE);
	}
	
	public Pool(final int initialCapacity, final int max) {
		freeObjects = new Array<>(false, initialCapacity);
		this.max = max;
	}
	
	protected abstract T newObject();
	
	public T obtain() {
		return freeObjects.size == 0 ? newObject() : freeObjects.pop();
	}
	
	public void free(final T object) {
		if (object == null)
			throw new IllegalArgumentException("object cannot be null.");
		if (freeObjects.size < max) {
			freeObjects.add(object);
			peak = Math.max(peak, freeObjects.size);
			reset(object);
		} else
			discard(object);
	}
	
	public void fill(final int size) {
		for (int i = 0; i < size; i++)
			if (freeObjects.size < max)
				freeObjects.add(newObject());
		peak = Math.max(peak, freeObjects.size);
	}
	
	protected void reset(final T object) {
		if (object instanceof Poolable poolable)
			poolable.reset();
	}
	
	protected void discard(final T object) {
		reset(object);
	}
	
	public void freeAll(final Array<T> objects) {
		if (objects == null)
			throw new IllegalArgumentException("objects cannot be null.");
		final Array<T> freeObjects = this.freeObjects;
		int max = this.max;
		for (int i = 0, n = objects.size; i < n; i++) {
			T object = objects.get(i);
			if (object == null)
				continue;
			if (freeObjects.size < max) {
				freeObjects.add(object);
				reset(object);
			} else
				discard(object);
		}
		peak = Math.max(peak, freeObjects.size);
	}
	
	public void clear() {
		final Array<T> freeObjects = this.freeObjects;
		for (int i = 0, n = freeObjects.size; i < n; i++)
			discard(freeObjects.get(i));
		freeObjects.clear();
	}
	
	public int getFree() {
		return freeObjects.size;
	}
	
	public interface Poolable {
		
		void reset();
		
	}
	
}
