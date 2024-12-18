package me.nulldoubt.micro.backends.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Debug;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import androidx.fragment.app.Fragment;
import me.nulldoubt.micro.*;
import me.nulldoubt.micro.backends.android.surfaceview.FillResolutionStrategy;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.utils.collections.Array;
import me.nulldoubt.micro.utils.collections.SnapshotArray;
import me.nulldoubt.micro.utils.natives.MicroNativesLoader;

public class AndroidFragmentApplication extends Fragment implements AndroidApplicationBase {
	
	public interface Callbacks {
		
		void exit();
		
	}
	
	protected AndroidGraphics graphics;
	protected AndroidInput input;
	protected AndroidAudio audio;
	protected AndroidFiles files;
	protected AndroidClipboard clipboard;
	protected ApplicationListener listener;
	public Handler handler;
	protected boolean firstResume = true;
	protected final Array<Runnable> runnables = new Array<Runnable>();
	protected final Array<Runnable> executedRunnables = new Array<Runnable>();
	protected final SnapshotArray<LifecycleListener> lifecycleListeners = new SnapshotArray<LifecycleListener>(
			LifecycleListener.class);
	private final Array<AndroidEventListener> androidEventListeners = new Array<AndroidEventListener>();
	protected int logLevel = LOG_INFO;
	protected ApplicationLogger applicationLogger;
	
	protected Callbacks callbacks;
	
	@Override
	public void onAttach(Activity activity) {
		if (activity instanceof Callbacks) {
			this.callbacks = (Callbacks) activity;
		} else if (getParentFragment() instanceof Callbacks) {
			this.callbacks = (Callbacks) getParentFragment();
		} else if (getTargetFragment() instanceof Callbacks) {
			this.callbacks = (Callbacks) getTargetFragment();
		} else {
			throw new RuntimeException(
					"Missing AndroidFragmentApplication.Callbacks. Please implement AndroidFragmentApplication.Callbacks on the parent activity, fragment or target fragment.");
		}
		super.onAttach(activity);
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		this.callbacks = null;
	}
	
