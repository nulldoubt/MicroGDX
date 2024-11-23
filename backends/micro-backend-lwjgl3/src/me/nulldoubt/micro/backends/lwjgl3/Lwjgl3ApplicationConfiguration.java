package me.nulldoubt.micro.backends.lwjgl3;

import me.nulldoubt.micro.Files;
import me.nulldoubt.micro.Files.FileType;
import me.nulldoubt.micro.Graphics.DisplayMode;
import me.nulldoubt.micro.Graphics.Monitor;
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
	
	public boolean disableAudio = false;
	
	public int audioDeviceSimultaneousSources = 16;
	public int audioDeviceBufferSize = 512;
	public int audioDeviceBufferCount = 9;
	
	public GLEmulation glEmulation = GLEmulation.GL20;
	public int gles30ContextMajorVersion = 3;
	public int gles30ContextMinorVersion = 2;
	public int r = 8, g = 8, b = 8, a = 8;
	
	public int stencil = 0;
	public int samples = 0;
	public boolean transparentFramebuffer;
	public int idleFPS = 60;
	public int foregroundFPS = 0;
	public boolean pauseWhenMinimized = true;
	public boolean pauseWhenLostFocus = false;
	public String preferencesDirectory = ".prefs/";
	public Files.FileType preferencesFileType = FileType.External;
	public HdpiUtils.HdpiMode hdpiMode = HdpiUtils.HdpiMode.Logical;
	public boolean debug = false;
	public PrintStream debugStream = System.err;
	
	protected static Lwjgl3ApplicationConfiguration copy(Lwjgl3ApplicationConfiguration config) {
		Lwjgl3ApplicationConfiguration copy = new Lwjgl3ApplicationConfiguration();
		copy.set(config);
		return copy;
	}
	
	public static DisplayMode getDisplayMode() {
		Lwjgl3Application.initializeGlfw();
		final GLFWVidMode videoMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
		return new Lwjgl3Graphics.Lwjgl3DisplayMode(GLFW.glfwGetPrimaryMonitor(), videoMode.width(), videoMode.height(), videoMode.refreshRate(), videoMode.redBits() + videoMode.greenBits() + videoMode.blueBits());
	}
	
	public static DisplayMode getDisplayMode(Monitor monitor) {
		Lwjgl3Application.initializeGlfw();
		final GLFWVidMode videoMode = GLFW.glfwGetVideoMode(((Lwjgl3Monitor) monitor).monitorHandle);
		return new Lwjgl3Graphics.Lwjgl3DisplayMode(((Lwjgl3Monitor) monitor).monitorHandle, videoMode.width(), videoMode.height(), videoMode.refreshRate(), videoMode.redBits() + videoMode.greenBits() + videoMode.blueBits());
	}
	
	public static DisplayMode[] getDisplayModes() {
		Lwjgl3Application.initializeGlfw();
		final Buffer videoModes = GLFW.glfwGetVideoModes(GLFW.glfwGetPrimaryMonitor());
		final DisplayMode[] result = new DisplayMode[videoModes.limit()];
		for (int i = 0; i < result.length; i++) {
			final GLFWVidMode videoMode = videoModes.get(i);
			result[i] = new Lwjgl3Graphics.Lwjgl3DisplayMode(GLFW.glfwGetPrimaryMonitor(), videoMode.width(), videoMode.height(), videoMode.refreshRate(), videoMode.redBits() + videoMode.greenBits() + videoMode.blueBits());
		}
		return result;
	}
	
	public static DisplayMode[] getDisplayModes(Monitor monitor) {
		Lwjgl3Application.initializeGlfw();
		final Buffer videoModes = GLFW.glfwGetVideoModes(((Lwjgl3Monitor) monitor).monitorHandle);
		final DisplayMode[] result = new DisplayMode[videoModes.limit()];
		for (int i = 0; i < result.length; i++) {
			final GLFWVidMode videoMode = videoModes.get(i);
			result[i] = new Lwjgl3Graphics.Lwjgl3DisplayMode(((Lwjgl3Monitor) monitor).monitorHandle, videoMode.width(), videoMode.height(), videoMode.refreshRate(), videoMode.redBits() + videoMode.greenBits() + videoMode.blueBits());
		}
		return result;
	}
	
	public static Monitor getPrimaryMonitor() {
		Lwjgl3Application.initializeGlfw();
		return toLwjgl3Monitor(GLFW.glfwGetPrimaryMonitor());
	}
	
	public static Monitor[] getMonitors() {
		Lwjgl3Application.initializeGlfw();
		final PointerBuffer glfwMonitors = GLFW.glfwGetMonitors();
		final Monitor[] monitors = new Monitor[glfwMonitors.limit()];
		for (int i = 0; i < glfwMonitors.limit(); i++)
			monitors[i] = toLwjgl3Monitor(glfwMonitors.get(i));
		return monitors;
	}
	
	protected static Lwjgl3Monitor toLwjgl3Monitor(final long glfwMonitor) {
		final IntBuffer tmp = BufferUtils.createIntBuffer(1);
		final IntBuffer tmp2 = BufferUtils.createIntBuffer(1);
		GLFW.glfwGetMonitorPos(glfwMonitor, tmp, tmp2);
		final int virtualX = tmp.get(0);
		final int virtualY = tmp2.get(0);
		return new Lwjgl3Monitor(glfwMonitor, virtualX, virtualY, GLFW.glfwGetMonitorName(glfwMonitor));
	}
	
	protected static GridPoint2 calculateCenteredWindowPosition(final Lwjgl3Monitor monitor, final int newWidth, final int newHeight) {
		final IntBuffer tmp = BufferUtils.createIntBuffer(1);
		final IntBuffer tmp2 = BufferUtils.createIntBuffer(1);
		final IntBuffer tmp3 = BufferUtils.createIntBuffer(1);
		final IntBuffer tmp4 = BufferUtils.createIntBuffer(1);
		final DisplayMode displayMode = getDisplayMode(monitor);
		
		GLFW.glfwGetMonitorWorkarea(monitor.monitorHandle, tmp, tmp2, tmp3, tmp4);
		final int workAreaWidth = tmp3.get(0);
		final int workAreaHeight = tmp4.get(0);
		
		int minX, minY, maxX, maxY;
		
		if (newWidth > workAreaWidth) {
			minX = monitor.virtualX;
			maxX = displayMode.width;
		} else {
			minX = tmp.get(0);
			maxX = workAreaWidth;
		}
		
		if (newHeight > workAreaHeight) {
			minY = monitor.virtualY;
			maxY = displayMode.height;
		} else {
			minY = tmp2.get(0);
			maxY = workAreaHeight;
		}
		
		return new GridPoint2(Math.max(minX, minX + (maxX - newWidth) / 2), Math.max(minY, minY + (maxY - newHeight) / 2));
	}
	
	protected void set(final Lwjgl3ApplicationConfiguration config) {
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
	
	public enum GLEmulation {
		ANGLE_GLES20, GL20, GL30, GL31, GL32
	}
	
}
