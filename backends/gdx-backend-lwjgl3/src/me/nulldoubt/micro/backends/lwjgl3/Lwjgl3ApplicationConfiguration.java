package me.nulldoubt.micro.backends.lwjgl3;

import me.nulldoubt.micro.*;
import me.nulldoubt.micro.Files.FileType;
import me.nulldoubt.micro.Graphics.DisplayMode;
import me.nulldoubt.micro.Graphics.Monitor;
import me.nulldoubt.micro.audio.Music;
import me.nulldoubt.micro.backends.lwjgl3.Lwjgl3Graphics.Lwjgl3Monitor;
import me.nulldoubt.micro.graphics.glutils.HdpiUtils;
import me.nulldoubt.micro.math.GridPoint2;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWVidMode.Buffer;

import java.io.PrintStream;
import java.nio.IntBuffer;

public class Lwjgl3ApplicationConfiguration extends Lwjgl3WindowConfiguration {
	
	public static PrintStream errorStream = System.err;
	
	boolean disableAudio = false;
	
	/**
	 * The maximum number of threads to use for network requests. Default is {@link Integer#MAX_VALUE}.
	 */
	int maxNetThreads = Integer.MAX_VALUE;
	
	int audioDeviceSimultaneousSources = 16;
	int audioDeviceBufferSize = 512;
	int audioDeviceBufferCount = 9;
	GLEmulation glEmulation = GLEmulation.GL20;
	int gles30ContextMajorVersion = 3;
	int gles30ContextMinorVersion = 2;
	int r = 8, g = 8, b = 8, a = 8;
	int depth = 16, stencil = 0;
	int samples = 0;
	boolean transparentFramebuffer;
	int idleFPS = 60;
	int foregroundFPS = 0;
	boolean pauseWhenMinimized = true;
	boolean pauseWhenLostFocus = false;
	String preferencesDirectory = ".prefs/";
	Files.FileType preferencesFileType = FileType.External;
	HdpiUtils.HdpiMode hdpiMode = HdpiUtils.HdpiMode.Logical;
	boolean debug = false;
	PrintStream debugStream = System.err;
	
	static Lwjgl3ApplicationConfiguration copy(Lwjgl3ApplicationConfiguration config) {
		Lwjgl3ApplicationConfiguration copy = new Lwjgl3ApplicationConfiguration();
		copy.set(config);
		return copy;
	}
	
	public static DisplayMode getDisplayMode() {
		Lwjgl3Application.initializeGlfw();
		GLFWVidMode videoMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
		return new Lwjgl3Graphics.Lwjgl3DisplayMode(GLFW.glfwGetPrimaryMonitor(), videoMode.width(), videoMode.height(),
				videoMode.refreshRate(), videoMode.redBits() + videoMode.greenBits() + videoMode.blueBits());
	}
	
	/**
	 * @return the currently active {@link DisplayMode} of the given monitor
	 */
	public static DisplayMode getDisplayMode(Monitor monitor) {
		Lwjgl3Application.initializeGlfw();
		GLFWVidMode videoMode = GLFW.glfwGetVideoMode(((Lwjgl3Monitor) monitor).monitorHandle);
		return new Lwjgl3Graphics.Lwjgl3DisplayMode(((Lwjgl3Monitor) monitor).monitorHandle, videoMode.width(), videoMode.height(),
				videoMode.refreshRate(), videoMode.redBits() + videoMode.greenBits() + videoMode.blueBits());
	}
	
	/**
	 * @return the available {@link DisplayMode}s of the primary monitor
	 */
	public static DisplayMode[] getDisplayModes() {
		Lwjgl3Application.initializeGlfw();
		Buffer videoModes = GLFW.glfwGetVideoModes(GLFW.glfwGetPrimaryMonitor());
		DisplayMode[] result = new DisplayMode[videoModes.limit()];
		for (int i = 0; i < result.length; i++) {
			GLFWVidMode videoMode = videoModes.get(i);
			result[i] = new Lwjgl3Graphics.Lwjgl3DisplayMode(GLFW.glfwGetPrimaryMonitor(), videoMode.width(), videoMode.height(),
					videoMode.refreshRate(), videoMode.redBits() + videoMode.greenBits() + videoMode.blueBits());
		}
		return result;
	}
	
	public static DisplayMode[] getDisplayModes(Monitor monitor) {
		Lwjgl3Application.initializeGlfw();
		Buffer videoModes = GLFW.glfwGetVideoModes(((Lwjgl3Monitor) monitor).monitorHandle);
		DisplayMode[] result = new DisplayMode[videoModes.limit()];
		for (int i = 0; i < result.length; i++) {
			GLFWVidMode videoMode = videoModes.get(i);
			result[i] = new Lwjgl3Graphics.Lwjgl3DisplayMode(((Lwjgl3Monitor) monitor).monitorHandle, videoMode.width(),
					videoMode.height(), videoMode.refreshRate(), videoMode.redBits() + videoMode.greenBits() + videoMode.blueBits());
		}
		return result;
	}
	
