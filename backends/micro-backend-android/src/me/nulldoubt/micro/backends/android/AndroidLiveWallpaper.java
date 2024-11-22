package me.nulldoubt.micro.backends.android;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import me.nulldoubt.micro.*;
import me.nulldoubt.micro.backends.android.surfaceview.FillResolutionStrategy;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.graphics.Color;
import me.nulldoubt.micro.utils.collections.Array;
import me.nulldoubt.micro.utils.collections.SnapshotArray;
import me.nulldoubt.micro.utils.natives.MicroNativesLoader;

public class AndroidLiveWallpaper implements AndroidApplicationBase {
	
	protected AndroidLiveWallpaperService service;
	
	protected AndroidGraphicsLiveWallpaper graphics;
	protected AndroidInput input;
	protected AndroidAudio audio;
	protected AndroidFiles files;
	protected AndroidClipboard clipboard;
	protected ApplicationListener listener;
	protected boolean firstResume = true;
	protected final Array<Runnable> runnables = new Array<>();
	protected final Array<Runnable> executedRunnables = new Array<>();
	protected final SnapshotArray<LifecycleListener> lifecycleListeners = new SnapshotArray<>(
			LifecycleListener.class);
	protected int logLevel = LOG_INFO;
	protected ApplicationLogger applicationLogger;
	protected volatile Color[] wallpaperColors = null;
	
	public AndroidLiveWallpaper(AndroidLiveWallpaperService service) {
		this.service = service;
	}
	
	public void initialize(ApplicationListener listener, AndroidApplicationConfiguration config) {
		if (this.getVersion() < MINIMUM_SDK) {
			throw new MicroRuntimeException("libGDX requires Android API Level " + MINIMUM_SDK + " or later.");
		}
		MicroNativesLoader.load();
		setApplicationLogger(new AndroidApplicationLogger());
		graphics = new AndroidGraphicsLiveWallpaper(this, config,
				config.resolutionStrategy == null ? new FillResolutionStrategy() : config.resolutionStrategy);
		
		input = createInput(this, this.getService(), graphics.view, config);
		
		audio = createAudio(this.getService(), config);
		files = createFiles();
		this.listener = listener;
		clipboard = new AndroidClipboard(this.getService());
		
		Micro.app = this;
		Micro.input = input;
		Micro.audio = audio;
		Micro.files = files;
		Micro.graphics = graphics;
	}
	
	public void onPause() {
		if (AndroidLiveWallpaperService.DEBUG)
			Log.d(AndroidLiveWallpaperService.TAG, " > AndroidLiveWallpaper - onPause()");
		
		audio.pause();
		input.onPause();
		
		if (graphics != null)
			graphics.onPauseGLSurfaceView();
		
		if (AndroidLiveWallpaperService.DEBUG)
			Log.d(AndroidLiveWallpaperService.TAG, " > AndroidLiveWallpaper - onPause() done!");
	}
	
	public void onResume() {
		Micro.app = this;
		Micro.input = input;
		Micro.audio = audio;
		Micro.files = files;
		Micro.graphics = graphics;
		
		input.onResume();
		
		if (graphics != null) {
			graphics.onResumeGLSurfaceView();
		}
		
		if (!firstResume) {
			audio.resume();
			graphics.resume();
		} else
			firstResume = false;
	}
	
	public void onDestroy() {
		if (graphics != null)
			graphics.onDestroyGLSurfaceView();
		if (audio != null)
			audio.dispose();
	}
	
	@Override
	public WindowManager getWindowManager() {
		return service.getWindowManager();
	}
	
	public AndroidLiveWallpaperService getService() {
		return service;
	}
	
	@Override
	public ApplicationListener getApplicationListener() {
		return listener;
	}
	
	@Override
	public void post(Runnable runnable) {
		synchronized (runnables) {
			runnables.add(runnable);
		}
	}
	
	@Override
	public Audio getAudio() {
		return audio;
	}
	
	@Override
	public Files getFiles() {
		return files;
	}
	
	@Override
	public Graphics getGraphics() {
		return graphics;
	}
	
	@Override
	public AndroidInput getInput() {
		return input;
	}
	
	@Override
	public ApplicationType getType() {
		return ApplicationType.Android;
	}
	
	@Override
	public int getVersion() {
		return android.os.Build.VERSION.SDK_INT;
	}
	
