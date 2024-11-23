package me.nulldoubt.micro.physics.box2d;

import com.badlogic.gdx.utils.SharedLibraryLoader;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.math.Vector2;
import me.nulldoubt.micro.physics.box2d.JointDef.JointType;
import me.nulldoubt.micro.physics.box2d.joints.*;
import me.nulldoubt.micro.utils.Disposable;
import me.nulldoubt.micro.utils.collections.Array;
import me.nulldoubt.micro.utils.collections.LongMap;
import me.nulldoubt.micro.utils.pools.Pool;

import java.util.Iterator;

public final class World implements Disposable {
	/*JNI
#include <Box2D/Box2D.h>

static jclass worldClass = 0;
static jmethodID shouldCollideID = 0;
static jmethodID beginContactID = 0;
static jmethodID endContactID = 0;
static jmethodID preSolveID = 0;
static jmethodID postSolveID = 0;
static jmethodID reportFixtureID = 0;
static jmethodID reportRayFixtureID = 0;

class CustomRayCastCallback: public b2RayCastCallback
{
private:
	JNIEnv* env;
	jobject obj;

public:
	CustomRayCastCallback( JNIEnv *env, jobject obj )
	{
		this->env = env;
		this->obj = obj;
	}

	virtual float32 ReportFixture( b2Fixture* fixture, const b2Vec2& point, const b2Vec2& normal, float32 fraction)
	{
		return env->CallFloatMethod(obj, reportRayFixtureID, (jlong)fixture, (jfloat)point.x, (jfloat)point.y,
																(jfloat)normal.x, (jfloat)normal.y, (jfloat)fraction );
	}
};

class CustomContactFilter: public b2ContactFilter
{
private:
	JNIEnv* env;
	jobject obj;

public:
	CustomContactFilter( JNIEnv* env, jobject obj )
	{
		this->env = env;
		this->obj = obj;
	}

	virtual bool ShouldCollide(b2Fixture* fixtureA, b2Fixture* fixtureB)
	{
		if( shouldCollideID != 0 )
			return env->CallBooleanMethod( obj, shouldCollideID, (jlong)fixtureA, (jlong)fixtureB );
		else
			return true;
	}
};

class CustomContactListener: public b2ContactListener
{
private:
	JNIEnv* env;
	jobject obj;

public:
		CustomContactListener( JNIEnv* env, jobject obj )
		{
			this->env = env;
			this->obj = obj;
		}

		/// Called when two fixtures begin to touch.
		virtual void BeginContact(b2Contact* contact)
		{
			if( beginContactID != 0 )
				env->CallVoidMethod(obj, beginContactID, (jlong)contact );
		}

		/// Called when two fixtures cease to touch.
		virtual void EndContact(b2Contact* contact)
		{
			if( endContactID != 0 )
				env->CallVoidMethod(obj, endContactID, (jlong)contact);
		}
		
		/// This is called after a contact is updated.
		virtual void PreSolve(b2Contact* contact, const b2Manifold* oldManifold)
		{
			if( preSolveID != 0 )
				env->CallVoidMethod(obj, preSolveID, (jlong)contact, (jlong)oldManifold);
		}
	
		/// This lets you inspect a contact after the solver is finished.
		virtual void PostSolve(b2Contact* contact, const b2ContactImpulse* impulse)
		{
			if( postSolveID != 0 )
				env->CallVoidMethod(obj, postSolveID, (jlong)contact, (jlong)impulse);
		}
};

class CustomQueryCallback: public b2QueryCallback
{
private:
	JNIEnv* env;
	jobject obj;

public:
	CustomQueryCallback( JNIEnv* env, jobject obj )
	{
		this->env = env;
		this->obj = obj;
	}

	virtual bool ReportFixture( b2Fixture* fixture )
	{
		return env->CallBooleanMethod(obj, reportFixtureID, (jlong)fixture );
	}
}; 

inline b2BodyType getBodyType( int type )
{
	switch( type )
	{
	case 0: return b2_staticBody;
	case 1: return b2_kinematicBody;
	case 2: return b2_dynamicBody;
	default:
		return b2_staticBody;
	}
}

b2ContactFilter defaultFilter;
	 */
	
	static {
		new SharedLibraryLoader().load("micro-box2d");
	}
	
	final Pool<Body> freeBodies = new Pool<>(100, 200) {
		@Override
		protected Body newObject() {
			return new Body(World.this, 0);
		}
	};
	
	final Pool<Fixture> freeFixtures = new Pool<>(100, 200) {
		@Override
		protected Fixture newObject() {
			return new Fixture(null, 0);
		}
	};
	
	final long addr;
	
	final LongMap<Body> bodies = new LongMap<Body>(100);
	final LongMap<Fixture> fixtures = new LongMap<>(100);
	final LongMap<Joint> joints = new LongMap<>(100);
	
