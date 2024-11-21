package me.nulldoubt.micro.math;

import java.util.Random;

public final class MathUtils {
	
	private MathUtils() {}
	
	public static final float nanoToSec = 1 / 1000000000f;
	
	public static final float FLOAT_ROUNDING_ERROR = 0.000001f; // 32 bits
	public static final float PI = (float) Math.PI;
	public static final float PI2 = PI * 2;
	public static final float HALF_PI = PI / 2;
	
	public static final float E = (float) Math.E;
	
	private static final int SIN_BITS = 14; // 16KB. Adjust for accuracy.
	private static final int SIN_MASK = ~(-1 << SIN_BITS);
	private static final int SIN_COUNT = SIN_MASK + 1;
	
	private static final float radFull = PI2;
	private static final float degFull = 360;
	private static final float radToIndex = SIN_COUNT / radFull;
	private static final float degToIndex = SIN_COUNT / degFull;
	
	public static final float radiansToDegrees = 180f / PI;
	public static final float radDeg = radiansToDegrees;
	
	public static final float degreesToRadians = PI / 180;
	public static final float degRad = degreesToRadians;
	
	private static class Sin {
		
		static final float[] table = new float[SIN_COUNT];
		
		static {
			for (int i = 0; i < SIN_COUNT; i++)
				table[i] = (float) Math.sin((i + 0.5f) / SIN_COUNT * radFull);
			// The four right angles get extra-precise values, because they are
			// the most likely to need to be correct.
			table[0] = 0f;
			table[(int) (90 * degToIndex) & SIN_MASK] = 1f;
			table[(int) (180 * degToIndex) & SIN_MASK] = 0f;
			table[(int) (270 * degToIndex) & SIN_MASK] = -1f;
		}
	}
	
	public static float sin(float radians) {
		return Sin.table[(int) (radians * radToIndex) & SIN_MASK];
	}
	
	public static float cos(float radians) {
		return Sin.table[(int) ((radians + HALF_PI) * radToIndex) & SIN_MASK];
	}
	
	public static float sinDeg(float degrees) {
		return Sin.table[(int) (degrees * degToIndex) & SIN_MASK];
	}
	
	public static float cosDeg(float degrees) {
		return Sin.table[(int) ((degrees + 90) * degToIndex) & SIN_MASK];
	}
	
	public static float tan(float radians) {
		radians /= PI;
		radians += 0.5f;
		radians -= (float) Math.floor(radians);
		radians -= 0.5f;
		radians *= PI;
		final float x2 = radians * radians, x4 = x2 * x2;
		return radians * ((0.0010582010582010583f) * x4 - (0.1111111111111111f) * x2 + 1f) / ((0.015873015873015872f) * x4 - (0.4444444444444444f) * x2 + 1f);
	}
	
	public static float tanDeg(float degrees) {
		degrees *= (1f / 180f);
		degrees += 0.5f;
		degrees -= (float) Math.floor(degrees);
		degrees -= 0.5f;
		degrees *= PI;
		final float x2 = degrees * degrees, x4 = x2 * x2;
		return degrees * ((0.0010582010582010583f) * x4 - (0.1111111111111111f) * x2 + 1f)
				/ ((0.015873015873015872f) * x4 - (0.4444444444444444f) * x2 + 1f);
	}
	
	public static float atanUnchecked(double i) {
		// We use double precision internally, because some constants need double precision.
		double n = Math.abs(i);
		// c uses the "equally-good" formulation that permits n to be from 0 to almost infinity.
		double c = (n - 1.0) / (n + 1.0);
		// The approximation needs 6 odd powers of c.
		double c2 = c * c;
		double c3 = c * c2;
		double c5 = c3 * c2;
		double c7 = c5 * c2;
		double c9 = c7 * c2;
		double c11 = c9 * c2;
		return (float) (Math.signum(i) * ((Math.PI * 0.25) + (0.99997726 * c - 0.33262347 * c3 + 0.19354346 * c5 - 0.11643287 * c7 + 0.05265332 * c9 - 0.0117212 * c11)));
	}
	
