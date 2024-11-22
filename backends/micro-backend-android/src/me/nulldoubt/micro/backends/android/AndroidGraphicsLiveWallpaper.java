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
	
	SurfaceHolder getSurfaceHolder() {
		synchronized (((AndroidLiveWallpaper) app).service.sync) {
			return ((AndroidLiveWallpaper) app).service.getSurfaceHolder();
		}
	}
	
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
				view.onDetachedFromWindow();
				if (AndroidLiveWallpaperService.DEBUG)
					Log.d(AndroidLiveWallpaperService.TAG, " > AndroidLiveWallpaper - onDestroy() stopped GLThread managed by GLSurfaceView");
			} catch (Throwable t) {
				Log.e(AndroidLiveWallpaperService.TAG, "failed to destroy GLSurfaceView's thread! GLSurfaceView.onDetachedFromWindow impl changed since API lvl 16!");
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
				} catch (InterruptedException _) {
					Micro.app.log("AndroidGraphics", "waiting for resume synchronization failed!");
				}
			}
		}
	}
	
	@Override
	public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl) {
		long time = System.nanoTime();
		
		if (!resume)
			deltaTime = (time - lastFrameTime) / 1000000000.0f;
		else
			deltaTime = 0;
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
			app.getApplicationListener().resume();
			Micro.app.log("AndroidGraphics", "resumed");
		}
		
		if (lrunning) {
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
			
			app.getInput().processEvents();
			frameId++;
			app.getApplicationListener().render();
		}
		
		if (lpause) {
			app.getApplicationListener().pause();
			Micro.app.log("AndroidGraphics", "paused");
		}
		
		if (ldestroy) {
			app.getApplicationListener().dispose();
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
		if (AndroidLiveWallpaperService.DEBUG)
			super.logManagedCachesStatus();
	}
	
}
