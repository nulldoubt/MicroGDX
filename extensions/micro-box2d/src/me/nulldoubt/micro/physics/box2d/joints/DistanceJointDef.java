package me.nulldoubt.micro.physics.box2d.joints;

import me.nulldoubt.micro.math.Vector2;
import me.nulldoubt.micro.physics.box2d.Body;
import me.nulldoubt.micro.physics.box2d.JointDef;

public class DistanceJointDef extends JointDef {
	
	public DistanceJointDef() {
		type = JointType.DistanceJoint;
	}
	
	public void initialize(Body bodyA, Body bodyB, Vector2 anchorA, Vector2 anchorB) {
		this.bodyA = bodyA;
		this.bodyB = bodyB;
		this.localAnchorA.set(bodyA.getLocalPoint(anchorA));
		this.localAnchorB.set(bodyB.getLocalPoint(anchorB));
		this.length = anchorA.dst(anchorB);
	}
	
	public final Vector2 localAnchorA = new Vector2();
	
	public final Vector2 localAnchorB = new Vector2();
	
	public float length = 1;
	
	public float frequencyHz = 0;
	
	public float dampingRatio = 0;
	
}
