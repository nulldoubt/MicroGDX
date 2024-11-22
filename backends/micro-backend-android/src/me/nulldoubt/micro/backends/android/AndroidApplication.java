package me.nulldoubt.micro.backends.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import me.nulldoubt.micro.*;
import me.nulldoubt.micro.backends.android.keyboardheight.AndroidXKeyboardHeightProvider;
import me.nulldoubt.micro.backends.android.keyboardheight.KeyboardHeightProvider;
import me.nulldoubt.micro.backends.android.keyboardheight.StandardKeyboardHeightProvider;
import me.nulldoubt.micro.backends.android.surfaceview.FillResolutionStrategy;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.utils.collections.Array;
import me.nulldoubt.micro.utils.collections.SnapshotArray;

public class AndroidApplication extends Activity implements AndroidApplicationBase {
	
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
	protected boolean useImmersiveMode = false;
	private int wasFocusChanged = -1;
	private boolean isWaitingForAudio = false;
	private KeyboardHeightProvider keyboardHeightProvider;
	
	protected boolean renderUnderCutout = false;
	
	public void initialize(ApplicationListener listener) {
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		initialize(listener, config);
	}
	
	public void initialize(ApplicationListener listener, AndroidApplicationConfiguration config) {
		init(listener, config, false);
	}
	
	public View initializeForView(ApplicationListener listener) {
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		return initializeForView(listener, config);
	}
	
	public View initializeForView(ApplicationListener listener, AndroidApplicationConfiguration config) {
		init(listener, config, true);
		return graphics.getView();
	}
	
	private void init(ApplicationListener listener, AndroidApplicationConfiguration config, boolean isForView) {
		if (this.getVersion() < MINIMUM_SDK) {
			throw new MicroRuntimeException("libGDX requires Android API Level " + MINIMUM_SDK + " or later.");
		}
		config.nativeLoader.load();
		setApplicationLogger(new AndroidApplicationLogger());
		graphics = new AndroidGraphics(this, config,
				config.resolutionStrategy == null ? new FillResolutionStrategy() : config.resolutionStrategy);
		input = createInput(this, this, graphics.view, config);
		audio = createAudio(this, config);
		files = createFiles();
		this.listener = listener;
		this.handler = new Handler();
		this.useImmersiveMode = config.useImmersiveMode;
		this.clipboard = new AndroidClipboard(this);
		this.renderUnderCutout = config.renderUnderCutout;
		
		// Add a specialized audio lifecycle listener
		register(new LifecycleListener() {
			
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
		
		if (!isForView) {
			try {
				requestWindowFeature(Window.FEATURE_NO_TITLE);
			} catch (Exception ex) {
				log("AndroidApplication", "Content already displayed, cannot request FEATURE_NO_TITLE", ex);
			}
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			setContentView(graphics.getView(), createLayoutParams());
		}
		
		createWakeLock(config.useWakelock);
		useImmersiveMode(this.useImmersiveMode);
		if (this.useImmersiveMode) {
			AndroidVisibilityListener vlistener = new AndroidVisibilityListener();
			vlistener.createListener(this);
		}
		
		if (getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS)
			input.setKeyboardAvailable(true);
		
		setLayoutInDisplayCutoutMode(this.renderUnderCutout);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
			keyboardHeightProvider = new AndroidXKeyboardHeightProvider(this);
		else
			keyboardHeightProvider = new StandardKeyboardHeightProvider(this);
	}
	
	protected FrameLayout.LayoutParams createLayoutParams() {
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
				android.view.ViewGroup.LayoutParams.MATCH_PARENT);
		layoutParams.gravity = Gravity.CENTER;
		return layoutParams;
	}
	
	protected void createWakeLock(boolean use) {
		if (use) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}
	
	@TargetApi(Build.VERSION_CODES.P)
	private void setLayoutInDisplayCutoutMode(boolean render) {
		if (render && getVersion() >= Build.VERSION_CODES.P) {
			WindowManager.LayoutParams lp = getWindow().getAttributes();
			lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
		}
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		useImmersiveMode(this.useImmersiveMode);
		if (hasFocus) {
			this.wasFocusChanged = 1;
			if (this.isWaitingForAudio) {
				this.audio.resume();
				this.isWaitingForAudio = false;
			}
		} else {
			this.wasFocusChanged = 0;
		}
	}
	
	@Override
	public void useImmersiveMode(boolean use) {
		if (!use)
			return;
		
		View view = getWindow().getDecorView();
		int code = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		
		view.setSystemUiVisibility(code);
	}
	
	@Override
	protected void onPause() {
		boolean isContinuous = graphics.isContinuousRendering();
		boolean isContinuousEnforced = AndroidGraphics.enforceContinuousRendering;
		
		AndroidGraphics.enforceContinuousRendering = true;
		graphics.setContinuousRendering(true);
		graphics.pause();
		
		input.onPause();
		
		if (isFinishing()) {
			graphics.clearManagedCaches();
			graphics.destroy();
		}
		
		AndroidGraphics.enforceContinuousRendering = isContinuousEnforced;
		graphics.setContinuousRendering(isContinuous);
		
		graphics.onPauseGLSurfaceView();
		
		super.onPause();
		keyboardHeightProvider.setKeyboardHeightObserver(null);
	}
	
	@Override
	protected void onResume() {
		Micro.app = this;
		Micro.input = this.getInput();
		Micro.audio = this.getAudio();
		Micro.files = this.getFiles();
		Micro.graphics = this.getGraphics();
		
		input.onResume();
		
		if (graphics != null) {
			graphics.onResumeGLSurfaceView();
		}
		
		if (!firstResume) {
			graphics.resume();
		} else
			firstResume = false;
		
		this.isWaitingForAudio = true;
		if (this.wasFocusChanged == 1 || this.wasFocusChanged == -1) {
			this.audio.resume();
			this.isWaitingForAudio = false;
		}
		super.onResume();
		keyboardHeightProvider.setKeyboardHeightObserver((DefaultAndroidInput) Micro.input);
		((AndroidGraphics) getGraphics()).getView().post(() -> keyboardHeightProvider.start());
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		keyboardHeightProvider.close();
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
	public AndroidInput getInput() {
		return input;
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
		return new AndroidPreferences(getSharedPreferences(name, Context.MODE_PRIVATE));
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
		handler.post(AndroidApplication.this::finish);
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
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
	public Context getContext() {
		return this;
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
	public Window getApplicationWindow() {
		return this.getWindow();
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
		return new DefaultAndroidInput(this, this, graphics.view, config);
	}
	
	protected AndroidFiles createFiles() {
		this.getFilesDir(); // workaround for Android bug #10515463
		return new DefaultAndroidFiles(this.getAssets(), this, true);
	}
	
	public KeyboardHeightProvider getKeyboardHeightProvider() {
		return keyboardHeightProvider;
	}
	
}
