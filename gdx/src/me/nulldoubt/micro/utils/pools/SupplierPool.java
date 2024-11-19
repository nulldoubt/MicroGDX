package me.nulldoubt.micro.utils.pools;

import java.util.function.Supplier;

public class SupplierPool<T> extends Pool<T> {
	
	private final Supplier<T> supplier;
	
	public SupplierPool(final Supplier<T> supplier, final int initial, final int max) {
		super(initial, max);
		this.supplier = supplier;
	}
	
	@Override
	protected T newObject() {
		return supplier.get();
	}
	
}
