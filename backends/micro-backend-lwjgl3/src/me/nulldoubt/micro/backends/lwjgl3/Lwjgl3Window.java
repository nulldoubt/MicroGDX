package me.nulldoubt.micro.backends.lwjgl3;

import com.badlogic.gdx.utils.Os;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import me.nulldoubt.micro.ApplicationListener;
import me.nulldoubt.micro.Files;
import me.nulldoubt.micro.LifecycleListener;
import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.graphics.Pixmap;
import me.nulldoubt.micro.utils.Disposable;
import me.nulldoubt.micro.utils.collections.Array;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;

import java.nio.IntBuffer;

public class Lwjgl3Window implements Disposable {
	
	final ApplicationListener listener;
	final Lwjgl3ApplicationBase application;
	private final Array<LifecycleListener> lifecycleListeners;
	private final Lwjgl3ApplicationConfiguration config;
	private final Array<Runnable> runnables = new Array<Runnable>();
	private final Array<Runnable> executedRunnables = new Array<Runnable>();
	private final IntBuffer tmpBuffer;
	private final IntBuffer tmpBuffer2;
	Lwjgl3WindowListener windowListener;
	private final GLFWWindowMaximizeCallback maximizeCallback = new GLFWWindowMaximizeCallback() {
		@Override
		public void invoke(long windowHandle, final boolean maximized) {
			postRunnable(new Runnable() {
				@Override
				public void run() {
					if (windowListener != null) {
						windowListener.maximized(maximized);
					}
				}
			});
		}
		
	};
	private final GLFWWindowCloseCallback closeCallback = new GLFWWindowCloseCallback() {
		@Override
		public void invoke(final long windowHandle) {
			postRunnable(new Runnable() {
				@Override
				public void run() {
					if (windowListener != null) {
						if (!windowListener.closeRequested()) {
							GLFW.glfwSetWindowShouldClose(windowHandle, false);
						}
					}
				}
			});
		}
	};
	private final GLFWDropCallback dropCallback = new GLFWDropCallback() {
		@Override
		public void invoke(final long windowHandle, final int count, final long names) {
			final String[] files = new String[count];
			for (int i = 0; i < count; i++) {
				files[i] = getName(names, i);
			}
			postRunnable(new Runnable() {
				@Override
				public void run() {
					if (windowListener != null) {
						windowListener.filesDropped(files);
					}
				}
			});
		}
	};
	private final GLFWWindowRefreshCallback refreshCallback = new GLFWWindowRefreshCallback() {
		@Override
		public void invoke(long windowHandle) {
			postRunnable(new Runnable() {
				@Override
				public void run() {
					if (windowListener != null) {
						windowListener.refreshRequested();
					}
				}
			});
		}
	};
	boolean iconified = false;
	private final GLFWWindowIconifyCallback iconifyCallback = new GLFWWindowIconifyCallback() {
		@Override
		public void invoke(long windowHandle, final boolean iconified) {
			postRunnable(new Runnable() {
				@Override
				public void run() {
					if (windowListener != null) {
						windowListener.iconified(iconified);
					}
					Lwjgl3Window.this.iconified = iconified;
					if (iconified) {
						if (config.pauseWhenMinimized) {
							synchronized (lifecycleListeners) {
								for (LifecycleListener lifecycleListener : lifecycleListeners) {
									lifecycleListener.pause();
								}
							}
							listener.pause();
						}
					} else {
						if (config.pauseWhenMinimized) {
							synchronized (lifecycleListeners) {
								for (LifecycleListener lifecycleListener : lifecycleListeners) {
									lifecycleListener.resume();
								}
							}
							listener.resume();
						}
					}
				}
			});
		}
	};
	boolean focused = false;
	private final GLFWWindowFocusCallback focusCallback = new GLFWWindowFocusCallback() {
		@Override
		public void invoke(long windowHandle, final boolean focused) {
			postRunnable(new Runnable() {
				@Override
				public void run() {
					if (windowListener != null) {
						if (focused) {
							if (config.pauseWhenLostFocus) {
								synchronized (lifecycleListeners) {
									for (LifecycleListener lifecycleListener : lifecycleListeners) {
										lifecycleListener.resume();
									}
								}
							}
							windowListener.focusGained();
						} else {
							windowListener.focusLost();
							if (config.pauseWhenLostFocus) {
								synchronized (lifecycleListeners) {
									for (LifecycleListener lifecycleListener : lifecycleListeners) {
										lifecycleListener.pause();
									}
								}
								listener.pause();
							}
						}
						Lwjgl3Window.this.focused = focused;
					}
				}
			});
		}
	};
	boolean asyncResized = false;
	private long windowHandle;
	private boolean listenerInitialized = false;
	private Lwjgl3Graphics graphics;
	private Lwjgl3Input input;
	private boolean requestRendering = false;
	