	public static Monitor getPrimaryMonitor() {
		Lwjgl3Application.initializeGlfw();
		return toLwjgl3Monitor(GLFW.glfwGetPrimaryMonitor());
	}
	
	public static Monitor[] getMonitors() {
		Lwjgl3Application.initializeGlfw();
		PointerBuffer glfwMonitors = GLFW.glfwGetMonitors();
		Monitor[] monitors = new Monitor[glfwMonitors.limit()];
		for (int i = 0; i < glfwMonitors.limit(); i++) {
			monitors[i] = toLwjgl3Monitor(glfwMonitors.get(i));
		}
		return monitors;
	}
	
	static Lwjgl3Monitor toLwjgl3Monitor(long glfwMonitor) {
		IntBuffer tmp = BufferUtils.createIntBuffer(1);
		IntBuffer tmp2 = BufferUtils.createIntBuffer(1);
		GLFW.glfwGetMonitorPos(glfwMonitor, tmp, tmp2);
		int virtualX = tmp.get(0);
		int virtualY = tmp2.get(0);
		String name = GLFW.glfwGetMonitorName(glfwMonitor);
		return new Lwjgl3Monitor(glfwMonitor, virtualX, virtualY, name);
	}
	
	static GridPoint2 calculateCenteredWindowPosition(Lwjgl3Monitor monitor, int newWidth, int newHeight) {
		IntBuffer tmp = BufferUtils.createIntBuffer(1);
		IntBuffer tmp2 = BufferUtils.createIntBuffer(1);
		IntBuffer tmp3 = BufferUtils.createIntBuffer(1);
		IntBuffer tmp4 = BufferUtils.createIntBuffer(1);
		
		DisplayMode displayMode = getDisplayMode(monitor);
		
		GLFW.glfwGetMonitorWorkarea(monitor.monitorHandle, tmp, tmp2, tmp3, tmp4);
		int workareaWidth = tmp3.get(0);
		int workareaHeight = tmp4.get(0);
		
		int minX, minY, maxX, maxY;
		
		// If the new width is greater than the working area, we have to ignore stuff like the taskbar for centering and use the
		// whole monitor's size
		if (newWidth > workareaWidth) {
			minX = monitor.virtualX;
			maxX = displayMode.width;
		} else {
			minX = tmp.get(0);
			maxX = workareaWidth;
		}
		// The same is true for height
		if (newHeight > workareaHeight) {
			minY = monitor.virtualY;
			maxY = displayMode.height;
		} else {
			minY = tmp2.get(0);
			maxY = workareaHeight;
		}
		
		return new GridPoint2(Math.max(minX, minX + (maxX - newWidth) / 2), Math.max(minY, minY + (maxY - newHeight) / 2));
	}
	
	void set(Lwjgl3ApplicationConfiguration config) {
		super.setWindowConfiguration(config);
		disableAudio = config.disableAudio;
		audioDeviceSimultaneousSources = config.audioDeviceSimultaneousSources;
		audioDeviceBufferSize = config.audioDeviceBufferSize;
		audioDeviceBufferCount = config.audioDeviceBufferCount;
		glEmulation = config.glEmulation;
		gles30ContextMajorVersion = config.gles30ContextMajorVersion;
		gles30ContextMinorVersion = config.gles30ContextMinorVersion;
		r = config.r;
		g = config.g;
		b = config.b;
		a = config.a;
		depth = config.depth;
		stencil = config.stencil;
		samples = config.samples;
		transparentFramebuffer = config.transparentFramebuffer;
		idleFPS = config.idleFPS;
		foregroundFPS = config.foregroundFPS;
		pauseWhenMinimized = config.pauseWhenMinimized;
		pauseWhenLostFocus = config.pauseWhenLostFocus;
		preferencesDirectory = config.preferencesDirectory;
		preferencesFileType = config.preferencesFileType;
		hdpiMode = config.hdpiMode;
		debug = config.debug;
		debugStream = config.debugStream;
	}
	
	/**
	 * @param visibility whether the window will be visible on creation. (default true)
	 */
	public void setInitialVisible(boolean visibility) {
		this.initialVisible = visibility;
	}
	
	/**
	 * Whether to disable audio or not. If set to true, the returned audio class instances like {@link Audio} or {@link Music}
	 * will be mock implementations.
	 */
	public void disableAudio(boolean disableAudio) {
		this.disableAudio = disableAudio;
	}
	
