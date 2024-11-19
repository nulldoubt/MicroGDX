package me.nulldoubt.micro.backends.android;

import android.opengl.GLSurfaceView.EGLConfigChooser;
import android.util.Log;
import android.view.SurfaceHolder;
import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.backends.android.surfaceview.GLSurfaceView20;
import me.nulldoubt.micro.backends.android.surfaceview.ResolutionStrategy;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;

public final class AndroidGraphicsLiveWallpaper extends AndroidGraphics {
	
	public AndroidGraphicsLiveWallpaper(AndroidLiveWallpaper lwp, AndroidApplicationConfiguration config, ResolutionStrategy resolutionStrategy) {
		super(lwp, config, resolutionStrategy, false);
	}
	
	// jw: I replaced GL..SurfaceViewLW classes with their original counterparts, if it will work
	// on known devices, on opengl 1.0 and 2.0, and all possible SDK versions.. You can remove
	// GL..SurfaceViewLW family of classes completely (there is no use for them).
	
	// -> specific for live wallpapers
	// jw: synchronized access to current wallpaper surface holder
	SurfaceHolder getSurfaceHolder() {
		synchronized (((AndroidLiveWallpaper) app).service.sync) {
			return ((AndroidLiveWallpaper) app).service.getSurfaceHolder();
		}
	}
	
	// <- specific for live wallpapers
	
	// Grabbed from AndroidGraphics superclass and modified to override
	// getHolder in created GLSurfaceView20 instances
	@Override
	protected GLSurfaceView20 createGLSurfaceView(AndroidApplicationBase application,
												  final ResolutionStrategy resolutionStrategy) {
		if (!checkGL20())
			throw new MicroRuntimeException("libGDX requires OpenGL ES 2.0");
		
		EGLConfigChooser configChooser = getEglConfigChooser();
		GLSurfaceView20 view = new GLSurfaceView20(application.getContext(), resolutionStrategy) {
			@Override
			public SurfaceHolder getHolder() {
				return getSurfaceHolder();
			}
		};
		
		if (configChooser != null)
			view.setEGLConfigChooser(configChooser);
		else
			view.setEGLConfigChooser(config.r, config.g, config.b, config.a, config.depth, config.stencil);
		view.setRenderer(this);
		return view;
	}
	
	// kill the GLThread managed by GLSurfaceView (only for GLSurfaceView because GLSurffaceViewCupcake stops thread in
	// onPause events - which is not as easy and safe for GLSurfaceView)
	public void onDestroyGLSurfaceView() {
		if (view != null) {
			try {
				// onDetachedFromWindow stops GLThread by calling mGLThread.requestExitAndWait()
				view.onDetachedFromWindow();
				if (AndroidLiveWallpaperService.DEBUG)
					Log.d(AndroidLiveWallpaperService.TAG,
							" > AndroidLiveWallpaper - onDestroy() stopped GLThread managed by GLSurfaceView");
			} catch (Throwable t) {
				// error while scheduling exit of GLThread, GLThread will remain live and wallpaper service
				// wouldn't be able to shutdown completely
				Log.e(AndroidLiveWallpaperService.TAG,
						"failed to destroy GLSurfaceView's thread! GLSurfaceView.onDetachedFromWindow impl changed since API lvl 16!");
				t.printStackTrace();
			}
		}
	}
	
	@Override
	void resume() {
		synchronized (synch) {
			running = true;
			resume = true;
			
			while (resume) {
				try {
					requestRendering();
					synch.wait();
				} catch (InterruptedException ignored) {
					Micro.app.log("AndroidGraphics", "waiting for resume synchronization failed!");
				}
			}
		}
	}
	
	@Override
	public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl) {
		long time = System.nanoTime();
		// After pause deltaTime can have somewhat huge value that destabilizes the mean, so let's cut it off
		if (!resume) {
			deltaTime = (time - lastFrameTime) / 1000000000.0f;
		} else {
			deltaTime = 0;
		}
		lastFrameTime = time;
		
		boolean lrunning = false;
		boolean lpause = false;
		boolean ldestroy = false;
		boolean lresume = false;
		
		synchronized (synch) {
			lrunning = running;
			lpause = pause;
			ldestroy = destroy;
			lresume = resume;
			
			if (resume) {
				resume = false;
				synch.notifyAll();
			}
			
			if (pause) {
				pause = false;
				synch.notifyAll();
			}
			
			if (destroy) {
				destroy = false;
				synch.notifyAll();
			}
		}
		
		if (lresume) {
			// ((AndroidAudio)app.getAudio()).resume(); // jw: moved to AndroidLiveWallpaper.onResume
			app.getApplicationListener().resume();
			Micro.app.log("AndroidGraphics", "resumed");
		}
		
		// HACK: added null check to handle set wallpaper from preview null
		// error in renderer
		// jw: this hack is not working always, renderer ends with error for some devices - because of uninitialized gl context
		// jw: now it shouldn't be necessary - after wallpaper backend refactoring:)
		if (lrunning) {
			
			// jw: changed
			synchronized (app.getRunnables()) {
				app.getExecutedRunnables().clear();
				app.getExecutedRunnables().addAll(app.getRunnables());
				app.getRunnables().clear();
				
				for (int i = 0; i < app.getExecutedRunnables().size; i++) {
					try {
						app.getExecutedRunnables().get(i).run();
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
			}
			/*
			 * synchronized (app.runnables) { for (int i = 0; i < app.runnables.size; i++) { app.runnables.get(i).run(); }
			 * app.runnables.clear(); }
			 */
			
			app.getInput().processEvents();
			frameId++;
			app.getApplicationListener().render();
		}
		
		// jw: never called on lvp, why? see description in AndroidLiveWallpaper.onPause
		if (lpause) {
			app.getApplicationListener().pause();
			// ((AndroidAudio)app.getAudio()).pause(); jw: moved to AndroidLiveWallpaper.onPause
			Micro.app.log("AndroidGraphics", "paused");
		}
		
		// jw: never called on lwp, why? see description in AndroidLiveWallpaper.onPause
		if (ldestroy) {
			app.getApplicationListener().dispose();
			// ((AndroidAudio)app.getAudio()).dispose(); jw: moved to AndroidLiveWallpaper.onDestroy
			Micro.app.log("AndroidGraphics", "destroyed");
		}
		
		if (time - frameStart > 1000000000) {
			fps = frames;
			frames = 0;
			frameStart = time;
		}
		frames++;
	}
	
	@Override
	protected void logManagedCachesStatus() {
		// to prevent creating too many string buffers in live wallpapers
		if (AndroidLiveWallpaperService.DEBUG) {
			super.logManagedCachesStatus();
		}
	}
	
}
