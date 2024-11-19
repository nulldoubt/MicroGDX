package me.nulldoubt.micro.graphics.glutils;

import me.nulldoubt.micro.Micro;

public class HdpiUtils {
	
	private static HdpiMode mode = HdpiMode.Logical;
	
	public static void setMode(HdpiMode mode) {
		HdpiUtils.mode = mode;
	}
	
	public static void glScissor(int x, int y, int width, int height) {
		if (mode == HdpiMode.Logical && (Micro.graphics.getWidth() != Micro.graphics.getBackBufferWidth()
				|| Micro.graphics.getHeight() != Micro.graphics.getBackBufferHeight())) {
			Micro.gl.glScissor(toBackBufferX(x), toBackBufferY(y), toBackBufferX(width), toBackBufferY(height));
		} else {
			Micro.gl.glScissor(x, y, width, height);
		}
	}
	
	public static void glViewport(int x, int y, int width, int height) {
		if (mode == HdpiMode.Logical && (Micro.graphics.getWidth() != Micro.graphics.getBackBufferWidth() || Micro.graphics.getHeight() != Micro.graphics.getBackBufferHeight()))
			Micro.gl.glViewport(toBackBufferX(x), toBackBufferY(y), toBackBufferX(width), toBackBufferY(height));
		else
			Micro.gl.glViewport(x, y, width, height);
	}
	
	public static int toLogicalX(int backBufferX) {
		return (int) (backBufferX * Micro.graphics.getWidth() / (float) Micro.graphics.getBackBufferWidth());
	}
	
	public static int toLogicalY(int backBufferY) {
		return (int) (backBufferY * Micro.graphics.getHeight() / (float) Micro.graphics.getBackBufferHeight());
	}
	
	public static int toBackBufferX(int logicalX) {
		return (int) (logicalX * Micro.graphics.getBackBufferWidth() / (float) Micro.graphics.getWidth());
	}
	
	public static int toBackBufferY(int logicalY) {
		return (int) (logicalY * Micro.graphics.getBackBufferHeight() / (float) Micro.graphics.getHeight());
	}
	
	public enum HdpiMode {
		
		Logical,
		Pixels
		
	}
	
}
