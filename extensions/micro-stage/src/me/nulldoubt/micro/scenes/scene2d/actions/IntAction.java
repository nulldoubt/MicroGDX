package me.nulldoubt.micro.scenes.scene2d.actions;

import me.nulldoubt.micro.math.Interpolation;

public class IntAction extends TemporalAction {
	
	public int start, end, value;
	
	public IntAction() {
		start = 0;
		end = 1;
	}
	
	public IntAction(int start, int end) {
		this.start = start;
		this.end = end;
	}
	
	public IntAction(int start, int end, float duration) {
		super(duration);
		this.start = start;
		this.end = end;
	}
	
	public IntAction(int start, int end, float duration, Interpolation interpolation) {
		super(duration, interpolation);
		this.start = start;
		this.end = end;
	}
	
	protected void begin() {
		value = start;
	}
	
	protected void update(float percent) {
		if (percent == 0)
			value = start;
		else if (percent == 1)
			value = end;
		else
			value = (int) (start + (end - start) * percent);
	}
	
}
