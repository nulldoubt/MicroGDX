package me.nulldoubt.micro.backends.android.surfaceview;

public interface ResolutionStrategy {
	
	MeasuredDimension calcMeasures(final int widthMeasureSpec, final int heightMeasureSpec);
	
	record MeasuredDimension(int width, int height) {}
	
}