	Lwjgl3Window(ApplicationListener listener, Array<LifecycleListener> lifecycleListeners, Lwjgl3ApplicationConfiguration config,
				 Lwjgl3ApplicationBase application) {
		this.listener = listener;
		this.lifecycleListeners = lifecycleListeners;
		this.windowListener = config.listener;
		this.config = config;
		this.application = application;
		this.tmpBuffer = BufferUtils.createIntBuffer(1);
		this.tmpBuffer2 = BufferUtils.createIntBuffer(1);
	}
	
	static void setIcon(long windowHandle, String[] imagePaths, Files.FileType imageFileType) {
		if (SharedLibraryLoader.os == Os.MacOsX)
			return;
		
		final Pixmap[] pixmaps = new Pixmap[imagePaths.length];
		for (int i = 0; i < imagePaths.length; i++)
			pixmaps[i] = new Pixmap(Micro.files.getFileHandle(imagePaths[i], imageFileType));
		
		setIcon(windowHandle, pixmaps);
		
		for (Pixmap pixmap : pixmaps) {
			pixmap.dispose();
		}
	}
	
	static void setIcon(long windowHandle, Pixmap[] images) {
		if (SharedLibraryLoader.os == Os.MacOsX)
			return;
		if (GLFW.glfwGetPlatform() == GLFW.GLFW_PLATFORM_WAYLAND)
			return;
		
		GLFWImage.Buffer buffer = GLFWImage.malloc(images.length);
		Pixmap[] tmpPixmaps = new Pixmap[images.length];
		
		for (int i = 0; i < images.length; i++) {
			Pixmap pixmap = images[i];
			
			if (pixmap.getFormat() != Pixmap.Format.RGBA8888) {
				Pixmap rgba = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), Pixmap.Format.RGBA8888);
				rgba.setBlending(Pixmap.Blending.None);
				rgba.drawPixmap(pixmap, 0, 0);
				tmpPixmaps[i] = rgba;
				pixmap = rgba;
			}
			
			GLFWImage icon = GLFWImage.malloc();
			icon.set(pixmap.getWidth(), pixmap.getHeight(), pixmap.getPixels());
			buffer.put(icon);
			
