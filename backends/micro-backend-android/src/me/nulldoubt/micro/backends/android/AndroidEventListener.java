package me.nulldoubt.micro.backends.android;

import android.content.Intent;

public interface AndroidEventListener {
	
	void onActivityResult(int requestCode, int resultCode, Intent data);
	
}
