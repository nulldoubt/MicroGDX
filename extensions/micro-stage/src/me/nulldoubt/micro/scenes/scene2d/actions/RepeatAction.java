package me.nulldoubt.micro.scenes.scene2d.actions;

public class RepeatAction extends DelegateAction {
	
	public static final int FOREVER = -1;
	
	public int repeatCount;
	private int executedCount;
	private boolean finished;
	
	protected boolean delegate(float delta) {
		if (executedCount == repeatCount)
			return true;
		if (action.act(delta)) {
			if (finished)
				return true;
			if (repeatCount > 0)
				executedCount++;
			if (executedCount == repeatCount)
				return true;
			if (action != null)
				action.restart();
		}
		return false;
	}
	
	public void finish() {
		finished = true;
	}
	
	public void restart() {
		super.restart();
		executedCount = 0;
		finished = false;
	}
	
}
