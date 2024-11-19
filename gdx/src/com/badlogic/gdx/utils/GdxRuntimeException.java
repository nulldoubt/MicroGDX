package com.badlogic.gdx.utils;

public class GdxRuntimeException extends RuntimeException {
	
	public GdxRuntimeException(final String message) {
		super(message);
	}
	
	public GdxRuntimeException(final Throwable t) {
		super(t);
	}
	
	public GdxRuntimeException(final String message, final Throwable t) {
		super(message, t);
	}
	
}