	/**
	 * Sets the maximum number of threads to use for network requests.
	 */
	public void setMaxNetThreads(int maxNetThreads) {
		this.maxNetThreads = maxNetThreads;
	}
	
	/**
	 * Sets the audio device configuration.
	 *
	 * @param simultaneousSources the maximum number of sources that can be played simultaniously (default 16)
	 * @param bufferSize          the audio device buffer size in samples (default 512)
	 * @param bufferCount         the audio device buffer count (default 9)
	 */
	public void setAudioConfig(int simultaneousSources, int bufferSize, int bufferCount) {
		this.audioDeviceSimultaneousSources = simultaneousSources;
		this.audioDeviceBufferSize = bufferSize;
		this.audioDeviceBufferCount = bufferCount;
	}
	
	/**
	 * Sets which OpenGL version to use to emulate OpenGL ES. If the given major/minor version is not supported, the backend falls
	 * back to OpenGL ES 2.0 emulation through OpenGL 2.0. The default parameters for major and minor should be 3 and 2
	 * respectively to be compatible with Mac OS X. Specifying major version 4 and minor version 2 will ensure that all OpenGL ES
	 * 3.0 features are supported. Note however that Mac OS X does only support 3.2.
	 *
	 * @param glVersion         which OpenGL ES emulation version to use
	 * @param gles3MajorVersion OpenGL ES major version, use 3 as default
	 * @param gles3MinorVersion OpenGL ES minor version, use 2 as default
	 * @see <a href= "http://legacy.lwjgl.org/javadoc/org/lwjgl/opengl/ContextAttribs.html"> LWJGL OSX ContextAttribs note</a>
	 */
	public void setOpenGLEmulation(GLEmulation glVersion, int gles3MajorVersion, int gles3MinorVersion) {
		this.glEmulation = glVersion;
		this.gles30ContextMajorVersion = gles3MajorVersion;
		this.gles30ContextMinorVersion = gles3MinorVersion;
	}
	
	/**
	 * Sets the bit depth of the color, depth and stencil buffer as well as multi-sampling.
	 *
	 * @param r       red bits (default 8)
	 * @param g       green bits (default 8)
	 * @param b       blue bits (default 8)
	 * @param a       alpha bits (default 8)
	 * @param depth   depth bits (default 16)
	 * @param stencil stencil bits (default 0)
	 * @param samples MSAA samples (default 0)
	 */
	public void setBackBufferConfig(int r, int g, int b, int a, int depth, int stencil, int samples) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
		this.depth = depth;
		this.stencil = stencil;
		this.samples = samples;
	}
	
	public void setTransparentFramebuffer(boolean transparentFramebuffer) {
		this.transparentFramebuffer = transparentFramebuffer;
	}
	
	/**
	 * Sets the polling rate during idle time in non-continuous rendering mode. Must be positive. Default is 60.
	 */
	public void setIdleFPS(int fps) {
		this.idleFPS = fps;
	}
	
	/**
	 * Sets the target framerate for the application. The CPU sleeps as needed. Must be positive. Use 0 to never sleep. Default is
	 * 0.
	 */
	public void setForegroundFPS(int fps) {
		this.foregroundFPS = fps;
	}
	
	/**
	 * Sets whether to pause the application {@link ApplicationListener#pause()} and fire
	 * {@link LifecycleListener#pause()}/{@link LifecycleListener#resume()} events on when window is minimized/restored.
	 **/
	public void setPauseWhenMinimized(boolean pauseWhenMinimized) {
		this.pauseWhenMinimized = pauseWhenMinimized;
	}
	
	/**
	 * Sets whether to pause the application {@link ApplicationListener#pause()} and fire
	 * {@link LifecycleListener#pause()}/{@link LifecycleListener#resume()} events on when window loses/gains focus.
	 **/
	public void setPauseWhenLostFocus(boolean pauseWhenLostFocus) {
		this.pauseWhenLostFocus = pauseWhenLostFocus;
	}
	
	/**
	 * Sets the directory where {@link Preferences} will be stored, as well as the file type to be used to store them. Defaults to
	 * "$USER_HOME/.prefs/" and {@link FileType#External}.
	 */
	public void setPreferencesConfig(String preferencesDirectory, Files.FileType preferencesFileType) {
		this.preferencesDirectory = preferencesDirectory;
		this.preferencesFileType = preferencesFileType;
	}
	
	public void setHdpiMode(HdpiUtils.HdpiMode mode) {
		this.hdpiMode = mode;
	}
	
	public void enableGLDebugOutput(boolean enable, PrintStream debugOutputStream) {
		debug = enable;
		debugStream = debugOutputStream;
	}
	
	public enum GLEmulation {
		ANGLE_GLES20, GL20, GL30, GL31, GL32
	}
	
}
