package me.nulldoubt.micro.physics.box2d.joints;

import me.nulldoubt.micro.math.Vector2;
import me.nulldoubt.micro.physics.box2d.Joint;
import me.nulldoubt.micro.physics.box2d.World;

/** A motor joint is used to control the relative motion between two bodies. A typical usage is to control the movement of a
 * dynamic body with respect to the ground. */
public class MotorJoint extends Joint {
	// @off
	/*JNI
#include <Box2D/Box2D.h>
	 */

	private final float[] tmp = new float[2];
	private final Vector2 linearOffset = new Vector2();

	public MotorJoint (World world, long addr) {
		super(world, addr);
	}

	public Vector2 getLinearOffset () {
		jniGetLinearOffset(addr, tmp);
		linearOffset.set(tmp[0], tmp[1]);
		return linearOffset;
	}

	private native void jniGetLinearOffset (long addr, float[] linearOffset); /*
		b2MotorJoint* joint = (b2MotorJoint*)addr;
		linearOffset[0] = joint->GetLinearOffset().x;
		linearOffset[1] = joint->GetLinearOffset().y;
	*/

	public void setLinearOffset(Vector2 linearOffset) {
		jniSetLinearOffset(addr, linearOffset.x, linearOffset.y);
	}

	private native void jniSetLinearOffset (long addr, float linearOffsetX, float linearOffsetY); /*
		b2MotorJoint* joint = (b2MotorJoint*)addr;
		joint->SetLinearOffset(b2Vec2(linearOffsetX, linearOffsetY));
	*/

	public float getAngularOffset () {
		return jniGetAngularOffset(addr);
	}

	private native float jniGetAngularOffset (long addr); /*
		b2MotorJoint* joint = (b2MotorJoint*)addr;
		return joint->GetAngularOffset();
	*/

	public void setAngularOffset (float angularOffset) {
		jniSetAngularOffset(addr, angularOffset);
	}

	private native void jniSetAngularOffset (long addr, float angularOffset); /*
		b2MotorJoint* joint = (b2MotorJoint*)addr;
		joint->SetAngularOffset(angularOffset);
	*/

	public float getMaxForce () {
		return jniGetMaxForce(addr);
	}

	private native float jniGetMaxForce (long addr); /*
		b2MotorJoint* joint = (b2MotorJoint*)addr;
		return joint->GetMaxForce();
	*/

	public void setMaxForce (float maxForce) {
		jniSetMaxForce(addr, maxForce);
	}

	private native void jniSetMaxForce (long addr, float maxForce); /*
		b2MotorJoint* joint = (b2MotorJoint*)addr;
		joint->SetMaxForce(maxForce);
	*/

	public float getMaxTorque () {
		return jniGetMaxTorque(addr);
	}

	private native float jniGetMaxTorque (long addr); /*
		b2MotorJoint* joint = (b2MotorJoint*)addr;
		return joint->GetMaxTorque();
	*/

	public void setMaxTorque (float maxTorque) {
		jniSetMaxTorque(addr, maxTorque);
	}

	private native void jniSetMaxTorque (long addr, float maxTorque); /*
		b2MotorJoint* joint = (b2MotorJoint*)addr;
		joint->SetMaxTorque(maxTorque);
	*/

	public float getCorrectionFactor () {
		return jniGetCorrectionFactor(addr);
	}

	private native float jniGetCorrectionFactor (long addr); /*
		b2MotorJoint* joint = (b2MotorJoint*)addr;
		return joint->GetCorrectionFactor();
	*/

	public void setCorrectionFactor (float correctionFactor) {
		jniSetCorrectionFactor(addr, correctionFactor);
	}

	private native void jniSetCorrectionFactor (long addr, float correctionFactor); /*
		b2MotorJoint* joint = (b2MotorJoint*)addr;
		joint->SetCorrectionFactor(correctionFactor);
	*/

}
