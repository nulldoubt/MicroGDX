package me.nulldoubt.micro.input;

import me.nulldoubt.micro.InputProcessor;
import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.math.Vector2;
import me.nulldoubt.micro.utils.Timer;
import me.nulldoubt.micro.utils.Timer.Task;

public class GestureDetector implements InputProcessor {
	
	final GestureListener listener;
	private float tapRectangleWidth;
	private float tapRectangleHeight;
	private long tapCountInterval;
	private float longPressSeconds;
	private long maxFlingDelay;
	
	private boolean inTapRectangle;
	private int tapCount;
	private long lastTapTime;
	private float lastTapX, lastTapY;
	private int lastTapButton, lastTapPointer;
	boolean longPressFired;
	private boolean pinching;
	private boolean panning;
	
	private final VelocityTracker tracker = new VelocityTracker();
	private float tapRectangleCenterX, tapRectangleCenterY;
	private long touchDownTime;
	Vector2 pointer1 = new Vector2();
	private final Vector2 pointer2 = new Vector2();
	private final Vector2 initialPointer1 = new Vector2();
	private final Vector2 initialPointer2 = new Vector2();
	
	private final Task longPressTask = new Task() {
		@Override
		public void run() {
			if (!longPressFired)
				longPressFired = listener.longPress(pointer1.x, pointer1.y);
		}
	};
	
	public GestureDetector(GestureListener listener) {
		this(20, 0.4f, 1.1f, Integer.MAX_VALUE, listener);
	}
	
	public GestureDetector(float halfTapSquareSize, float tapCountInterval, float longPressDuration, float maxFlingDelay, GestureListener listener) {
		this(halfTapSquareSize, halfTapSquareSize, tapCountInterval, longPressDuration, maxFlingDelay, listener);
	}
	
	public GestureDetector(float halfTapRectangleWidth, float halfTapRectangleHeight, float tapCountInterval, float longPressDuration, float maxFlingDelay, GestureListener listener) {
		if (listener == null)
			throw new IllegalArgumentException("listener cannot be null.");
		this.tapRectangleWidth = halfTapRectangleWidth;
		this.tapRectangleHeight = halfTapRectangleHeight;
		this.tapCountInterval = (long) (tapCountInterval * 1000000000L);
		this.longPressSeconds = longPressDuration;
		this.maxFlingDelay = (long) (maxFlingDelay * 1000000000L);
		this.listener = listener;
	}
	
	@Override
	public boolean touchDown(int x, int y, int pointer, int button) {
		return touchDown((float) x, (float) y, pointer, button);
	}
	
	public boolean touchDown(float x, float y, int pointer, int button) {
		if (pointer > 1)
			return false;
		
		if (pointer == 0) {
			pointer1.set(x, y);
			touchDownTime = Micro.input.getCurrentEventTime();
			tracker.start(x, y, touchDownTime);
			if (Micro.input.isTouched(1)) {
				// Start pinch.
				inTapRectangle = false;
				pinching = true;
				initialPointer1.set(pointer1);
				initialPointer2.set(pointer2);
				longPressTask.cancel();
			} else {
				// Normal touch down.
				inTapRectangle = true;
				pinching = false;
				longPressFired = false;
				tapRectangleCenterX = x;
				tapRectangleCenterY = y;
				if (!longPressTask.isScheduled())
					Timer.schedule(longPressTask, longPressSeconds);
			}
		} else {
			// Start pinch.
			pointer2.set(x, y);
			inTapRectangle = false;
			pinching = true;
			initialPointer1.set(pointer1);
			initialPointer2.set(pointer2);
			longPressTask.cancel();
		}
		return listener.touchDown(x, y, pointer, button);
	}
	
	@Override
	public boolean touchDragged(int x, int y, int pointer) {
		return touchDragged((float) x, (float) y, pointer);
	}
	