	ContactFilter contactFilter = null;
	ContactListener contactListener = null;
	
	public World(Vector2 gravity, boolean doSleep) {
		addr = newWorld(gravity.x, gravity.y, doSleep);
		
		contacts.ensureCapacity(contactAddrs.length);
		freeContacts.ensureCapacity(contactAddrs.length);
		
		for (int i = 0; i < contactAddrs.length; i++)
			freeContacts.add(new Contact(this, 0));
	}
	
	private native long newWorld(float gravityX, float gravityY, boolean doSleep); /*
		if(!worldClass) {
			worldClass = (jclass)env->NewGlobalRef(env->GetObjectClass(object));
			beginContactID = env->GetMethodID(worldClass, "beginContact", "(J)V" );
			endContactID = env->GetMethodID( worldClass, "endContact", "(J)V" );
			preSolveID = env->GetMethodID( worldClass, "preSolve", "(JJ)V" );
			postSolveID = env->GetMethodID( worldClass, "postSolve", "(JJ)V" );
			reportFixtureID = env->GetMethodID(worldClass, "reportFixture", "(J)Z" );
			reportRayFixtureID = env->GetMethodID(worldClass, "reportRayFixture", "(JFFFFF)F" );
			shouldCollideID = env->GetMethodID( worldClass, "contactFilter", "(JJ)Z");
		}
	
		b2World* world = new b2World( b2Vec2( gravityX, gravityY ));
		world->SetAllowSleeping( doSleep );
		return (jlong)world;
	*/
	
	public void setDestructionListener(DestructionListener listener) {}
	
	public void setContactFilter(ContactFilter filter) {
		this.contactFilter = filter;
		setUseDefaultContactFilter(filter == null);
	}
	
	private native void setUseDefaultContactFilter(boolean use); /*
		// FIXME
	*/
	
	public void setContactListener(ContactListener listener) {
		this.contactListener = listener;
	}
	
	public Body createBody(BodyDef def) {
		long bodyAddr = jniCreateBody(addr, def.type.getValue(), def.position.x, def.position.y, def.angle, def.linearVelocity.x,
				def.linearVelocity.y, def.angularVelocity, def.linearDamping, def.angularDamping, def.allowSleep, def.awake,
				def.fixedRotation, def.bullet, def.active, def.gravityScale);
		Body body = freeBodies.obtain();
		body.reset(bodyAddr);
		this.bodies.put(body.addr, body);
		return body;
	}
	
	private native long jniCreateBody(long addr, int type, float positionX, float positionY, float angle, float linearVelocityX,
									  float linearVelocityY, float angularVelocity, float linearDamping, float angularDamping, boolean allowSleep, boolean awake,
									  boolean fixedRotation, boolean bullet, boolean active, float inertiaScale); /*
		b2BodyDef bodyDef;
		bodyDef.type = getBodyType(type);
		bodyDef.position.Set( positionX, positionY );
		bodyDef.angle = angle;
		bodyDef.linearVelocity.Set( linearVelocityX, linearVelocityY );
		bodyDef.angularVelocity = angularVelocity;
		bodyDef.linearDamping = linearDamping;
		bodyDef.angularDamping = angularDamping;
		bodyDef.allowSleep = allowSleep;
		bodyDef.awake = awake;
		bodyDef.fixedRotation = fixedRotation;
		bodyDef.bullet = bullet;
		bodyDef.active = active;
		bodyDef.gravityScale = inertiaScale;
	
		b2World* world = (b2World*)addr;
		b2Body* body = world->CreateBody( &bodyDef );
		return (jlong)body;
	*/
	
	public void destroyBody(Body body) {
		Array<JointEdge> jointList = body.getJointList();
		while (jointList.size > 0)
			destroyJoint(body.getJointList().get(0).joint);
		jniDestroyBody(addr, body.addr);
		body.setUserData(null);
		this.bodies.remove(body.addr);
		Array<Fixture> fixtureList = body.getFixtureList();
		while (fixtureList.size > 0) {
			Fixture fixtureToDelete = fixtureList.removeIndex(0);
			fixtureToDelete.setUserData(null);
			this.fixtures.remove(fixtureToDelete.addr);
			freeFixtures.free(fixtureToDelete);
		}
		
		freeBodies.free(body);
	}
	
	private native void jniDestroyBody(long addr, long bodyAddr); /*
		b2World* world = (b2World*)addr;
		b2Body* body = (b2Body*)bodyAddr;
		CustomContactFilter contactFilter(env, object);
		CustomContactListener contactListener(env,object);
		world->SetContactFilter(&contactFilter);
		world->SetContactListener(&contactListener);
		world->DestroyBody(body);
		world->SetContactFilter(&defaultFilter);
		world->SetContactListener(0);
	*/
	
