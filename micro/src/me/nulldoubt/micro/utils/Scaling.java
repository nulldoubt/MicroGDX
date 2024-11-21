package me.nulldoubt.micro.utils;

import me.nulldoubt.micro.math.Vector2;

public abstract class Scaling {
	
	protected static final Vector2 temp = new Vector2();
	
	public abstract Vector2 apply(float sourceWidth, float sourceHeight, float targetWidth, float targetHeight);
	
	public static final Scaling fit = new Scaling() {
		public Vector2 apply(float sourceWidth, float sourceHeight, float targetWidth, float targetHeight) {
			float targetRatio = targetHeight / targetWidth;
			float sourceRatio = sourceHeight / sourceWidth;
			float scale = targetRatio > sourceRatio ? targetWidth / sourceWidth : targetHeight / sourceHeight;
			temp.x = sourceWidth * scale;
			temp.y = sourceHeight * scale;
			return temp;
		}
	};
	
	public static final Scaling contain = new Scaling() {
		public Vector2 apply(float sourceWidth, float sourceHeight, float targetWidth, float targetHeight) {
			float targetRatio = targetHeight / targetWidth;
			float sourceRatio = sourceHeight / sourceWidth;
			float scale = targetRatio > sourceRatio ? targetWidth / sourceWidth : targetHeight / sourceHeight;
			if (scale > 1)
				scale = 1;
			temp.x = sourceWidth * scale;
			temp.y = sourceHeight * scale;
			return temp;
		}
	};
	
	public static final Scaling fill = new Scaling() {
		public Vector2 apply(float sourceWidth, float sourceHeight, float targetWidth, float targetHeight) {
			float targetRatio = targetHeight / targetWidth;
			float sourceRatio = sourceHeight / sourceWidth;
			float scale = targetRatio < sourceRatio ? targetWidth / sourceWidth : targetHeight / sourceHeight;
			temp.x = sourceWidth * scale;
			temp.y = sourceHeight * scale;
			return temp;
		}
	};
	
	public static final Scaling fillX = new Scaling() {
		public Vector2 apply(float sourceWidth, float sourceHeight, float targetWidth, float targetHeight) {
			float scale = targetWidth / sourceWidth;
			temp.x = sourceWidth * scale;
			temp.y = sourceHeight * scale;
			return temp;
		}
	};
	
	public static final Scaling fillY = new Scaling() {
		public Vector2 apply(float sourceWidth, float sourceHeight, float targetWidth, float targetHeight) {
			float scale = targetHeight / sourceHeight;
			temp.x = sourceWidth * scale;
			temp.y = sourceHeight * scale;
			return temp;
		}
	};
	
	public static final Scaling stretch = new Scaling() {
		public Vector2 apply(float sourceWidth, float sourceHeight, float targetWidth, float targetHeight) {
			temp.x = targetWidth;
			temp.y = targetHeight;
			return temp;
		}
	};
	
	public static final Scaling stretchX = new Scaling() {
		public Vector2 apply(float sourceWidth, float sourceHeight, float targetWidth, float targetHeight) {
			temp.x = targetWidth;
			temp.y = sourceHeight;
			return temp;
		}
	};
	
	public static final Scaling stretchY = new Scaling() {
		public Vector2 apply(float sourceWidth, float sourceHeight, float targetWidth, float targetHeight) {
			temp.x = sourceWidth;
			temp.y = targetHeight;
			return temp;
		}
	};
	
	public static final Scaling none = new Scaling() {
		public Vector2 apply(float sourceWidth, float sourceHeight, float targetWidth, float targetHeight) {
			temp.x = sourceWidth;
			temp.y = sourceHeight;
			return temp;
		}
	};
	
}
