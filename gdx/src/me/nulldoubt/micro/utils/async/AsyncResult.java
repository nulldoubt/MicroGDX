package me.nulldoubt.micro.utils.async;

import me.nulldoubt.micro.exceptions.MicroRuntimeException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class AsyncResult<T> {
	
	private final Future<T> future;
	
	AsyncResult(Future<T> future) {
		this.future = future;
	}
	
	public boolean isDone() {
		return future.isDone();
	}
	
	public T get() {
		try {
			return future.get();
		} catch (InterruptedException ex) {
			return null;
		} catch (ExecutionException ex) {
			throw new MicroRuntimeException(ex.getCause());
		}
	}
	
}