	public static float atan2(final float y, float x) {
		float n = y / x;
		if (n != n)
			n = (y == x ? 1f : -1f); // if both y and x are infinite, n would be NaN
		else if (n - n != n - n)
			x = 0f; // if n is infinite, y is infinitely larger than x.
		if (x > 0)
			return atanUnchecked(n);
		else if (x < 0) {
			if (y >= 0)
				return atanUnchecked(n) + PI;
			return atanUnchecked(n) - PI;
		} else if (y > 0)
			return x + HALF_PI;
		else if (y < 0)
			return x - HALF_PI;
		return x + y; // returns 0 for 0,0 or NaN if either y or x is NaN
	}
	
	public static double atanUncheckedDeg(double i) {
		// We use double precision internally, because some constants need double precision.
		double n = Math.abs(i);
		// c uses the "equally-good" formulation that permits n to be from 0 to almost infinity.
		double c = (n - 1.0) / (n + 1.0);
		// The approximation needs 6 odd powers of c.
		double c2 = c * c;
		double c3 = c * c2;
		double c5 = c3 * c2;
		double c7 = c5 * c2;
		double c9 = c7 * c2;
		double c11 = c9 * c2;
		return (Math.signum(i) * (45.0 + (57.2944766070562 * c - 19.05792099799635 * c3 + 11.089223410359068 * c5 - 6.6711120475953765 * c7 + 3.016813013351768 * c9 - 0.6715752908287405 * c11)));
	}
	
	public static float atan2Deg(final float y, float x) {
		float n = y / x;
		if (n != n)
			n = (y == x ? 1f : -1.0f); // if both y and x are infinite, n would be NaN
		else if (n - n != n - n)
			x = 0f; // if n is infinite, y is infinitely larger than x.
		if (x > 0)
			return (float) atanUncheckedDeg(n);
		else if (x < 0) {
			if (y >= 0)
				return (float) (atanUncheckedDeg(n) + 180.0);
			return (float) (atanUncheckedDeg(n) - 180.0);
		} else if (y > 0)
			return x + 90f;
		else if (y < 0)
			return x - 90f;
		return x + y; // returns 0 for 0,0 or NaN if either y or x is NaN
	}
	
	public static float atan2Deg360(final float y, float x) {
		float n = y / x;
		if (n != n)
			n = (y == x ? 1f : -1.0f); // if both y and x are infinite, n would be NaN
		else if (n - n != n - n)
			x = 0f; // if n is infinite, y is infinitely larger than x.
		if (x > 0) {
			if (y >= 0)
				return (float) atanUncheckedDeg(n);
			else
				return (float) (atanUncheckedDeg(n) + 360.0);
		} else if (x < 0) {
			return (float) (atanUncheckedDeg(n) + 180.0);
		} else if (y > 0)
			return x + 90f;
		else if (y < 0)
			return x + 270f;
		return x + y; // returns 0 for 0,0 or NaN if either y or x is NaN
	}
	
	public static float acos(float a) {
		float a2 = a * a; // a squared
		float a3 = a * a2; // a cubed
		if (a >= 0f) {
			return (float) Math.sqrt(1f - a) * (1.5707288f - 0.2121144f * a + 0.0742610f * a2 - 0.0187293f * a3);
		}
		return 3.14159265358979323846f
				- (float) Math.sqrt(1f + a) * (1.5707288f + 0.2121144f * a + 0.0742610f * a2 + 0.0187293f * a3);
	}
	
	public static float asin(float a) {
		float a2 = a * a; // a squared
		float a3 = a * a2; // a cubed
		if (a >= 0f) {
			return 1.5707963267948966f
					- (float) Math.sqrt(1f - a) * (1.5707288f - 0.2121144f * a + 0.0742610f * a2 - 0.0187293f * a3);
		}
		return -1.5707963267948966f + (float) Math.sqrt(1f + a) * (1.5707288f + 0.2121144f * a + 0.0742610f * a2 + 0.0187293f * a3);
	}
	
	public static float atan(float i) {
		// We use double precision internally, because some constants need double precision.
		// This clips infinite inputs at Double.MAX_VALUE, which still probably becomes infinite
		// again when converted back to float.
		double n = Math.min(Math.abs(i), Double.MAX_VALUE);
		// c uses the "equally-good" formulation that permits n to be from 0 to almost infinity.
		double c = (n - 1.0) / (n + 1.0);
		// The approximation needs 6 odd powers of c.
		double c2 = c * c;
		double c3 = c * c2;
		double c5 = c3 * c2;
		double c7 = c5 * c2;
		double c9 = c7 * c2;
		double c11 = c9 * c2;
		return Math.signum(i) * (float) ((Math.PI * 0.25) + (0.99997726 * c - 0.33262347 * c3 + 0.19354346 * c5 - 0.11643287 * c7 + 0.05265332 * c9 - 0.0117212 * c11));
	}
	
