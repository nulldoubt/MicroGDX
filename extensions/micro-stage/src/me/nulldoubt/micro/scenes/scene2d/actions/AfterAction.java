package me.nulldoubt.micro.scenes.scene2d.actions;

import me.nulldoubt.micro.scenes.scene2d.Action;
import me.nulldoubt.micro.scenes.scene2d.Actor;
import me.nulldoubt.micro.utils.collections.Array;

public class AfterAction extends DelegateAction {
	
	private final Array<Action> waitForActions = new Array<>(false, 4);
	
	public void setTarget(Actor target) {
		if (target != null)
			waitForActions.addAll(target.getActions());
		super.setTarget(target);
	}
	
	public void restart() {
		super.restart();
		waitForActions.clear();
	}
	
	protected boolean delegate(float delta) {
		Array<Action> currentActions = target.getActions();
		if (currentActions.size == 1)
			waitForActions.clear();
		for (int i = waitForActions.size - 1; i >= 0; i--) {
			Action action = waitForActions.get(i);
			int index = currentActions.indexOf(action, true);
			if (index == -1)
				waitForActions.removeIndex(i);
		}
		if (waitForActions.size > 0)
			return false;
		return action.act(delta);
	}
	
}
