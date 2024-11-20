package me.nulldoubt.micro.math;

public interface Interpolation {
	
	float apply(float a);
	
	default float apply(float start, float end, float a) {
		return start + (end - start) * apply(a);
	}
	
	Interpolation linear = a -> a;
	
	Interpolation smooth = a -> a * a * (3 - 2 * a);
	Interpolation smooth2 = a -> {
		a = a * a * (3 - 2 * a);
		return a * a * (3 - 2 * a);
	};
	
	Interpolation smoother = a -> a * a * a * (a * (a * 6 - 15) + 10);
	Interpolation fade = smoother;
	
	Pow pow2 = new Pow(2);
	
	PowIn pow2In = new PowIn(2);
	PowIn slowFast = pow2In;
	
	PowOut pow2Out = new PowOut(2);
	PowOut fastSlow = pow2Out;
	Interpolation pow2InInverse = a -> {
		if (a < MathUtils.FLOAT_ROUNDING_ERROR)
			return 0;
		return (float) Math.sqrt(a);
	};
	Interpolation pow2OutInverse = a -> {
		if (a < MathUtils.FLOAT_ROUNDING_ERROR)
			return 0;
		if (a > 1)
			return 1;
		return 1 - (float) Math.sqrt(-(a - 1));
	};
	
	Pow pow3 = new Pow(3);
	PowIn pow3In = new PowIn(3);
	PowOut pow3Out = new PowOut(3);
	Interpolation pow3InInverse = a -> (float) Math.cbrt(a);
	Interpolation pow3OutInverse = a -> 1 - (float) Math.cbrt(-(a - 1));
	
	Pow pow4 = new Pow(4);
	PowIn pow4In = new PowIn(4);
	PowOut pow4Out = new PowOut(4);
	
	Pow pow5 = new Pow(5);
	PowIn pow5In = new PowIn(5);
	PowOut pow5Out = new PowOut(5);
	
	Interpolation sine = a -> (1 - MathUtils.cos(a * MathUtils.PI)) / 2;
	
	Interpolation sineIn = a -> 1 - MathUtils.cos(a * MathUtils.HALF_PI);
	
	Interpolation sineOut = a -> MathUtils.sin(a * MathUtils.HALF_PI);
	
	Exp exp10 = new Exp(2, 10);
	ExpIn exp10In = new ExpIn(2, 10);
	ExpOut exp10Out = new ExpOut(2, 10);
	
	Exp exp5 = new Exp(2, 5);
	ExpIn exp5In = new ExpIn(2, 5);
	ExpOut exp5Out = new ExpOut(2, 5);
	
	Interpolation circle = a -> {
		if (a <= 0.5f) {
			a *= 2;
			return (1 - (float) Math.sqrt(1 - a * a)) / 2;
		}
		a--;
		a *= 2;
		return ((float) Math.sqrt(1 - a * a) + 1) / 2;
	};
	
	Interpolation circleIn = a -> 1 - (float) Math.sqrt(1 - a * a);
	
	Interpolation circleOut = a -> {
		a--;
		return (float) Math.sqrt(1 - a * a);
	};
	
	Elastic elastic = new Elastic(2, 10, 7, 1);
	ElasticIn elasticIn = new ElasticIn(2, 10, 6, 1);
	ElasticOut elasticOut = new ElasticOut(2, 10, 7, 1);
	
	Swing swing = new Swing(1.5f);
	SwingIn swingIn = new SwingIn(2f);
	SwingOut swingOut = new SwingOut(2f);
	
	Bounce bounce = new Bounce(4);
	BounceIn bounceIn = new BounceIn(4);
	BounceOut bounceOut = new BounceOut(4);
	
	//
	
	class Pow implements Interpolation {
		
		final int power;
		
		private Pow(int power) {
			this.power = power;
		}
		
		public float apply(float a) {
			if (a <= 0.5f)
				return (float) Math.pow(a * 2, power) / 2;
			return (float) Math.pow((a - 1) * 2, power) / (power % 2 == 0 ? -2 : 2) + 1;
		}
		
	}
	
	class PowIn extends Pow {
		
		private PowIn(int power) {
			super(power);
		}
		
		public float apply(float a) {
			return (float) Math.pow(a, power);
		}
		
	}
	
	class PowOut extends Pow {
		
		private PowOut(int power) {
			super(power);
		}
		
		public float apply(float a) {
			return (float) Math.pow(a - 1, power) * (power % 2 == 0 ? -1 : 1) + 1;
		}
		
	}
	
	//
	
	class Exp implements Interpolation {
		
		final float value, power, min, scale;
		
		private Exp(float value, float power) {
			this.value = value;
			this.power = power;
			min = (float) Math.pow(value, -power);
			scale = 1 / (1 - min);
		}
		
		public float apply(float a) {
			if (a <= 0.5f)
				return ((float) Math.pow(value, power * (a * 2 - 1)) - min) * scale / 2;
			return (2 - ((float) Math.pow(value, -power * (a * 2 - 1)) - min) * scale) / 2;
		}
		
	}
	
	;
	
	class ExpIn extends Exp {
		
		private ExpIn(float value, float power) {
			super(value, power);
		}
		
		public float apply(float a) {
			return ((float) Math.pow(value, power * (a - 1)) - min) * scale;
		}
		
	}
	
	class ExpOut extends Exp {
		
		private ExpOut(float value, float power) {
			super(value, power);
		}
		
