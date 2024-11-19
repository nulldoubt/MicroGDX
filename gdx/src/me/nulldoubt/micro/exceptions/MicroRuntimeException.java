package me.nulldoubt.micro.exceptions;

public class MicroRuntimeException extends RuntimeException {
	
	public MicroRuntimeException(final String message) {
		super(message);
	}
	
	public MicroRuntimeException(final Throwable t) {
		super(t);
	}
	
	public MicroRuntimeException(final String message, final Throwable t) {
		super(message, t);
	}
	
}
