package me.nulldoubt.micro.backends.android;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import me.nulldoubt.micro.audio.Sound;
import me.nulldoubt.micro.files.FileHandle;

public class AsynchronousAndroidAudio extends DefaultAndroidAudio {
	
	private final HandlerThread handlerThread;
	private final Handler handler;
	
	public AsynchronousAndroidAudio(Context context, AndroidApplicationConfiguration config) {
		super(context, config);
		handlerThread = new HandlerThread("libGDX Sound Management");
		handlerThread.start();
		handler = new Handler(handlerThread.getLooper());
	}
	
	@Override
	public void dispose() {
		super.dispose();
		if (handlerThread != null)
			handlerThread.quit();
	}
	
	@Override
	public Sound newSound(FileHandle file) {
		Sound sound = super.newSound(file);
		return new AsynchronousSound(sound, handler);
	}
	
}
