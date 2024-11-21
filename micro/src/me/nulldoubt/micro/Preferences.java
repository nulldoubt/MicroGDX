package me.nulldoubt.micro;

import java.util.Map;

public interface Preferences {
	
	Preferences putBoolean(final String key, final boolean val);
	
	Preferences putInteger(final String key, final int val);
	
	Preferences putLong(final String key, final long val);
	
	Preferences putFloat(final String key, final float val);
	
	Preferences putString(final String key, final String val);
	
	Preferences put(final Map<String, ?> vals);
	
	boolean getBoolean(final String key);
	
	int getInteger(final String key);
	
	long getLong(final String key);
	
	float getFloat(final String key);
	
	String getString(final String key);
	
	boolean getBoolean(final String key, final boolean defValue);
	
	int getInteger(final String key, final int defValue);
	
	long getLong(final String key, final long defValue);
	
	float getFloat(final String key, final float defValue);
	
	String getString(final String key, final String defValue);
	
	Map<String, ?> get();
	
	boolean contains(final String key);
	
	void clear();
	
	void remove(final String key);
	
	void flush();
	
}
