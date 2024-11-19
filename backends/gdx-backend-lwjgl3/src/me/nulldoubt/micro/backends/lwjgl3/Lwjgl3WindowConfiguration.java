package me.nulldoubt.micro.backends.lwjgl3;

import me.nulldoubt.micro.Files.FileType;
import me.nulldoubt.micro.Graphics;
import me.nulldoubt.micro.Graphics.DisplayMode;
import me.nulldoubt.micro.backends.lwjgl3.Lwjgl3Graphics.Lwjgl3DisplayMode;
import me.nulldoubt.micro.graphics.Color;

import java.util.Arrays;

public class Lwjgl3WindowConfiguration {
	
	int windowX = -1;
	int windowY = -1;
	int windowWidth = 640;
	int windowHeight = 480;
	int windowMinWidth = -1, windowMinHeight = -1, windowMaxWidth = -1, windowMaxHeight = -1;
	boolean windowResizable = true;
	boolean windowDecorated = true;
	boolean windowMaximized = false;
	Lwjgl3Graphics.Lwjgl3Monitor maximizedMonitor;
	boolean autoIconify = true;
	FileType windowIconFileType;
	String[] windowIconPaths;
	Lwjgl3WindowListener windowListener;
	Lwjgl3DisplayMode fullscreenMode;
	String title;
	Color initialBackgroundColor = Color.BLACK;
	boolean initialVisible = true;
	boolean vSyncEnabled = true;
	
	void setWindowConfiguration(Lwjgl3WindowConfiguration config) {
		windowX = config.windowX;
		windowY = config.windowY;
		windowWidth = config.windowWidth;
		windowHeight = config.windowHeight;
		windowMinWidth = config.windowMinWidth;
		windowMinHeight = config.windowMinHeight;
		windowMaxWidth = config.windowMaxWidth;
		windowMaxHeight = config.windowMaxHeight;
		windowResizable = config.windowResizable;
		windowDecorated = config.windowDecorated;
		windowMaximized = config.windowMaximized;
		maximizedMonitor = config.maximizedMonitor;
		autoIconify = config.autoIconify;
		windowIconFileType = config.windowIconFileType;
		if (config.windowIconPaths != null)
			windowIconPaths = Arrays.copyOf(config.windowIconPaths, config.windowIconPaths.length);
		windowListener = config.windowListener;
		fullscreenMode = config.fullscreenMode;
		title = config.title;
		initialBackgroundColor = config.initialBackgroundColor;
		initialVisible = config.initialVisible;
		vSyncEnabled = config.vSyncEnabled;
	}
	
	public void setInitialVisible(boolean visibility) {
		this.initialVisible = visibility;
	}
	
	public void setWindowedMode(int width, int height) {
		this.windowWidth = width;
		this.windowHeight = height;
	}
	
	/**
	 * @param resizable whether the windowed mode window is resizable (default true)
	 */
	public void setResizable(boolean resizable) {
		this.windowResizable = resizable;
	}
	
	public void setDecorated(boolean decorated) {
		this.windowDecorated = decorated;
	}
	
	public void setMaximized(boolean maximized) {
		this.windowMaximized = maximized;
	}
	
	public void setMaximizedMonitor(Graphics.Monitor monitor) {
		this.maximizedMonitor = (Lwjgl3Graphics.Lwjgl3Monitor) monitor;
	}
	
	public void setAutoIconify(boolean autoIconify) {
		this.autoIconify = autoIconify;
	}
	
	public void setWindowPosition(int x, int y) {
		windowX = x;
		windowY = y;
	}
	
	public void setWindowSizeLimits(int minWidth, int minHeight, int maxWidth, int maxHeight) {
		windowMinWidth = minWidth;
		windowMinHeight = minHeight;
		windowMaxWidth = maxWidth;
		windowMaxHeight = maxHeight;
	}
	
	public void setWindowIcon(String... filePaths) {
		setWindowIcon(FileType.Internal, filePaths);
	}
	
	public void setWindowIcon(FileType fileType, String... filePaths) {
		windowIconFileType = fileType;
		windowIconPaths = filePaths;
	}
	
	public void setWindowListener(Lwjgl3WindowListener windowListener) {
		this.windowListener = windowListener;
	}
	
	public void setFullscreenMode(DisplayMode mode) {
		this.fullscreenMode = (Lwjgl3DisplayMode) mode;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public void setInitialBackgroundColor(Color color) {
		initialBackgroundColor = color;
	}
	
	public void useVsync(boolean vsync) {
		this.vSyncEnabled = vsync;
	}
	
}
