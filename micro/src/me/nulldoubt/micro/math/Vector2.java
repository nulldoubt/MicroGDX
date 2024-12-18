package me.nulldoubt.micro.math;

import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.utils.Numbers;

import java.io.Serializable;

public class Vector2 implements Serializable, Vector<Vector2> {
	
	public final static Vector2 X = new Vector2(1, 0);
	public final static Vector2 Y = new Vector2(0, 1);
	public final static Vector2 Zero = new Vector2(0, 0);
	
	public float x;
	public float y;
	
	public Vector2() {}
	
	public Vector2(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	public Vector2(Vector2 v) {
		set(v);
	}
	
	@Override
	public Vector2 cpy() {
		return new Vector2(this);
	}
	
	public static float len(float x, float y) {
		return (float) Math.sqrt(x * x + y * y);
	}
	
	@Override
	public float len() {
		return (float) Math.sqrt(x * x + y * y);
	}
	
	public static float len2(float x, float y) {
		return x * x + y * y;
	}
	
	@Override
	public float len2() {
		return x * x + y * y;
	}
	
	@Override
	public Vector2 set(Vector2 v) {
		x = v.x;
		y = v.y;
		return this;
	}
	
	public Vector2 set(float x, float y) {
		this.x = x;
		this.y = y;
		return this;
	}
	
	@Override
	public Vector2 sub(Vector2 v) {
		x -= v.x;
		y -= v.y;
		return this;
	}
	
	public Vector2 sub(float x, float y) {
		this.x -= x;
		this.y -= y;
		return this;
	}
	
	@Override
	public Vector2 nor() {
		float len = len();
		if (len != 0) {
			x /= len;
			y /= len;
		}
		return this;
	}
	
	@Override
	public Vector2 add(Vector2 v) {
		x += v.x;
		y += v.y;
		return this;
	}
	
	public Vector2 add(float x, float y) {
		this.x += x;
		this.y += y;
		return this;
	}
	
	public static float dot(float x1, float y1, float x2, float y2) {
		return x1 * x2 + y1 * y2;
	}
	
	@Override
	public float dot(Vector2 v) {
		return x * v.x + y * v.y;
	}
	
	public float dot(float ox, float oy) {
		return x * ox + y * oy;
	}
	
	@Override
	public Vector2 scl(float scalar) {
		x *= scalar;
		y *= scalar;
		return this;
	}
	
	public Vector2 scl(float x, float y) {
		this.x *= x;
		this.y *= y;
		return this;
	}
	
	@Override
	public Vector2 scl(Vector2 v) {
		this.x *= v.x;
		this.y *= v.y;
		return this;
	}
	
	@Override
	public Vector2 mulAdd(Vector2 vec, float scalar) {
		this.x += vec.x * scalar;
		this.y += vec.y * scalar;
		return this;
	}
	
	@Override
	public Vector2 mulAdd(Vector2 vec, Vector2 mulVec) {
		this.x += vec.x * mulVec.x;
		this.y += vec.y * mulVec.y;
		return this;
	}
	
	public boolean idt(final Vector2 vector) {
		return x == vector.x && y == vector.y;
	}
	
	public static float dst(float x1, float y1, float x2, float y2) {
		final float x_d = x2 - x1;
		final float y_d = y2 - y1;
		return (float) Math.sqrt(x_d * x_d + y_d * y_d);
	}
	
	@Override
	public float dst(Vector2 v) {
		final float x_d = v.x - x;
		final float y_d = v.y - y;
		return (float) Math.sqrt(x_d * x_d + y_d * y_d);
	}
	
	public float dst(float x, float y) {
		final float x_d = x - this.x;
		final float y_d = y - this.y;
		return (float) Math.sqrt(x_d * x_d + y_d * y_d);
	}
	
	public static float dst2(float x1, float y1, float x2, float y2) {
		final float x_d = x2 - x1;
		final float y_d = y2 - y1;
		return x_d * x_d + y_d * y_d;
	}
	
	@Override
	public float dst2(Vector2 v) {
		final float x_d = v.x - x;
		final float y_d = v.y - y;
		return x_d * x_d + y_d * y_d;
	}
	
	public float dst2(float x, float y) {
		final float x_d = x - this.x;
		final float y_d = y - this.y;
		return x_d * x_d + y_d * y_d;
	}
	
	@Override
	public Vector2 limit(float limit) {
		return limit2(limit * limit);
	}
	
	@Override
	public Vector2 limit2(float limit2) {
		float len2 = len2();
		if (len2 > limit2) {
			return scl((float) Math.sqrt(limit2 / len2));
		}
		return this;
	}
	
	@Override
	public Vector2 clamp(float min, float max) {
		final float len2 = len2();
		if (len2 == 0f)
			return this;
		float max2 = max * max;
		if (len2 > max2)
			return scl((float) Math.sqrt(max2 / len2));
		float min2 = min * min;
		if (len2 < min2)
			return scl((float) Math.sqrt(min2 / len2));
		return this;
	}
	
	@Override
	public Vector2 setLength(float len) {
		return setLength2(len * len);
	}
	
	@Override
	public Vector2 setLength2(float len2) {
		float oldLen2 = len2();
		return (oldLen2 == 0 || oldLen2 == len2) ? this : scl((float) Math.sqrt(len2 / oldLen2));
	}
	
	@Override
	public String toString() {
		return "(" + x + "," + y + ")";
	}
	
	public Vector2 fromString(String v) {
		int s = v.indexOf(',', 1);
		if (s != -1 && v.charAt(0) == '(' && v.charAt(v.length() - 1) == ')') {
			try {
				float x = Float.parseFloat(v.substring(1, s));
				float y = Float.parseFloat(v.substring(s + 1, v.length() - 1));
				return this.set(x, y);
			} catch (NumberFormatException _) {}
		}
		throw new MicroRuntimeException("Malformed Vector2: " + v);
	}
	
	public Vector2 mul(Matrix3 mat) {
		float x = this.x * mat.val[0] + this.y * mat.val[3] + mat.val[6];
		float y = this.x * mat.val[1] + this.y * mat.val[4] + mat.val[7];
		this.x = x;
		this.y = y;
		return this;
	}
	
	public float crs(Vector2 v) {
		return this.x * v.y - this.y * v.x;
	}
	
	public float crs(float x, float y) {
		return this.x * y - this.y * x;
	}
	
	public float angleDeg() {
		float angle = (float) Math.atan2(y, x) * MathUtils.radiansToDegrees;
		if (angle < 0)
			angle += 360;
		return angle;
	}
	
	public float angleDeg(Vector2 reference) {
		float angle = (float) Math.atan2(reference.crs(this), reference.dot(this)) * MathUtils.radiansToDegrees;
		if (angle < 0)
			angle += 360;
		return angle;
	}
	
	public static float angleDeg(float x, float y) {
		float angle = (float) Math.atan2(y, x) * MathUtils.radiansToDegrees;
		if (angle < 0)
			angle += 360;
		return angle;
	}
	
	public float angleRad() {
		return (float) Math.atan2(y, x);
	}
	
	public float angleRad(Vector2 reference) {
		return (float) Math.atan2(reference.crs(this), reference.dot(this));
	}
	
	public static float angleRad(float x, float y) {
		return (float) Math.atan2(y, x);
	}
	
	public Vector2 setAngleDeg(float degrees) {
		return setAngleRad(degrees * MathUtils.degreesToRadians);
	}
	
	public Vector2 setAngleRad(float radians) {
		this.set(len(), 0f);
		this.rotateRad(radians);
		return this;
	}
	
	public Vector2 rotateDeg(float degrees) {
		return rotateRad(degrees * MathUtils.degreesToRadians);
	}
	
	public Vector2 rotateRad(float radians) {
		float cos = (float) Math.cos(radians);
		float sin = (float) Math.sin(radians);
		
		float newX = this.x * cos - this.y * sin;
		float newY = this.x * sin + this.y * cos;
		
		this.x = newX;
		this.y = newY;
		
		return this;
	}
	
	public Vector2 rotateAroundDeg(Vector2 reference, float degrees) {
		return this.sub(reference).rotateDeg(degrees).add(reference);
	}
	
	public Vector2 rotateAroundRad(Vector2 reference, float radians) {
		return this.sub(reference).rotateRad(radians).add(reference);
	}
	
	public Vector2 rotate90(int dir) {
		float x = this.x;
		if (dir >= 0) {
			this.x = -y;
			y = x;
		} else {
			this.x = y;
			y = -x;
		}
		return this;
	}
	
	@Override
	public Vector2 lerp(Vector2 target, float alpha) {
		final float invAlpha = 1.0f - alpha;
		this.x = (x * invAlpha) + (target.x * alpha);
		this.y = (y * invAlpha) + (target.y * alpha);
		return this;
	}
	
	@Override
	public Vector2 interpolate(Vector2 target, float alpha, Interpolation interpolation) {
		return lerp(target, interpolation.apply(alpha));
	}
	
	@Override
	public Vector2 setToRandomDirection() {
		float theta = MathUtils.random(0f, MathUtils.PI2);
		return this.set(MathUtils.cos(theta), MathUtils.sin(theta));
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Numbers.floatToIntBits(x);
		result = prime * result + Numbers.floatToIntBits(y);
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Vector2 other = (Vector2) obj;
		if (Numbers.floatToIntBits(x) != Numbers.floatToIntBits(other.x))
			return false;
		return Numbers.floatToIntBits(y) == Numbers.floatToIntBits(other.y);
	}
	
	@Override
	public boolean epsilonEquals(Vector2 other, float epsilon) {
		if (other == null)
			return false;
		if (Math.abs(other.x - x) > epsilon)
			return false;
		return !(Math.abs(other.y - y) > epsilon);
	}
	
	public boolean epsilonEquals(float x, float y, float epsilon) {
		if (Math.abs(x - this.x) > epsilon)
			return false;
		return !(Math.abs(y - this.y) > epsilon);
	}
	
	public boolean epsilonEquals(final Vector2 other) {
		return epsilonEquals(other, MathUtils.FLOAT_ROUNDING_ERROR);
	}
	
	public boolean epsilonEquals(float x, float y) {
		return epsilonEquals(x, y, MathUtils.FLOAT_ROUNDING_ERROR);
	}
	
	@Override
	public boolean isUnit() {
		return isUnit(0.000000001f);
	}
	
	@Override
	public boolean isUnit(final float margin) {
		return Math.abs(len2() - 1f) < margin;
	}
	
	@Override
	public boolean isZero() {
		return x == 0 && y == 0;
	}
	
	@Override
	public boolean isZero(final float margin) {
		return len2() < margin;
	}
	
	@Override
	public boolean isOnLine(Vector2 other) {
		return MathUtils.isZero(x * other.y - y * other.x);
	}
	
	@Override
	public boolean isOnLine(Vector2 other, float epsilon) {
		return MathUtils.isZero(x * other.y - y * other.x, epsilon);
	}
	
	@Override
	public boolean isCollinear(Vector2 other, float epsilon) {
		return isOnLine(other, epsilon) && dot(other) > 0f;
	}
	
	@Override
	public boolean isCollinear(Vector2 other) {
		return isOnLine(other) && dot(other) > 0f;
	}
	
	@Override
	public boolean isCollinearOpposite(Vector2 other, float epsilon) {
		return isOnLine(other, epsilon) && dot(other) < 0f;
	}
	
	@Override
	public boolean isCollinearOpposite(Vector2 other) {
		return isOnLine(other) && dot(other) < 0f;
	}
	
	@Override
	public boolean isPerpendicular(Vector2 vector) {
		return MathUtils.isZero(dot(vector));
	}
	
	@Override
	public boolean isPerpendicular(Vector2 vector, float epsilon) {
		return MathUtils.isZero(dot(vector), epsilon);
	}
	
	@Override
	public boolean hasSameDirection(Vector2 vector) {
		return dot(vector) > 0;
	}
	
	@Override
	public boolean hasOppositeDirection(Vector2 vector) {
		return dot(vector) < 0;
	}
	
	@Override
	public Vector2 setZero() {
		this.x = 0;
		this.y = 0;
		return this;
	}
	
}
