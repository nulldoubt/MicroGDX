package me.nulldoubt.micro.math;

import me.nulldoubt.micro.exceptions.MicroRuntimeException;

import java.io.Serializable;

public final class Matrix3 implements Serializable {
	
	public static final int M00 = 0;
	public static final int M01 = 3;
	public static final int M02 = 6;
	public static final int M10 = 1;
	public static final int M11 = 4;
	public static final int M12 = 7;
	public static final int M20 = 2;
	public static final int M21 = 5;
	public static final int M22 = 8;
	public float[] val = new float[9];
	private final float[] tmp = new float[9];
	
	{
		tmp[M22] = 1;
	}
	
	public Matrix3() {
		idt();
	}
	
	public Matrix3(Matrix3 matrix) {
		set(matrix);
	}
	
	public Matrix3(float[] values) {
		this.set(values);
	}
	
	public Matrix3 idt() {
		float[] val = this.val;
		val[M00] = 1;
		val[M10] = 0;
		val[M20] = 0;
		val[M01] = 0;
		val[M11] = 1;
		val[M21] = 0;
		val[M02] = 0;
		val[M12] = 0;
		val[M22] = 1;
		return this;
	}
	
	public Matrix3 mul(Matrix3 m) {
		float[] val = this.val;
		
		float v00 = val[M00] * m.val[M00] + val[M01] * m.val[M10] + val[M02] * m.val[M20];
		float v01 = val[M00] * m.val[M01] + val[M01] * m.val[M11] + val[M02] * m.val[M21];
		float v02 = val[M00] * m.val[M02] + val[M01] * m.val[M12] + val[M02] * m.val[M22];
		
		float v10 = val[M10] * m.val[M00] + val[M11] * m.val[M10] + val[M12] * m.val[M20];
		float v11 = val[M10] * m.val[M01] + val[M11] * m.val[M11] + val[M12] * m.val[M21];
		float v12 = val[M10] * m.val[M02] + val[M11] * m.val[M12] + val[M12] * m.val[M22];
		
		float v20 = val[M20] * m.val[M00] + val[M21] * m.val[M10] + val[M22] * m.val[M20];
		float v21 = val[M20] * m.val[M01] + val[M21] * m.val[M11] + val[M22] * m.val[M21];
		float v22 = val[M20] * m.val[M02] + val[M21] * m.val[M12] + val[M22] * m.val[M22];
		
		val[M00] = v00;
		val[M10] = v10;
		val[M20] = v20;
		val[M01] = v01;
		val[M11] = v11;
		val[M21] = v21;
		val[M02] = v02;
		val[M12] = v12;
		val[M22] = v22;
		
		return this;
	}
	
	public Matrix3 mulLeft(Matrix3 m) {
		float[] val = this.val;
		
		float v00 = m.val[M00] * val[M00] + m.val[M01] * val[M10] + m.val[M02] * val[M20];
		float v01 = m.val[M00] * val[M01] + m.val[M01] * val[M11] + m.val[M02] * val[M21];
		float v02 = m.val[M00] * val[M02] + m.val[M01] * val[M12] + m.val[M02] * val[M22];
		
		float v10 = m.val[M10] * val[M00] + m.val[M11] * val[M10] + m.val[M12] * val[M20];
		float v11 = m.val[M10] * val[M01] + m.val[M11] * val[M11] + m.val[M12] * val[M21];
		float v12 = m.val[M10] * val[M02] + m.val[M11] * val[M12] + m.val[M12] * val[M22];
		
		float v20 = m.val[M20] * val[M00] + m.val[M21] * val[M10] + m.val[M22] * val[M20];
		float v21 = m.val[M20] * val[M01] + m.val[M21] * val[M11] + m.val[M22] * val[M21];
		float v22 = m.val[M20] * val[M02] + m.val[M21] * val[M12] + m.val[M22] * val[M22];
		
		val[M00] = v00;
		val[M10] = v10;
		val[M20] = v20;
		val[M01] = v01;
		val[M11] = v11;
		val[M21] = v21;
		val[M02] = v02;
		val[M12] = v12;
		val[M22] = v22;
		
		return this;
	}
	
