package me.nulldoubt.micro.backends.lwjgl3;

import me.nulldoubt.micro.AbstractGraphics;
import me.nulldoubt.micro.Application;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.graphics.*;
import me.nulldoubt.micro.graphics.Cursor.SystemCursor;
import me.nulldoubt.micro.graphics.glutils.GLVersion;
import me.nulldoubt.micro.graphics.glutils.HdpiUtils;
import me.nulldoubt.micro.math.GridPoint2;
import me.nulldoubt.micro.utils.Disposable;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.Configuration;

import java.nio.IntBuffer;

public class Lwjgl3Graphics extends AbstractGraphics implements Disposable {
	
	final Lwjgl3Window window;
	GL20 gl20;
	IntBuffer tmpBuffer = BufferUtils.createIntBuffer(1);
	IntBuffer tmpBuffer2 = BufferUtils.createIntBuffer(1);
	private GL30 gl30;
	private GL31 gl31;
	private GL32 gl32;
	private GLVersion glVersion;
	private volatile int backBufferWidth;
	private volatile int backBufferHeight;
	private volatile int logicalWidth;
	private volatile int logicalHeight;
	private volatile boolean isContinuous = true;
	private BufferFormat bufferFormat;
	private long lastFrameTime = -1;
	private float deltaTime;
	private boolean resetDeltaTime = false;
	private long frameId;
	private long frameCounterStart = 0;
	private int frames;
	private int fps;
	GLFWFramebufferSizeCallback resizeCallback = new GLFWFramebufferSizeCallback() {
		@Override
		public void invoke(long windowHandle, final int width, final int height) {
			if (!"glfw_async".equals(Configuration.GLFW_LIBRARY_NAME.get())) {
				updateFramebufferInfo();
				if (!window.isListenerInitialized()) {
					return;
				}
				window.makeCurrent();
				gl20.glViewport(0, 0, backBufferWidth, backBufferHeight);
				window.getListener().resize(getWidth(), getHeight());
				update();
				window.getListener().render();
				GLFW.glfwSwapBuffers(windowHandle);
			} else {
				window.asyncResized = true;
			}
		}
	};
	private int windowPosXBeforeFullscreen;
	private int windowPosYBeforeFullscreen;
	private int windowWidthBeforeFullscreen;
	private int windowHeightBeforeFullscreen;
	private DisplayMode displayModeBeforeFullscreen = null;
	
	public Lwjgl3Graphics(Lwjgl3Window window) {
		this.window = window;
		if (window.getConfig().glEmulation == Lwjgl3ApplicationConfiguration.GLEmulation.GL32) {
			this.gl20 = this.gl30 = this.gl31 = this.gl32 = new Lwjgl3GL32();
		} else if (window.getConfig().glEmulation == Lwjgl3ApplicationConfiguration.GLEmulation.GL31) {
			this.gl20 = this.gl30 = this.gl31 = new Lwjgl3GL31();
		} else if (window.getConfig().glEmulation == Lwjgl3ApplicationConfiguration.GLEmulation.GL30) {
			this.gl20 = this.gl30 = new Lwjgl3GL30();
		} else {
			try {
				this.gl20 = window.getConfig().glEmulation == Lwjgl3ApplicationConfiguration.GLEmulation.GL20 ? new Lwjgl3GL20() : (GL20) Class.forName("com.nulldoubt.micro.backends.lwjgl3.angle.Lwjgl3GLES20").newInstance();
			} catch (Throwable t) {
				throw new MicroRuntimeException("Couldn't instantiate GLES20.", t);
			}
			this.gl30 = null;
		}
		updateFramebufferInfo();
		initiateGL();
		GLFW.glfwSetFramebufferSizeCallback(window.getWindowHandle(), resizeCallback);
	}
	
	private void initiateGL() {
		String versionString = gl20.glGetString(GL11.GL_VERSION);
		String vendorString = gl20.glGetString(GL11.GL_VENDOR);
		String rendererString = gl20.glGetString(GL11.GL_RENDERER);
		glVersion = new GLVersion(Application.ApplicationType.Desktop, versionString, vendorString, rendererString);
	}
	
	public Lwjgl3Window getWindow() {
		return window;
	}
	
	void updateFramebufferInfo() {
		GLFW.glfwGetFramebufferSize(window.getWindowHandle(), tmpBuffer, tmpBuffer2);
		this.backBufferWidth = tmpBuffer.get(0);
		this.backBufferHeight = tmpBuffer2.get(0);
		GLFW.glfwGetWindowSize(window.getWindowHandle(), tmpBuffer, tmpBuffer2);
		Lwjgl3Graphics.this.logicalWidth = tmpBuffer.get(0);
		Lwjgl3Graphics.this.logicalHeight = tmpBuffer2.get(0);
		Lwjgl3ApplicationConfiguration config = window.getConfig();
		bufferFormat = new BufferFormat(config.r, config.g, config.b, config.a, 0, config.stencil, config.samples, false);
	}
	
