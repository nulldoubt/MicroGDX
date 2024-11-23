package me.nulldoubt.micro.scenes.scene2d.ui;

import me.nulldoubt.micro.graphics.g2d.Batch;
import me.nulldoubt.micro.scenes.scene2d.Actor;
import me.nulldoubt.micro.scenes.scene2d.Group;
import me.nulldoubt.micro.scenes.scene2d.Stage;
import me.nulldoubt.micro.scenes.scene2d.utils.Layout;
import me.nulldoubt.micro.utils.collections.SnapshotArray;

public class WidgetGroup extends Group implements Layout {
	
	private boolean needsLayout = true;
	private boolean fillParent;
	private boolean layoutEnabled = true;
	
	public WidgetGroup() {}
	
	public WidgetGroup(Actor... actors) {
		for (Actor actor : actors)
			addActor(actor);
	}
	
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
		setLayoutEnabled(this, enabled);
	}
	
	private void setLayoutEnabled(Group parent, boolean enabled) {
		SnapshotArray<Actor> children = parent.getChildren();
		for (int i = 0, n = children.size; i < n; i++) {
			Actor actor = children.get(i);
			if (actor instanceof Layout layout)
				layout.setLayoutEnabled(enabled);
			else if (actor instanceof Group group)
				setLayoutEnabled(group, enabled);
		}
	}
	
	public void validate() {
		if (!layoutEnabled)
			return;
		
		Group parent = getParent();
		if (fillParent && parent != null) {
			Stage stage = getStage();
			if (stage != null && parent == stage.getRoot())
				setSize(stage.getWidth(), stage.getHeight());
			else
				setSize(parent.getWidth(), parent.getHeight());
		}
		
		if (!needsLayout)
			return;
		needsLayout = false;
		layout();
		
		if (needsLayout) {
			if (parent instanceof WidgetGroup)
				return;
			for (int i = 0; i < 5; i++) {
				needsLayout = false;
				layout();
				if (!needsLayout)
					break;
			}
		}
	}
	
	public boolean needsLayout() {
		return needsLayout;
	}
	
	public void invalidate() {
		needsLayout = true;
	}
	
	public void invalidateHierarchy() {
		invalidate();
		Group parent = getParent();
		if (parent instanceof Layout)
			((Layout) parent).invalidateHierarchy();
	}
	
	protected void childrenChanged() {
		invalidateHierarchy();
	}
	
	protected void sizeChanged() {
		invalidate();
	}
	
	public void pack() {
		setSize(getPrefWidth(), getPrefHeight());
		validate();
		setSize(getPrefWidth(), getPrefHeight());
		validate();
	}
	
	public void setFillParent(boolean fillParent) {
		this.fillParent = fillParent;
	}
	
	public void layout() {
	}
	
	public Actor hit(float x, float y, boolean touchable) {
		validate();
		return super.hit(x, y, touchable);
	}
	
	public void draw(Batch batch, float parentAlpha) {
		validate();
		super.draw(batch, parentAlpha);
	}
	
}