	public boolean touchDragged(float x, float y, int pointer) {
		if (pointer > 1)
			return false;
		if (longPressFired)
			return false;
		
		if (pointer == 0)
			pointer1.set(x, y);
		else
			pointer2.set(x, y);
		
		// handle pinch zoom
		if (pinching) {
			boolean result = listener.pinch(initialPointer1, initialPointer2, pointer1, pointer2);
			return listener.zoom(initialPointer1.dst(initialPointer2), pointer1.dst(pointer2)) || result;
		}
		
		// update tracker
		tracker.update(x, y, Micro.input.getCurrentEventTime());
		
		// check if we are still tapping.
		if (inTapRectangle && !isWithinTapRectangle(x, y, tapRectangleCenterX, tapRectangleCenterY)) {
			longPressTask.cancel();
			inTapRectangle = false;
		}
		
		// if we have left the tap square, we are panning
		if (!inTapRectangle) {
			panning = true;
			return listener.pan(x, y, tracker.deltaX, tracker.deltaY);
		}
		
		return false;
	}
	
	@Override
	public boolean touchUp(int x, int y, int pointer, int button) {
		return touchUp((float) x, (float) y, pointer, button);
	}
	
	public boolean touchUp(float x, float y, int pointer, int button) {
		if (pointer > 1)
			return false;
		
		// check if we are still tapping.
		if (inTapRectangle && !isWithinTapRectangle(x, y, tapRectangleCenterX, tapRectangleCenterY))
			inTapRectangle = false;
		
		boolean wasPanning = panning;
		panning = false;
		
		longPressTask.cancel();
		if (longPressFired)
			return false;
		
		if (inTapRectangle) {
			// handle taps
			if (lastTapButton != button || lastTapPointer != pointer || System.nanoTime() - lastTapTime > tapCountInterval
					|| !isWithinTapRectangle(x, y, lastTapX, lastTapY))
				tapCount = 0;
			tapCount++;
			lastTapTime = System.nanoTime();
			lastTapX = x;
			lastTapY = y;
			lastTapButton = button;
			lastTapPointer = pointer;
			touchDownTime = 0;
			return listener.tap(x, y, tapCount, button);
		}
		
		if (pinching) {
			// handle pinch end
			pinching = false;
			listener.pinchStop();
			panning = true;
			// we are in pan mode again, reset velocity tracker
			if (pointer == 0) {
				// first pointer has lifted off, set up panning to use the second pointer...
				tracker.start(pointer2.x, pointer2.y, Micro.input.getCurrentEventTime());
			} else {
				// second pointer has lifted off, set up panning to use the first pointer...
				tracker.start(pointer1.x, pointer1.y, Micro.input.getCurrentEventTime());
			}
			return false;
		}
		
		// handle no longer panning
		boolean handled = false;
		if (wasPanning && !panning)
			handled = listener.panStop(x, y, pointer, button);
		
		// handle fling
		long time = Micro.input.getCurrentEventTime();
		if (time - touchDownTime <= maxFlingDelay) {
			tracker.update(x, y, time);
			handled = listener.fling(tracker.getVelocityX(), tracker.getVelocityY(), button) || handled;
		}
		touchDownTime = 0;
		return handled;
	}
	
