package me.nulldoubt.micro.physics.box2d.joints;

import me.nulldoubt.micro.math.Vector2;
import me.nulldoubt.micro.physics.box2d.Body;
import me.nulldoubt.micro.physics.box2d.JointDef;

public class WeldJointDef extends JointDef {
	public WeldJointDef () {
		type = JointType.WeldJoint;
	}

	/** Initialize the bodies, anchors, and reference angle using a world anchor point. */
	public void initialize (Body body1, Body body2, Vector2 anchor) {
		this.bodyA = body1;
		this.bodyB = body2;
		this.localAnchorA.set(body1.getLocalPoint(anchor));
		this.localAnchorB.set(body2.getLocalPoint(anchor));
		referenceAngle = body2.getAngle() - body1.getAngle();
	}

	/** The local anchor point relative to body1's origin. */
	public final Vector2 localAnchorA = new Vector2();

	/** The local anchor point relative to body2's origin. */
	public final Vector2 localAnchorB = new Vector2();

	/** The body2 angle minus body1 angle in the reference state (radians). */
	public float referenceAngle = 0;

	/** The mass-spring-damper frequency in Hertz. Rotation only. Disable softness with a value of 0. */
	public float frequencyHz = 0;

	/** The damping ratio. 0 = no damping, 1 = critical damping. */
	public float dampingRatio = 0;

}
