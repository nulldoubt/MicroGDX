package me.nulldoubt.micro.utils.pools;

import me.nulldoubt.micro.utils.Array;
import me.nulldoubt.micro.utils.ObjectMap;

import java.util.function.Supplier;

public class Pools {
	
	private static final ObjectMap<Class<?>, Pool<?>> typePools = new ObjectMap<>();
	
	public static <T> Pool<T> get(final Class<T> type, final int max, final Supplier<T> supplier) {
		Pool<T> pool = (Pool<T>) typePools.get(type);
		if (pool == null)
			set(type, new SupplierPool<>(supplier, 4, max));
		return pool;
	}
	
	public static <T> Pool<T> get(final Class<T> type, final Supplier<T> supplier) {
		return get(type, 100, supplier);
	}
	
	public static <T> void set(final Class<T> type, final Pool<T> pool) {
		typePools.put(type, pool);
	}
	
	public static <T> T obtain(final Class<T> type, final Supplier<T> supplier) {
		return get(type, supplier).obtain();
	}
	
	public static void free(final Object object) {
		if (object == null)
			throw new IllegalArgumentException("object cannot be null.");
		final Pool pool = typePools.get(object.getClass());
		if (pool == null)
			return; // Ignore freeing an object that was never retained.
		pool.free(object);
	}
	
	public static void freeAll(final Array<?> objects) {
		freeAll(objects, false);
	}
	
	public static void freeAll(final Array<?> objects, final boolean samePool) {
		if (objects == null)
			throw new IllegalArgumentException("objects cannot be null.");
		Pool pool = null;
		for (int i = 0, n = objects.size; i < n; i++) {
			final Object object = objects.get(i);
			if (object == null)
				continue;
			if (pool == null) {
				pool = typePools.get(object.getClass());
				if (pool == null)
					continue; // Ignore freeing an object that was never retained.
			}
			pool.free(object);
			if (!samePool)
				pool = null;
		}
	}
	
	private Pools() {}
	
}
