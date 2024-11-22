package me.nulldoubt.micro.physics.box2d;

import com.badlogic.gdx.utils.SharedLibraryLoader;

public final class Box2D {
	
	private Box2D() {}
	
	public static void init() {
		new SharedLibraryLoader().load("micro-box2d");
	}
	
}
