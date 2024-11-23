package me.nulldoubt.micro.scenes.scene2d.actions;

public class ScaleToAction extends TemporalAction {
	
	private float startX, startY;
	private float endX, endY;
	
	protected void begin() {
		startX = target.getScaleX();
		startY = target.getScaleY();
	}
	
	protected void update(float percent) {
		float x, y;
		if (percent == 0) {
			x = startX;
			y = startY;
		} else if (percent == 1) {
			x = endX;
			y = endY;
		} else {
			x = startX + (endX - startX) * percent;
			y = startY + (endY - startY) * percent;
		}
		target.setScale(x, y);
	}
	
	public void setScale(float x, float y) {
		endX = x;
		endY = y;
	}
	
	public void setScale(float scale) {
		endX = scale;
		endY = scale;
	}
	
	public float getX() {
		return endX;
	}
	
	public void setX(float x) {
		this.endX = x;
	}
	
	public float getY() {
		return endY;
	}
	
	public void setY(float y) {
		this.endY = y;
	}
	
}
