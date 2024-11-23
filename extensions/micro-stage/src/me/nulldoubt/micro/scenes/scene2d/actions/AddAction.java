package me.nulldoubt.micro.scenes.scene2d.actions;

import me.nulldoubt.micro.scenes.scene2d.Action;

public class AddAction extends Action {
	
	private Action action;
	
	public boolean act(float delta) {
		target.addAction(action);
		return true;
	}
	
	public Action getAction() {
		return action;
	}
	
	public void setAction(Action action) {
		this.action = action;
	}
	
	public void restart() {
		if (action != null)
			action.restart();
	}
	
	public void reset() {
		super.reset();
		action = null;
	}
	
}
