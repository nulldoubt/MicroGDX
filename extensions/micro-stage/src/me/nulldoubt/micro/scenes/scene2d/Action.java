package me.nulldoubt.micro.scenes.scene2d;

import me.nulldoubt.micro.utils.pools.Pool;
import me.nulldoubt.micro.utils.pools.Pool.Poolable;

public abstract class Action implements Poolable {
	
	protected Actor actor;
	
	protected Actor target;
	
	private Pool pool;
	
	public abstract boolean act(float delta);
	
	public void restart() {}
	
	public void setActor(Actor actor) {
		this.actor = actor;
		if (target == null)
			setTarget(actor);
		if (actor == null) {
			if (pool != null) {
				pool.free(this);
				pool = null;
			}
		}
	}
	
	public Actor getActor() {
		return actor;
	}
	
	public void setTarget(Actor target) {
		this.target = target;
	}
	
	public Actor getTarget() {
		return target;
	}
	
	public void reset() {
		actor = null;
		target = null;
		pool = null;
		restart();
	}
	
	public Pool getPool() {
		return pool;
	}
	
	public void setPool(Pool pool) {
		this.pool = pool;
	}
	
	public String toString() {
		String name = getClass().getName();
		int dotIndex = name.lastIndexOf('.');
		if (dotIndex != -1)
			name = name.substring(dotIndex + 1);
		if (name.endsWith("Action"))
			name = name.substring(0, name.length() - 6);
		return name;
	}
	
}
