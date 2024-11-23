package me.nulldoubt.micro.scenes.scene2d.actions;

import me.nulldoubt.micro.scenes.scene2d.Action;
import me.nulldoubt.micro.utils.pools.Pool;

public class RunnableAction extends Action {
	
	public Runnable runnable;
	private boolean ran;
	
	public boolean act(float delta) {
		if (!ran) {
			ran = true;
			run();
		}
		return true;
	}
	
	public void run() {
		Pool pool = getPool();
		setPool(null);
		try {
			runnable.run();
		} finally {
			setPool(pool);
		}
	}
	
	public void restart() {
		ran = false;
	}
	
	public void reset() {
		super.reset();
		runnable = null;
	}
	
}
