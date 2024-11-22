package me.nulldoubt.micro.backends.android.keyboardheight;

public interface KeyboardHeightObserver {
	
	void onKeyboardHeightChanged(int height, int leftInset, int rightInset, int orientation);
	
}
