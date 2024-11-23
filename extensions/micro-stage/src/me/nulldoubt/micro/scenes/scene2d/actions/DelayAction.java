package me.nulldoubt.micro.scenes.scene2d.actions;

public class DelayAction extends DelegateAction {
	
	public float duration, time;
	
	public DelayAction() {}
	
	public DelayAction(float duration) {
		this.duration = duration;
	}
	
	protected boolean delegate(float delta) {
		if (time < duration) {
			time += delta;
			if (time < duration)
				return false;
			delta = time - duration;
		}
		if (action == null)
			return true;
		return action.act(delta);
	}
	
	public void finish() {
		time = duration;
	}
	
	public void restart() {
		super.restart();
		time = 0;
	}
	
}
