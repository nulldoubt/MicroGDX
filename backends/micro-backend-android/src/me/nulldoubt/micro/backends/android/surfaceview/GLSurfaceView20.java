package me.nulldoubt.micro.backends.android.surfaceview;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import me.nulldoubt.micro.Input.OnscreenKeyboardType;
import me.nulldoubt.micro.backends.android.DefaultAndroidInput;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

public class GLSurfaceView20 extends GLSurfaceView {
	
	static String TAG = "GL2JNIView";
	private static final boolean DEBUG = false;
	
	final ResolutionStrategy resolutionStrategy;
	static int targetGLESVersion;
	public OnscreenKeyboardType onscreenKeyboardType = OnscreenKeyboardType.Default;
	
	public GLSurfaceView20(Context context, ResolutionStrategy resolutionStrategy, int targetGLESVersion) {
		super(context);
		GLSurfaceView20.targetGLESVersion = targetGLESVersion;
		this.resolutionStrategy = resolutionStrategy;
		init(false, 16, 0);
	}
	
	public GLSurfaceView20(Context context, ResolutionStrategy resolutionStrategy) {
		this(context, resolutionStrategy, 2);
	}
	
	public GLSurfaceView20(Context context, boolean translucent, int depth, int stencil, ResolutionStrategy resolutionStrategy) {
		super(context);
		this.resolutionStrategy = resolutionStrategy;
		init(translucent, depth, stencil);
		
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		ResolutionStrategy.MeasuredDimension measures = resolutionStrategy.calcMeasures(widthMeasureSpec, heightMeasureSpec);
		setMeasuredDimension(measures.width(), measures.height());
	}
	
	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		
		// add this line, the IME can show the selectable words when use chinese input method editor.
		if (outAttrs != null) {
			outAttrs.imeOptions = outAttrs.imeOptions | EditorInfo.IME_FLAG_NO_EXTRACT_UI;
			outAttrs.inputType = DefaultAndroidInput.getAndroidInputType(onscreenKeyboardType, true);
		}
		
