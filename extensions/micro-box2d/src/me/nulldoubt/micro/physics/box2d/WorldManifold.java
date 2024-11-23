package me.nulldoubt.micro.physics.box2d;

import me.nulldoubt.micro.math.Vector2;

/** This is used to compute the current state of a contact manifold. */
public class WorldManifold {
	protected final Vector2 normal = new Vector2();
	protected final Vector2[] points = {new Vector2(), new Vector2()};
	protected final float[] separations = new float[2];
	protected int numContactPoints;

	protected WorldManifold () {
	}

	/** Returns the normal of this manifold */
	public Vector2 getNormal () {
		return normal;
	}

	/** Returns the contact points of this manifold. Use getNumberOfContactPoints to determine how many contact points there are
	 * (0,1 or 2) */
	public Vector2[] getPoints () {
		return points;
	}

	/** Returns the separations of this manifold, a negative value indicates overlap, in meters. Use getNumberOfContactPoints to
	 * determine how many separations there are (0,1 or 2) */
	public float[] getSeparations () {
		return separations;
	}

	/** @return the number of contact points */
	public int getNumberOfContactPoints () {
		return numContactPoints;
	}
}
