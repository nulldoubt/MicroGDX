package me.nulldoubt.micro.backends.android;

import android.app.WallpaperColors;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import me.nulldoubt.micro.Application;
import me.nulldoubt.micro.ApplicationListener;
import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.graphics.Color;

public abstract class AndroidLiveWallpaperService extends WallpaperService {
	
	static final String TAG = "WallpaperService";
	static boolean DEBUG = false; // TODO remember to disable this
	
	protected volatile AndroidLiveWallpaper app = null; // can be accessed from GL render thread
	protected SurfaceHolder.Callback view = null;
	
	// current format of surface (one GLSurfaceView is shared between all engines)
	protected int viewFormat;
	protected int viewWidth;
	protected int viewHeight;
	
	// app is initialized when engines == 1 first time, app is destroyed in WallpaperService.onDestroy, but
	// ApplicationListener.dispose is not called for wallpapers
	protected int engines = 0;
	protected int visibleEngines = 0;
	
	// engine currently associated with app instance, linked engine serves surface handler for GLSurfaceView
	protected volatile AndroidWallpaperEngine linkedEngine = null; // can be accessed from GL render thread by getSurfaceHolder
	
	protected void setLinkedEngine(AndroidWallpaperEngine linkedEngine) {
		synchronized (sync) {
			this.linkedEngine = linkedEngine;
		}
	}
	
	// if preview state notified ever
	protected volatile boolean isPreviewNotified = false;
	
	// the value of last preview state notified to app listener
	protected volatile boolean notifiedPreviewState = false;
	
	volatile int[] sync = new int[0];
	
	// volatile ReentrantLock lock = new ReentrantLock();
	
	// lifecycle methods - the order of calling (flow) is maintained ///////////////
	
	public AndroidLiveWallpaperService() {
		super();
	}
	
	/**
	 * Service is starting, libGDX application is shutdown now
	 */
	@Override
	public void onCreate() {
		if (DEBUG)
			Log.d(TAG, " > AndroidLiveWallpaperService - onCreate() " + hashCode());
		Log.i(TAG, "service created");
		
		super.onCreate();
	}
	
	/**
	 * One of wallpaper engines is starting. Do not override this method, service manages them internally.
	 */
	@Override
	public Engine onCreateEngine() {
		if (DEBUG)
			Log.d(TAG, " > AndroidLiveWallpaperService - onCreateEngine()");
		Log.i(TAG, "engine created");
		
		return new AndroidWallpaperEngine();
	}
	
	/**
	 * libGDX application is starting, it occurs after first wallpaper engine had started. Override this method an invoke
	 * {@link AndroidLiveWallpaperService#initialize(ApplicationListener, AndroidApplicationConfiguration)} from there.
	 */
	public void onCreateApplication() {
		if (DEBUG)
			Log.d(TAG, " > AndroidLiveWallpaperService - onCreateApplication()");
	}
	
	public void initialize(ApplicationListener listener) {
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		initialize(listener, config);
	}
	
	public void initialize(ApplicationListener listener, AndroidApplicationConfiguration config) {
		if (DEBUG)
			Log.d(TAG, " > AndroidLiveWallpaperService - initialize()");
		
		app.initialize(listener, config);
		
		if (config.getTouchEventsForLiveWallpaper && Integer.parseInt(android.os.Build.VERSION.SDK) >= 7)
			linkedEngine.setTouchEventsEnabled(true);
		
	}
	
	public SurfaceHolder getSurfaceHolder() {
		if (DEBUG)
			Log.d(TAG, " > AndroidLiveWallpaperService - getSurfaceHolder()");
		
		synchronized (sync) {
			if (linkedEngine == null)
				return null;
			else
				return linkedEngine.getSurfaceHolder();
		}
	}
	
	// engines live there
	
