package com.badlogic.gdx;

public interface Clipboard {
	
	boolean hasContents();
	
	String getContents();
	
	void setContents(final String content);
	
}
