package me.nulldoubt.micro.scenes.scene2d.ui;

import me.nulldoubt.micro.Input.Keys;
import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.math.Interpolation;
import me.nulldoubt.micro.scenes.scene2d.Actor;
import me.nulldoubt.micro.scenes.scene2d.InputEvent;
import me.nulldoubt.micro.scenes.scene2d.InputListener;
import me.nulldoubt.micro.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import me.nulldoubt.micro.scenes.scene2d.utils.Drawable;
import me.nulldoubt.micro.utils.pools.Pools;

public class Slider extends ProgressBar {
	
	int button = -1;
	int draggingPointer = -1;
	boolean mouseOver;
	private Interpolation visualInterpolationInverse = Interpolation.linear;
	private float[] snapValues;
	private float threshold;
	
	public Slider(float min, float max, float stepSize, boolean vertical, Skin skin) {
		this(min, max, stepSize, vertical, skin.get("default-" + (vertical ? "vertical" : "horizontal"), SliderStyle.class));
	}
	
	public Slider(float min, float max, float stepSize, boolean vertical, Skin skin, String styleName) {
		this(min, max, stepSize, vertical, skin.get(styleName, SliderStyle.class));
	}
	
	public Slider(float min, float max, float stepSize, boolean vertical, SliderStyle style) {
		super(min, max, stepSize, vertical, style);
		
		addListener(new InputListener() {
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if (disabled)
					return false;
				if (Slider.this.button != -1 && Slider.this.button != button)
					return false;
				if (draggingPointer != -1)
					return false;
				draggingPointer = pointer;
				calculatePositionAndValue(x, y);
				return true;
			}
			
			public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
				if (pointer != draggingPointer)
					return;
				draggingPointer = -1;
				// The position is invalid when focus is cancelled
				if (event.isTouchFocusCancel() || !calculatePositionAndValue(x, y)) {
					// Fire an event on touchUp even if the value didn't change, so listeners can see when a drag ends via isDragging.
					ChangeEvent changeEvent = Pools.obtain(ChangeEvent.class, ChangeEvent::new);
					fire(changeEvent);
					Pools.free(changeEvent);
				}
			}
			
			public void touchDragged(InputEvent event, float x, float y, int pointer) {
				calculatePositionAndValue(x, y);
			}
			
			public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
				if (pointer == -1)
					mouseOver = true;
			}
			