		public float apply(float a) {
			return 1 - ((float) Math.pow(value, -power * a) - min) * scale;
		}
		
	}
	
	//
	
	class Elastic implements Interpolation {
		
		final float value, power, scale, bounces;
		
		private Elastic(float value, float power, int bounces, float scale) {
			this.value = value;
			this.power = power;
			this.scale = scale;
			this.bounces = bounces * MathUtils.PI * (bounces % 2 == 0 ? 1 : -1);
		}
		
		public float apply(float a) {
			if (a <= 0.5f) {
				a *= 2;
				return (float) Math.pow(value, power * (a - 1)) * MathUtils.sin(a * bounces) * scale / 2;
			}
			a = 1 - a;
			a *= 2;
			return 1 - (float) Math.pow(value, power * (a - 1)) * MathUtils.sin((a) * bounces) * scale / 2;
		}
		
	}
	
	class ElasticIn extends Elastic {
		
		private ElasticIn(float value, float power, int bounces, float scale) {
			super(value, power, bounces, scale);
		}
		
		public float apply(float a) {
			if (a >= 0.99)
				return 1;
			return (float) Math.pow(value, power * (a - 1)) * MathUtils.sin(a * bounces) * scale;
		}
		
	}
	
	class ElasticOut extends Elastic {
		
		private ElasticOut(float value, float power, int bounces, float scale) {
			super(value, power, bounces, scale);
		}
		
		public float apply(float a) {
			if (a == 0)
				return 0;
			a = 1 - a;
			return (1 - (float) Math.pow(value, power * (a - 1)) * MathUtils.sin(a * bounces) * scale);
		}
		
	}
	
	//
	
	class Bounce extends BounceOut {
		
		private Bounce(float[] widths, float[] heights) {
			super(widths, heights);
		}
		
		public Bounce(int bounces) {
			super(bounces);
		}
		
		private float out(float a) {
			float test = a + widths[0] / 2;
			if (test < widths[0])
				return test / (widths[0] / 2) - 1;
			return super.apply(a);
		}
		
		public float apply(float a) {
			if (a <= 0.5f)
				return (1 - out(1 - a * 2)) / 2;
			return out(a * 2 - 1) / 2 + 0.5f;
		}
		
	}
	
	class BounceOut implements Interpolation {
		
		final float[] widths, heights;
		
		public BounceOut(float[] widths, float[] heights) {
			if (widths.length != heights.length)
				throw new IllegalArgumentException("Must be the same number of widths and heights.");
			this.widths = widths;
			this.heights = heights;
		}
		
		private BounceOut(int bounces) {
			if (bounces < 2 || bounces > 5)
				throw new IllegalArgumentException("bounces cannot be < 2 or > 5: " + bounces);
			widths = new float[bounces];
			heights = new float[bounces];
			heights[0] = 1;
			switch (bounces) {
				case 2:
					widths[0] = 0.6f;
					widths[1] = 0.4f;
					heights[1] = 0.33f;
					break;
				case 3:
					widths[0] = 0.4f;
					widths[1] = 0.4f;
					widths[2] = 0.2f;
					heights[1] = 0.33f;
					heights[2] = 0.1f;
					break;
				case 4:
					widths[0] = 0.34f;
					widths[1] = 0.34f;
					widths[2] = 0.2f;
					widths[3] = 0.15f;
					heights[1] = 0.26f;
					heights[2] = 0.11f;
					heights[3] = 0.03f;
					break;
				case 5:
					widths[0] = 0.3f;
					widths[1] = 0.3f;
					widths[2] = 0.2f;
					widths[3] = 0.1f;
					widths[4] = 0.1f;
					heights[1] = 0.45f;
					heights[2] = 0.3f;
					heights[3] = 0.15f;
					heights[4] = 0.06f;
					break;
			}
			widths[0] *= 2;
		}
		
		public float apply(float a) {
			if (a == 1)
				return 1;
			a += widths[0] / 2;
			float width = 0, height = 0;
			for (int i = 0, n = widths.length; i < n; i++) {
				width = widths[i];
				if (a <= width) {
					height = heights[i];
					break;
				}
				a -= width;
			}
			a /= width;
			float z = 4 / width * height * a;
			return 1 - (z - z * a) * width;
		}
		
	}
	
	class BounceIn extends BounceOut {
		
		private BounceIn(float[] widths, float[] heights) {
			super(widths, heights);
		}
		
		public BounceIn(int bounces) {
			super(bounces);
		}
		
		public float apply(float a) {
			return 1 - super.apply(1 - a);
		}
		
	}
	
	//
	
	class Swing implements Interpolation {
		
		private final float scale;
		
		private Swing(float scale) {
			this.scale = scale * 2;
		}
		
		public float apply(float a) {
			if (a <= 0.5f) {
				a *= 2;
				return a * a * ((scale + 1) * a - scale) / 2;
			}
			a--;
			a *= 2;
			return a * a * ((scale + 1) * a + scale) / 2 + 1;
		}
		
	}
	
	class SwingOut implements Interpolation {
		
		private final float scale;
		
		private SwingOut(float scale) {
			this.scale = scale;
		}
		
		public float apply(float a) {
			a--;
			return a * a * ((scale + 1) * a + scale) + 1;
		}
		
	}
	
	class SwingIn implements Interpolation {
		
		private final float scale;
		
		private SwingIn(float scale) {
			this.scale = scale;
		}
		
		public float apply(float a) {
			return a * a * ((scale + 1) * a - scale);
		}
		
	}
	
}