	/**
	 * Called when the last engine is ending its live, it can occur when: 1. service is dying 2. service is switching from one
	 * engine to another 3. [only my assumption] when wallpaper is not visible and system is going to restore some memory for
	 * foreground processing by disposing not used wallpaper engine We can't destroy app there, because: 1. in won't work - gl
	 * context is disposed right now and after app.onDestroy() app would stuck somewhere in gl thread synchronizing code 2. we
	 * don't know if service create more engines, app is shared between them and should stay initialized waiting for new engines
	 */
	public void onDeepPauseApplication() {
		if (DEBUG)
			Log.d(TAG, " > AndroidLiveWallpaperService - onDeepPauseApplication()");
		
		// free native resources consuming runtime memory, note that it can cause some lag when resuming wallpaper
		if (app != null) {
			app.graphics.clearManagedCaches();
		}
	}
	
	/**
	 * Service is dying, and will not be used again. You have to finish execution off all living threads there or short after
	 * there, besides the new wallpaper service wouldn't be able to start.
	 */
	@Override
	public void onDestroy() {
		if (DEBUG)
			Log.d(TAG, " > AndroidLiveWallpaperService - onDestroy() " + hashCode());
		Log.i(TAG, "service destroyed");
		
		super.onDestroy(); // can call engine.onSurfaceDestroyed, must be before bellow code:
		
		if (app != null) {
			app.onDestroy();
			
			app = null;
			view = null;
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		Log.i(TAG, "service finalized");
		super.finalize();
	}
	
	// end of lifecycle methods ////////////////////////////////////////////////////////
	
	public AndroidLiveWallpaper getLiveWallpaper() {
		return app;
	}
	
	public WindowManager getWindowManager() {
		return (WindowManager) getSystemService(Context.WINDOW_SERVICE);
	}
	
	/**
	 * Bridge between surface on which wallpaper is rendered and the wallpaper service. The problem is that there can be a group
	 * of Engines at one time and we must share libGDX application between them.
	 *
	 * @author libGDX team and Jaroslaw Wisniewski <j.wisniewski@appsisle.com>
	 */
	public class AndroidWallpaperEngine extends Engine {
		
		protected boolean engineIsVisible = false;
		
		// destination format of surface when this engine is active (updated in onSurfaceChanged)
		protected int engineFormat;
		protected int engineWidth;
		protected int engineHeight;
		
		// lifecycle methods - the order of calling (flow) is maintained /////////////////
		
		public AndroidWallpaperEngine() {
			if (DEBUG)
				Log.d(TAG, " > AndroidWallpaperEngine() " + hashCode());
		}
		
		@Override
		public void onCreate(final SurfaceHolder surfaceHolder) {
			if (DEBUG)
				Log.d(TAG, " > AndroidWallpaperEngine - onCreate() " + hashCode() + " running: " + engines + ", linked: "
						+ (linkedEngine == this) + ", thread: " + Thread.currentThread().toString());
			super.onCreate(surfaceHolder);
		}
		
		/**
		 * Called before surface holder callbacks (ex for GLSurfaceView)! This is called immediately after the surface is first
		 * created. Implementations of this should start up whatever rendering code they desire. Note that only one thread can ever
		 * draw into a Surface, so you should not draw into the Surface here if your normal rendering will be in another thread.
		 */
		@Override
		public void onSurfaceCreated(final SurfaceHolder holder) {
			engines++;
			setLinkedEngine(this);
			
			if (DEBUG)
				Log.d(TAG, " > AndroidWallpaperEngine - onSurfaceCreated() " + hashCode() + ", running: " + engines
						+ ", linked: " + (linkedEngine == this));
			Log.i(TAG, "engine surface created");
			
			super.onSurfaceCreated(holder);
			
			if (engines == 1) {
				// safeguard: recover attributes that could suffered by unexpected surfaceDestroy event
				visibleEngines = 0;
			}
			
			if (engines == 1 && app == null) {
				viewFormat = 0; // must be initialized with zeroes
				viewWidth = 0;
				viewHeight = 0;
				
				app = new AndroidLiveWallpaper(AndroidLiveWallpaperService.this);
				
				onCreateApplication();
				if (app.graphics == null)
					throw new Error(
							"You must override 'AndroidLiveWallpaperService.onCreateApplication' method and call 'initialize' from its body.");
			}
			
			view = (SurfaceHolder.Callback) app.graphics.view;
			this.getSurfaceHolder().removeCallback(view); // we are going to call this events manually
			
			// inherit format from shared surface view
			engineFormat = viewFormat;
			engineWidth = viewWidth;
			engineHeight = viewHeight;
			
			if (engines == 1) {
				view.surfaceCreated(holder);
			} else {
				// this combination of methods is described in AndroidWallpaperEngine.onResume
				view.surfaceDestroyed(holder);
				notifySurfaceChanged(engineFormat, engineWidth, engineHeight, false);
				view.surfaceCreated(holder);
			}
			
			notifyPreviewState();
			notifyOffsetsChanged();
			if (!Micro.graphics.isContinuousRendering()) {
				Micro.graphics.requestRendering();
			}
		}
		
		/**
		 * This is called immediately after any structural changes (format or size) have been made to the surface. You should at
		 * this point update the imagery in the surface. This method is always called at least once, after
		 * surfaceCreated(SurfaceHolder).
		 */
		@Override
		public void onSurfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
			if (DEBUG)
				Log.d(TAG,
						" > AndroidWallpaperEngine - onSurfaceChanged() isPreview: " + isPreview() + ", " + hashCode() + ", running: "
								+ engines + ", linked: " + (linkedEngine == this) + ", sufcace valid: "
								+ getSurfaceHolder().getSurface().isValid());
			Log.i(TAG, "engine surface changed");
			
			super.onSurfaceChanged(holder, format, width, height);
			
			notifySurfaceChanged(format, width, height, true);
			
			// it shouldn't be required there (as I understand android.service.wallpaper.WallpaperService impl)
			// notifyPreviewState();
		}
		
		/**
		 * Notifies shared GLSurfaceView about changed surface format.
		 *
		 * @param format
		 * @param width
		 * @param height
		 * @param forceUpdate if false, surface view will be notified only if currently contains expired information
		 */
		private void notifySurfaceChanged(final int format, final int width, final int height, boolean forceUpdate) {
			if (!forceUpdate && format == viewFormat && width == viewWidth && height == viewHeight) {
				// skip if didn't changed
				if (DEBUG)
					Log.d(TAG, " > surface is current, skipping surfaceChanged event");
			} else {
				// update engine desired surface format
				engineFormat = format;
				engineWidth = width;
				engineHeight = height;
				
				// update surface view if engine is linked with it already
				if (linkedEngine == this) {
					viewFormat = engineFormat;
					viewWidth = engineWidth;
					viewHeight = engineHeight;
					view.surfaceChanged(this.getSurfaceHolder(), viewFormat, viewWidth, viewHeight);
				} else {
					if (DEBUG)
						Log.d(TAG, " > engine is not active, skipping surfaceChanged event");
				}
			}
		}
		
		/**
		 * Called to inform you of the wallpaper becoming visible or hidden. It is very important that a wallpaper only use CPU
		 * while it is visible..
		 */
		@Override
		public void onVisibilityChanged(final boolean visible) {
			boolean reportedVisible = isVisible();
			
			if (DEBUG)
				Log.d(TAG, " > AndroidWallpaperEngine - onVisibilityChanged(paramVisible: " + visible + " reportedVisible: "
						+ reportedVisible + ") " + hashCode() + ", sufcace valid: " + getSurfaceHolder().getSurface().isValid());
			super.onVisibilityChanged(visible);
			
			// Android WallpaperService sends fake visibility changed events to force some buggy live wallpapers to shut down after
			// onSurfaceChanged when they aren't visible, it can cause problems in current implementation and it is not necessary
			if (reportedVisible == false && visible == true) {
				if (DEBUG)
					Log.d(TAG, " > fake visibilityChanged event! Android WallpaperService likes do that!");
				return;
			}
			
			notifyVisibilityChanged(visible);
		}
		
		private void notifyVisibilityChanged(final boolean visible) {
			if (this.engineIsVisible != visible) {
				this.engineIsVisible = visible;
				
				if (this.engineIsVisible)
					onResume();
				else
					onPause();
			} else {
				if (DEBUG)
					Log.d(TAG, " > visible state is current, skipping visibilityChanged event!");
			}
		}
		
		public void onResume() {
			visibleEngines++;
			if (DEBUG)
				Log.d(TAG, " > AndroidWallpaperEngine - onResume() " + hashCode() + ", running: " + engines + ", linked: "
						+ (linkedEngine == this) + ", visible: " + visibleEngines);
			Log.i(TAG, "engine resumed");
			
			if (linkedEngine != null) {
				if (linkedEngine != this) {
					setLinkedEngine(this);
					
					// disconnect surface view from previous window
					view.surfaceDestroyed(this.getSurfaceHolder()); // force gl surface reload, new instance will be created on current
					// surface holder
					
					// resize surface to match window associated with current engine
					notifySurfaceChanged(engineFormat, engineWidth, engineHeight, false);
					
					// connect surface view to current engine
					view.surfaceCreated(this.getSurfaceHolder());
				} else {
					// update if surface changed when engine wasn't active
					notifySurfaceChanged(engineFormat, engineWidth, engineHeight, false);
				}
				
				if (visibleEngines == 1)
					app.onResume();
				
				notifyPreviewState();
				notifyOffsetsChanged();
				if (!Micro.graphics.isContinuousRendering()) {
					Micro.graphics.requestRendering();
				}
			}
		}
		
		public void onPause() {
			visibleEngines--;
			if (DEBUG)
				Log.d(TAG, " > AndroidWallpaperEngine - onPause() " + hashCode() + ", running: " + engines + ", linked: "
						+ (linkedEngine == this) + ", visible: " + visibleEngines);
			Log.i(TAG, "engine paused");
			
			// this shouldn't never happen, but if it will.. live wallpaper will not be stopped when device will pause and lwp will
			// drain battery.. shortly!
			if (visibleEngines >= engines) {
				Log.e(AndroidLiveWallpaperService.TAG, "wallpaper lifecycle error, counted too many visible engines! repairing..");
				visibleEngines = Math.max(engines - 1, 0);
			}
			
			if (linkedEngine != null) {
				if (visibleEngines == 0)
					app.onPause();
			}
			
			if (DEBUG)
				Log.d(TAG, " > AndroidWallpaperEngine - onPause() done!");
		}
		
		@Override
		public void onSurfaceDestroyed(final SurfaceHolder holder) {
			engines--;
			if (DEBUG)
				Log.d(TAG, " > AndroidWallpaperEngine - onSurfaceDestroyed() " + hashCode() + ", running: " + engines + " ,linked: " + (linkedEngine == this) + ", isVisible: " + engineIsVisible);
			Log.i(TAG, "engine surface destroyed");
			
			if (engines == 0)
				onDeepPauseApplication();
			
			if (linkedEngine == this && view != null)
				view.surfaceDestroyed(holder);
			
			engineFormat = 0;
			engineWidth = 0;
			engineHeight = 0;
			
			if (engines == 0)
				linkedEngine = null;
			
			super.onSurfaceDestroyed(holder);
		}
		
		@Override
		public void onDestroy() {
			super.onDestroy();
		}
		
		boolean iconDropConsumed = true;
		int xIconDrop, yIconDrop;
		
		@Override
		public Bundle onCommand(final String pAction, final int pX, final int pY, final int pZ, final Bundle pExtras, final boolean pResultRequested) {
			if (DEBUG)
				Log.d(TAG, " > AndroidWallpaperEngine - onCommand(" + pAction + " " + pX + " " + pY + " " + pZ + " " + pExtras + " " + pResultRequested + ")" + ", linked: " + (linkedEngine == this));
			
			if (pAction.equals("android.home.drop")) {
				iconDropConsumed = false;
				xIconDrop = pX;
				yIconDrop = pY;
				notifyIconDropped();
			}
			
			return super.onCommand(pAction, pX, pY, pZ, pExtras, pResultRequested);
		}
		
		protected void notifyIconDropped() {
			if (linkedEngine == this && app.listener instanceof AndroidWallpaperListener) {
				if (!iconDropConsumed) {
					iconDropConsumed = true;
					
					app.post(() -> {
						boolean isCurrent = false;
						synchronized (sync) {
							isCurrent = (linkedEngine == AndroidWallpaperEngine.this);
						}
						if (isCurrent)
							((AndroidWallpaperListener) app.listener).iconDropped(xIconDrop, yIconDrop);
					});
				}
			}
		}
		
		@Override
		public void onTouchEvent(MotionEvent event) {
			if (linkedEngine == this)
				app.input.onTouch(null, event);
		}
		
		boolean offsetsConsumed = true;
		float xOffset = 0.0f;
		float yOffset = 0.0f;
		float xOffsetStep = 0.0f;
		float yOffsetStep = 0.0f;
		int xPixelOffset = 0;
		int yPixelOffset = 0;
		
		@Override
		public void onOffsetsChanged(final float xOffset, final float yOffset, final float xOffsetStep, final float yOffsetStep, final int xPixelOffset, final int yPixelOffset) {
			
			this.offsetsConsumed = false;
			this.xOffset = xOffset;
			this.yOffset = yOffset;
			this.xOffsetStep = xOffsetStep;
			this.yOffsetStep = yOffsetStep;
			this.xPixelOffset = xPixelOffset;
			this.yPixelOffset = yPixelOffset;
			
			notifyOffsetsChanged();
			if (!Micro.graphics.isContinuousRendering())
				Micro.graphics.requestRendering();
			
			super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset);
		}
		