	void destroyFixture(Body body, Fixture fixture) {
		jniDestroyFixture(addr, body.addr, fixture.addr);
	}
	
	private native void jniDestroyFixture(long addr, long bodyAddr, long fixtureAddr); /*
		b2World* world = (b2World*)(addr);
		b2Body* body = (b2Body*)(bodyAddr);
		b2Fixture* fixture = (b2Fixture*)(fixtureAddr);
		CustomContactFilter contactFilter(env, object);
		CustomContactListener contactListener(env, object);
		world->SetContactFilter(&contactFilter);
		world->SetContactListener(&contactListener);
		body->DestroyFixture(fixture);
		world->SetContactFilter(&defaultFilter);
		world->SetContactListener(0);
	*/
	
	void deactivateBody(Body body) {
		jniDeactivateBody(addr, body.addr);
	}
	
	private native void jniDeactivateBody(long addr, long bodyAddr); /*
		b2World* world = (b2World*)(addr);
		b2Body* body = (b2Body*)(bodyAddr);	
		CustomContactFilter contactFilter(env, object);
		CustomContactListener contactListener(env, object);
		world->SetContactFilter(&contactFilter);
		world->SetContactListener(&contactListener);
		body->SetActive(false);
		world->SetContactFilter(&defaultFilter);
		world->SetContactListener(0);
	*/
	
	public Joint createJoint(JointDef def) {
		long jointAddr = createProperJoint(def);
		Joint joint = null;
		if (def.type == JointType.DistanceJoint)
			joint = new DistanceJoint(this, jointAddr);
		if (def.type == JointType.FrictionJoint)
			joint = new FrictionJoint(this, jointAddr);
		if (def.type == JointType.GearJoint)
			joint = new GearJoint(this, jointAddr, ((GearJointDef) def).joint1, ((GearJointDef) def).joint2);
		if (def.type == JointType.MotorJoint)
			joint = new MotorJoint(this, jointAddr);
		if (def.type == JointType.MouseJoint)
			joint = new MouseJoint(this, jointAddr);
		if (def.type == JointType.PrismaticJoint)
			joint = new PrismaticJoint(this, jointAddr);
		if (def.type == JointType.PulleyJoint)
			joint = new PulleyJoint(this, jointAddr);
		if (def.type == JointType.RevoluteJoint)
			joint = new RevoluteJoint(this, jointAddr);
		if (def.type == JointType.RopeJoint)
			joint = new RopeJoint(this, jointAddr);
		if (def.type == JointType.WeldJoint)
			joint = new WeldJoint(this, jointAddr);
		if (def.type == JointType.WheelJoint)
			joint = new WheelJoint(this, jointAddr);
		if (joint == null)
			throw new MicroRuntimeException("Unknown joint type: " + def.type);
		joints.put(joint.addr, joint);
		JointEdge jointEdgeA = new JointEdge(def.bodyB, joint);
		JointEdge jointEdgeB = new JointEdge(def.bodyA, joint);
		joint.jointEdgeA = jointEdgeA;
		joint.jointEdgeB = jointEdgeB;
		def.bodyA.joints.add(jointEdgeA);
		def.bodyB.joints.add(jointEdgeB);
		return joint;
	}
	