	public Matrix3 setToRotation(float degrees) {
		return setToRotationRad(MathUtils.degreesToRadians * degrees);
	}
	
	public Matrix3 setToRotationRad(float radians) {
		float cos = (float) Math.cos(radians);
		float sin = (float) Math.sin(radians);
		float[] val = this.val;
		
		val[M00] = cos;
		val[M10] = sin;
		val[M20] = 0;
		
		val[M01] = -sin;
		val[M11] = cos;
		val[M21] = 0;
		
		val[M02] = 0;
		val[M12] = 0;
		val[M22] = 1;
		
		return this;
	}
	
	public Matrix3 setToRotation(Vector3 axis, float degrees) {
		return setToRotation(axis, MathUtils.cosDeg(degrees), MathUtils.sinDeg(degrees));
	}
	
	public Matrix3 setToRotation(Vector3 axis, float cos, float sin) {
		float[] val = this.val;
		float oc = 1.0f - cos;
		val[M00] = oc * axis.x * axis.x + cos;
		val[M01] = oc * axis.x * axis.y - axis.z * sin;
		val[M02] = oc * axis.z * axis.x + axis.y * sin;
		val[M10] = oc * axis.x * axis.y + axis.z * sin;
		val[M11] = oc * axis.y * axis.y + cos;
		val[M12] = oc * axis.y * axis.z - axis.x * sin;
		val[M20] = oc * axis.z * axis.x - axis.y * sin;
		val[M21] = oc * axis.y * axis.z + axis.x * sin;
		val[M22] = oc * axis.z * axis.z + cos;
		return this;
	}
	
	public Matrix3 setToTranslation(float x, float y) {
		float[] val = this.val;
		
		val[M00] = 1;
		val[M10] = 0;
		val[M20] = 0;
		
		val[M01] = 0;
		val[M11] = 1;
		val[M21] = 0;
		
		val[M02] = x;
		val[M12] = y;
		val[M22] = 1;
		
		return this;
	}
	
	public Matrix3 setToTranslation(Vector2 translation) {
		float[] val = this.val;
		
		val[M00] = 1;
		val[M10] = 0;
		val[M20] = 0;
		
		val[M01] = 0;
		val[M11] = 1;
		val[M21] = 0;
		
		val[M02] = translation.x;
		val[M12] = translation.y;
		val[M22] = 1;
		
		return this;
	}
	
	public Matrix3 setToScaling(float scaleX, float scaleY) {
		float[] val = this.val;
		val[M00] = scaleX;
		val[M10] = 0;
		val[M20] = 0;
		val[M01] = 0;
		val[M11] = scaleY;
		val[M21] = 0;
		val[M02] = 0;
		val[M12] = 0;
		val[M22] = 1;
		return this;
	}
	
	public Matrix3 setToScaling(Vector2 scale) {
		float[] val = this.val;
		val[M00] = scale.x;
		val[M10] = 0;
		val[M20] = 0;
		val[M01] = 0;
		val[M11] = scale.y;
		val[M21] = 0;
		val[M02] = 0;
		val[M12] = 0;
		val[M22] = 1;
		return this;
	}
	
	public String toString() {
		float[] val = this.val;
		return "[" + val[M00] + "|" + val[M01] + "|" + val[M02] + "]\n" //
				+ "[" + val[M10] + "|" + val[M11] + "|" + val[M12] + "]\n" //
				+ "[" + val[M20] + "|" + val[M21] + "|" + val[M22] + "]";
	}
	
	public float det() {
		float[] val = this.val;
		return val[M00] * val[M11] * val[M22] + val[M01] * val[M12] * val[M20] + val[M02] * val[M10] * val[M21]
				- val[M00] * val[M12] * val[M21] - val[M01] * val[M10] * val[M22] - val[M02] * val[M11] * val[M20];
	}
	