	void update() {
		long time = System.nanoTime();
		if (lastFrameTime == -1)
			lastFrameTime = time;
		if (resetDeltaTime) {
			resetDeltaTime = false;
			deltaTime = 0;
		} else
			deltaTime = (time - lastFrameTime) / 1000000000.0f;
		lastFrameTime = time;
		
		if (time - frameCounterStart >= 1000000000) {
			fps = frames;
			frames = 0;
			frameCounterStart = time;
		}
		frames++;
		frameId++;
	}
	
	@Override
	public boolean isGL30Available() {
		return gl30 != null;
	}
	
	@Override
	public boolean isGL31Available() {
		return gl31 != null;
	}
	
	@Override
	public boolean isGL32Available() {
		return gl32 != null;
	}
	
	@Override
	public GL20 getGL20() {
		return gl20;
	}
	
	@Override
	public GL30 getGL30() {
		return gl30;
	}
	
	@Override
	public GL31 getGL31() {
		return gl31;
	}
	
	@Override
	public GL32 getGL32() {
		return gl32;
	}
	
	@Override
	public void setGL32(GL32 gl32) {
		this.gl32 = gl32;
	}
	
	@Override
	public int getWidth() {
		if (window.getConfig().hdpiMode == HdpiUtils.HdpiMode.Pixels) {
			return backBufferWidth;
		} else {
			return logicalWidth;
		}
	}
	
	@Override
	public int getHeight() {
		if (window.getConfig().hdpiMode == HdpiUtils.HdpiMode.Pixels) {
			return backBufferHeight;
		} else {
			return logicalHeight;
		}
	}
	
	@Override
	public int getBackBufferWidth() {
		return backBufferWidth;
	}
	
	@Override
	public int getBackBufferHeight() {
		return backBufferHeight;
	}
	
	@Override
	public int getSafeInsetLeft() {
		return 0;
	}
	
	@Override
	public int getSafeInsetTop() {
		return 0;
	}
	
	@Override
	public int getSafeInsetBottom() {
		return 0;
	}
	
	@Override
	public int getSafeInsetRight() {
		return 0;
	}
	
	@Override
	public long getFrameId() {
		return frameId;
	}
	
	@Override
	public float getDeltaTime() {
		return deltaTime;
	}
	
	@Override
	public int getFPS() {
		return fps;
	}
	
	@Override
	public GraphicsType getType() {
		return GraphicsType.LWJGL3;
	}
	
	@Override
	public GLVersion getGLVersion() {
		return glVersion;
	}
	
	@Override
	public float getPpiX() {
		return getPpcX() * 2.54f;
	}
	
	@Override
	public float getPpiY() {
		return getPpcY() * 2.54f;
	}
	
	@Override
	public float getPpcX() {
		Lwjgl3Monitor monitor = (Lwjgl3Monitor) getMonitor();
		GLFW.glfwGetMonitorPhysicalSize(monitor.monitorHandle, tmpBuffer, tmpBuffer2);
		int sizeX = tmpBuffer.get(0);
		DisplayMode mode = getDisplayMode();
		return mode.width / (float) sizeX * 10;
	}
	
	@Override
	public float getPpcY() {
		Lwjgl3Monitor monitor = (Lwjgl3Monitor) getMonitor();
		GLFW.glfwGetMonitorPhysicalSize(monitor.monitorHandle, tmpBuffer, tmpBuffer2);
		int sizeY = tmpBuffer2.get(0);
		DisplayMode mode = getDisplayMode();
		return mode.height / (float) sizeY * 10;
	}
	
	@Override
	public boolean supportsDisplayModeChange() {
		return true;
	}
	
	@Override
	public Monitor getPrimaryMonitor() {
		return Lwjgl3ApplicationConfiguration.toLwjgl3Monitor(GLFW.glfwGetPrimaryMonitor());
	}
	
