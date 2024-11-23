package me.nulldoubt.micro.scenes.scene2d.utils;

import me.nulldoubt.micro.math.Interpolation;
import me.nulldoubt.micro.math.Vector2;
import me.nulldoubt.micro.scenes.scene2d.InputEvent;
import me.nulldoubt.micro.scenes.scene2d.ui.ScrollPane;
import me.nulldoubt.micro.utils.Timer;
import me.nulldoubt.micro.utils.Timer.Task;

public class DragScrollListener extends DragListener {
	
	static final Vector2 tmpCoords = new Vector2();
	
	private final ScrollPane scroll;
	private final Task scrollUp;
	private final Task scrollDown;
	Interpolation interpolation = Interpolation.exp5In;
	float minSpeed = 15, maxSpeed = 75, tickSecs = 0.05f;
	long startTime, rampTime = 1750;
	float padTop, padBottom;
	
	public DragScrollListener(final ScrollPane scroll) {
		this.scroll = scroll;
		
		scrollUp = new Task() {
			public void run() {
				scroll(scroll.getScrollY() - getScrollPixels());
			}
		};
		scrollDown = new Task() {
			public void run() {
				scroll(scroll.getScrollY() + getScrollPixels());
			}
		};
	}
	
	public void setup(float minSpeedPixels, float maxSpeedPixels, float tickSecs, float rampSecs) {
		this.minSpeed = minSpeedPixels;
		this.maxSpeed = maxSpeedPixels;
		this.tickSecs = tickSecs;
		rampTime = (long) (rampSecs * 1000);
	}
	
	float getScrollPixels() {
		return interpolation.apply(minSpeed, maxSpeed, Math.min(1, (System.currentTimeMillis() - startTime) / (float) rampTime));
	}
	
	public void drag(InputEvent event, float x, float y, int pointer) {
		event.getListenerActor().localToActorCoordinates(scroll, tmpCoords.set(x, y));
		if (isAbove(tmpCoords.y)) {
			scrollDown.cancel();
			if (!scrollUp.isScheduled()) {
				startTime = System.currentTimeMillis();
				Timer.schedule(scrollUp, tickSecs, tickSecs);
			}
			return;
		} else if (isBelow(tmpCoords.y)) {
			scrollUp.cancel();
			if (!scrollDown.isScheduled()) {
				startTime = System.currentTimeMillis();
				Timer.schedule(scrollDown, tickSecs, tickSecs);
			}
			return;
		}
		scrollUp.cancel();
		scrollDown.cancel();
	}
	
	public void dragStop(InputEvent event, float x, float y, int pointer) {
		scrollUp.cancel();
		scrollDown.cancel();
	}
	
	protected boolean isAbove(float y) {
		return y >= scroll.getHeight() - padTop;
	}
	
	protected boolean isBelow(float y) {
		return y < padBottom;
	}
	
	protected void scroll(float y) {
		scroll.setScrollY(y);
	}
	
	public void setPadding(float padTop, float padBottom) {
		this.padTop = padTop;
		this.padBottom = padBottom;
	}
	
}
