package me.nulldoubt.micro.scenes.scene2d.actions;

import me.nulldoubt.micro.scenes.scene2d.Action;

public class RemoveActorAction extends Action {
	
	private boolean removed;
	
	public boolean act(float delta) {
		if (!removed) {
			removed = true;
			target.remove();
		}
		return true;
	}
	
	public void restart() {
		removed = false;
	}
	
}
