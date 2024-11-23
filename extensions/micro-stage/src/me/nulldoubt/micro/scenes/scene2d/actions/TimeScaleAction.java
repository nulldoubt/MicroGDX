package me.nulldoubt.micro.scenes.scene2d.actions;

public class TimeScaleAction extends DelegateAction {
	
	public float scale;
	
	protected boolean delegate(float delta) {
		if (action == null)
			return true;
		return action.act(delta * scale);
	}
	
}
