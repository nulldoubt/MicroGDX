package me.nulldoubt.micro.scenes.scene2d.actions;

public class RotateByAction extends RelativeTemporalAction {
	
	public float amount;
	
	protected void updateRelative(float percentDelta) {
		target.rotateBy(amount * percentDelta);
	}
	
}
