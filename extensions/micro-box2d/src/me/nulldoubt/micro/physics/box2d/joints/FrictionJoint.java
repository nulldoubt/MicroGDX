package me.nulldoubt.micro.physics.box2d.joints;

import me.nulldoubt.micro.math.Vector2;
import me.nulldoubt.micro.physics.box2d.Joint;
import me.nulldoubt.micro.physics.box2d.World;

/** Friction joint. This is used for top-down friction. It provides 2D translational friction and angular friction. */
public class FrictionJoint extends Joint {
	// @off
	/*JNI
#include <Box2D/Box2D.h> 
	 */

	private final float[] tmp = new float[2];
	private final Vector2 localAnchorA = new Vector2();
	private final Vector2 localAnchorB = new Vector2();

	public FrictionJoint (World world, long addr) {
		super(world, addr);
	}

	public Vector2 getLocalAnchorA () {
		jniGetLocalAnchorA(addr, tmp);
		localAnchorA.set(tmp[0], tmp[1]);
		return localAnchorA;
	}

	private native void jniGetLocalAnchorA (long addr, float[] anchor); /*
		b2FrictionJoint* joint = (b2FrictionJoint*)addr;
		anchor[0] = joint->GetLocalAnchorA().x;
		anchor[1] = joint->GetLocalAnchorA().y;
	*/

	public Vector2 getLocalAnchorB () {
		jniGetLocalAnchorB(addr, tmp);
		localAnchorB.set(tmp[0], tmp[1]);
		return localAnchorB;
	}

	private native void jniGetLocalAnchorB (long addr, float[] anchor); /*
		b2FrictionJoint* joint = (b2FrictionJoint*)addr;
		anchor[0] = joint->GetLocalAnchorB().x;
		anchor[1] = joint->GetLocalAnchorB().y;
	*/

	/** Set the maximum friction force in N. */
	public void setMaxForce (float force) {
		jniSetMaxForce(addr, force);
	}

	private native void jniSetMaxForce (long addr, float force); /*
		b2FrictionJoint* joint = (b2FrictionJoint*)addr;
		joint->SetMaxForce( force );
	*/

	/** Get the maximum friction force in N. */
	public float getMaxForce () {
		return jniGetMaxForce(addr);
	}

	private native float jniGetMaxForce (long addr); /*
		b2FrictionJoint* joint = (b2FrictionJoint*)addr;
		return joint->GetMaxForce();
	*/

	/** Set the maximum friction torque in N*m. */
	public void setMaxTorque (float torque) {
		jniSetMaxTorque(addr, torque);
	}

	private native void jniSetMaxTorque (long addr, float torque); /*
		b2FrictionJoint* joint = (b2FrictionJoint*)addr;
		joint->SetMaxTorque( torque );
	*/

	/** Get the maximum friction torque in N*m. */
	public float getMaxTorque () {
		return jniGetMaxTorque(addr);
	}

	private native float jniGetMaxTorque (long addr); /*
		b2FrictionJoint* joint = (b2FrictionJoint*)addr;
		return joint->GetMaxTorque();
	*/
}