	public static float asinDeg(float a) {
		float a2 = a * a; // a squared
		float a3 = a * a2; // a cubed
		if (a >= 0f)
			return 90f - (float) Math.sqrt(1f - a) * (89.99613099964837f - 12.153259893949748f * a + 4.2548418824210055f * a2 - 1.0731098432343729f * a3);
		return (float) Math.sqrt(1f + a) * (89.99613099964837f + 12.153259893949748f * a + 4.2548418824210055f * a2 + 1.0731098432343729f * a3) - 90f;
	}
	
	public static float acosDeg(float a) {
		float a2 = a * a; // a squared
		float a3 = a * a2; // a cubed
		if (a >= 0f) {
			return (float) Math.sqrt(1f - a)
					* (89.99613099964837f - 12.153259533621753f * a + 4.254842010910525f * a2 - 1.0731098035209208f * a3);
		}
		return 180f - (float) Math.sqrt(1f + a)
				* (89.99613099964837f + 12.153259533621753f * a + 4.254842010910525f * a2 + 1.0731098035209208f * a3);
	}
	
	public static float atanDeg(float i) {
		// We use double precision internally, because some constants need double precision.
		// This clips infinite inputs at Double.MAX_VALUE, which still probably becomes infinite
		// again when converted back to float.
		double n = Math.min(Math.abs(i), Double.MAX_VALUE);
		// c uses the "equally-good" formulation that permits n to be from 0 to almost infinity.
		double c = (n - 1.0) / (n + 1.0);
		// The approximation needs 6 odd powers of c.
		double c2 = c * c;
		double c3 = c * c2;
		double c5 = c3 * c2;
		double c7 = c5 * c2;
		double c9 = c7 * c2;
		double c11 = c9 * c2;
		return (float) (Math.signum(i) * (45.0 + (57.2944766070562 * c - 19.05792099799635 * c3 + 11.089223410359068 * c5
				- 6.6711120475953765 * c7 + 3.016813013351768 * c9 - 0.6715752908287405 * c11)));
	}
	
	public static Random random = new RandomXS128();
	
	public static int random(int range) {
		return random.nextInt(range + 1);
	}
	
	public static int random(int start, int end) {
		return start + random.nextInt(end - start + 1);
	}
	
	public static long random(long range) {
		// Uses the lower-bounded overload defined below, which is simpler and doesn't lose much optimization.
		return random(0L, range);
	}
	
	public static long random(long start, long end) {
		final long rand = random.nextLong();
		// In order to get the range to go from start to end, instead of overflowing after end and going
		// back around to start, start must be less than end.
		if (end < start) {
			long t = end;
			end = start;
			start = t;
		}
		long bound = end - start + 1L; // inclusive on end
		// Credit to https://oroboro.com/large-random-in-range/ for the following technique
		// It's a 128-bit-product where only the upper 64 of 128 bits are used.
		final long randLow = rand & 0xFFFFFFFFL;
		final long boundLow = bound & 0xFFFFFFFFL;
		final long randHigh = (rand >>> 32);
		final long boundHigh = (bound >>> 32);
		return start + (randHigh * boundLow >>> 32) + (randLow * boundHigh >>> 32) + randHigh * boundHigh;
	}
	
	public static boolean randomBoolean() {
		return random.nextBoolean();
	}
	
	public static boolean randomBoolean(float chance) {
		return MathUtils.random() < chance;
	}
	
	public static float random() {
		return random.nextFloat();
	}
	
	public static float random(float range) {
		return random.nextFloat() * range;
	}
	
	public static float random(float start, float end) {
		return start + random.nextFloat() * (end - start);
	}
	
	public static int randomSign() {
		return 1 | (random.nextInt() >> 31);
	}
	
	public static float randomTriangular() {
		return random.nextFloat() - random.nextFloat();
	}
	
	public static float randomTriangular(float max) {
		return (random.nextFloat() - random.nextFloat()) * max;
	}
	
	public static float randomTriangular(float min, float max) {
		return randomTriangular(min, max, (min + max) * 0.5f);
	}
	
