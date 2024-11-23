package me.nulldoubt.micro.physics.box2d;

import me.nulldoubt.micro.math.Vector2;
import me.nulldoubt.micro.physics.box2d.BodyDef.BodyType;
import me.nulldoubt.micro.utils.collections.Array;

public class Body {
	// @off
	/*JNI
#include <Box2D/Box2D.h>
	 */
	
	protected long addr;
	
	private final float[] tmp = new float[4];
	
	private final World world;
	
	private final Array<Fixture> fixtures = new Array<>(2);
	
	protected Array<JointEdge> joints = new Array<>(2);
	
	private Object userData;
	
	protected Body(World world, long addr) {
		this.world = world;
		this.addr = addr;
	}
	
	protected void reset(long addr) {
		this.addr = addr;
		this.userData = null;
		for (int i = 0; i < fixtures.size; i++)
			this.world.freeFixtures.free(fixtures.get(i));
		fixtures.clear();
		this.joints.clear();
	}
	
	public Fixture createFixture(FixtureDef def) {
		long fixtureAddr = jniCreateFixture(addr, def.shape.addr, def.friction, def.restitution, def.density, def.isSensor,
				def.filter.categoryBits, def.filter.maskBits, def.filter.groupIndex);
		Fixture fixture = this.world.freeFixtures.obtain();
		fixture.reset(this, fixtureAddr);
		this.world.fixtures.put(fixture.addr, fixture);
		this.fixtures.add(fixture);
		return fixture;
	}
	
	private native long jniCreateFixture(long addr, long shapeAddr, float friction, float restitution, float density,
										 boolean isSensor, short filterCategoryBits, short filterMaskBits, short filterGroupIndex); /*
	b2Body* body = (b2Body*)addr;
	b2Shape* shape = (b2Shape*)shapeAddr;
	b2FixtureDef fixtureDef;

	fixtureDef.shape = shape;
	fixtureDef.friction = friction;
	fixtureDef.restitution = restitution;
	fixtureDef.density = density;
	fixtureDef.isSensor = isSensor;
	fixtureDef.filter.maskBits = filterMaskBits;
	fixtureDef.filter.categoryBits = filterCategoryBits;
	fixtureDef.filter.groupIndex = filterGroupIndex;

	return (jlong)body->CreateFixture( &fixtureDef );
	*/
	
	public Fixture createFixture(Shape shape, float density) {
		long fixtureAddr = jniCreateFixture(addr, shape.addr, density);
		Fixture fixture = this.world.freeFixtures.obtain();
		fixture.reset(this, fixtureAddr);
		this.world.fixtures.put(fixture.addr, fixture);
		this.fixtures.add(fixture);
		return fixture;
	}
	
	private native long jniCreateFixture(long addr, long shapeAddr, float density); /*
		b2Body* body = (b2Body*)addr;
		b2Shape* shape = (b2Shape*)shapeAddr;
		return (jlong)body->CreateFixture( shape, density );
	*/
	
	public void destroyFixture(Fixture fixture) {
		this.world.destroyFixture(this, fixture);
		fixture.setUserData(null);
		this.world.fixtures.remove(fixture.addr);
		this.fixtures.removeValue(fixture, true);
		this.world.freeFixtures.free(fixture);
	}
	
	public void setTransform(Vector2 position, float angle) {
		jniSetTransform(addr, position.x, position.y, angle);
	}
	
	public void setTransform(float x, float y, float angle) {
		jniSetTransform(addr, x, y, angle);
	}
	
	private native void jniSetTransform(long addr, float positionX, float positionY, float angle); /*
		b2Body* body = (b2Body*)addr;
		body->SetTransform(b2Vec2(positionX, positionY), angle);
	*/
	
	private final Transform transform = new Transform();
	
	public Transform getTransform() {
		jniGetTransform(addr, transform.vals);
		return transform;
	}
	
	private native void jniGetTransform(long addr, float[] vals); /*
		b2Body* body = (b2Body*)addr;
		b2Transform t = body->GetTransform();
		vals[0] = t.p.x;
		vals[1] = t.p.y;
		vals[2] = t.q.c;
		vals[3] = t.q.s;
	*/
	
	private final Vector2 position = new Vector2();
	
