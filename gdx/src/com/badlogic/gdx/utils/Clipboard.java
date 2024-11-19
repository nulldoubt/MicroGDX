package com.badlogic.gdx.utils;

public interface Clipboard {
	
	boolean hasContents();
	
	String getContents();
	
	void setContents(final String content);
	
}
