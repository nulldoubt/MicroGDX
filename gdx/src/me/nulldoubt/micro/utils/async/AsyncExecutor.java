package me.nulldoubt.micro.utils.async;

import me.nulldoubt.micro.utils.Disposable;
import me.nulldoubt.micro.utils.MicroRuntimeException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AsyncExecutor implements Disposable {
	
	private final ExecutorService executor;
	
	public AsyncExecutor(final int maxConcurrent) {
		this(maxConcurrent, "AsyncExecutor-Thread");
	}
	
	public AsyncExecutor(final int maxConcurrent, final String name) {
		executor = Executors.newFixedThreadPool(maxConcurrent, (runnable) -> {
			Thread thread = new Thread(runnable, name);
			thread.setDaemon(true);
			return thread;
		});
	}
	
	public <T> AsyncResult<T> submit(final AsyncTask<T> task) {
		if (executor.isShutdown())
			throw new MicroRuntimeException("Cannot run tasks on an executor that has been shutdown (disposed)");
		return new AsyncResult<>(executor.submit(task::call));
	}
	
	@Override
	public void dispose() {
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new MicroRuntimeException("Couldn't shutdown loading thread", e);
		}
	}
	
}
