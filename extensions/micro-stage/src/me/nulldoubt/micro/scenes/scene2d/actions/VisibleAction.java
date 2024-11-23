package me.nulldoubt.micro.scenes.scene2d.actions;

import me.nulldoubt.micro.scenes.scene2d.Action;

public class VisibleAction extends Action {
	
	public boolean visible;
	
	public boolean act(final float delta) {
		target.setVisible(visible);
		return true;
	}
	
}
