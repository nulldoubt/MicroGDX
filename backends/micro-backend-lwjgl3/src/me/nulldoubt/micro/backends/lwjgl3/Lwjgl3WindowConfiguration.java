package me.nulldoubt.micro.backends.lwjgl3;

import me.nulldoubt.micro.Files.FileType;
import me.nulldoubt.micro.Graphics.DisplayMode;
import me.nulldoubt.micro.backends.lwjgl3.Lwjgl3Graphics.Lwjgl3DisplayMode;
import me.nulldoubt.micro.graphics.Color;

import java.util.Arrays;

public class Lwjgl3WindowConfiguration {
	
	public int x = -1;
	public int y = -1;
	public int width = 640;
	public int height = 480;
	public int minWidth = -1,
			minHeight = -1,
			maxWidth = -1,
			maxHeight = -1;
	public boolean resizable = true;
	public boolean decorated = true;
	public boolean maximized = false;
	public Lwjgl3Graphics.Lwjgl3Monitor maximizedMonitor;
	public boolean autoIconify = true;
	public FileType iconFileType;
	public String[] iconPaths;
	public Lwjgl3WindowListener listener;
	public Lwjgl3DisplayMode fullscreen;
	public String title;
	public Color backgroundColor = Color.BLACK;
	public boolean visible = true;
	public boolean vSync = true;
	
	protected void setWindowConfiguration(final Lwjgl3WindowConfiguration config) {
		x = config.x;
		y = config.y;
		width = config.width;
		height = config.height;
		minWidth = config.minWidth;
		minHeight = config.minHeight;
		maxWidth = config.maxWidth;
		maxHeight = config.maxHeight;
		resizable = config.resizable;
		decorated = config.decorated;
		maximized = config.maximized;
		maximizedMonitor = config.maximizedMonitor;
		autoIconify = config.autoIconify;
		iconFileType = config.iconFileType;
		if (config.iconPaths != null)
			iconPaths = Arrays.copyOf(config.iconPaths, config.iconPaths.length);
		listener = config.listener;
		fullscreen = config.fullscreen;
		title = config.title;
		backgroundColor = config.backgroundColor;
		visible = config.visible;
		vSync = config.vSync;
	}
	
	public void setSize(int width, int height) {
		this.width = width;
		this.height = height;
	}
	
	public void setPosition(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public void setSizeLimits(int minWidth, int minHeight, int maxWidth, int maxHeight) {
		this.minWidth = minWidth;
		this.minHeight = minHeight;
		this.maxWidth = maxWidth;
		this.maxHeight = maxHeight;
	}
	
	public void setIcon(final String... filePaths) {
		setIcon(FileType.Internal, filePaths);
	}
	
	public void setIcon(final FileType fileType, final String... filePaths) {
		iconFileType = fileType;
		iconPaths = filePaths;
	}
	
	public void setFullscreen(final DisplayMode mode) {
		this.fullscreen = (Lwjgl3DisplayMode) mode;
	}
	
}