	private long createProperJoint(JointDef def) {
		if (def.type == JointType.DistanceJoint) {
			DistanceJointDef d = (DistanceJointDef) def;
			return jniCreateDistanceJoint(addr, d.bodyA.addr, d.bodyB.addr, d.collideConnected, d.localAnchorA.x, d.localAnchorA.y,
					d.localAnchorB.x, d.localAnchorB.y, d.length, d.frequencyHz, d.dampingRatio);
		}
		if (def.type == JointType.FrictionJoint) {
			FrictionJointDef d = (FrictionJointDef) def;
			return jniCreateFrictionJoint(addr, d.bodyA.addr, d.bodyB.addr, d.collideConnected, d.localAnchorA.x, d.localAnchorA.y,
					d.localAnchorB.x, d.localAnchorB.y, d.maxForce, d.maxTorque);
		}
		if (def.type == JointType.GearJoint) {
			GearJointDef d = (GearJointDef) def;
			return jniCreateGearJoint(addr, d.bodyA.addr, d.bodyB.addr, d.collideConnected, d.joint1.addr, d.joint2.addr, d.ratio);
		}
		if (def.type == JointType.MotorJoint) {
			MotorJointDef d = (MotorJointDef) def;
			return jniCreateMotorJoint(addr, d.bodyA.addr, d.bodyB.addr, d.collideConnected, d.linearOffset.x, d.linearOffset.y,
					d.angularOffset, d.maxForce, d.maxTorque, d.correctionFactor);
		}
		if (def.type == JointType.MouseJoint) {
			MouseJointDef d = (MouseJointDef) def;
			return jniCreateMouseJoint(addr, d.bodyA.addr, d.bodyB.addr, d.collideConnected, d.target.x, d.target.y, d.maxForce,
					d.frequencyHz, d.dampingRatio);
		}
		if (def.type == JointType.PrismaticJoint) {
			PrismaticJointDef d = (PrismaticJointDef) def;
			return jniCreatePrismaticJoint(addr, d.bodyA.addr, d.bodyB.addr, d.collideConnected, d.localAnchorA.x, d.localAnchorA.y,
					d.localAnchorB.x, d.localAnchorB.y, d.localAxisA.x, d.localAxisA.y, d.referenceAngle, d.enableLimit,
					d.lowerTranslation, d.upperTranslation, d.enableMotor, d.maxMotorForce, d.motorSpeed);
		}
		if (def.type == JointType.PulleyJoint) {
			PulleyJointDef d = (PulleyJointDef) def;
			return jniCreatePulleyJoint(addr, d.bodyA.addr, d.bodyB.addr, d.collideConnected, d.groundAnchorA.x, d.groundAnchorA.y,
					d.groundAnchorB.x, d.groundAnchorB.y, d.localAnchorA.x, d.localAnchorA.y, d.localAnchorB.x, d.localAnchorB.y,
					d.lengthA, d.lengthB, d.ratio);
			
		}
		if (def.type == JointType.RevoluteJoint) {
			RevoluteJointDef d = (RevoluteJointDef) def;
			return jniCreateRevoluteJoint(addr, d.bodyA.addr, d.bodyB.addr, d.collideConnected, d.localAnchorA.x, d.localAnchorA.y,
					d.localAnchorB.x, d.localAnchorB.y, d.referenceAngle, d.enableLimit, d.lowerAngle, d.upperAngle, d.enableMotor,
					d.motorSpeed, d.maxMotorTorque);
		}
		if (def.type == JointType.RopeJoint) {
			RopeJointDef d = (RopeJointDef) def;
			return jniCreateRopeJoint(addr, d.bodyA.addr, d.bodyB.addr, d.collideConnected, d.localAnchorA.x, d.localAnchorA.y,
					d.localAnchorB.x, d.localAnchorB.y, d.maxLength);
		}
		if (def.type == JointType.WeldJoint) {
			WeldJointDef d = (WeldJointDef) def;
			return jniCreateWeldJoint(addr, d.bodyA.addr, d.bodyB.addr, d.collideConnected, d.localAnchorA.x, d.localAnchorA.y,
					d.localAnchorB.x, d.localAnchorB.y, d.referenceAngle, d.frequencyHz, d.dampingRatio);
		}
		if (def.type == JointType.WheelJoint) {
			WheelJointDef d = (WheelJointDef) def;
			return jniCreateWheelJoint(addr, d.bodyA.addr, d.bodyB.addr, d.collideConnected, d.localAnchorA.x, d.localAnchorA.y,
					d.localAnchorB.x, d.localAnchorB.y, d.localAxisA.x, d.localAxisA.y, d.enableMotor, d.maxMotorTorque, d.motorSpeed,
					d.frequencyHz, d.dampingRatio);
		}
		
		return 0;
	}
	
	private native long jniCreateWheelJoint(long addr, long bodyA, long bodyB, boolean collideConnected, float localAnchorAX,
											float localAnchorAY, float localAnchorBX, float localAnchorBY, float localAxisAX, float localAxisAY, boolean enableMotor,
											float maxMotorTorque, float motorSpeed, float frequencyHz, float dampingRatio); /*
		b2World* world = (b2World*)addr;
		b2WheelJointDef def;
		def.bodyA = (b2Body*)bodyA;
		def.bodyB = (b2Body*)bodyB;
		def.collideConnected = collideConnected;
		def.localAnchorA = b2Vec2(localAnchorAX, localAnchorAY);
		def.localAnchorB = b2Vec2(localAnchorBX, localAnchorBY);
		def.localAxisA = b2Vec2(localAxisAX, localAxisAY);
		def.enableMotor = enableMotor;
		def.maxMotorTorque = maxMotorTorque;
		def.motorSpeed = motorSpeed;
		def.frequencyHz = frequencyHz;
		def.dampingRatio = dampingRatio;
		
		return (jlong)world->CreateJoint(&def);
	*/
	
	private native long jniCreateRopeJoint(long addr, long bodyA, long bodyB, boolean collideConnected, float localAnchorAX,
										   float localAnchorAY, float localAnchorBX, float localAnchorBY, float maxLength); /*
		b2World* world = (b2World*)addr;
		b2RopeJointDef def;
		def.bodyA = (b2Body*)bodyA;
		def.bodyB = (b2Body*)bodyB;
		def.collideConnected = collideConnected;
		def.localAnchorA = b2Vec2(localAnchorAX, localAnchorAY);
		def.localAnchorB = b2Vec2(localAnchorBX, localAnchorBY);
		def.maxLength = maxLength;
	
		return (jlong)world->CreateJoint(&def);
	*/
	