	@Override
	public Monitor getMonitor() {
		Monitor[] monitors = getMonitors();
		Monitor result = monitors[0];
		
		GLFW.glfwGetWindowPos(window.getWindowHandle(), tmpBuffer, tmpBuffer2);
		int windowX = tmpBuffer.get(0);
		int windowY = tmpBuffer2.get(0);
		GLFW.glfwGetWindowSize(window.getWindowHandle(), tmpBuffer, tmpBuffer2);
		int windowWidth = tmpBuffer.get(0);
		int windowHeight = tmpBuffer2.get(0);
		int overlap;
		int bestOverlap = 0;
		
		for (Monitor monitor : monitors) {
			DisplayMode mode = getDisplayMode(monitor);
			overlap = Math.max(0, Math.min(windowX + windowWidth, monitor.virtualX + mode.width) - Math.max(windowX, monitor.virtualX)) * Math.max(0, Math.min(windowY + windowHeight, monitor.virtualY + mode.height) - Math.max(windowY, monitor.virtualY));
			if (bestOverlap < overlap) {
				bestOverlap = overlap;
				result = monitor;
			}
		}
		return result;
	}
	
	@Override
	public Monitor[] getMonitors() {
		PointerBuffer glfwMonitors = GLFW.glfwGetMonitors();
		Monitor[] monitors = new Monitor[glfwMonitors.limit()];
		for (int i = 0; i < glfwMonitors.limit(); i++)
			monitors[i] = Lwjgl3ApplicationConfiguration.toLwjgl3Monitor(glfwMonitors.get(i));
		return monitors;
	}
	
	@Override
	public DisplayMode[] getDisplayModes() {
		return Lwjgl3ApplicationConfiguration.getDisplayModes(getMonitor());
	}
	
	@Override
	public DisplayMode[] getDisplayModes(Monitor monitor) {
		return Lwjgl3ApplicationConfiguration.getDisplayModes(monitor);
	}
	
	@Override
	public DisplayMode getDisplayMode() {
		return Lwjgl3ApplicationConfiguration.getDisplayMode(getMonitor());
	}
	
	@Override
	public DisplayMode getDisplayMode(Monitor monitor) {
		return Lwjgl3ApplicationConfiguration.getDisplayMode(monitor);
	}
	
	@Override
	public boolean setFullscreenMode(DisplayMode displayMode) {
		window.getInput().resetPollingStates();
		Lwjgl3DisplayMode newMode = (Lwjgl3DisplayMode) displayMode;
		if (isFullscreen()) {
			Lwjgl3DisplayMode currentMode = (Lwjgl3DisplayMode) getDisplayMode();
			if (currentMode.getMonitor() == newMode.getMonitor() && currentMode.refreshRate == newMode.refreshRate) {
				// same monitor and refresh rate
				GLFW.glfwSetWindowSize(window.getWindowHandle(), newMode.width, newMode.height);
			} else {
				// different monitor and/or refresh rate
				GLFW.glfwSetWindowMonitor(window.getWindowHandle(), newMode.getMonitor(), 0, 0, newMode.width, newMode.height,
						newMode.refreshRate);
			}
		} else {
			// store window position so we can restore it when switching from fullscreen to windowed later
			storeCurrentWindowPositionAndDisplayMode();
			
			// switch from windowed to fullscreen
			GLFW.glfwSetWindowMonitor(window.getWindowHandle(), newMode.getMonitor(), 0, 0, newMode.width, newMode.height, newMode.refreshRate);
		}
		updateFramebufferInfo();
		
		setVSync(window.getConfig().vSync);
		
		return true;
	}
	
	@Override
	public boolean setWindowedMode(int width, int height) {
		window.getInput().resetPollingStates();
		if (!isFullscreen()) {
			GridPoint2 newPos = null;
			boolean centerWindow = false;
			if (width != logicalWidth || height != logicalHeight) {
				centerWindow = true; // recenter the window since its size changed
				newPos = Lwjgl3ApplicationConfiguration.calculateCenteredWindowPosition((Lwjgl3Monitor) getMonitor(), width, height);
			}
			GLFW.glfwSetWindowSize(window.getWindowHandle(), width, height);
			if (centerWindow) {
				window.setPosition(newPos.x, newPos.y); // on macOS the centering has to happen _after_ the new window size was set
			}
		} else { // if we were in fullscreen mode, we should consider restoring a previous display mode
			if (displayModeBeforeFullscreen == null) {
				storeCurrentWindowPositionAndDisplayMode();
			}
			if (width != windowWidthBeforeFullscreen || height != windowHeightBeforeFullscreen) { // center the window since its size
				// changed
				GridPoint2 newPos = Lwjgl3ApplicationConfiguration.calculateCenteredWindowPosition((Lwjgl3Monitor) getMonitor(), width,
						height);
				GLFW.glfwSetWindowMonitor(window.getWindowHandle(), 0, newPos.x, newPos.y, width, height,
						displayModeBeforeFullscreen.refreshRate);
			} else { // restore previous position
				GLFW.glfwSetWindowMonitor(window.getWindowHandle(), 0, windowPosXBeforeFullscreen, windowPosYBeforeFullscreen, width,
						height, displayModeBeforeFullscreen.refreshRate);
			}
		}
		updateFramebufferInfo();
		return true;
	}
	
