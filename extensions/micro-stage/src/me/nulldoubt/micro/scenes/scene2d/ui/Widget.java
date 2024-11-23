package me.nulldoubt.micro.scenes.scene2d.ui;

import me.nulldoubt.micro.graphics.g2d.Batch;
import me.nulldoubt.micro.scenes.scene2d.Actor;
import me.nulldoubt.micro.scenes.scene2d.Group;
import me.nulldoubt.micro.scenes.scene2d.Stage;
import me.nulldoubt.micro.scenes.scene2d.utils.Layout;

public class Widget extends Actor implements Layout {
	
	private boolean needsLayout = true;
	private boolean fillParent;
	private boolean layoutEnabled = true;
	
	public float getMinWidth() {
		return getPrefWidth();
	}
	
	public float getMinHeight() {
		return getPrefHeight();
	}
	
	public float getPrefWidth() {
		return 0;
	}
	
	public float getPrefHeight() {
		return 0;
	}
	
	public float getMaxWidth() {
		return 0;
	}
	
	public float getMaxHeight() {
		return 0;
	}
	
	public void setLayoutEnabled(boolean enabled) {
		layoutEnabled = enabled;
		if (enabled)
			invalidateHierarchy();
	}
	
	public void validate() {
		if (!layoutEnabled)
			return;
		
		Group parent = getParent();
		if (fillParent && parent != null) {
			float parentWidth, parentHeight;
			Stage stage = getStage();
			if (stage != null && parent == stage.getRoot()) {
				parentWidth = stage.getWidth();
				parentHeight = stage.getHeight();
			} else {
				parentWidth = parent.getWidth();
				parentHeight = parent.getHeight();
			}
			setSize(parentWidth, parentHeight);
		}
		
		if (!needsLayout)
			return;
		needsLayout = false;
		layout();
	}
	
	/**
	 * Returns true if the widget's layout has been {@link #invalidate() invalidated}.
	 */
	public boolean needsLayout() {
		return needsLayout;
	}
	
	public void invalidate() {
		needsLayout = true;
	}
	
	public void invalidateHierarchy() {
		if (!layoutEnabled)
			return;
		invalidate();
		Group parent = getParent();
		if (parent instanceof Layout)
			((Layout) parent).invalidateHierarchy();
	}
	
	protected void sizeChanged() {
		invalidate();
	}
	
	public void pack() {
		setSize(getPrefWidth(), getPrefHeight());
		validate();
	}
	
	public void setFillParent(boolean fillParent) {
		this.fillParent = fillParent;
	}
	
	/**
	 * If this method is overridden, the super method or {@link #validate()} should be called to ensure the widget is laid out.
	 */
	public void draw(Batch batch, float parentAlpha) {
		validate();
	}
	
	public void layout() {
	}
	
}
