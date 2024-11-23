package me.nulldoubt.micro.scenes.scene2d.actions;

import me.nulldoubt.micro.scenes.scene2d.Action;
import me.nulldoubt.micro.utils.pools.Pool;

public class SequenceAction extends ParallelAction {
	
	private int index;
	
	public SequenceAction() {}
	
	public SequenceAction(Action action1) {
		addAction(action1);
	}
	
	public SequenceAction(Action action1, Action action2) {
		addAction(action1);
		addAction(action2);
	}
	
	public SequenceAction(Action action1, Action action2, Action action3) {
		addAction(action1);
		addAction(action2);
		addAction(action3);
	}
	
	public SequenceAction(Action action1, Action action2, Action action3, Action action4) {
		addAction(action1);
		addAction(action2);
		addAction(action3);
		addAction(action4);
	}
	
	public SequenceAction(Action action1, Action action2, Action action3, Action action4, Action action5) {
		addAction(action1);
		addAction(action2);
		addAction(action3);
		addAction(action4);
		addAction(action5);
	}
	
	public boolean act(float delta) {
		if (index >= actions.size)
			return true;
		Pool pool = getPool();
		setPool(null); // Ensure this action can't be returned to the pool while executings.
		try {
			if (actions.get(index).act(delta)) {
				if (actor == null)
					return true; // This action was removed.
				index++;
				if (index >= actions.size)
					return true;
			}
			return false;
		} finally {
			setPool(pool);
		}
	}
	
	public void restart() {
		super.restart();
		index = 0;
	}
	
}
