package me.nulldoubt.micro.scenes.scene2d.ui;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.graphics.g2d.Batch;
import me.nulldoubt.micro.scenes.scene2d.Actor;
import me.nulldoubt.micro.scenes.scene2d.InputEvent;
import me.nulldoubt.micro.scenes.scene2d.Stage;
import me.nulldoubt.micro.scenes.scene2d.Touchable;
import me.nulldoubt.micro.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import me.nulldoubt.micro.scenes.scene2d.utils.ClickListener;
import me.nulldoubt.micro.scenes.scene2d.utils.Disableable;
import me.nulldoubt.micro.scenes.scene2d.utils.Drawable;
import me.nulldoubt.micro.utils.collections.Array;
import me.nulldoubt.micro.utils.pools.Pools;

public class Button extends Table implements Disableable {
	
	private ButtonStyle style;
	boolean isChecked, isDisabled;
	ButtonGroup buttonGroup;
	private ClickListener clickListener;
	private boolean programmaticChangeEvents = true;
	
	public Button(Skin skin) {
		super(skin);
		initialize();
		setStyle(skin.get(ButtonStyle.class));
		setSize(getPrefWidth(), getPrefHeight());
	}
	
	public Button(Skin skin, String styleName) {
		super(skin);
		initialize();
		setStyle(skin.get(styleName, ButtonStyle.class));
		setSize(getPrefWidth(), getPrefHeight());
	}
	
	public Button(Actor child, Skin skin, String styleName) {
		this(child, skin.get(styleName, ButtonStyle.class));
		setSkin(skin);
	}
	
	public Button(Actor child, ButtonStyle style) {
		initialize();
		add(child);
		setStyle(style);
		setSize(getPrefWidth(), getPrefHeight());
	}
	
	public Button(ButtonStyle style) {
		initialize();
		setStyle(style);
		setSize(getPrefWidth(), getPrefHeight());
	}
	
	public Button() {
		initialize();
	}
	
