package me.nulldoubt.micro.scenes.scene2d.actions;

public class SizeByAction extends RelativeTemporalAction {
	
	public float amountWidth, amountHeight;
	
	protected void updateRelative(float percentDelta) {
		target.sizeBy(amountWidth * percentDelta, amountHeight * percentDelta);
	}
	
	public void setAmount(float width, float height) {
		amountWidth = width;
		amountHeight = height;
	}
	
}