	protected FrameLayout.LayoutParams createLayoutParams() {
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
				android.view.ViewGroup.LayoutParams.MATCH_PARENT);
		layoutParams.gravity = Gravity.CENTER;
		return layoutParams;
	}
	
	protected void createWakeLock(boolean use) {
		if (use)
			getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
	
	@Override
	public void useImmersiveMode(boolean use) {
		if (!use)
			return;
		
		View view = this.graphics.getView();
		
		int code = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		view.setSystemUiVisibility(code);
	}
	
	public View initializeForView(ApplicationListener listener) {
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		return initializeForView(listener, config);
	}
	
	public View initializeForView(ApplicationListener listener, AndroidApplicationConfiguration config) {
		if (this.getVersion() < MINIMUM_SDK) {
			throw new MicroRuntimeException("libGDX requires Android API Level " + MINIMUM_SDK + " or later.");
		}
		MicroNativesLoader.load();
		setApplicationLogger(new AndroidApplicationLogger());
		graphics = new AndroidGraphics(this, config,
				config.resolutionStrategy == null ? new FillResolutionStrategy() : config.resolutionStrategy);
		input = createInput(this, getActivity(), graphics.view, config);
		audio = createAudio(getActivity(), config);
		files = createFiles();
		this.listener = listener;
		this.handler = new Handler();
		this.clipboard = new AndroidClipboard(getActivity());
		
		// Add a specialized audio lifecycle listener
		register(new LifecycleListener() {
			
			@Override
			public void resume() {
				audio.resume();
			}
			
			@Override
			public void pause() {
				audio.pause();
			}
			
			@Override
			public void dispose() {
				audio.dispose();
			}
		});
		
		Micro.app = this;
		Micro.input = this.getInput();
		Micro.audio = this.getAudio();
		Micro.files = this.getFiles();
		Micro.graphics = this.getGraphics();
		createWakeLock(config.useWakelock);
		useImmersiveMode(config.useImmersiveMode);
		if (config.useImmersiveMode) {
			AndroidVisibilityListener vlistener = new AndroidVisibilityListener();
			vlistener.createListener(this);
		}
		
		// detect an already connected bluetooth keyboardAvailable
		if (getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS)
			input.setKeyboardAvailable(true);
		return graphics.getView();
	}
	
	@Override
	public void onPause() {
		boolean isContinuous = graphics.isContinuousRendering();
		boolean isContinuousEnforced = AndroidGraphics.enforceContinuousRendering;
		
		AndroidGraphics.enforceContinuousRendering = true;
		graphics.setContinuousRendering(true);
		graphics.pause();
		
		input.onPause();
		
		// davebaol & mobidevelop:
		// This fragment (or one of the parent) is currently being removed from its activity or the activity is in the process of
		// finishing
		if (isRemoving() || isAnyParentFragmentRemoving() || getActivity().isFinishing()) {
			graphics.clearManagedCaches();
			graphics.destroy();
		}
		
		AndroidGraphics.enforceContinuousRendering = isContinuousEnforced;
		graphics.setContinuousRendering(isContinuous);
		
		graphics.onPauseGLSurfaceView();
		
		super.onPause();
	}
	
	@Override
	public void onResume() {
		Micro.app = this;
		Micro.input = this.getInput();
		Micro.audio = this.getAudio();
		Micro.files = this.getFiles();
		Micro.graphics = this.getGraphics();
		
		input.onResume();
		
		if (graphics != null)
			graphics.onResumeGLSurfaceView();
		
		if (!firstResume)
			graphics.resume();
		else
			firstResume = false;
		
		super.onResume();
	}
	
	@Override
	public ApplicationListener getApplicationListener() {
		return listener;
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
		return new AndroidPreferences(getActivity().getSharedPreferences(name, Context.MODE_PRIVATE));
	}
	
	@Override
	public Clipboard getClipboard() {
		return clipboard;
	}
	
	@Override
	public void post(Runnable runnable) {
		synchronized (runnables) {
			runnables.add(runnable);
			Micro.graphics.requestRendering();
		}
	}
	
	@Override
	public void onConfigurationChanged(Configuration config) {
		super.onConfigurationChanged(config);
		boolean keyboardAvailable = config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;
		input.setKeyboardAvailable(keyboardAvailable);
	}
	
	@Override
	public void exit() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				callbacks.exit();
			}
		});
	}
	
	@Override
	public void debug(String tag, String message) {
		if (logLevel >= LOG_DEBUG) {
			Log.d(tag, message);
		}
	}
	
	@Override
	public void debug(String tag, String message, Throwable exception) {
		if (logLevel >= LOG_DEBUG) {
			Log.d(tag, message, exception);
		}
	}
	
	@Override
	public void log(String tag, String message) {
		if (logLevel >= LOG_INFO)
			Log.i(tag, message);
	}
	
	@Override
	public void log(String tag, String message, Throwable exception) {
		if (logLevel >= LOG_INFO)
			Log.i(tag, message, exception);
	}
	
	@Override
	public void error(String tag, String message) {
		if (logLevel >= LOG_ERROR)
			Log.e(tag, message);
	}
	
	@Override
	public void error(String tag, String message, Throwable exception) {
		if (logLevel >= LOG_ERROR)
			Log.e(tag, message, exception);
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
		return getActivity();
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
	public void runOnUiThread(Runnable runnable) {
		getActivity().runOnUiThread(runnable);
	}
	
	@Override
	public SnapshotArray<LifecycleListener> getLifecycleListeners() {
		return lifecycleListeners;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		// forward events to our listeners if there are any installed
		synchronized (androidEventListeners) {
			for (int i = 0; i < androidEventListeners.size; i++) {
				androidEventListeners.get(i).onActivityResult(requestCode, resultCode, data);
			}
		}
	}
	
	public void addAndroidEventListener(AndroidEventListener listener) {
		synchronized (androidEventListeners) {
			androidEventListeners.add(listener);
		}
	}
	
	public void removeAndroidEventListener(AndroidEventListener listener) {
		synchronized (androidEventListeners) {
			androidEventListeners.removeValue(listener, true);
		}
	}
	
	@Override
	public Window getApplicationWindow() {
		return this.getActivity().getWindow();
	}
	
	@Override
	public Handler getHandler() {
		return this.handler;
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
		return new DefaultAndroidInput(this, getActivity(), graphics.view, config);
	}
	
	protected AndroidFiles createFiles() {
		return new DefaultAndroidFiles(getResources().getAssets(), getActivity(), true);
	}
	
	@Override
	public WindowManager getWindowManager() {
		return (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
	}
	
	private boolean isAnyParentFragmentRemoving() {
		Fragment fragment = getParentFragment();
		
		while (fragment != null) {
			if (fragment.isRemoving())
				return true;
			fragment = fragment.getParentFragment();
		}
		
		return false;
	}
	
}