	public Vector2 getPosition() {
		jniGetPosition(addr, tmp);
		position.x = tmp[0];
		position.y = tmp[1];
		return position;
	}
	
	private native void jniGetPosition(long addr, float[] position); /*
		b2Body* body = (b2Body*)addr;
		b2Vec2 p = body->GetPosition();
		position[0] = p.x;
		position[1] = p.y;
	*/
	
	public float getAngle() {
		return jniGetAngle(addr);
	}
	
	private native float jniGetAngle(long addr); /*
		b2Body* body = (b2Body*)addr;
		return body->GetAngle();
	*/
	
	private final Vector2 worldCenter = new Vector2();
	
	public Vector2 getWorldCenter() {
		jniGetWorldCenter(addr, tmp);
		worldCenter.x = tmp[0];
		worldCenter.y = tmp[1];
		return worldCenter;
	}
	
	private native void jniGetWorldCenter(long addr, float[] worldCenter); /*
		b2Body* body = (b2Body*)addr;
		b2Vec2 w = body->GetWorldCenter();
		worldCenter[0] = w.x;
		worldCenter[1] = w.y;
	*/
	
	private final Vector2 localCenter = new Vector2();
	
	public Vector2 getLocalCenter() {
		jniGetLocalCenter(addr, tmp);
		localCenter.x = tmp[0];
		localCenter.y = tmp[1];
		return localCenter;
	}
	
	private native void jniGetLocalCenter(long addr, float[] localCenter); /*
		b2Body* body = (b2Body*)addr;
		b2Vec2 w = body->GetLocalCenter();
		localCenter[0] = w.x;
		localCenter[1] = w.y;
	*/
	
	public void setLinearVelocity(Vector2 v) {
		jniSetLinearVelocity(addr, v.x, v.y);
	}
	
	public void setLinearVelocity(float vX, float vY) {
		jniSetLinearVelocity(addr, vX, vY);
	}
	
	private native void jniSetLinearVelocity(long addr, float x, float y); /*
		b2Body* body = (b2Body*)addr;
		body->SetLinearVelocity(b2Vec2(x, y));
	*/
	
	private final Vector2 linearVelocity = new Vector2();
	
	public Vector2 getLinearVelocity() {
		jniGetLinearVelocity(addr, tmp);
		linearVelocity.x = tmp[0];
		linearVelocity.y = tmp[1];
		return linearVelocity;
	}
	
	private native void jniGetLinearVelocity(long addr, float[] linearVelocity); /*
		b2Body* body = (b2Body*)addr;
		b2Vec2 l = body->GetLinearVelocity();
		linearVelocity[0] = l.x;
		linearVelocity[1] = l.y;
	*/
	
	public void setAngularVelocity(float omega) {
		jniSetAngularVelocity(addr, omega);
	}
	
	private native void jniSetAngularVelocity(long addr, float omega); /*
		b2Body* body = (b2Body*)addr;
		body->SetAngularVelocity(omega);
	*/
	
	public float getAngularVelocity() {
		return jniGetAngularVelocity(addr);
	}
	
	private native float jniGetAngularVelocity(long addr); /*
		b2Body* body = (b2Body*)addr;
		return body->GetAngularVelocity();
	*/
	
	public void applyForce(Vector2 force, Vector2 point, boolean wake) {
		jniApplyForce(addr, force.x, force.y, point.x, point.y, wake);
	}
	
	public void applyForce(float forceX, float forceY, float pointX, float pointY, boolean wake) {
		jniApplyForce(addr, forceX, forceY, pointX, pointY, wake);
	}
	
	private native void jniApplyForce(long addr, float forceX, float forceY, float pointX, float pointY, boolean wake); /*
		b2Body* body = (b2Body*)addr;
		body->ApplyForce(b2Vec2(forceX, forceY), b2Vec2(pointX, pointY), wake);
	*/
	
	public void applyForceToCenter(Vector2 force, boolean wake) {
		jniApplyForceToCenter(addr, force.x, force.y, wake);
	}
	
	public void applyForceToCenter(float forceX, float forceY, boolean wake) {
		jniApplyForceToCenter(addr, forceX, forceY, wake);
	}
	
