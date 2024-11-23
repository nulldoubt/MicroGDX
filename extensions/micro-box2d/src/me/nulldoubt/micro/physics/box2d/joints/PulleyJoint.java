package me.nulldoubt.micro.physics.box2d.joints;

import me.nulldoubt.micro.math.Vector2;
import me.nulldoubt.micro.physics.box2d.Joint;
import me.nulldoubt.micro.physics.box2d.World;

public class PulleyJoint extends Joint {
	/*JNI
#include <Box2D/Box2D.h> 
	 */
	
	public PulleyJoint(World world, long addr) {
		super(world, addr);
	}
	
	private final float[] tmp = new float[2];
	private final Vector2 groundAnchorA = new Vector2();
	
	public Vector2 getGroundAnchorA() {
		jniGetGroundAnchorA(addr, tmp);
		groundAnchorA.set(tmp[0], tmp[1]);
		return groundAnchorA;
	}
	
	private native void jniGetGroundAnchorA(long addr, float[] anchor); /*
		b2PulleyJoint* joint = (b2PulleyJoint*)addr;
		anchor[0] = joint->GetGroundAnchorA().x;
		anchor[1] = joint->GetGroundAnchorA().y;
	*/
	
	private final Vector2 groundAnchorB = new Vector2();
	
	public Vector2 getGroundAnchorB() {
		jniGetGroundAnchorB(addr, tmp);
		groundAnchorB.set(tmp[0], tmp[1]);
		return groundAnchorB;
	}
	
	private native void jniGetGroundAnchorB(long addr, float[] anchor); /*
		b2PulleyJoint* joint = (b2PulleyJoint*)addr;
		anchor[0] = joint->GetGroundAnchorB().x;
		anchor[1] = joint->GetGroundAnchorB().y;
	*/
	
	public float getLength1() {
		return jniGetLength1(addr);
	}
	
	private native float jniGetLength1(long addr); /*
		b2PulleyJoint* joint = (b2PulleyJoint*)addr;
		return joint->GetLengthA();
	*/
	
	public float getLength2() {
		return jniGetLength2(addr);
	}
	
	private native float jniGetLength2(long addr); /*
		b2PulleyJoint* joint = (b2PulleyJoint*)addr;
		return joint->GetLengthB();
	*/
	
	public float getRatio() {
		return jniGetRatio(addr);
	}
	
	private native float jniGetRatio(long addr); /*
		b2PulleyJoint* joint = (b2PulleyJoint*)addr;
		return joint->GetRatio();
	*/
	
}
