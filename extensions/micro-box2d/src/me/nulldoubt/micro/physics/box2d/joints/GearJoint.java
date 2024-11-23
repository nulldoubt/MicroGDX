package me.nulldoubt.micro.physics.box2d.joints;

import me.nulldoubt.micro.physics.box2d.Joint;
import me.nulldoubt.micro.physics.box2d.World;

public class GearJoint extends Joint {
	// @off
	/*JNI
#include <Box2D/Box2D.h> 
	 */
	
	private Joint joint1;
	private Joint joint2;
	
	public GearJoint(World world, long addr, Joint joint1, Joint joint2) {
		super(world, addr);
		this.joint1 = joint1;
		this.joint2 = joint2;
	}
	
	public Joint getJoint1() {
		return joint1;
	}
	
	private native long jniGetJoint1(long addr); /*
		b2GearJoint* joint =  (b2GearJoint*)addr;
		b2Joint* joint1 = joint->GetJoint1();
		return (jlong)joint1;
	*/
	
	/**
	 * Get first joint.
	 */
	public Joint getJoint2() {
		return joint2;
	}
	
	private native long jniGetJoint2(long addr); /*
		b2GearJoint* joint =  (b2GearJoint*)addr;
		b2Joint* joint2 = joint->GetJoint2();
		return (jlong)joint2;
	*/
	
	/**
	 * Set the gear ratio.
	 */
	public void setRatio(float ratio) {
		jniSetRatio(addr, ratio);
	}
	
	private native void jniSetRatio(long addr, float ratio); /*
		b2GearJoint* joint =  (b2GearJoint*)addr;
		joint->SetRatio( ratio );
	*/
	
	/**
	 * Get the gear ratio.
	 */
	public float getRatio() {
		return jniGetRatio(addr);
	}
	
	private native float jniGetRatio(long addr); /*
		b2GearJoint* joint =  (b2GearJoint*)addr;
		return joint->GetRatio();
	*/
	
}
