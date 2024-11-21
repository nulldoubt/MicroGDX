package me.nulldoubt.micro.graphics.profiling;

import me.nulldoubt.micro.Graphics;
import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.graphics.GL30;
import me.nulldoubt.micro.graphics.GL31;
import me.nulldoubt.micro.graphics.GL32;
import me.nulldoubt.micro.math.FloatCounter;

public class GLProfiler {
	
	private final Graphics graphics;
	private final GLInterceptor glInterceptor;
	private GLErrorListener listener;
	private boolean enabled = false;
	
	/**
	 * Create a new instance of GLProfiler to monitor a {@link Graphics} instance's gl calls
	 *
	 * @param graphics instance to monitor with this instance, With Lwjgl 2.x you can pass in Gdx.graphics, with Lwjgl3 use
	 *                 Lwjgl3Window.getGraphics()
	 */
	public GLProfiler(Graphics graphics) {
		this.graphics = graphics;
		GL32 gl32 = graphics.getGL32();
		GL31 gl31 = graphics.getGL31();
		GL30 gl30 = graphics.getGL30();
		if (gl32 != null) {
			glInterceptor = new GL32Interceptor(this, gl32);
		} else if (gl31 != null) {
			glInterceptor = new GL31Interceptor(this, gl31);
		} else if (gl30 != null) {
			glInterceptor = new GL30Interceptor(this, gl30);
		} else {
			glInterceptor = new GL20Interceptor(this, graphics.getGL20());
		}
		listener = GLErrorListener.LOGGING_LISTENER;
	}
	
	/**
	 * Enables profiling by replacing the {@code GL20} and {@code GL30} instances with profiling ones.
	 */
	public void enable() {
		if (enabled)
			return;
		
		if (glInterceptor instanceof GL32) {
			graphics.setGL32((GL32) glInterceptor);
		}
		if (glInterceptor instanceof GL31) {
			graphics.setGL31((GL31) glInterceptor);
		}
		if (glInterceptor instanceof GL30) {
			graphics.setGL30((GL30) glInterceptor);
		}
		graphics.setGL20(glInterceptor);
		
		Micro.gl32 = graphics.getGL32();
		Micro.gl31 = graphics.getGL31();
		Micro.gl30 = graphics.getGL30();
		Micro.gl20 = graphics.getGL20();
		Micro.gl = graphics.getGL20();
		
		enabled = true;
	}
	
	/**
	 * Disables profiling by resetting the {@code GL20} and {@code GL30} instances with the original ones.
	 */
	public void disable() {
		if (!enabled)
			return;
		
		if (glInterceptor instanceof GL32Interceptor) {
			graphics.setGL32(((GL32Interceptor) glInterceptor).gl32);
		}
		if (glInterceptor instanceof GL31Interceptor) {
			graphics.setGL31(((GL31Interceptor) glInterceptor).gl31);
		}
		if (glInterceptor instanceof GL30Interceptor) {
			graphics.setGL30(((GL30Interceptor) glInterceptor).gl30);
		}
		if (glInterceptor instanceof GL20Interceptor) {
			graphics.setGL20(((GL20Interceptor) graphics.getGL20()).gl20);
		}
		
		Micro.gl32 = graphics.getGL32();
		Micro.gl31 = graphics.getGL31();
		Micro.gl30 = graphics.getGL30();
		Micro.gl20 = graphics.getGL20();
		Micro.gl = graphics.getGL20();
		
		enabled = false;
	}
	
	/**
	 * Set the current listener for the {@link GLProfiler} to {@code errorListener}
	 */
	public void setListener(GLErrorListener errorListener) {
		this.listener = errorListener;
	}
	
	/**
	 * @return the current {@link GLErrorListener}
	 */
	public GLErrorListener getListener() {
		return listener;
	}
	
	/**
	 * @return true if the GLProfiler is currently profiling
	 */
	public boolean isEnabled() {
		return enabled;
	}
	
	/**
	 * @return the total gl calls made since the last reset
	 */
	public int getCalls() {
		return glInterceptor.getCalls();
	}
	
	/**
	 * @return the total amount of texture bindings made since the last reset
	 */
	public int getTextureBindings() {
		return glInterceptor.getTextureBindings();
	}
	
	/**
	 * @return the total amount of draw calls made since the last reset
	 */
	public int getDrawCalls() {
		return glInterceptor.getDrawCalls();
	}
	
	/**
	 * @return the total amount of shader switches made since the last reset
	 */
	public int getShaderSwitches() {
		return glInterceptor.getShaderSwitches();
	}
	
	/**
	 * @return {@link FloatCounter} containing information about rendered vertices since the last reset
	 */
	public FloatCounter getVertexCount() {
		return glInterceptor.getVertexCount();
	}
	
	/**
	 * Will reset the statistical information which has been collected so far. This should be called after every frame. Error
	 * listener is kept as it is.
	 */
	public void reset() {
		glInterceptor.reset();
	}
	
}