	private native void jniApplyForceToCenter(long addr, float forceX, float forceY, boolean wake); /*
		b2Body* body = (b2Body*)addr;
		body->ApplyForceToCenter(b2Vec2(forceX, forceY), wake);
	*/
	
	public void applyTorque(float torque, boolean wake) {
		jniApplyTorque(addr, torque, wake);
	}
	
	private native void jniApplyTorque(long addr, float torque, boolean wake); /*
		b2Body* body = (b2Body*)addr;
		body->ApplyTorque(torque, wake);
	*/
	
	public void applyLinearImpulse(Vector2 impulse, Vector2 point, boolean wake) {
		jniApplyLinearImpulse(addr, impulse.x, impulse.y, point.x, point.y, wake);
	}
	
	public void applyLinearImpulse(float impulseX, float impulseY, float pointX, float pointY, boolean wake) {
		jniApplyLinearImpulse(addr, impulseX, impulseY, pointX, pointY, wake);
	}
	
	private native void jniApplyLinearImpulse(long addr, float impulseX, float impulseY, float pointX, float pointY, boolean wake); /*
		b2Body* body = (b2Body*)addr;
		body->ApplyLinearImpulse( b2Vec2( impulseX, impulseY ), b2Vec2( pointX, pointY ), wake);
	*/
	
	public void applyAngularImpulse(float impulse, boolean wake) {
		jniApplyAngularImpulse(addr, impulse, wake);
	}
	
	private native void jniApplyAngularImpulse(long addr, float impulse, boolean wake); /*
		b2Body* body = (b2Body*)addr;
		body->ApplyAngularImpulse(impulse, wake);
	*/
	
	public float getMass() {
		return jniGetMass(addr);
	}
	
	private native float jniGetMass(long addr); /*
		b2Body* body = (b2Body*)addr;
		return body->GetMass();
	*/
	
	public float getInertia() {
		return jniGetInertia(addr);
	}
	
	private native float jniGetInertia(long addr); /*
		b2Body* body = (b2Body*)addr;
		return body->GetInertia();
	*/
	
	private final MassData massData = new MassData();
	
	public MassData getMassData() {
		jniGetMassData(addr, tmp);
		massData.mass = tmp[0];
		massData.center.x = tmp[1];
		massData.center.y = tmp[2];
		massData.I = tmp[3];
		return massData;
	}
	
	private native void jniGetMassData(long addr, float[] massData); /*
		b2Body* body = (b2Body*)addr;
		b2MassData m;
		body->GetMassData(&m);
		massData[0] = m.mass;
		massData[1] = m.center.x;
		massData[2] = m.center.y;
		massData[3] = m.I;
	*/
	
	public void setMassData(MassData data) {
		jniSetMassData(addr, data.mass, data.center.x, data.center.y, data.I);
	}
	
	private native void jniSetMassData(long addr, float mass, float centerX, float centerY, float I); /*
		b2Body* body = (b2Body*)addr;
		b2MassData m;
		m.mass = mass;
		m.center.x = centerX;
		m.center.y = centerY;
		m.I = I;
		body->SetMassData(&m);
	*/
	
	public void resetMassData() {
		jniResetMassData(addr);
	}
	
	private native void jniResetMassData(long addr); /*
		b2Body* body = (b2Body*)addr;
		body->ResetMassData();
	*/
	
	private final Vector2 localPoint = new Vector2();
	
	public Vector2 getWorldPoint(Vector2 localPoint) {
		jniGetWorldPoint(addr, localPoint.x, localPoint.y, tmp);
		this.localPoint.x = tmp[0];
		this.localPoint.y = tmp[1];
		return this.localPoint;
	}
	
	private native void jniGetWorldPoint(long addr, float localPointX, float localPointY, float[] worldPoint); /*
		b2Body* body = (b2Body*)addr;
		b2Vec2 w = body->GetWorldPoint( b2Vec2( localPointX, localPointY ) );
		worldPoint[0] = w.x;
		worldPoint[1] = w.y;
	*/
	
	private final Vector2 worldVector = new Vector2();
	
	public Vector2 getWorldVector(Vector2 localVector) {
		jniGetWorldVector(addr, localVector.x, localVector.y, tmp);
		worldVector.x = tmp[0];
		worldVector.y = tmp[1];
		return worldVector;
	}
	