	@Override
	public void setTitle(String title) {
		if (title == null) {
			title = "";
		}
		GLFW.glfwSetWindowTitle(window.getWindowHandle(), title);
	}
	
	@Override
	public void setUndecorated(boolean undecorated) {
		getWindow().getConfig().decorated = !undecorated;
		GLFW.glfwSetWindowAttrib(window.getWindowHandle(), GLFW.GLFW_DECORATED, undecorated ? GLFW.GLFW_FALSE : GLFW.GLFW_TRUE);
	}
	
	@Override
	public void setResizable(boolean resizable) {
		getWindow().getConfig().resizable = resizable;
		GLFW.glfwSetWindowAttrib(window.getWindowHandle(), GLFW.GLFW_RESIZABLE, resizable ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
	}
	
	@Override
	public void setVSync(boolean vsync) {
		getWindow().getConfig().vSync = vsync;
		GLFW.glfwSwapInterval(vsync ? 1 : 0);
	}
	
	/**
	 * Sets the target framerate for the application, when using continuous rendering. Must be positive. The cpu sleeps as needed.
	 * Use 0 to never sleep. If there are multiple windows, the value for the first window created is used for all. Default is 0.
	 *
	 * @param fps fps
	 */
	@Override
	public void setFPS(int fps) {
		getWindow().getConfig().foregroundFPS = fps;
	}
	
	@Override
	public BufferFormat getBufferFormat() {
		return bufferFormat;
	}
	
	@Override
	public boolean supportsExtension(String extension) {
		return GLFW.glfwExtensionSupported(extension);
	}
	
	@Override
	public void setGL31(GL31 gl31) {
		this.gl31 = gl31;
	}
	
	@Override
	public void setGL30(GL30 gl30) {
		this.gl30 = gl30;
	}
	
	@Override
	public void setGL20(GL20 gl20) {
		this.gl20 = gl20;
	}
	
	public int getLogicalWidth() {
		return logicalWidth;
	}
	
	public int getLogicalHeight() {
		return logicalHeight;
	}
	
	public void resetDeltaTime() {
		resetDeltaTime = true;
	}
	
	private void storeCurrentWindowPositionAndDisplayMode() {
		windowPosXBeforeFullscreen = window.getPositionX();
		windowPosYBeforeFullscreen = window.getPositionY();
		windowWidthBeforeFullscreen = logicalWidth;
		windowHeightBeforeFullscreen = logicalHeight;
		displayModeBeforeFullscreen = getDisplayMode();
	}
	
	@Override
	public void dispose() {
		this.resizeCallback.free();
	}
	
	@Override
	public void setContinuousRendering(boolean continuous) {
		this.isContinuous = continuous;
	}
	
	public static class Lwjgl3DisplayMode extends DisplayMode {
		
		final long monitorHandle;
		
		Lwjgl3DisplayMode(long monitor, int width, int height, int refreshRate, int bitsPerPixel) {
			super(width, height, refreshRate, bitsPerPixel);
			this.monitorHandle = monitor;
		}
		
		public long getMonitor() {
			return monitorHandle;
		}
		
	}
	
	@Override
	public boolean isContinuousRendering() {
		return isContinuous;
	}
	
	public static class Lwjgl3Monitor extends Monitor {
		
		final long monitorHandle;
		
		Lwjgl3Monitor(long monitor, int virtualX, int virtualY, String name) {
			super(virtualX, virtualY, name);
			this.monitorHandle = monitor;
		}
		
		public long getMonitorHandle() {
			return monitorHandle;
		}
		
	}
	
	@Override
	public void requestRendering() {
		window.requestRendering();
	}
	
	@Override
	public boolean isFullscreen() {
		return GLFW.glfwGetWindowMonitor(window.getWindowHandle()) != 0;
	}
	
	@Override
	public Cursor newCursor(Pixmap pixmap, int xHotspot, int yHotspot) {
		return new Lwjgl3Cursor(getWindow(), pixmap, xHotspot, yHotspot);
	}
	
	@Override
	public void setCursor(Cursor cursor) {
		GLFW.glfwSetCursor(getWindow().getWindowHandle(), ((Lwjgl3Cursor) cursor).glfwCursor);
	}
	
	@Override
	public void setSystemCursor(SystemCursor systemCursor) {
		Lwjgl3Cursor.setSystemCursor(getWindow().getWindowHandle(), systemCursor);
	}
	
}