			public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
				if (pointer == -1)
					mouseOver = false;
			}
		});
	}
	
	public SliderStyle getStyle() {
		return (SliderStyle) super.getStyle();
	}
	
	public boolean isOver() {
		return mouseOver;
	}
	
	protected Drawable getBackgroundDrawable() {
		SliderStyle style = (SliderStyle) super.getStyle();
		if (disabled && style.disabledBackground != null)
			return style.disabledBackground;
		if (isDragging() && style.backgroundDown != null)
			return style.backgroundDown;
		if (mouseOver && style.backgroundOver != null)
			return style.backgroundOver;
		return style.background;
	}
	
	protected Drawable getKnobDrawable() {
		SliderStyle style = (SliderStyle) super.getStyle();
		if (disabled && style.disabledKnob != null)
			return style.disabledKnob;
		if (isDragging() && style.knobDown != null)
			return style.knobDown;
		if (mouseOver && style.knobOver != null)
			return style.knobOver;
		return style.knob;
	}
	
	protected Drawable getKnobBeforeDrawable() {
		SliderStyle style = (SliderStyle) super.getStyle();
		if (disabled && style.disabledKnobBefore != null)
			return style.disabledKnobBefore;
		if (isDragging() && style.knobBeforeDown != null)
			return style.knobBeforeDown;
		if (mouseOver && style.knobBeforeOver != null)
			return style.knobBeforeOver;
		return style.knobBefore;
	}
	
	protected Drawable getKnobAfterDrawable() {
		SliderStyle style = (SliderStyle) super.getStyle();
		if (disabled && style.disabledKnobAfter != null)
			return style.disabledKnobAfter;
		if (isDragging() && style.knobAfterDown != null)
			return style.knobAfterDown;
		if (mouseOver && style.knobAfterOver != null)
			return style.knobAfterOver;
		return style.knobAfter;
	}
	
	boolean calculatePositionAndValue(float x, float y) {
		SliderStyle style = getStyle();
		Drawable knob = style.knob;
		Drawable bg = getBackgroundDrawable();
		
		float value;
		float oldPosition = position;
		
		float min = getMinValue();
		float max = getMaxValue();
		
		if (vertical) {
			float height = getHeight() - bg.getTopHeight() - bg.getBottomHeight();
			float knobHeight = knob == null ? 0 : knob.getMinHeight();
			position = y - bg.getBottomHeight() - knobHeight * 0.5f;
			value = min + (max - min) * visualInterpolationInverse.apply(position / (height - knobHeight));
			position = Math.max(Math.min(0, bg.getBottomHeight()), position);
			position = Math.min(height - knobHeight, position);
		} else {
			float width = getWidth() - bg.getLeftWidth() - bg.getRightWidth();
			float knobWidth = knob == null ? 0 : knob.getMinWidth();
			position = x - bg.getLeftWidth() - knobWidth * 0.5f;
			value = min + (max - min) * visualInterpolationInverse.apply(position / (width - knobWidth));
			position = Math.max(Math.min(0, bg.getLeftWidth()), position);
			position = Math.min(width - knobWidth, position);
		}
		
		float oldValue = value;
		if (!Micro.input.isKeyPressed(Keys.SHIFT_LEFT) && !Micro.input.isKeyPressed(Keys.SHIFT_RIGHT))
			value = snap(value);
		boolean valueSet = setValue(value);
		if (value == oldValue)
			position = oldPosition;
		return valueSet;
	}
	
	protected float snap(float value) {
		if (snapValues == null || snapValues.length == 0)
			return value;
		float bestDiff = -1, bestValue = 0;
		for (float snapValue : snapValues) {
			float diff = Math.abs(value - snapValue);
			if (diff <= threshold) {
				if (bestDiff == -1 || diff < bestDiff) {
					bestDiff = diff;
					bestValue = snapValue;
				}
			}
		}
		return bestDiff == -1 ? value : bestValue;
	}
	
	public void setSnapToValues(float threshold, float... values) {
		if (values != null && values.length == 0)
			throw new IllegalArgumentException("values cannot be empty.");
		this.snapValues = values;
		this.threshold = threshold;
	}
	
	/**
	 * Makes this progress bar snap to the specified values, if the knob is within the threshold.
	 *
	 * @param values May be null to disable snapping.
	 * @deprecated Use {@link #setSnapToValues(float, float...)}.
	 */
	@Deprecated
	public void setSnapToValues(float[] values, float threshold) {
		setSnapToValues(threshold, values);
	}
	
	public float[] getSnapToValues() {
		return snapValues;
	}
	
	public float getSnapToValuesThreshold() {
		return threshold;
	}
	
	/**
	 * Returns true if the slider is being dragged.
	 */
	public boolean isDragging() {
		return draggingPointer != -1;
	}
	
	/**
	 * Sets the mouse button, which can trigger a change of the slider. Is -1, so every button, by default.
	 */
	public void setButton(int button) {
		this.button = button;
	}
	
	/**
	 * Sets the inverse interpolation to use for display. This should perform the inverse of the
	 * {@link #setVisualInterpolation(Interpolation) visual interpolation}.
	 */
	public void setVisualInterpolationInverse(Interpolation interpolation) {
		this.visualInterpolationInverse = interpolation;
	}
	
	/**
	 * Sets the value using the specified visual percent.
	 *
	 * @see #setVisualInterpolation(Interpolation)
	 */
	public void setVisualPercent(float percent) {
		setValue(min + (max - min) * visualInterpolationInverse.apply(percent));
	}
	
	/**
	 * The style for a slider, see {@link Slider}.
	 *
	 * @author mzechner
	 * @author Nathan Sweet
	 */
	public static class SliderStyle extends ProgressBarStyle {
		
		public Drawable backgroundOver, backgroundDown;
		public Drawable knobOver, knobDown;
		public Drawable knobBeforeOver, knobBeforeDown;
		public Drawable knobAfterOver, knobAfterDown;
		
		public SliderStyle() {
		}
		
		public SliderStyle(Drawable background, Drawable knob) {
			super(background, knob);
		}
		
		public SliderStyle(SliderStyle style) {
			super(style);
			backgroundOver = style.backgroundOver;
			backgroundDown = style.backgroundDown;
			
			knobOver = style.knobOver;
			knobDown = style.knobDown;
			
			knobBeforeOver = style.knobBeforeOver;
			knobBeforeDown = style.knobBeforeDown;
			
			knobAfterOver = style.knobAfterOver;
			knobAfterDown = style.knobAfterDown;
		}
		
	}
	
}
