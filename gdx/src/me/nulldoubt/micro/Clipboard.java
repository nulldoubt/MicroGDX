package me.nulldoubt.micro;

public interface Clipboard {
	
	boolean hasContents();
	
	String getContents();
	
	void setContents(final String content);
	
}
