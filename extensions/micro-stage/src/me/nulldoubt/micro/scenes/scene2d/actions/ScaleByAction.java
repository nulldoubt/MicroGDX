package me.nulldoubt.micro.scenes.scene2d.actions;

public class ScaleByAction extends RelativeTemporalAction {
	
	public float amountX, amountY;
	
	protected void updateRelative(float percentDelta) {
		target.scaleBy(amountX * percentDelta, amountY * percentDelta);
	}
	
	public void setAmount(float x, float y) {
		amountX = x;
		amountY = y;
	}
	
	public void setAmount(float scale) {
		amountX = scale;
		amountY = scale;
	}
	
}
