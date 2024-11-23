package me.nulldoubt.micro.scenes.scene2d.actions;

public class MoveByAction extends RelativeTemporalAction {
	
	public float amountX, amountY;
	
	protected void updateRelative(float percentDelta) {
		target.moveBy(amountX * percentDelta, amountY * percentDelta);
	}
	
	public void setAmount(float x, float y) {
		amountX = x;
		amountY = y;
	}
	
}
