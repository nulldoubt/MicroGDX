package me.nulldoubt.micro.utils.async;

public interface AsyncTask<T> {
	
	T call() throws Exception;
	
}