	private native long jniCreateDistanceJoint(long addr, long bodyA, long bodyB, boolean collideConnected, float localAnchorAX,
											   float localAnchorAY, float localAnchorBX, float localAnchorBY, float length, float frequencyHz, float dampingRatio); /*
		b2World* world = (b2World*)addr;
		b2DistanceJointDef def;
		def.bodyA = (b2Body*)bodyA;
		def.bodyB = (b2Body*)bodyB;
		def.collideConnected = collideConnected;
		def.localAnchorA = b2Vec2(localAnchorAX, localAnchorAY);
		def.localAnchorB = b2Vec2(localAnchorBX, localAnchorBY);
		def.length = length;
		def.frequencyHz = frequencyHz;
		def.dampingRatio = dampingRatio;
	
		return (jlong)world->CreateJoint(&def);
	*/
	
	private native long jniCreateFrictionJoint(long addr, long bodyA, long bodyB, boolean collideConnected, float localAnchorAX,
											   float localAnchorAY, float localAnchorBX, float localAnchorBY, float maxForce, float maxTorque); /*
		b2World* world = (b2World*)addr;
		b2FrictionJointDef def;
		def.bodyA = (b2Body*)bodyA;
		def.bodyB = (b2Body*)bodyB;
		def.collideConnected = collideConnected;
		def.localAnchorA = b2Vec2(localAnchorAX, localAnchorAY);
		def.localAnchorB = b2Vec2(localAnchorBX, localAnchorBY);
		def.maxForce = maxForce;
		def.maxTorque = maxTorque;
		return (jlong)world->CreateJoint(&def);
	*/
	
	private native long jniCreateGearJoint(long addr, long bodyA, long bodyB, boolean collideConnected, long joint1, long joint2,
										   float ratio); /*
		b2World* world = (b2World*)addr;
		b2GearJointDef def;
		def.bodyA = (b2Body*)bodyA;
		def.bodyB = (b2Body*)bodyB;
		def.collideConnected = collideConnected;
		def.joint1 = (b2Joint*)joint1;
		def.joint2 = (b2Joint*)joint2;
		def.ratio = ratio;
		return (jlong)world->CreateJoint(&def);
	*/
	
	private native long jniCreateMotorJoint(long addr, long bodyA, long bodyB, boolean collideConnected, float linearOffsetX,
											float linearOffsetY, float angularOffset, float maxForce, float maxTorque, float correctionFactor); /*
		b2World* world = (b2World*)addr;
		b2MotorJointDef def;
		def.bodyA = (b2Body*)bodyA;
		def.bodyB = (b2Body*)bodyB;
		def.collideConnected = collideConnected;
		def.linearOffset = b2Vec2( linearOffsetX, linearOffsetY );
		def.angularOffset = angularOffset;
		def.maxForce = maxForce;
		def.maxTorque = maxTorque;
		def.correctionFactor = correctionFactor;
		return (jlong)world->CreateJoint(&def);
	*/
	
	private native long jniCreateMouseJoint(long addr, long bodyA, long bodyB, boolean collideConnected, float targetX,
											float targetY, float maxForce, float frequencyHz, float dampingRatio); /*
		b2World* world = (b2World*)addr;
		b2MouseJointDef def;
		def.bodyA = (b2Body*)bodyA;
		def.bodyB = (b2Body*)bodyB;
		def.collideConnected = collideConnected;
		def.target = b2Vec2( targetX, targetY );
		def.maxForce = maxForce;
		def.frequencyHz = frequencyHz;
		def.dampingRatio = dampingRatio;
		return (jlong)world->CreateJoint(&def);
	*/
	
	private native long jniCreatePrismaticJoint(long addr, long bodyA, long bodyB, boolean collideConnected, float localAnchorAX,
												float localAnchorAY, float localAnchorBX, float localAnchorBY, float localAxisAX, float localAxisAY, float referenceAngle,
												boolean enableLimit, float lowerTranslation, float upperTranslation, boolean enableMotor, float maxMotorForce,
												float motorSpeed); /*
		b2World* world = (b2World*)addr;
		b2PrismaticJointDef def;
		def.bodyA = (b2Body*)bodyA;
		def.bodyB = (b2Body*)bodyB;
		def.collideConnected = collideConnected;
		def.localAnchorA = b2Vec2(localAnchorAX, localAnchorAY);
		def.localAnchorB = b2Vec2(localAnchorBX, localAnchorBY);
		def.localAxisA = b2Vec2( localAxisAX, localAxisAY );
		def.referenceAngle = referenceAngle;
		def.enableLimit = enableLimit;
		def.lowerTranslation = lowerTranslation;
		def.upperTranslation = upperTranslation;
		def.enableMotor = enableMotor;
		def.maxMotorForce = maxMotorForce;
		def.motorSpeed = motorSpeed;
		return (jlong)world->CreateJoint(&def);
	*/
	
