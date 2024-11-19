package com.badlogic.gdx;

import com.badlogic.gdx.utils.Clipboard;

public interface Application {
	
	enum ApplicationType {
		Android, Desktop, Applet
	}
	
	int LOG_NONE = 0;
	int LOG_DEBUG = 3;
	int LOG_INFO = 2;
	int LOG_ERROR = 1;
	
	ApplicationListener getApplicationListener();
	
	Graphics getGraphics();
	
	Audio getAudio();
	
	Input getInput();
	
	Files getFiles();
	
	void log(final String tag, final String message);
	
	void log(final String tag, final String message, final Throwable exception);
	
	void error(final String tag, final String message);
	
	void error(final String tag, final String message, final Throwable exception);
	
	void debug(final String tag, final String message);
	
	void debug(final String tag, final String message, final Throwable exception);
	
	void setLogLevel(final int logLevel);
	
	int getLogLevel();
	
	void setApplicationLogger(final ApplicationLogger applicationLogger);
	
	ApplicationLogger getApplicationLogger();
	
	ApplicationType getType();
	
	int getVersion();
	
	long getJavaHeap();
	
	long getNativeHeap();
	
	Preferences getPreferences(String name);
	
	Clipboard getClipboard();
	
	void post(final Runnable runnable);
	
	void exit();
	
	void register(final LifecycleListener listener);
	
	void unregister(final LifecycleListener listener);
	
}
