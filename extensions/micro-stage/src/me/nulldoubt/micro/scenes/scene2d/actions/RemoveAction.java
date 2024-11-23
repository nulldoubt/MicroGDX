package me.nulldoubt.micro.scenes.scene2d.actions;

import me.nulldoubt.micro.scenes.scene2d.Action;

public class RemoveAction extends Action {
	
	private Action action;
	
	public boolean act(float delta) {
		target.removeAction(action);
		return true;
	}
	
	public Action getAction() {
		return action;
	}
	
	public void setAction(Action action) {
		this.action = action;
	}
	
	public void reset() {
		super.reset();
		action = null;
	}
	
}