	private native void jniGetWorldVector(long addr, float localVectorX, float localVectorY, float[] worldVector); /*
		b2Body* body = (b2Body*)addr;
		b2Vec2 w = body->GetWorldVector( b2Vec2( localVectorX, localVectorY ) );
		worldVector[0] = w.x;
		worldVector[1] = w.y;
	*/
	
	public final Vector2 localPoint2 = new Vector2();
	
	public Vector2 getLocalPoint(Vector2 worldPoint) {
		jniGetLocalPoint(addr, worldPoint.x, worldPoint.y, tmp);
		localPoint2.x = tmp[0];
		localPoint2.y = tmp[1];
		return localPoint2;
	}
	
	private native void jniGetLocalPoint(long addr, float worldPointX, float worldPointY, float[] localPoint); /*
		b2Body* body = (b2Body*)addr;
		b2Vec2 w = body->GetLocalPoint( b2Vec2( worldPointX, worldPointY ) );
		localPoint[0] = w.x;
		localPoint[1] = w.y;
	*/
	
	public final Vector2 localVector = new Vector2();
	
	public Vector2 getLocalVector(Vector2 worldVector) {
		jniGetLocalVector(addr, worldVector.x, worldVector.y, tmp);
		localVector.x = tmp[0];
		localVector.y = tmp[1];
		return localVector;
	}
	
	private native void jniGetLocalVector(long addr, float worldVectorX, float worldVectorY, float[] worldVector); /*
		b2Body* body = (b2Body*)addr;
		b2Vec2 w = body->GetLocalVector( b2Vec2( worldVectorX, worldVectorY ) );
		worldVector[0] = w.x;
		worldVector[1] = w.y;
	*/
	
	public final Vector2 linVelWorld = new Vector2();
	
	public Vector2 getLinearVelocityFromWorldPoint(Vector2 worldPoint) {
		jniGetLinearVelocityFromWorldPoint(addr, worldPoint.x, worldPoint.y, tmp);
		linVelWorld.x = tmp[0];
		linVelWorld.y = tmp[1];
		return linVelWorld;
	}
	
	private native void jniGetLinearVelocityFromWorldPoint(long addr, float worldPointX, float worldPointY, float[] linVelWorld); /*
		b2Body* body = (b2Body*)addr;
		b2Vec2 w = body->GetLinearVelocityFromWorldPoint( b2Vec2( worldPointX, worldPointY ) );
		linVelWorld[0] = w.x;
		linVelWorld[1] = w.y;
	*/
	
	public final Vector2 linVelLoc = new Vector2();
	
	public Vector2 getLinearVelocityFromLocalPoint(Vector2 localPoint) {
		jniGetLinearVelocityFromLocalPoint(addr, localPoint.x, localPoint.y, tmp);
		linVelLoc.x = tmp[0];
		linVelLoc.y = tmp[1];
		return linVelLoc;
	}
	
	private native void jniGetLinearVelocityFromLocalPoint(long addr, float localPointX, float localPointY, float[] linVelLoc); /*
		b2Body* body = (b2Body*)addr;
		b2Vec2 w = body->GetLinearVelocityFromLocalPoint( b2Vec2( localPointX, localPointY ) );
		linVelLoc[0] = w.x;
		linVelLoc[1] = w.y;
	*/
	
	public float getLinearDamping() {
		return jniGetLinearDamping(addr);
	}
	
	private native float jniGetLinearDamping(long addr); /*
		b2Body* body = (b2Body*)addr;
		return body->GetLinearDamping();
	*/
	
	public void setLinearDamping(float linearDamping) {
		jniSetLinearDamping(addr, linearDamping);
	}
	
	private native void jniSetLinearDamping(long addr, float linearDamping); /*
		b2Body* body = (b2Body*)addr;
		body->SetLinearDamping(linearDamping);
	*/
	
	public float getAngularDamping() {
		return jniGetAngularDamping(addr);
	}
	
	private native float jniGetAngularDamping(long addr); /*
		b2Body* body = (b2Body*)addr;
		return body->GetAngularDamping();
	*/
	
	public void setAngularDamping(float angularDamping) {
		jniSetAngularDamping(addr, angularDamping);
	}
	
