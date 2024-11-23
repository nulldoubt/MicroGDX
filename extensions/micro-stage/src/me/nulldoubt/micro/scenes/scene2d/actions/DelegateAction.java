package me.nulldoubt.micro.scenes.scene2d.actions;

import me.nulldoubt.micro.scenes.scene2d.Action;
import me.nulldoubt.micro.scenes.scene2d.Actor;
import me.nulldoubt.micro.utils.pools.Pool;

public abstract class DelegateAction extends Action {
	
	protected Action action;
	
	public void setAction(Action action) {
		this.action = action;
	}
	
	public Action getAction() {
		return action;
	}
	
	protected abstract boolean delegate(float delta);
	
	public final boolean act(float delta) {
		Pool pool = getPool();
		setPool(null); // Ensure this action can't be returned to the pool inside the delegate action.
		try {
			return delegate(delta);
		} finally {
			setPool(pool);
		}
	}
	
	public void restart() {
		if (action != null)
			action.restart();
	}
	
	public void reset() {
		super.reset();
		action = null;
	}
	
	public void setActor(Actor actor) {
		if (action != null)
			action.setActor(actor);
		super.setActor(actor);
	}
	
	public void setTarget(Actor target) {
		if (action != null)
			action.setTarget(target);
		super.setTarget(target);
	}
	
	public String toString() {
		return super.toString() + (action == null ? "" : "(" + action + ")");
	}
	
}