	private native long jniCreatePulleyJoint(long addr, long bodyA, long bodyB, boolean collideConnected, float groundAnchorAX,
											 float groundAnchorAY, float groundAnchorBX, float groundAnchorBY, float localAnchorAX, float localAnchorAY,
											 float localAnchorBX, float localAnchorBY, float lengthA, float lengthB, float ratio); /*
		b2World* world = (b2World*)addr;
		b2PulleyJointDef def;
		def.bodyA = (b2Body*)bodyA;
		def.bodyB = (b2Body*)bodyB;
		def.collideConnected = collideConnected;
		def.groundAnchorA = b2Vec2( groundAnchorAX, groundAnchorAY );
		def.groundAnchorB = b2Vec2( groundAnchorBX, groundAnchorBY );
		def.localAnchorA = b2Vec2(localAnchorAX, localAnchorAY);
		def.localAnchorB = b2Vec2(localAnchorBX, localAnchorBY);
		def.lengthA = lengthA;
		def.lengthB = lengthB;
		def.ratio = ratio;
	
		return (jlong)world->CreateJoint(&def);
	*/
	
	private native long jniCreateRevoluteJoint(long addr, long bodyA, long bodyB, boolean collideConnected, float localAnchorAX,
											   float localAnchorAY, float localAnchorBX, float localAnchorBY, float referenceAngle, boolean enableLimit, float lowerAngle,
											   float upperAngle, boolean enableMotor, float motorSpeed, float maxMotorTorque); /*
		b2World* world = (b2World*)addr;
		b2RevoluteJointDef def;
		def.bodyA = (b2Body*)bodyA;
		def.bodyB = (b2Body*)bodyB;
		def.collideConnected = collideConnected;
		def.localAnchorA = b2Vec2(localAnchorAX, localAnchorAY);
		def.localAnchorB = b2Vec2(localAnchorBX, localAnchorBY);
		def.referenceAngle = referenceAngle;
		def.enableLimit = enableLimit;
		def.lowerAngle = lowerAngle;
		def.upperAngle = upperAngle;
		def.enableMotor = enableMotor;
		def.motorSpeed = motorSpeed;
		def.maxMotorTorque = maxMotorTorque;
		return (jlong)world->CreateJoint(&def);
	*/
	
	private native long jniCreateWeldJoint(long addr, long bodyA, long bodyB, boolean collideConnected, float localAnchorAX,
										   float localAnchorAY, float localAnchorBX, float localAnchorBY, float referenceAngle, float frequencyHz, float dampingRatio); /*
		b2World* world = (b2World*)addr;
		b2WeldJointDef def;
		def.bodyA = (b2Body*)bodyA;
		def.bodyB = (b2Body*)bodyB;
		def.collideConnected = collideConnected;
		def.localAnchorA = b2Vec2(localAnchorAX, localAnchorAY);
		def.localAnchorB = b2Vec2(localAnchorBX, localAnchorBY);
		def.referenceAngle = referenceAngle;
		def.frequencyHz = frequencyHz;
		def.dampingRatio = dampingRatio;
	
		return (jlong)world->CreateJoint(&def);
	*/
	
	public void destroyJoint(Joint joint) {
		joint.setUserData(null);
		joints.remove(joint.addr);
		joint.jointEdgeA.other.joints.removeValue(joint.jointEdgeB, true);
		joint.jointEdgeB.other.joints.removeValue(joint.jointEdgeA, true);
		jniDestroyJoint(addr, joint.addr);
	}
	
	private native void jniDestroyJoint(long addr, long jointAddr); /*
		b2World* world = (b2World*)addr;
		b2Joint* joint = (b2Joint*)jointAddr;
		CustomContactFilter contactFilter(env, object);
		CustomContactListener contactListener(env,object);
		world->SetContactFilter(&contactFilter);
		world->SetContactListener(&contactListener);
		world->DestroyJoint( joint );
		world->SetContactFilter(&defaultFilter);
		world->SetContactListener(0);
	*/
	
	public void step(float timeStep, int velocityIterations, int positionIterations) {
		jniStep(addr, timeStep, velocityIterations, positionIterations);
	}
	
	private native void jniStep(long addr, float timeStep, int velocityIterations, int positionIterations); /*
		b2World* world = (b2World*)addr;
		CustomContactFilter contactFilter(env, object);
		CustomContactListener contactListener(env,object);
		world->SetContactFilter(&contactFilter);
		world->SetContactListener(&contactListener);
		world->Step( timeStep, velocityIterations, positionIterations );
		world->SetContactFilter(&defaultFilter);
		world->SetContactListener(0);
	*/
	