		protected void notifyOffsetsChanged() {
			if (linkedEngine == this && app.listener instanceof AndroidWallpaperListener) {
				if (!offsetsConsumed) {
					offsetsConsumed = true;
					
					app.post(() -> {
						boolean isCurrent = false;
						synchronized (sync) {
							isCurrent = (linkedEngine == AndroidWallpaperEngine.this);
						}
						if (isCurrent)
							((AndroidWallpaperListener) app.listener).offsetChange(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset);
					});
				}
			}
		}
		
		protected void notifyPreviewState() {
			if (linkedEngine == this && app.listener instanceof AndroidWallpaperListener) {
				final boolean currentPreviewState = linkedEngine.isPreview();
				app.post(() -> {
					boolean shouldNotify = false;
					synchronized (sync) {
						if (!isPreviewNotified || notifiedPreviewState != currentPreviewState) {
							notifiedPreviewState = currentPreviewState;
							isPreviewNotified = true;
							shouldNotify = true;
						}
					}
					
					if (shouldNotify) {
						AndroidLiveWallpaper currentApp = app;
						if (currentApp != null)
							((AndroidWallpaperListener) currentApp.listener).previewStateChange(currentPreviewState);
					}
				});
			}
		}
		
		@Override
		public WallpaperColors onComputeColors() {
			Application app = Micro.app;
			if (Build.VERSION.SDK_INT >= 27 && app instanceof AndroidLiveWallpaper liveWallpaper) {
				Color[] colors = liveWallpaper.wallpaperColors;
				if (colors != null)
					return new WallpaperColors(android.graphics.Color.valueOf(colors[0].r, colors[0].g, colors[0].b, colors[0].a),
							android.graphics.Color.valueOf(colors[1].r, colors[1].g, colors[1].b, colors[1].a),
							android.graphics.Color.valueOf(colors[2].r, colors[2].g, colors[2].b, colors[2].a));
			}
			return super.onComputeColors();
		}
		
	}
	
}
