package me.nulldoubt.micro.backends.android;

import android.view.View.OnGenericMotionListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import me.nulldoubt.micro.Input;

public interface AndroidInput extends Input, OnTouchListener, OnKeyListener, OnGenericMotionListener {
	
	void onPause();
	
	void onResume();
	
	void onDreamingStarted();
	
	void onDreamingStopped();
	
	void addKeyListener(OnKeyListener listener);
	
	void addGenericMotionListener(OnGenericMotionListener listener);
	
	void processEvents();
	
	void setKeyboardAvailable(boolean available);
	
}