			icon.free();
		}
		
		buffer.position(0);
		GLFW.glfwSetWindowIcon(windowHandle, buffer);
		
		buffer.free();
		for (Pixmap pixmap : tmpPixmaps) {
			if (pixmap != null) {
				pixmap.dispose();
			}
		}
		
	}
	
	static void setSizeLimits(long windowHandle, int minWidth, int minHeight, int maxWidth, int maxHeight) {
		GLFW.glfwSetWindowSizeLimits(windowHandle, minWidth > -1 ? minWidth : GLFW.GLFW_DONT_CARE,
				minHeight > -1 ? minHeight : GLFW.GLFW_DONT_CARE, maxWidth > -1 ? maxWidth : GLFW.GLFW_DONT_CARE,
				maxHeight > -1 ? maxHeight : GLFW.GLFW_DONT_CARE);
	}
	
	void create(long windowHandle) {
		this.windowHandle = windowHandle;
		this.input = application.createInput(this);
		this.graphics = new Lwjgl3Graphics(this);
		
		GLFW.glfwSetWindowFocusCallback(windowHandle, focusCallback);
		GLFW.glfwSetWindowIconifyCallback(windowHandle, iconifyCallback);
		GLFW.glfwSetWindowMaximizeCallback(windowHandle, maximizeCallback);
		GLFW.glfwSetWindowCloseCallback(windowHandle, closeCallback);
		GLFW.glfwSetDropCallback(windowHandle, dropCallback);
		GLFW.glfwSetWindowRefreshCallback(windowHandle, refreshCallback);
		
		if (windowListener != null) {
			windowListener.created(this);
		}
	}
	
	public ApplicationListener getListener() {
		return listener;
	}
	
	public Lwjgl3WindowListener getWindowListener() {
		return windowListener;
	}
	
	public void setWindowListener(Lwjgl3WindowListener listener) {
		this.windowListener = listener;
	}
	
	public void postRunnable(Runnable runnable) {
		synchronized (runnables) {
			runnables.add(runnable);
		}
	}
	
	public void setPosition(int x, int y) {
		if (GLFW.glfwGetPlatform() == GLFW.GLFW_PLATFORM_WAYLAND)
			return;
		GLFW.glfwSetWindowPos(windowHandle, x, y);
	}
	
	public int getPositionX() {
		GLFW.glfwGetWindowPos(windowHandle, tmpBuffer, tmpBuffer2);
		return tmpBuffer.get(0);
	}
	
	public int getPositionY() {
		GLFW.glfwGetWindowPos(windowHandle, tmpBuffer, tmpBuffer2);
		return tmpBuffer2.get(0);
	}
	
	public void setVisible(boolean visible) {
		if (visible)
			GLFW.glfwShowWindow(windowHandle);
		else
			GLFW.glfwHideWindow(windowHandle);
	}
	
	public void closeWindow() {
		GLFW.glfwSetWindowShouldClose(windowHandle, true);
	}
	
	/**
	 * Minimizes (iconifies) the window. Iconified windows do not call their {@link ApplicationListener} until the window is
	 * restored.
	 */
	public void iconifyWindow() {
		GLFW.glfwIconifyWindow(windowHandle);
	}
	
	/**
	 * Whether the window is iconfieid
	 */
	public boolean isIconified() {
		return iconified;
	}
	
	/**
	 * De-minimizes (de-iconifies) and de-maximizes the window.
	 */
	public void restoreWindow() {
		GLFW.glfwRestoreWindow(windowHandle);
	}
	
	/**
	 * Maximizes the window.
	 */
	public void maximizeWindow() {
		GLFW.glfwMaximizeWindow(windowHandle);
	}
	
	public void focusWindow() {
		GLFW.glfwFocusWindow(windowHandle);
	}
	
	public boolean isFocused() {
		return focused;
	}
	
	public void setIcon(Pixmap... image) {
		setIcon(windowHandle, image);
	}
	
	public void setTitle(CharSequence title) {
		GLFW.glfwSetWindowTitle(windowHandle, title);
	}
	
	public void setSizeLimits(int minWidth, int minHeight, int maxWidth, int maxHeight) {
		setSizeLimits(windowHandle, minWidth, minHeight, maxWidth, maxHeight);
	}
	
	Lwjgl3Graphics getGraphics() {
		return graphics;
	}
	
	Lwjgl3Input getInput() {
		return input;
	}
	
	public long getWindowHandle() {
		return windowHandle;
	}
	
	void windowHandleChanged(long windowHandle) {
		this.windowHandle = windowHandle;
		input.windowHandleChanged(windowHandle);
	}
	
	boolean update() {
		if (!listenerInitialized) {
			initializeListener();
		}
		synchronized (runnables) {
			executedRunnables.addAll(runnables);
			runnables.clear();
		}
		for (Runnable runnable : executedRunnables) {
			runnable.run();
		}
		boolean shouldRender = executedRunnables.size > 0 || graphics.isContinuousRendering();
		executedRunnables.clear();
		
		if (!iconified)
			input.update();
		
		synchronized (this) {
			shouldRender |= requestRendering && !iconified;
			requestRendering = false;
		}
		
		// In case glfw_async is used, we need to resize outside the GLFW
		if (asyncResized) {
			asyncResized = false;
			graphics.updateFramebufferInfo();
			graphics.gl20.glViewport(0, 0, graphics.getBackBufferWidth(), graphics.getBackBufferHeight());
			listener.resize(graphics.getWidth(), graphics.getHeight());
			graphics.update();
			listener.render();
			GLFW.glfwSwapBuffers(windowHandle);
			return true;
		}
		
		if (shouldRender) {
			graphics.update();
			listener.render();
			GLFW.glfwSwapBuffers(windowHandle);
		}
		
		if (!iconified)
			input.prepareNext();
		
		return shouldRender;
	}
	
	void requestRendering() {
		synchronized (this) {
			this.requestRendering = true;
		}
	}
	
	boolean shouldClose() {
		return GLFW.glfwWindowShouldClose(windowHandle);
	}
	
	Lwjgl3ApplicationConfiguration getConfig() {
		return config;
	}
	
	boolean isListenerInitialized() {
		return listenerInitialized;
	}
	
	void initializeListener() {
		if (!listenerInitialized) {
			listener.create();
			listener.resize(graphics.getWidth(), graphics.getHeight());
			listenerInitialized = true;
		}
	}
	
	void makeCurrent() {
		Micro.graphics = graphics;
		Micro.gl32 = graphics.getGL32();
		Micro.gl31 = Micro.gl32 != null ? Micro.gl32 : graphics.getGL31();
		Micro.gl30 = Micro.gl31 != null ? Micro.gl31 : graphics.getGL30();
		Micro.gl20 = Micro.gl30 != null ? Micro.gl30 : graphics.getGL20();
		Micro.gl = Micro.gl20;
		Micro.input = input;
		
		GLFW.glfwMakeContextCurrent(windowHandle);
	}
	
	@Override
	public void dispose() {
		listener.pause();
		listener.dispose();
		Lwjgl3Cursor.dispose(this);
		graphics.dispose();
		input.dispose();
		GLFW.glfwSetWindowFocusCallback(windowHandle, null);
		GLFW.glfwSetWindowIconifyCallback(windowHandle, null);
		GLFW.glfwSetWindowCloseCallback(windowHandle, null);
		GLFW.glfwSetDropCallback(windowHandle, null);
		GLFW.glfwDestroyWindow(windowHandle);
		
		focusCallback.free();
		iconifyCallback.free();
		maximizeCallback.free();
		closeCallback.free();
		dropCallback.free();
		refreshCallback.free();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Long.hashCode(windowHandle);
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Lwjgl3Window other = (Lwjgl3Window) obj;
		return windowHandle == other.windowHandle;
	}
	
	public void flash() {
		GLFW.glfwRequestWindowAttention(windowHandle);
	}
	
}
