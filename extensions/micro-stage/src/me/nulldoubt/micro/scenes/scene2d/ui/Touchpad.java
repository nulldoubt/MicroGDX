package me.nulldoubt.micro.scenes.scene2d.ui;

import me.nulldoubt.micro.graphics.Color;
import me.nulldoubt.micro.graphics.g2d.Batch;
import me.nulldoubt.micro.math.Vector2;
import me.nulldoubt.micro.math.shapes.Circle;
import me.nulldoubt.micro.scenes.scene2d.Actor;
import me.nulldoubt.micro.scenes.scene2d.InputEvent;
import me.nulldoubt.micro.scenes.scene2d.InputListener;
import me.nulldoubt.micro.scenes.scene2d.Touchable;
import me.nulldoubt.micro.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import me.nulldoubt.micro.scenes.scene2d.utils.Drawable;
import me.nulldoubt.micro.utils.pools.Pools;

public class Touchpad extends Widget {
	
	private TouchpadStyle style;
	boolean touched;
	boolean resetOnTouchUp = true;
	private float deadzoneRadius;
	private final Circle knobBounds = new Circle(0, 0, 0);
	private final Circle touchBounds = new Circle(0, 0, 0);
	private final Circle deadzoneBounds = new Circle(0, 0, 0);
	private final Vector2 knobPosition = new Vector2();
	private final Vector2 knobPercent = new Vector2();
	
	public Touchpad(float deadzoneRadius, Skin skin) {
		this(deadzoneRadius, skin.get(TouchpadStyle.class));
	}
	
	public Touchpad(float deadzoneRadius, Skin skin, String styleName) {
		this(deadzoneRadius, skin.get(styleName, TouchpadStyle.class));
	}
	
	public Touchpad(float deadzoneRadius, TouchpadStyle style) {
		if (deadzoneRadius < 0)
			throw new IllegalArgumentException("deadzoneRadius must be > 0");
		this.deadzoneRadius = deadzoneRadius;
		
		knobPosition.set(getWidth() / 2f, getHeight() / 2f);
		
		setStyle(style);
		setSize(getPrefWidth(), getPrefHeight());
		
		addListener(new InputListener() {
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if (touched)
					return false;
				touched = true;
				calculatePositionAndValue(x, y, false);
				return true;
			}
			
			public void touchDragged(InputEvent event, float x, float y, int pointer) {
				calculatePositionAndValue(x, y, false);
			}
			
			public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
				touched = false;
				calculatePositionAndValue(x, y, resetOnTouchUp);
			}
		});
	}
	
	void calculatePositionAndValue(float x, float y, boolean isTouchUp) {
		float oldPositionX = knobPosition.x;
		float oldPositionY = knobPosition.y;
		float oldPercentX = knobPercent.x;
		float oldPercentY = knobPercent.y;
		float centerX = knobBounds.x;
		float centerY = knobBounds.y;
		knobPosition.set(centerX, centerY);
		knobPercent.set(0f, 0f);
		if (!isTouchUp) {
			if (!deadzoneBounds.contains(x, y)) {
				knobPercent.set((x - centerX) / knobBounds.radius, (y - centerY) / knobBounds.radius);
				float length = knobPercent.len();
				if (length > 1)
					knobPercent.scl(1 / length);
				if (knobBounds.contains(x, y)) {
					knobPosition.set(x, y);
				} else {
					knobPosition.set(knobPercent).nor().scl(knobBounds.radius).add(knobBounds.x, knobBounds.y);
				}
			}
		}
		if (oldPercentX != knobPercent.x || oldPercentY != knobPercent.y) {
			ChangeEvent changeEvent = Pools.obtain(ChangeEvent.class, ChangeEvent::new);
			if (fire(changeEvent)) {
				knobPercent.set(oldPercentX, oldPercentY);
				knobPosition.set(oldPositionX, oldPositionY);
			}
			Pools.free(changeEvent);
		}
	}
	
	public void setStyle(TouchpadStyle style) {
		if (style == null)
			throw new IllegalArgumentException("style cannot be null");
		this.style = style;
		invalidateHierarchy();
	}
	
	public TouchpadStyle getStyle() {
		return style;
	}
	
	public Actor hit(float x, float y, boolean touchable) {
		if (touchable && this.getTouchable() != Touchable.enabled)
			return null;
		if (!isVisible())
			return null;
		return touchBounds.contains(x, y) ? this : null;
	}
	
	public void layout() {
		float halfWidth = getWidth() / 2;
		float halfHeight = getHeight() / 2;
		float radius = Math.min(halfWidth, halfHeight);
		touchBounds.set(halfWidth, halfHeight, radius);
		if (style.knob != null)
			radius -= Math.max(style.knob.getMinWidth(), style.knob.getMinHeight()) / 2;
		knobBounds.set(halfWidth, halfHeight, radius);
		deadzoneBounds.set(halfWidth, halfHeight, deadzoneRadius);
		knobPosition.set(halfWidth, halfHeight);
		knobPercent.set(0, 0);
	}
	
	public void draw(Batch batch, float parentAlpha) {
		validate();
		
		Color c = getColor();
		batch.setColor(c.r, c.g, c.b, c.a * parentAlpha);
		
		float x = getX();
		float y = getY();
		float w = getWidth();
		float h = getHeight();
		
		final Drawable bg = style.background;
		if (bg != null)
			bg.draw(batch, x, y, w, h);
		
		final Drawable knob = style.knob;
		if (knob != null) {
			x += knobPosition.x - knob.getMinWidth() / 2f;
			y += knobPosition.y - knob.getMinHeight() / 2f;
			knob.draw(batch, x, y, knob.getMinWidth(), knob.getMinHeight());
		}
	}
	
	public float getPrefWidth() {
		return style.background != null ? style.background.getMinWidth() : 0;
	}
	
	public float getPrefHeight() {
		return style.background != null ? style.background.getMinHeight() : 0;
	}
	
	public boolean isTouched() {
		return touched;
	}
	
	public boolean getResetOnTouchUp() {
		return resetOnTouchUp;
	}
	
	public void setResetOnTouchUp(boolean reset) {
		this.resetOnTouchUp = reset;
	}
	
	public void setDeadzone(float deadzoneRadius) {
		if (deadzoneRadius < 0)
			throw new IllegalArgumentException("deadzoneRadius must be > 0");
		this.deadzoneRadius = deadzoneRadius;
		invalidate();
	}
	
	public float getKnobX() {
		return knobPosition.x;
	}
	
	public float getKnobY() {
		return knobPosition.y;
	}
	
	public float getKnobPercentX() {
		return knobPercent.x;
	}
	
	public float getKnobPercentY() {
		return knobPercent.y;
	}
	
	public static class TouchpadStyle {
		
		public Drawable background;
		public Drawable knob;
		
		public TouchpadStyle() {}
		
		public TouchpadStyle(Drawable background, Drawable knob) {
			this.background = background;
			this.knob = knob;
		}
		
		public TouchpadStyle(TouchpadStyle style) {
			background = style.background;
			knob = style.knob;
		}
		
	}
	
}