		return new BaseInputConnection(GLSurfaceView20.this, false) {
			@Override
			public boolean deleteSurroundingText(int beforeLength, int afterLength) {
				if (beforeLength == 1 && afterLength == 0) {
					sendDownUpKeyEventForBackwardCompatibility(KeyEvent.KEYCODE_DEL);
					return true;
				}
				return super.deleteSurroundingText(beforeLength, afterLength);
			}
			
			private void sendDownUpKeyEventForBackwardCompatibility(final int code) {
				final long eventTime = SystemClock.uptimeMillis();
				super.sendKeyEvent(new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, code, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
				super.sendKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), eventTime, KeyEvent.ACTION_UP, code, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
			}
		};
	}
	
	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
	}
	
	private void init(boolean translucent, int depth, int stencil) {
		if (translucent)
			this.getHolder().setFormat(PixelFormat.TRANSLUCENT);
		setEGLContextFactory(new ContextFactory());
		setEGLConfigChooser(translucent ? new ConfigChooser(8, 8, 8, 8, depth, stencil) : new ConfigChooser(8, 8, 8, 0, depth, stencil));
	}
	
	static class ContextFactory implements GLSurfaceView.EGLContextFactory {
		
		private static int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
		
		public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig) {
			Log.w(TAG, "creating OpenGL ES " + GLSurfaceView20.targetGLESVersion + ".0 context");
			checkEglError("Before eglCreateContext " + targetGLESVersion, egl);
			int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, GLSurfaceView20.targetGLESVersion, EGL10.EGL_NONE};
			EGLContext context = egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
			boolean success = checkEglError("After eglCreateContext " + targetGLESVersion, egl);
			
			if ((!success || context == null) && GLSurfaceView20.targetGLESVersion > 2) {
				Log.w(TAG, "Falling back to GLES 2");
				GLSurfaceView20.targetGLESVersion = 2;
				return createContext(egl, display, eglConfig);
			}
			Log.w(TAG, "Returning a GLES " + targetGLESVersion + " context");
			return context;
		}
		
		public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
			egl.eglDestroyContext(display, context);
		}
		
	}
	
	static boolean checkEglError(String prompt, EGL10 egl) {
		int error;
		boolean result = true;
		while ((error = egl.eglGetError()) != EGL10.EGL_SUCCESS) {
			result = false;
			Log.e(TAG, String.format("%s: EGL error: 0x%x", prompt, error));
		}
		return result;
	}
	
	private static class ConfigChooser implements GLSurfaceView.EGLConfigChooser {
		
		public ConfigChooser(int r, int g, int b, int a, int depth, int stencil) {
			mRedSize = r;
			mGreenSize = g;
			mBlueSize = b;
			mAlphaSize = a;
			mDepthSize = depth;
			mStencilSize = stencil;
		}
		
		private static final int EGL_OPENGL_ES2_BIT = 4;
		private static final int[] s_configAttribs2 = {EGL10.EGL_RED_SIZE, 4, EGL10.EGL_GREEN_SIZE, 4, EGL10.EGL_BLUE_SIZE, 4, EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL10.EGL_NONE};
		
		public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
			final int[] num_config = new int[1];
			egl.eglChooseConfig(display, s_configAttribs2, null, 0, num_config);
			final int numConfigs = num_config[0];
			if (numConfigs <= 0)
				throw new IllegalArgumentException("No configs match configSpec");
			EGLConfig[] configs = new EGLConfig[numConfigs];
			egl.eglChooseConfig(display, s_configAttribs2, configs, numConfigs, num_config);
			if (DEBUG)
				printConfigs(egl, display, configs);
			return chooseConfig(egl, display, configs);
		}
		
		public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display, EGLConfig[] configs) {
			for (EGLConfig config : configs) {
				int d = findConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0);
				int s = findConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0);
				
				if (d < mDepthSize || s < mStencilSize)
					continue;
				
				int r = findConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE, 0);
				int g = findConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0);
				int b = findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0);
				int a = findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0);
				
				if (r == mRedSize && g == mGreenSize && b == mBlueSize && a == mAlphaSize)
					return config;
			}
			return null;
		}
		
		private int findConfigAttrib(EGL10 egl, EGLDisplay display, EGLConfig config, int attribute, int defaultValue) {
			
			if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
				return mValue[0];
			}
			return defaultValue;
		}
		
		private void printConfigs(EGL10 egl, EGLDisplay display, EGLConfig[] configs) {
			int numConfigs = configs.length;
			Log.w(TAG, String.format("%d configurations", numConfigs));
			for (int i = 0; i < numConfigs; i++) {
				Log.w(TAG, String.format("Configuration %d:\n", i));
				printConfig(egl, display, configs[i]);
			}
		}
		
		private void printConfig(EGL10 egl, EGLDisplay display, EGLConfig config) {
			int[] attributes = {EGL10.EGL_BUFFER_SIZE, EGL10.EGL_ALPHA_SIZE, EGL10.EGL_BLUE_SIZE, EGL10.EGL_GREEN_SIZE,
					EGL10.EGL_RED_SIZE, EGL10.EGL_DEPTH_SIZE, EGL10.EGL_STENCIL_SIZE, EGL10.EGL_CONFIG_CAVEAT, EGL10.EGL_CONFIG_ID,
					EGL10.EGL_LEVEL, EGL10.EGL_MAX_PBUFFER_HEIGHT, EGL10.EGL_MAX_PBUFFER_PIXELS, EGL10.EGL_MAX_PBUFFER_WIDTH,
					EGL10.EGL_NATIVE_RENDERABLE, EGL10.EGL_NATIVE_VISUAL_ID, EGL10.EGL_NATIVE_VISUAL_TYPE, 0x3030, // EGL10.EGL_PRESERVED_RESOURCES,
					EGL10.EGL_SAMPLES, EGL10.EGL_SAMPLE_BUFFERS, EGL10.EGL_SURFACE_TYPE, EGL10.EGL_TRANSPARENT_TYPE,
					EGL10.EGL_TRANSPARENT_RED_VALUE, EGL10.EGL_TRANSPARENT_GREEN_VALUE, EGL10.EGL_TRANSPARENT_BLUE_VALUE, 0x3039, // EGL10.EGL_BIND_TO_TEXTURE_RGB,
					0x303A, // EGL10.EGL_BIND_TO_TEXTURE_RGBA,
					0x303B, // EGL10.EGL_MIN_SWAP_INTERVAL,
					0x303C, // EGL10.EGL_MAX_SWAP_INTERVAL,
					EGL10.EGL_LUMINANCE_SIZE, EGL10.EGL_ALPHA_MASK_SIZE, EGL10.EGL_COLOR_BUFFER_TYPE, EGL10.EGL_RENDERABLE_TYPE, 0x3042 // EGL10.EGL_CONFORMANT
			};
			String[] names = {"EGL_BUFFER_SIZE", "EGL_ALPHA_SIZE", "EGL_BLUE_SIZE", "EGL_GREEN_SIZE", "EGL_RED_SIZE",
					"EGL_DEPTH_SIZE", "EGL_STENCIL_SIZE", "EGL_CONFIG_CAVEAT", "EGL_CONFIG_ID", "EGL_LEVEL", "EGL_MAX_PBUFFER_HEIGHT",
					"EGL_MAX_PBUFFER_PIXELS", "EGL_MAX_PBUFFER_WIDTH", "EGL_NATIVE_RENDERABLE", "EGL_NATIVE_VISUAL_ID",
					"EGL_NATIVE_VISUAL_TYPE", "EGL_PRESERVED_RESOURCES", "EGL_SAMPLES", "EGL_SAMPLE_BUFFERS", "EGL_SURFACE_TYPE",
					"EGL_TRANSPARENT_TYPE", "EGL_TRANSPARENT_RED_VALUE", "EGL_TRANSPARENT_GREEN_VALUE", "EGL_TRANSPARENT_BLUE_VALUE",
					"EGL_BIND_TO_TEXTURE_RGB", "EGL_BIND_TO_TEXTURE_RGBA", "EGL_MIN_SWAP_INTERVAL", "EGL_MAX_SWAP_INTERVAL",
					"EGL_LUMINANCE_SIZE", "EGL_ALPHA_MASK_SIZE", "EGL_COLOR_BUFFER_TYPE", "EGL_RENDERABLE_TYPE", "EGL_CONFORMANT"};
			int[] value = new int[1];
			for (int i = 0; i < attributes.length; i++) {
				int attribute = attributes[i];
				String name = names[i];
				if (egl.eglGetConfigAttrib(display, config, attribute, value))
					Log.w(TAG, String.format("  %s: %d\n", name, value[0]));
				else
					while (egl.eglGetError() != EGL10.EGL_SUCCESS)
						;
			}
		}
		
		protected int mRedSize;
		protected int mGreenSize;
		protected int mBlueSize;
		protected int mAlphaSize;
		protected int mDepthSize;
		protected int mStencilSize;
		private final int[] mValue = new int[1];
		
	}
	
}
