package me.nulldoubt.micro.backends.android;

import android.view.View;

public class AndroidVisibilityListener {
	
	public void createListener(final AndroidApplicationBase application) {
		try {
			View rootView = application.getApplicationWindow().getDecorView();
			rootView.setOnSystemUiVisibilityChangeListener(_ -> application.getHandler().post(() -> application.useImmersiveMode(true)));
		} catch (Throwable t) {
			application.log("AndroidApplication", "Can't create OnSystemUiVisibilityChangeListener, unable to use immersive mode.", t);
		}
	}
	
}