	public void clearForces() {
		jniClearForces(addr);
	}
	
	private native void jniClearForces(long addr); /*
		b2World* world = (b2World*)addr;
		world->ClearForces();
	*/
	
	public void setWarmStarting(boolean flag) {
		jniSetWarmStarting(addr, flag);
	}
	
	private native void jniSetWarmStarting(long addr, boolean flag); /*
		b2World* world = (b2World*)addr;
		world->SetWarmStarting(flag);
	*/
	
	public void setContinuousPhysics(boolean flag) {
		jniSetContiousPhysics(addr, flag);
	}
	
	private native void jniSetContiousPhysics(long addr, boolean flag); /*
		b2World* world = (b2World*)addr;
		world->SetContinuousPhysics(flag);
	*/
	
	public int getProxyCount() {
		return jniGetProxyCount(addr);
	}
	
	private native int jniGetProxyCount(long addr); /*
		b2World* world = (b2World*)addr;
		return world->GetProxyCount();
	*/
	
	public int getBodyCount() {
		return jniGetBodyCount(addr);
	}
	
	private native int jniGetBodyCount(long addr); /*
		b2World* world = (b2World*)addr;
		return world->GetBodyCount();
	*/
	
	public int getFixtureCount() {
		return fixtures.size;
	}
	
	public int getJointCount() {
		return jniGetJointcount(addr);
	}
	
	private native int jniGetJointcount(long addr); /*
		b2World* world = (b2World*)addr;
		return world->GetJointCount();
	*/
	
	public int getContactCount() {
		return jniGetContactCount(addr);
	}
	
	private native int jniGetContactCount(long addr); /*
		b2World* world = (b2World*)addr;
		return world->GetContactCount();
	*/
	
	public void setGravity(Vector2 gravity) {
		jniSetGravity(addr, gravity.x, gravity.y);
	}
	
	private native void jniSetGravity(long addr, float gravityX, float gravityY); /*
		b2World* world = (b2World*)addr;
		world->SetGravity( b2Vec2( gravityX, gravityY ) );
	*/
	
	final float[] tmpGravity = new float[2];
	final Vector2 gravity = new Vector2();
	
	public Vector2 getGravity() {
		jniGetGravity(addr, tmpGravity);
		gravity.x = tmpGravity[0];
		gravity.y = tmpGravity[1];
		return gravity;
	}
	
	private native void jniGetGravity(long addr, float[] gravity); /*
		b2World* world = (b2World*)addr;
		b2Vec2 g = world->GetGravity();
		gravity[0] = g.x;
		gravity[1] = g.y;
	*/
	
	public boolean isLocked() {
		return jniIsLocked(addr);
	}
	
	private native boolean jniIsLocked(long addr); /*
		b2World* world = (b2World*)addr;
		return world->IsLocked();
	*/
	
	public void setAutoClearForces(boolean flag) {
		jniSetAutoClearForces(addr, flag);
	}
	
	private native void jniSetAutoClearForces(long addr, boolean flag); /*
		b2World* world = (b2World*)addr;
		world->SetAutoClearForces(flag);
	*/
	
	public boolean getAutoClearForces() {
		return jniGetAutoClearForces(addr);
	}
	
	private native boolean jniGetAutoClearForces(long addr); /*
		b2World* world = (b2World*)addr;
		return world->GetAutoClearForces();
	*/
	
	public void QueryAABB(QueryCallback callback, float lowerX, float lowerY, float upperX, float upperY) {
		queryCallback = callback;
		jniQueryAABB(addr, lowerX, lowerY, upperX, upperY);
	}
	
	private QueryCallback queryCallback = null;
	
	private native void jniQueryAABB(long addr, float lowX, float lowY, float upX, float upY); /*
		b2World* world = (b2World*)addr;
		b2AABB aabb;
		aabb.lowerBound = b2Vec2( lowX, lowY );
		aabb.upperBound = b2Vec2( upX, upY );
	
		CustomQueryCallback callback( env, object );
		world->QueryAABB( &callback, aabb );
	*/
	
	private long[] contactAddrs = new long[200];
	private final Array<Contact> contacts = new Array<>();
	private final Array<Contact> freeContacts = new Array<>();
	
	public Array<Contact> getContactList() {
		int numContacts = getContactCount();
		if (numContacts > contactAddrs.length) {
			int newSize = 2 * numContacts;
			contactAddrs = new long[newSize];
			contacts.ensureCapacity(newSize);
			freeContacts.ensureCapacity(newSize);
		}
		if (numContacts > freeContacts.size) {
			int freeConts = freeContacts.size;
			for (int i = 0; i < numContacts - freeConts; i++)
				freeContacts.add(new Contact(this, 0));
		}
		jniGetContactList(addr, contactAddrs);
		
		contacts.clear();
		for (int i = 0; i < numContacts; i++) {
			Contact contact = freeContacts.get(i);
			contact.addr = contactAddrs[i];
			contacts.add(contact);
		}
		
		return contacts;
	}
	
