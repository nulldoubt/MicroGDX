package me.nulldoubt.micro.exceptions;

public class SharedLibraryException extends RuntimeException {
	
	public SharedLibraryException(final String message) {
		super(message);
	}
	
	public SharedLibraryException(final Throwable t) {
		super(t);
	}
	
	public SharedLibraryException(final String message, final Throwable t) {
		super(message, t);
	}
	
}
