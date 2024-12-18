package me.nulldoubt.micro.backends.android.surfaceview;

import android.view.View;

public class RatioResolutionStrategy implements ResolutionStrategy {
	
	private final float ratio;
	
	public RatioResolutionStrategy(float ratio) {
		this.ratio = ratio;
	}
	
	public RatioResolutionStrategy(final float width, final float height) {
		this.ratio = width / height;
	}
	
	@Override
	public MeasuredDimension calcMeasures(int widthMeasureSpec, int heightMeasureSpec) {
		
		final int specWidth = View.MeasureSpec.getSize(widthMeasureSpec);
		final int specHeight = View.MeasureSpec.getSize(heightMeasureSpec);
		
		final float desiredRatio = ratio;
		final float realRatio = (float) specWidth / specHeight;
		
		int width;
		int height;
		if (realRatio < desiredRatio) {
			width = specWidth;
			height = Math.round(width / desiredRatio);
		} else {
			height = specHeight;
			width = Math.round(height * desiredRatio);
		}
		
		return new MeasuredDimension(width, height);
	}
	
}