	@Override
	public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
		cancel();
		return InputProcessor.super.touchCancelled(screenX, screenY, pointer, button);
	}
	
	public void cancel() {
		longPressTask.cancel();
		longPressFired = true;
	}
	
	public boolean isLongPressed() {
		return isLongPressed(longPressSeconds);
	}
	
	public boolean isLongPressed(float duration) {
		if (touchDownTime == 0)
			return false;
		return System.nanoTime() - touchDownTime > (long) (duration * 1000000000L);
	}
	
	public boolean isPanning() {
		return panning;
	}
	
	public void reset() {
		longPressTask.cancel();
		touchDownTime = 0;
		panning = false;
		inTapRectangle = false;
		tracker.lastTime = 0;
	}
	
	private boolean isWithinTapRectangle(float x, float y, float centerX, float centerY) {
		return Math.abs(x - centerX) < tapRectangleWidth && Math.abs(y - centerY) < tapRectangleHeight;
	}
	
	public void invalidateTapSquare() {
		inTapRectangle = false;
	}
	
	public void setTapSquareSize(float halfTapSquareSize) {
		setTapRectangleSize(halfTapSquareSize, halfTapSquareSize);
	}
	
	public void setTapRectangleSize(float halfTapRectangleWidth, float halfTapRectangleHeight) {
		this.tapRectangleWidth = halfTapRectangleWidth;
		this.tapRectangleHeight = halfTapRectangleHeight;
	}
	
	public void setTapCountInterval(float tapCountInterval) {
		this.tapCountInterval = (long) (tapCountInterval * 1000000000L);
	}
	
	public void setLongPressSeconds(float longPressSeconds) {
		this.longPressSeconds = longPressSeconds;
	}
	
	public void setMaxFlingDelay(long maxFlingDelay) {
		this.maxFlingDelay = maxFlingDelay;
	}
	
	public interface GestureListener {
		
		default boolean touchDown(float x, float y, int pointer, int button) {
			return false;
		}
		
		default boolean tap(float x, float y, int count, int button) {
			return false;
		}
		
		default boolean longPress(float x, float y) {
			return false;
		}
		
		default boolean fling(float velocityX, float velocityY, int button) {
			return false;
		}
		
		default boolean pan(float x, float y, float deltaX, float deltaY) {
			return false;
		}
		
		default boolean panStop(float x, float y, int pointer, int button) {
			return false;
		}
		
		default boolean zoom(float initialDistance, float distance) {
			return false;
		}
		
		default boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
			return false;
		}
		
		default void pinchStop() {}
		
	}
	
	static class VelocityTracker {
		
		int sampleSize = 10;
		float lastX, lastY;
		float deltaX, deltaY;
		long lastTime;
		int numSamples;
		float[] meanX = new float[sampleSize];
		float[] meanY = new float[sampleSize];
		long[] meanTime = new long[sampleSize];
		
		public void start(float x, float y, long timeStamp) {
			lastX = x;
			lastY = y;
			deltaX = 0;
			deltaY = 0;
			numSamples = 0;
			for (int i = 0; i < sampleSize; i++) {
				meanX[i] = 0;
				meanY[i] = 0;
				meanTime[i] = 0;
			}
			lastTime = timeStamp;
		}
		
		public void update(float x, float y, long currTime) {
			deltaX = x - lastX;
			deltaY = y - lastY;
			lastX = x;
			lastY = y;
			long deltaTime = currTime - lastTime;
			lastTime = currTime;
			int index = numSamples % sampleSize;
			meanX[index] = deltaX;
			meanY[index] = deltaY;
			meanTime[index] = deltaTime;
			numSamples++;
		}
		
		public float getVelocityX() {
			float meanX = getAverage(this.meanX, numSamples);
			float meanTime = getAverage(this.meanTime, numSamples) / 1000000000.0f;
			if (meanTime == 0)
				return 0;
			return meanX / meanTime;
		}
		
		public float getVelocityY() {
			float meanY = getAverage(this.meanY, numSamples);
			float meanTime = getAverage(this.meanTime, numSamples) / 1000000000.0f;
			if (meanTime == 0)
				return 0;
			return meanY / meanTime;
		}
		
		private float getAverage(float[] values, int numSamples) {
			numSamples = Math.min(sampleSize, numSamples);
			float sum = 0;
			for (int i = 0; i < numSamples; i++) {
				sum += values[i];
			}
			return sum / numSamples;
		}
		
		private long getAverage(long[] values, int numSamples) {
			numSamples = Math.min(sampleSize, numSamples);
			long sum = 0;
			for (int i = 0; i < numSamples; i++) {
				sum += values[i];
			}
			if (numSamples == 0)
				return 0;
			return sum / numSamples;
		}
		
		private float getSum(float[] values, int numSamples) {
			numSamples = Math.min(sampleSize, numSamples);
			float sum = 0;
			for (int i = 0; i < numSamples; i++) {
				sum += values[i];
			}
			if (numSamples == 0)
				return 0;
			return sum;
		}
		
	}
	
}