	public Matrix3 inv() {
		float det = det();
		if (det == 0)
			throw new MicroRuntimeException("Can't invert a singular matrix");
		
		float inv_det = 1.0f / det;
		float[] val = this.val;
		
		float v00 = val[M11] * val[M22] - val[M21] * val[M12];
		float v10 = val[M20] * val[M12] - val[M10] * val[M22];
		float v20 = val[M10] * val[M21] - val[M20] * val[M11];
		float v01 = val[M21] * val[M02] - val[M01] * val[M22];
		float v11 = val[M00] * val[M22] - val[M20] * val[M02];
		float v21 = val[M20] * val[M01] - val[M00] * val[M21];
		float v02 = val[M01] * val[M12] - val[M11] * val[M02];
		float v12 = val[M10] * val[M02] - val[M00] * val[M12];
		float v22 = val[M00] * val[M11] - val[M10] * val[M01];
		
		val[M00] = inv_det * v00;
		val[M10] = inv_det * v10;
		val[M20] = inv_det * v20;
		val[M01] = inv_det * v01;
		val[M11] = inv_det * v11;
		val[M21] = inv_det * v21;
		val[M02] = inv_det * v02;
		val[M12] = inv_det * v12;
		val[M22] = inv_det * v22;
		
		return this;
	}
	
	public Matrix3 set(Matrix3 mat) {
		System.arraycopy(mat.val, 0, val, 0, val.length);
		return this;
	}
	
	public Matrix3 set(Affine2 affine) {
		float[] val = this.val;
		val[M00] = affine.m00;
		val[M10] = affine.m10;
		val[M20] = 0;
		val[M01] = affine.m01;
		val[M11] = affine.m11;
		val[M21] = 0;
		val[M02] = affine.m02;
		val[M12] = affine.m12;
		val[M22] = 1;
		return this;
	}
	
	public Matrix3 set(Matrix4 mat) {
		float[] val = this.val;
		val[M00] = mat.val[Matrix4.M00];
		val[M10] = mat.val[Matrix4.M10];
		val[M20] = mat.val[Matrix4.M20];
		val[M01] = mat.val[Matrix4.M01];
		val[M11] = mat.val[Matrix4.M11];
		val[M21] = mat.val[Matrix4.M21];
		val[M02] = mat.val[Matrix4.M02];
		val[M12] = mat.val[Matrix4.M12];
		val[M22] = mat.val[Matrix4.M22];
		return this;
	}
	
	public Matrix3 set(float[] values) {
		System.arraycopy(values, 0, val, 0, val.length);
		return this;
	}
	
	public Matrix3 trn(Vector2 vector) {
		val[M02] += vector.x;
		val[M12] += vector.y;
		return this;
	}
	
	public Matrix3 trn(float x, float y) {
		val[M02] += x;
		val[M12] += y;
		return this;
	}
	
	public Matrix3 trn(Vector3 vector) {
		val[M02] += vector.x;
		val[M12] += vector.y;
		return this;
	}
	
	public Matrix3 translate(float x, float y) {
		float[] tmp = this.tmp;
		tmp[M00] = 1;
		tmp[M10] = 0;
		
		tmp[M01] = 0;
		tmp[M11] = 1;
		
		tmp[M02] = x;
		tmp[M12] = y;
		mul(val, tmp);
		return this;
	}
	
	public Matrix3 translate(Vector2 translation) {
		float[] tmp = this.tmp;
		tmp[M00] = 1;
		tmp[M10] = 0;
		
		tmp[M01] = 0;
		tmp[M11] = 1;
		
		tmp[M02] = translation.x;
		tmp[M12] = translation.y;
		mul(val, tmp);
		return this;
	}
	
	public Matrix3 rotate(float degrees) {
		return rotateRad(MathUtils.degreesToRadians * degrees);
	}
	
	public Matrix3 rotateRad(float radians) {
		if (radians == 0)
			return this;
		float cos = (float) Math.cos(radians);
		float sin = (float) Math.sin(radians);
		
		float[] tmp = this.tmp;
		tmp[M00] = cos;
		tmp[M10] = sin;
		
		tmp[M01] = -sin;
		tmp[M11] = cos;
		
		tmp[M02] = 0;
		tmp[M12] = 0;
		
		mul(val, tmp);
		return this;
	}
	