	private void initialize() {
		setTouchable(Touchable.enabled);
		addListener(clickListener = new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				if (isDisabled())
					return;
				setChecked(!isChecked, true);
			}
		});
	}
	
	public Button(Drawable up) {
		this(new ButtonStyle(up, null, null));
	}
	
	public Button(Drawable up, Drawable down) {
		this(new ButtonStyle(up, down, null));
	}
	
	public Button(Drawable up, Drawable down, Drawable checked) {
		this(new ButtonStyle(up, down, checked));
	}
	
	public Button(Actor child, Skin skin) {
		this(child, skin.get(ButtonStyle.class));
	}
	
	public void setChecked(boolean isChecked) {
		setChecked(isChecked, programmaticChangeEvents);
	}
	
	void setChecked(boolean isChecked, boolean fireEvent) {
		if (this.isChecked == isChecked)
			return;
		if (buttonGroup != null && !buttonGroup.canCheck(this, isChecked))
			return;
		this.isChecked = isChecked;
		
		if (fireEvent) {
			ChangeEvent changeEvent = Pools.obtain(ChangeEvent.class, ChangeEvent::new);
			if (fire(changeEvent))
				this.isChecked = !isChecked;
			Pools.free(changeEvent);
		}
	}
	
	public void toggle() {
		setChecked(!isChecked);
	}
	
	public boolean isChecked() {
		return isChecked;
	}
	
	public boolean isPressed() {
		return clickListener.isVisualPressed();
	}
	
	public boolean isOver() {
		return clickListener.isOver();
	}
	
	public ClickListener getClickListener() {
		return clickListener;
	}
	
	public boolean isDisabled() {
		return isDisabled;
	}
	
	public void setDisabled(boolean isDisabled) {
		this.isDisabled = isDisabled;
	}
	
	public void setProgrammaticChangeEvents(boolean programmaticChangeEvents) {
		this.programmaticChangeEvents = programmaticChangeEvents;
	}
	
	public void setStyle(ButtonStyle style) {
		if (style == null)
			throw new IllegalArgumentException("style cannot be null.");
		this.style = style;
		
		setBackground(getBackgroundDrawable());
	}
	
	public ButtonStyle getStyle() {
		return style;
	}
	
	public ButtonGroup getButtonGroup() {
		return buttonGroup;
	}
	
	protected Drawable getBackgroundDrawable() {
		if (isDisabled() && style.disabled != null)
			return style.disabled;
		if (isPressed()) {
			if (isChecked() && style.checkedDown != null)
				return style.checkedDown;
			if (style.down != null)
				return style.down;
		}
		if (isOver()) {
			if (isChecked()) {
				if (style.checkedOver != null)
					return style.checkedOver;
			} else {
				if (style.over != null)
					return style.over;
			}
		}
		boolean focused = hasKeyboardFocus();
		if (isChecked()) {
			if (focused && style.checkedFocused != null)
				return style.checkedFocused;
			if (style.checked != null)
				return style.checked;
			if (isOver() && style.over != null)
				return style.over;
		}
		if (focused && style.focused != null)
			return style.focused;
		return style.up;
	}
	
	public void draw(Batch batch, float parentAlpha) {
		validate();
		
		setBackground(getBackgroundDrawable());
		
		float offsetX = 0, offsetY = 0;
		if (isPressed() && !isDisabled()) {
			offsetX = style.pressedOffsetX;
			offsetY = style.pressedOffsetY;
		} else if (isChecked() && !isDisabled()) {
			offsetX = style.checkedOffsetX;
			offsetY = style.checkedOffsetY;
		} else {
			offsetX = style.unpressedOffsetX;
			offsetY = style.unpressedOffsetY;
		}
		boolean offset = offsetX != 0 || offsetY != 0;
		
		Array<Actor> children = getChildren();
		if (offset) {
			for (int i = 0; i < children.size; i++)
				children.get(i).moveBy(offsetX, offsetY);
		}
		super.draw(batch, parentAlpha);
		if (offset) {
			for (int i = 0; i < children.size; i++)
				children.get(i).moveBy(-offsetX, -offsetY);
		}
		
		Stage stage = getStage();
		if (stage != null && stage.getActionsRequestRendering() && isPressed() != clickListener.isPressed())
			Micro.graphics.requestRendering();
	}
	
	public float getPrefWidth() {
		float width = super.getPrefWidth();
		if (style.up != null)
			width = Math.max(width, style.up.getMinWidth());
		if (style.down != null)
			width = Math.max(width, style.down.getMinWidth());
		if (style.checked != null)
			width = Math.max(width, style.checked.getMinWidth());
		return width;
	}
	
	public float getPrefHeight() {
		float height = super.getPrefHeight();
		if (style.up != null)
			height = Math.max(height, style.up.getMinHeight());
		if (style.down != null)
			height = Math.max(height, style.down.getMinHeight());
		if (style.checked != null)
			height = Math.max(height, style.checked.getMinHeight());
		return height;
	}
	
	public float getMinWidth() {
		return getPrefWidth();
	}
	
	public float getMinHeight() {
		return getPrefHeight();
	}
	
	public static class ButtonStyle {
		
		public Drawable up, down, over, focused, disabled;
		public Drawable checked, checkedOver, checkedDown, checkedFocused;
		public float pressedOffsetX, pressedOffsetY, unpressedOffsetX, unpressedOffsetY, checkedOffsetX, checkedOffsetY;
		
		public ButtonStyle() {
		}
		
		public ButtonStyle(Drawable up, Drawable down, Drawable checked) {
			this.up = up;
			this.down = down;
			this.checked = checked;
		}
		
		public ButtonStyle(ButtonStyle style) {
			up = style.up;
			down = style.down;
			over = style.over;
			focused = style.focused;
			disabled = style.disabled;
			
			checked = style.checked;
			checkedOver = style.checkedOver;
			checkedDown = style.checkedDown;
			checkedFocused = style.checkedFocused;
			
			pressedOffsetX = style.pressedOffsetX;
			pressedOffsetY = style.pressedOffsetY;
			unpressedOffsetX = style.unpressedOffsetX;
			unpressedOffsetY = style.unpressedOffsetY;
			checkedOffsetX = style.checkedOffsetX;
			checkedOffsetY = style.checkedOffsetY;
		}
		
	}
	
}