	public void getBodies(Array<Body> bodies) {
		bodies.clear();
		bodies.ensureCapacity(this.bodies.size);
		for (Iterator<Body> iter = this.bodies.values(); iter.hasNext(); ) {
			bodies.add(iter.next());
		}
	}
	
	public void getFixtures(Array<Fixture> fixtures) {
		fixtures.clear();
		fixtures.ensureCapacity(this.fixtures.size);
		for (Iterator<Fixture> iter = this.fixtures.values(); iter.hasNext(); ) {
			fixtures.add(iter.next());
		}
	}
	
	public void getJoints(Array<Joint> joints) {
		joints.clear();
		joints.ensureCapacity(this.joints.size);
		for (Iterator<Joint> iter = this.joints.values(); iter.hasNext(); ) {
			joints.add(iter.next());
		}
	}
	
	private native void jniGetContactList(long addr, long[] contacts); /*
		b2World* world = (b2World*)addr;
	
		b2Contact* contact = world->GetContactList();
		int i = 0;
		while( contact != 0 )
		{
			contacts[i++] = (long long)contact;
			contact = contact->GetNext();
		}
	*/
	
	public void dispose() {
		jniDispose(addr);
	}
	
	private native void jniDispose(long addr); /*
		b2World* world = (b2World*)(addr);
		delete world;
	*/
	
	private boolean contactFilter(long fixtureA, long fixtureB) {
		if (contactFilter != null)
			return contactFilter.shouldCollide(fixtures.get(fixtureA), fixtures.get(fixtureB));
		else {
			Filter filterA = fixtures.get(fixtureA).getFilterData();
			Filter filterB = fixtures.get(fixtureB).getFilterData();
			
			if (filterA.groupIndex == filterB.groupIndex && filterA.groupIndex != 0)
				return filterA.groupIndex > 0;
			
			return (filterA.maskBits & filterB.categoryBits) != 0 && (filterA.categoryBits & filterB.maskBits) != 0;
		}
	}
	
	private final Contact contact = new Contact(this, 0);
	private final Manifold manifold = new Manifold(0);
	private final ContactImpulse impulse = new ContactImpulse(this, 0);
	
	private void beginContact(long contactAddr) {
		if (contactListener != null) {
			contact.addr = contactAddr;
			contactListener.beginContact(contact);
		}
	}
	
	private void endContact(long contactAddr) {
		if (contactListener != null) {
			contact.addr = contactAddr;
			contactListener.endContact(contact);
		}
	}
	
	private void preSolve(long contactAddr, long manifoldAddr) {
		if (contactListener != null) {
			contact.addr = contactAddr;
			manifold.addr = manifoldAddr;
			contactListener.preSolve(contact, manifold);
		}
	}
	
	private void postSolve(long contactAddr, long impulseAddr) {
		if (contactListener != null) {
			contact.addr = contactAddr;
			impulse.addr = impulseAddr;
			contactListener.postSolve(contact, impulse);
		}
	}
	
	private boolean reportFixture(long addr) {
		if (queryCallback != null)
			return queryCallback.reportFixture(fixtures.get(addr));
		else
			return false;
	}
	
	public static native void setVelocityThreshold(float threshold); /*
		b2_velocityThreshold = threshold;
	*/
	
	public static native float getVelocityThreshold(); /*
		return b2_velocityThreshold;
	*/
	
	public void rayCast(RayCastCallback callback, Vector2 point1, Vector2 point2) {
		rayCast(callback, point1.x, point1.y, point2.x, point2.y);
	}
	
	public void rayCast(RayCastCallback callback, float point1X, float point1Y, float point2X, float point2Y) {
		rayCastCallback = callback;
		jniRayCast(addr, point1X, point1Y, point2X, point2Y);
	}
	
	private RayCastCallback rayCastCallback = null;
	
	private native void jniRayCast(long addr, float aX, float aY, float bX, float bY); /*
		b2World *world = (b2World*)addr;
		CustomRayCastCallback callback( env, object );	
		world->RayCast( &callback, b2Vec2(aX,aY), b2Vec2(bX,bY) );
	*/
	
	private Vector2 rayPoint = new Vector2();
	private Vector2 rayNormal = new Vector2();
	
	private float reportRayFixture(long addr, float pX, float pY, float nX, float nY, float fraction) {
		if (rayCastCallback != null) {
			rayPoint.x = pX;
			rayPoint.y = pY;
			rayNormal.x = nX;
			rayNormal.y = nY;
			return rayCastCallback.reportRayFixture(fixtures.get(addr), rayPoint, rayNormal, fraction);
		} else {
			return 0.0f;
		}
	}
	
}
