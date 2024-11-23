package me.nulldoubt.micro.scenes.scene2d.actions;

public abstract class RelativeTemporalAction extends TemporalAction {
	
	private float lastPercent;
	
	protected void begin() {
		lastPercent = 0;
	}
	
	protected void update(float percent) {
		updateRelative(percent - lastPercent);
		lastPercent = percent;
	}
	
	protected abstract void updateRelative(float percentDelta);
	
}
