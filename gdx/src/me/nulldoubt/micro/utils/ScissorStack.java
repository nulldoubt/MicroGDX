package me.nulldoubt.micro.utils;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.graphics.Camera;
import me.nulldoubt.micro.graphics.GL20;
import me.nulldoubt.micro.graphics.glutils.HdpiUtils;
import me.nulldoubt.micro.math.Matrix4;
import me.nulldoubt.micro.math.shapes.Rectangle;
import me.nulldoubt.micro.math.Vector3;

public class ScissorStack {
	
	private static final Array<Rectangle> scissors = new Array<>();
	static Vector3 tmp = new Vector3();
	static final Rectangle viewport = new Rectangle();
	
	public static boolean pushScissors(Rectangle scissor) {
		fix(scissor);
		
		if (scissors.size == 0) {
			if (scissor.width < 1 || scissor.height < 1)
				return false;
			Micro.gl.glEnable(GL20.GL_SCISSOR_TEST);
		} else {
			// merge scissors
			Rectangle parent = scissors.get(scissors.size - 1);
			float minX = Math.max(parent.x, scissor.x);
			float maxX = Math.min(parent.x + parent.width, scissor.x + scissor.width);
			if (maxX - minX < 1)
				return false;
			
			float minY = Math.max(parent.y, scissor.y);
			float maxY = Math.min(parent.y + parent.height, scissor.y + scissor.height);
			if (maxY - minY < 1)
				return false;
			
			scissor.x = minX;
			scissor.y = minY;
			scissor.width = maxX - minX;
			scissor.height = Math.max(1, maxY - minY);
		}
		scissors.add(scissor);
		HdpiUtils.glScissor((int) scissor.x, (int) scissor.y, (int) scissor.width, (int) scissor.height);
		return true;
	}
	
	/**
	 * Pops the current scissor rectangle from the stack and sets the new scissor area to the new top of stack rectangle. In case
	 * no more rectangles are on the stack, {@link GL20#GL_SCISSOR_TEST} is disabled.
	 * <p>
	 * Any drawing should be flushed before popping scissors.
	 */
	public static Rectangle popScissors() {
		Rectangle old = scissors.pop();
		if (scissors.size == 0)
			Micro.gl.glDisable(GL20.GL_SCISSOR_TEST);
		else {
			Rectangle scissor = scissors.peek();
			HdpiUtils.glScissor((int) scissor.x, (int) scissor.y, (int) scissor.width, (int) scissor.height);
		}
		return old;
	}
	
	public static Rectangle peekScissors() {
		if (scissors.size == 0)
			return null;
		return scissors.peek();
	}
	
	private static void fix(Rectangle rect) {
		rect.x = Math.round(rect.x);
		rect.y = Math.round(rect.y);
		rect.width = Math.round(rect.width);
		rect.height = Math.round(rect.height);
		if (rect.width < 0) {
			rect.width = -rect.width;
			rect.x -= rect.width;
		}
		if (rect.height < 0) {
			rect.height = -rect.height;
			rect.y -= rect.height;
		}
	}
	
	/**
	 * Calculates a scissor rectangle using 0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight() as the viewport.
	 *
	 * @see #calculateScissors(Camera, float, float, float, float, Matrix4, Rectangle, Rectangle)
	 */
	public static void calculateScissors(Camera camera, Matrix4 batchTransform, Rectangle area, Rectangle scissor) {
		calculateScissors(camera, 0, 0, Micro.graphics.getWidth(), Micro.graphics.getHeight(), batchTransform, area, scissor);
	}
	
	/**
	 * Calculates a scissor rectangle in OpenGL ES window coordinates from a {@link Camera}, a transformation {@link Matrix4} and
	 * an axis aligned {@link Rectangle}. The rectangle will get transformed by the camera and transform matrices and is then
	 * projected to screen coordinates. Note that only axis aligned rectangles will work with this method. If either the Camera or
	 * the Matrix4 have rotational components, the output of this method will not be suitable for
	 * {@link GL20#glScissor(int, int, int, int)}.
	 *
	 * @param camera         the {@link Camera}
	 * @param batchTransform the transformation {@link Matrix4}
	 * @param area           the {@link Rectangle} to transform to window coordinates
	 * @param scissor        the Rectangle to store the result in
	 */
	public static void calculateScissors(Camera camera, float viewportX, float viewportY, float viewportWidth,
										 float viewportHeight, Matrix4 batchTransform, Rectangle area, Rectangle scissor) {
		tmp.set(area.x, area.y, 0);
		tmp.mul(batchTransform);
		camera.project(tmp, viewportX, viewportY, viewportWidth, viewportHeight);
		scissor.x = tmp.x;
		scissor.y = tmp.y;
		
		tmp.set(area.x + area.width, area.y + area.height, 0);
		tmp.mul(batchTransform);
		camera.project(tmp, viewportX, viewportY, viewportWidth, viewportHeight);
		scissor.width = tmp.x - scissor.x;
		scissor.height = tmp.y - scissor.y;
	}
	
	public static Rectangle getViewport() {
		if (scissors.size == 0)
			viewport.set(0, 0, Micro.graphics.getWidth(), Micro.graphics.getHeight());
		else {
			Rectangle scissor = scissors.peek();
			viewport.set(scissor);
		}
		return viewport;
	}
	
}