	public Matrix3 scale(float scaleX, float scaleY) {
		float[] tmp = this.tmp;
		tmp[M00] = scaleX;
		tmp[M10] = 0;
		
		tmp[M01] = 0;
		tmp[M11] = scaleY;
		
		tmp[M02] = 0;
		tmp[M12] = 0;
		
		mul(val, tmp);
		return this;
	}
	
	public Matrix3 scale(Vector2 scale) {
		float[] tmp = this.tmp;
		tmp[M00] = scale.x;
		tmp[M10] = 0;
		
		tmp[M01] = 0;
		tmp[M11] = scale.y;
		
		tmp[M02] = 0;
		tmp[M12] = 0;
		
		mul(val, tmp);
		return this;
	}
	
	public float[] getValues() {
		return val;
	}
	
	public Vector2 getTranslation(Vector2 position) {
		position.x = val[M02];
		position.y = val[M12];
		return position;
	}
	
	public Vector2 getScale(Vector2 scale) {
		float[] val = this.val;
		scale.x = (float) Math.sqrt(val[M00] * val[M00] + val[M01] * val[M01]);
		scale.y = (float) Math.sqrt(val[M10] * val[M10] + val[M11] * val[M11]);
		return scale;
	}
	
	public float getRotation() {
		return MathUtils.radiansToDegrees * (float) Math.atan2(val[M10], val[M00]);
	}
	
	public float getRotationRad() {
		return (float) Math.atan2(val[M10], val[M00]);
	}
	
	public Matrix3 scl(float scale) {
		val[M00] *= scale;
		val[M11] *= scale;
		return this;
	}
	
	public Matrix3 scl(Vector2 scale) {
		val[M00] *= scale.x;
		val[M11] *= scale.y;
		return this;
	}
	
	public Matrix3 scl(Vector3 scale) {
		val[M00] *= scale.x;
		val[M11] *= scale.y;
		return this;
	}
	
	public Matrix3 transpose() {
		float[] val = this.val;
		float v01 = val[M10];
		float v02 = val[M20];
		float v10 = val[M01];
		float v12 = val[M21];
		float v20 = val[M02];
		float v21 = val[M12];
		val[M01] = v01;
		val[M02] = v02;
		val[M10] = v10;
		val[M12] = v12;
		val[M20] = v20;
		val[M21] = v21;
		return this;
	}
	
	private static void mul(float[] mata, float[] matb) {
		float v00 = mata[M00] * matb[M00] + mata[M01] * matb[M10] + mata[M02] * matb[M20];
		float v01 = mata[M00] * matb[M01] + mata[M01] * matb[M11] + mata[M02] * matb[M21];
		float v02 = mata[M00] * matb[M02] + mata[M01] * matb[M12] + mata[M02] * matb[M22];
		
		float v10 = mata[M10] * matb[M00] + mata[M11] * matb[M10] + mata[M12] * matb[M20];
		float v11 = mata[M10] * matb[M01] + mata[M11] * matb[M11] + mata[M12] * matb[M21];
		float v12 = mata[M10] * matb[M02] + mata[M11] * matb[M12] + mata[M12] * matb[M22];
		
		float v20 = mata[M20] * matb[M00] + mata[M21] * matb[M10] + mata[M22] * matb[M20];
		float v21 = mata[M20] * matb[M01] + mata[M21] * matb[M11] + mata[M22] * matb[M21];
		float v22 = mata[M20] * matb[M02] + mata[M21] * matb[M12] + mata[M22] * matb[M22];
		
		mata[M00] = v00;
		mata[M10] = v10;
		mata[M20] = v20;
		mata[M01] = v01;
		mata[M11] = v11;
		mata[M21] = v21;
		mata[M02] = v02;
		mata[M12] = v12;
		mata[M22] = v22;
	}
	
}
