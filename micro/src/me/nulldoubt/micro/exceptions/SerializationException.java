package me.nulldoubt.micro.exceptions;

import me.nulldoubt.micro.utils.strings.StringBuilder;

public class SerializationException extends RuntimeException {
	
	private StringBuilder trace;
	
	public SerializationException() {
		super();
	}
	
	public SerializationException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public SerializationException(String message) {
		super(message);
	}
	
	public SerializationException(Throwable cause) {
		super("", cause);
	}
	
	public boolean causedBy(Class type) {
		return causedBy(this, type);
	}
	
	private boolean causedBy(Throwable ex, Class type) {
		Throwable cause = ex.getCause();
		if (cause == null || cause == ex)
			return false;
		if (type.isAssignableFrom(cause.getClass()))
			return true;
		return causedBy(cause, type);
	}
	
	public String getMessage() {
		if (trace == null)
			return super.getMessage();
		me.nulldoubt.micro.utils.strings.StringBuilder sb = new me.nulldoubt.micro.utils.strings.StringBuilder(512);
		sb.append(super.getMessage());
		if (!sb.isEmpty())
			sb.append('\n');
		sb.append("Serialization trace:");
		sb.append(trace);
		return sb.toString();
	}
	
	public void addTrace(String info) {
		if (info == null)
			throw new IllegalArgumentException("info cannot be null.");
		if (trace == null)
			trace = new StringBuilder(512);
		trace.append('\n');
		trace.append(info);
	}
	
}