	public static float randomTriangular(float min, float max, float mode) {
		float u = random.nextFloat();
		float d = max - min;
		if (u <= (mode - min) / d)
			return min + (float) Math.sqrt(u * d * (mode - min));
		return max - (float) Math.sqrt((1 - u) * d * (max - mode));
	}
	
	public static int nextPowerOfTwo(int value) {
		if (value == 0)
			return 1;
		value--;
		value |= value >> 1;
		value |= value >> 2;
		value |= value >> 4;
		value |= value >> 8;
		value |= value >> 16;
		return value + 1;
	}
	
	public static boolean isPowerOfTwo(int value) {
		return value != 0 && (value & value - 1) == 0;
	}
	
	public static short clamp(short value, short min, short max) {
		if (value < min)
			return min;
		if (value > max)
			return max;
		return value;
	}
	
	public static int clamp(int value, int min, int max) {
		if (value < min)
			return min;
		if (value > max)
			return max;
		return value;
	}
	
	public static long clamp(long value, long min, long max) {
		if (value < min)
			return min;
		if (value > max)
			return max;
		return value;
	}
	
	public static float clamp(float value, float min, float max) {
		if (value < min)
			return min;
		if (value > max)
			return max;
		return value;
	}
	
	public static double clamp(double value, double min, double max) {
		if (value < min)
			return min;
		if (value > max)
			return max;
		return value;
	}
	
	public static float lerp(float fromValue, float toValue, float progress) {
		return fromValue + (toValue - fromValue) * progress;
	}
	
	public static float norm(float rangeStart, float rangeEnd, float value) {
		return (value - rangeStart) / (rangeEnd - rangeStart);
	}
	
	public static float map(float inRangeStart, float inRangeEnd, float outRangeStart, float outRangeEnd, float value) {
		return outRangeStart + (value - inRangeStart) * (outRangeEnd - outRangeStart) / (inRangeEnd - inRangeStart);
	}
	
	public static float lerpAngle(float fromRadians, float toRadians, float progress) {
		float delta = (((toRadians - fromRadians) % PI2 + PI2 + PI) % PI2) - PI;
		return ((fromRadians + delta * progress) % PI2 + PI2) % PI2;
	}
	
	public static float lerpAngleDeg(float fromDegrees, float toDegrees, float progress) {
		float delta = (((toDegrees - fromDegrees) % 360f + 360f + 180f) % 360f) - 180f;
		return ((fromDegrees + delta * progress) % 360f + 360f) % 360f;
	}
	
	private static final int BIG_ENOUGH_INT = 16 * 1024;
	private static final double BIG_ENOUGH_FLOOR = BIG_ENOUGH_INT;
	private static final double CEIL = 0.9999999;
	private static final double BIG_ENOUGH_CEIL = 16384.999999999996;
	private static final double BIG_ENOUGH_ROUND = BIG_ENOUGH_INT + 0.5f;
	
	public static int floor(float value) {
		return (int) (value + BIG_ENOUGH_FLOOR) - BIG_ENOUGH_INT;
	}
	
	public static int floorPositive(float value) {
		return (int) value;
	}
	
	public static int ceil(float value) {
		return BIG_ENOUGH_INT - (int) (BIG_ENOUGH_FLOOR - value);
	}
	
	public static int ceilPositive(float value) {
		return (int) (value + CEIL);
	}
	
	public static int round(float value) {
		return (int) (value + BIG_ENOUGH_ROUND) - BIG_ENOUGH_INT;
	}
	
	public static int roundPositive(float value) {
		return (int) (value + 0.5f);
	}
	
	public static boolean isZero(float value) {
		return Math.abs(value) <= FLOAT_ROUNDING_ERROR;
	}
	
	public static boolean isZero(float value, float tolerance) {
		return Math.abs(value) <= tolerance;
	}
	
	public static boolean isEqual(float a, float b) {
		return Math.abs(a - b) <= FLOAT_ROUNDING_ERROR;
	}
	
	public static boolean isEqual(float a, float b, float tolerance) {
		return Math.abs(a - b) <= tolerance;
	}
	
	public static float log(float a, float value) {
		return (float) (Math.log(value) / Math.log(a));
	}
	
	public static float log2(float value) {
		return log(2, value);
	}
	
}
