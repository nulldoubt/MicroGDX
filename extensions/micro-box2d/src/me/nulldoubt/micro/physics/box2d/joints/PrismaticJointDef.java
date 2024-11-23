package me.nulldoubt.micro.physics.box2d.joints;

import me.nulldoubt.micro.math.Vector2;
import me.nulldoubt.micro.physics.box2d.Body;
import me.nulldoubt.micro.physics.box2d.JointDef;

public class PrismaticJointDef extends JointDef {
	
	public PrismaticJointDef() {
		type = JointType.PrismaticJoint;
	}
	
	public void initialize(Body bodyA, Body bodyB, Vector2 anchor, Vector2 axis) {
		this.bodyA = bodyA;
		this.bodyB = bodyB;
		localAnchorA.set(bodyA.getLocalPoint(anchor));
		localAnchorB.set(bodyB.getLocalPoint(anchor));
		localAxisA.set(bodyA.getLocalVector(axis));
		referenceAngle = bodyB.getAngle() - bodyA.getAngle();
		
	}
	
	public final Vector2 localAnchorA = new Vector2();
	
	public final Vector2 localAnchorB = new Vector2();
	
	public final Vector2 localAxisA = new Vector2(1, 0);
	
	public float referenceAngle = 0;
	
	public boolean enableLimit = false;
	
	public float lowerTranslation = 0;
	
	public float upperTranslation = 0;
	
	public boolean enableMotor = false;
	
	public float maxMotorForce = 0;
	
	public float motorSpeed = 0;
	
}
