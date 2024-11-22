package me.nulldoubt.micro.backends.android;

import android.util.Log;
import me.nulldoubt.micro.ApplicationLogger;

public class AndroidApplicationLogger implements ApplicationLogger {
	
	@Override
	public void log(String tag, String message) {
		Log.i(tag, message);
	}
	
	@Override
	public void log(String tag, String message, Throwable exception) {
		Log.i(tag, message, exception);
	}
	
	@Override
	public void error(String tag, String message) {
		Log.e(tag, message);
	}
	
	@Override
	public void error(String tag, String message, Throwable exception) {
		Log.e(tag, message, exception);
	}
	
	@Override
	public void debug(String tag, String message) {
		Log.d(tag, message);
	}
	
	@Override
	public void debug(String tag, String message, Throwable exception) {
		Log.d(tag, message, exception);
	}
	
}