	private native void jniSetAngularDamping(long addr, float angularDamping); /*
		b2Body* body = (b2Body*)addr;
		body->SetAngularDamping(angularDamping);
	*/
	
	public void setType(BodyType type) {
		jniSetType(addr, type.getValue());
	}
	
	// @off
	/*JNI
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
*/
	
	private native void jniSetType(long addr, int type); /*
		b2Body* body = (b2Body*)addr;
		body->SetType(getBodyType(type));
	*/
	
	public BodyType getType() {
		int type = jniGetType(addr);
		if (type == 0)
			return BodyType.StaticBody;
		if (type == 1)
			return BodyType.KinematicBody;
		if (type == 2)
			return BodyType.DynamicBody;
		return BodyType.StaticBody;
	}
	
	private native int jniGetType(long addr); /*
		b2Body* body = (b2Body*)addr;
		return body->GetType();
	*/
	
	public void setBullet(boolean flag) {
		jniSetBullet(addr, flag);
	}
	
	private native void jniSetBullet(long addr, boolean flag); /*
		b2Body* body = (b2Body*)addr;
		body->SetBullet(flag);
	*/
	
	public boolean isBullet() {
		return jniIsBullet(addr);
	}
	
	private native boolean jniIsBullet(long addr); /*
		b2Body* body = (b2Body*)addr;
		return body->IsBullet();
	*/
	
	public void setSleepingAllowed(boolean flag) {
		jniSetSleepingAllowed(addr, flag);
	}
	
	private native void jniSetSleepingAllowed(long addr, boolean flag); /*
		b2Body* body = (b2Body*)addr;
		body->SetSleepingAllowed(flag);
	*/
	
	public boolean isSleepingAllowed() {
		return jniIsSleepingAllowed(addr);
	}
	
	private native boolean jniIsSleepingAllowed(long addr); /*
		b2Body* body = (b2Body*)addr;
		return body->IsSleepingAllowed();
	*/
	
	public void setAwake(boolean flag) {
		jniSetAwake(addr, flag);
	}
	
	private native void jniSetAwake(long addr, boolean flag); /*
		b2Body* body = (b2Body*)addr;
		body->SetAwake(flag);
	*/
	
	public boolean isAwake() {
		return jniIsAwake(addr);
	}
	
	private native boolean jniIsAwake(long addr); /*
		b2Body* body = (b2Body*)addr;
		return body->IsAwake();
	*/
	
	public void setActive(boolean flag) {
		if (flag) {
			jniSetActive(addr, flag);
		} else {
			this.world.deactivateBody(this);
		}
	}
	
	private native void jniSetActive(long addr, boolean flag); /*
		b2Body* body = (b2Body*)addr;
		body->SetActive(flag);
	*/
	
	public boolean isActive() {
		return jniIsActive(addr);
	}
	
	private native boolean jniIsActive(long addr); /*
		b2Body* body = (b2Body*)addr;
		return body->IsActive();
	*/
	
	public void setFixedRotation(boolean flag) {
		jniSetFixedRotation(addr, flag);
	}
	
	private native void jniSetFixedRotation(long addr, boolean flag); /*
		b2Body* body = (b2Body*)addr;
		body->SetFixedRotation(flag);
	*/
	
	public boolean isFixedRotation() {
		return jniIsFixedRotation(addr);
	}
	
	private native boolean jniIsFixedRotation(long addr); /*
		b2Body* body = (b2Body*)addr;
		return body->IsFixedRotation();
	*/
	
	public Array<Fixture> getFixtureList() {
		return fixtures;
	}
	
	public Array<JointEdge> getJointList() {
		return joints;
	}
	
	public float getGravityScale() {
		return jniGetGravityScale(addr);
	}
	
	private native float jniGetGravityScale(long addr); /*
		b2Body* body = (b2Body*)addr;
		return body->GetGravityScale();
	*/
	
	public void setGravityScale(float scale) {
		jniSetGravityScale(addr, scale);
	}
	
	private native void jniSetGravityScale(long addr, float scale); /*
		b2Body* body = (b2Body*)addr;
		body->SetGravityScale(scale);
	*/
	
	public World getWorld() {
		return world;
	}
	
	public Object getUserData() {
		return userData;
	}
	
	public void setUserData(Object userData) {
		this.userData = userData;
	}
	
}
