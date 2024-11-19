package com.badlogic.gdx.utils.async;

public interface AsyncTask<T> {
	
	T call() throws Exception;
	
}