	@Override
	public long getJavaHeap() {
		return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	}
	
	@Override
	public long getNativeHeap() {
		return Debug.getNativeHeapAllocatedSize();
	}
	
	@Override
	public Preferences getPreferences(String name) {
		return new AndroidPreferences(service.getSharedPreferences(name, Context.MODE_PRIVATE));
	}
	
	@Override
	public Clipboard getClipboard() {
		return clipboard;
	}
	
	@Override
	public void debug(String tag, String message) {
		if (logLevel >= LOG_DEBUG)
			getApplicationLogger().debug(tag, message);
	}
	
	@Override
	public void debug(String tag, String message, Throwable exception) {
		if (logLevel >= LOG_DEBUG)
			getApplicationLogger().debug(tag, message, exception);
	}
	
	@Override
	public void log(String tag, String message) {
		if (logLevel >= LOG_INFO)
			getApplicationLogger().log(tag, message);
	}
	
	@Override
	public void log(String tag, String message, Throwable exception) {
		if (logLevel >= LOG_INFO)
			getApplicationLogger().log(tag, message, exception);
	}
	
	@Override
	public void error(String tag, String message) {
		if (logLevel >= LOG_ERROR)
			getApplicationLogger().error(tag, message);
	}
	
	@Override
	public void error(String tag, String message, Throwable exception) {
		if (logLevel >= LOG_ERROR)
			getApplicationLogger().error(tag, message, exception);
	}
	
	@Override
	public void setLogLevel(int logLevel) {
		this.logLevel = logLevel;
	}
	
	@Override
	public int getLogLevel() {
		return logLevel;
	}
	
	@Override
	public void setApplicationLogger(ApplicationLogger applicationLogger) {
		this.applicationLogger = applicationLogger;
	}
	
	@Override
	public ApplicationLogger getApplicationLogger() {
		return applicationLogger;
	}
	
	@Override
	public void exit() {}
	
	@Override
	public void register(LifecycleListener listener) {
		synchronized (lifecycleListeners) {
			lifecycleListeners.add(listener);
		}
	}
	
	@Override
	public void unregister(LifecycleListener listener) {
		synchronized (lifecycleListeners) {
			lifecycleListeners.removeValue(listener, true);
		}
	}
	
	@Override
	public Context getContext() {
		return service;
	}
	
	@Override
	public Array<Runnable> getRunnables() {
		return runnables;
	}
	
	@Override
	public Array<Runnable> getExecutedRunnables() {
		return executedRunnables;
	}
	
	@Override
	public SnapshotArray<LifecycleListener> getLifecycleListeners() {
		return lifecycleListeners;
	}
	
	@Override
	public void startActivity(Intent intent) {
		service.startActivity(intent);
	}
	
	@Override
	public Window getApplicationWindow() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Handler getHandler() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public AndroidAudio createAudio(Context context, AndroidApplicationConfiguration config) {
		if (!config.disableAudio)
			return new DefaultAndroidAudio(context, config);
		else
			return new DisabledAndroidAudio();
	}
	
	@Override
	public AndroidInput createInput(Application activity, Context context, Object view, AndroidApplicationConfiguration config) {
		return new DefaultAndroidInput(this, this.getService(), graphics.view, config);
	}
	
	protected AndroidFiles createFiles() {
		this.getService().getFilesDir();
		return new DefaultAndroidFiles(this.getService().getAssets(), this.getService(), true);
	}
	
	@Override
	public void runOnUiThread(Runnable runnable) {
		if (Looper.myLooper() != Looper.getMainLooper())
			new Handler(Looper.getMainLooper()).post(runnable);
		else
			runnable.run();
	}
	
	@Override
	public void useImmersiveMode(boolean b) {
		throw new UnsupportedOperationException();
	}
	
	public void notifyColorsChanged(Color primaryColor, Color secondaryColor, Color tertiaryColor) {
		if (Build.VERSION.SDK_INT < 27)
			return;
		final Color[] colors = new Color[3];
		colors[0] = new Color(primaryColor);
		colors[1] = new Color(secondaryColor);
		colors[2] = new Color(tertiaryColor);
		wallpaperColors = colors;
		AndroidLiveWallpaperService.AndroidWallpaperEngine engine = service.linkedEngine;
		if (engine != null)
			engine.notifyColorsChanged();
	}
	
}
