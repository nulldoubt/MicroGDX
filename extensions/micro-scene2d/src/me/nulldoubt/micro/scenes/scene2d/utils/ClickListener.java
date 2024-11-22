package me.nulldoubt.micro.scenes.scene2d.utils;

import me.nulldoubt.micro.Input.Buttons;
import me.nulldoubt.micro.scenes.scene2d.Actor;
import me.nulldoubt.micro.scenes.scene2d.InputEvent;
import me.nulldoubt.micro.scenes.scene2d.InputListener;

public class ClickListener extends InputListener {
	
	/**
	 * Time in seconds {@link #isVisualPressed()} reports true after a press resulting in a click is released.
	 */
	public static float visualPressedDuration = 0.1f;
	
	private float tapSquareSize = 14, touchDownX = -1, touchDownY = -1;
	private int pressedPointer = -1;
	private int pressedButton = -1;
	private int button;
	private boolean pressed, over, cancelled;
	private long visualPressedTime;
	private long tapCountInterval = (long) (0.4f * 1000000000l);
	private int tapCount;
	private long lastTapTime;
	
	/**
	 * Create a listener where {@link #clicked(InputEvent, float, float)} is only called for left clicks.
	 *
	 * @see #ClickListener(int)
	 */
	public ClickListener() {
	}
	
	/**
	 * @see #setButton(int)
	 */
	public ClickListener(int button) {
		this.button = button;
	}
	
	public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
		if (pressed)
			return false;
		if (pointer == 0 && this.button != -1 && button != this.button)
			return false;
		pressed = true;
		pressedPointer = pointer;
		pressedButton = button;
		touchDownX = x;
		touchDownY = y;
		setVisualPressed(true);
		return true;
	}
	
	public void touchDragged(InputEvent event, float x, float y, int pointer) {
		if (pointer != pressedPointer || cancelled)
			return;
		pressed = isOver(event.getListenerActor(), x, y);
		if (!pressed) {
			// Once outside the tap square, don't use the tap square anymore.
			invalidateTapSquare();
		}
	}
	
	public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
		if (pointer == pressedPointer) {
			if (!cancelled) {
				boolean touchUpOver = isOver(event.getListenerActor(), x, y);
				// Ignore touch up if the wrong mouse button.
				if (touchUpOver && pointer == 0 && this.button != -1 && button != this.button)
					touchUpOver = false;
				if (touchUpOver) {
					long time = System.nanoTime();
					if (time - lastTapTime > tapCountInterval)
						tapCount = 0;
					tapCount++;
					lastTapTime = time;
					clicked(event, x, y);
				}
			}
			pressed = false;
			pressedPointer = -1;
			pressedButton = -1;
			cancelled = false;
		}
	}
	
	public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
		if (pointer == -1 && !cancelled)
			over = true;
	}
	
	public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
		if (pointer == -1 && !cancelled)
			over = false;
	}
	
	public void cancel() {
		if (pressedPointer == -1)
			return;
		cancelled = true;
		pressed = false;
	}
	
	public void clicked(InputEvent event, float x, float y) {
	}
	
	/**
	 * Returns true if the specified position is over the specified actor or within the tap square.
	 */
	public boolean isOver(Actor actor, float x, float y) {
		Actor hit = actor.hit(x, y, true);
		if (hit == null || !hit.isDescendantOf(actor))
			return inTapSquare(x, y);
		return true;
	}
	
	public boolean inTapSquare(float x, float y) {
		if (touchDownX == -1 && touchDownY == -1)
			return false;
		return Math.abs(x - touchDownX) < tapSquareSize && Math.abs(y - touchDownY) < tapSquareSize;
	}
	
	/**
	 * Returns true if a touch is within the tap square.
	 */
	public boolean inTapSquare() {
		return touchDownX != -1;
	}
	
	/**
	 * The tap square will no longer be used for the current touch.
	 */
	public void invalidateTapSquare() {
		touchDownX = -1;
		touchDownY = -1;
	}
	
	/**
	 * Returns true if a touch is over the actor or within the tap square.
	 */
	public boolean isPressed() {
		return pressed;
	}
	
	/**
	 * Returns true if a touch is over the actor or within the tap square or has been very recently. This allows the UI to show a
	 * press and release that was so fast it occurred within a single frame.
	 */
	public boolean isVisualPressed() {
		if (pressed)
			return true;
		if (visualPressedTime <= 0)
			return false;
		if (visualPressedTime > System.currentTimeMillis())
			return true;
		visualPressedTime = 0;
		return false;
	}
	
	/**
	 * If true, sets the visual pressed time to now. If false, clears the visual pressed time.
	 */
	public void setVisualPressed(boolean visualPressed) {
		if (visualPressed)
			visualPressedTime = System.currentTimeMillis() + (long) (visualPressedDuration * 1000);
		else
			visualPressedTime = 0;
	}
	
	/**
	 * Returns true if the mouse or touch is over the actor or pressed and within the tap square.
	 */
	public boolean isOver() {
		return over || pressed;
	}
	
	public void setTapSquareSize(float halfTapSquareSize) {
		tapSquareSize = halfTapSquareSize;
	}
	
	public float getTapSquareSize() {
		return tapSquareSize;
	}
	
	/**
	 * @param tapCountInterval time in seconds that must pass for two touch down/up sequences to be detected as consecutive
	 *                         taps.
	 */
	public void setTapCountInterval(float tapCountInterval) {
		this.tapCountInterval = (long) (tapCountInterval * 1000000000l);
	}
	
	/**
	 * Returns the number of taps within the tap count interval for the most recent click event.
	 */
	public int getTapCount() {
		return tapCount;
	}
	
	public void setTapCount(int tapCount) {
		this.tapCount = tapCount;
	}
	
	public float getTouchDownX() {
		return touchDownX;
	}
	
	public float getTouchDownY() {
		return touchDownY;
	}
	
	/**
	 * The button that initially pressed this button or -1 if the button is not pressed.
	 */
	public int getPressedButton() {
		return pressedButton;
	}
	
	/**
	 * The pointer that initially pressed this button or -1 if the button is not pressed.
	 */
	public int getPressedPointer() {
		return pressedPointer;
	}
	
	/**
	 * @see #setButton(int)
	 */
	public int getButton() {
		return button;
	}
	
	/**
	 */
	public void setButton(int button) {
		this.button = button;
	}
	
}
