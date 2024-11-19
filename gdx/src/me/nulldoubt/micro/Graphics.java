package me.nulldoubt.micro;

import me.nulldoubt.micro.graphics.*;
import me.nulldoubt.micro.graphics.Cursor.SystemCursor;
import me.nulldoubt.micro.graphics.glutils.GLVersion;

public interface Graphics {
	
	enum GraphicsType {
		AndroidGL, LWJGL3
	}
	
	class DisplayMode {
		
		public final int width;
		public final int height;
		public final int refreshRate;
		public final int bitsPerPixel;
		
		protected DisplayMode(final int width, final int height, final int refreshRate, final int bitsPerPixel) {
			this.width = width;
			this.height = height;
			this.refreshRate = refreshRate;
			this.bitsPerPixel = bitsPerPixel;
		}
		
		public String toString() {
			return width + "x" + height + ", bpp: " + bitsPerPixel + ", hz: " + refreshRate;
		}
		
	}
	
	class Monitor {
		
		public final int virtualX;
		public final int virtualY;
		public final String name;
		
		protected Monitor(final int virtualX, final int virtualY, final String name) {
			this.virtualX = virtualX;
			this.virtualY = virtualY;
			this.name = name;
		}
		
	}
	
	record BufferFormat(int r, int g, int b, int a, int depth, int stencil, int samples, boolean coverageSampling) {
		
		public String toString() {
			return "r: " + r + ", g: " + g + ", b: " + b + ", a: " + a + ", depth: " + depth + ", stencil: " + stencil
					+ ", num samples: " + samples + ", coverage sampling: " + coverageSampling;
		}
		
	}
	
	boolean isGL30Available();
	
	boolean isGL31Available();
	
	boolean isGL32Available();
	
	GL20 getGL20();
	
	GL30 getGL30();
	
	GL31 getGL31();
	
	GL32 getGL32();
	
	void setGL20(final GL20 gl20);
	
	void setGL30(final GL30 gl30);
	
	void setGL31(final GL31 gl31);
	
	void setGL32(final GL32 gl32);
	
	int getWidth();
	
	int getHeight();
	
	int getBackBufferWidth();
	
	int getBackBufferHeight();
	
	float getBackBufferScale();
	
	int getSafeInsetLeft();
	
	int getSafeInsetTop();
	
	int getSafeInsetBottom();
	
	int getSafeInsetRight();
	
	long getFrameId();
	
	float getDeltaTime();
	
	int getFPS();
	
	GraphicsType getType();
	
	GLVersion getGLVersion();
	
	float getPpiX();
	
	float getPpiY();
	
	float getPpcX();
	
	float getPpcY();
	
	float getDensity();
	
	boolean supportsDisplayModeChange();
	
	Monitor getPrimaryMonitor();
	
	Monitor getMonitor();
	
	Monitor[] getMonitors();
	
	DisplayMode[] getDisplayModes();
	
	DisplayMode[] getDisplayModes(final Monitor monitor);
	
	DisplayMode getDisplayMode();
	
	DisplayMode getDisplayMode(final Monitor monitor);
	
	boolean setFullscreenMode(final DisplayMode displayMode);
	
	boolean setWindowedMode(final int width, final int height);
	
	void setTitle(final String title);
	
	void setUndecorated(final boolean undecorated);
	
	void setResizable(final boolean resizable);
	
	void setVSync(final boolean vsync);
	
	void setFPS(final int fps);
	
	BufferFormat getBufferFormat();
	
	boolean supportsExtension(final String extension);
	
	void setContinuousRendering(final boolean continuous);
	
	boolean isContinuousRendering();
	
	void requestRendering();
	
	boolean isFullscreen();
	
	Cursor newCursor(final Pixmap pixmap, final int xHotspot, final int yHotspot);
	
	void setCursor(final Cursor cursor);
	
	void setSystemCursor(final SystemCursor systemCursor);
	
}
