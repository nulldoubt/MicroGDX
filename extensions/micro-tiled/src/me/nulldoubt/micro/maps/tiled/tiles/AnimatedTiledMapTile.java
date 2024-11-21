package me.nulldoubt.micro.maps.tiled.tiles;

import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.graphics.g2d.TextureRegion;
import me.nulldoubt.micro.maps.MapObjects;
import me.nulldoubt.micro.maps.MapProperties;
import me.nulldoubt.micro.maps.tiled.TiledMapTile;
import me.nulldoubt.micro.utils.collections.Array;
import me.nulldoubt.micro.utils.collections.IntArray;

public class AnimatedTiledMapTile implements TiledMapTile {
	
	private static long lastTiledMapRenderTime = 0;
	
	private int id;
	
	private BlendMode blendMode = BlendMode.ALPHA;
	
	private MapProperties properties;
	
	private MapObjects objects;
	
	private final StaticTiledMapTile[] frameTiles;
	
	private int[] animationIntervals;
	private int loopDuration;
	private static final long initialTimeOffset = System.currentTimeMillis();
	
	@Override
	public int getId() {
		return id;
	}
	
	@Override
	public void setId(int id) {
		this.id = id;
	}
	
	@Override
	public BlendMode getBlendMode() {
		return blendMode;
	}
	
	@Override
	public void setBlendMode(BlendMode blendMode) {
		this.blendMode = blendMode;
	}
	
	public int getCurrentFrameIndex() {
		int currentTime = (int) (lastTiledMapRenderTime % loopDuration);
		
		for (int i = 0; i < animationIntervals.length; ++i) {
			int animationInterval = animationIntervals[i];
			if (currentTime <= animationInterval)
				return i;
			currentTime -= animationInterval;
		}
		
		throw new MicroRuntimeException(
				"Could not determine current animation frame in AnimatedTiledMapTile.  This should never happen.");
	}
	
	public TiledMapTile getCurrentFrame() {
		return frameTiles[getCurrentFrameIndex()];
	}
	
	@Override
	public TextureRegion getTextureRegion() {
		return getCurrentFrame().getTextureRegion();
	}
	
	@Override
	public void setTextureRegion(TextureRegion textureRegion) {
		throw new MicroRuntimeException("Cannot set the texture region of AnimatedTiledMapTile.");
	}
	
	@Override
	public float getOffsetX() {
		return getCurrentFrame().getOffsetX();
	}
	
	@Override
	public void setOffsetX(float offsetX) {
		throw new MicroRuntimeException("Cannot set offset of AnimatedTiledMapTile.");
	}
	
	@Override
	public float getOffsetY() {
		return getCurrentFrame().getOffsetY();
	}
	
	@Override
	public void setOffsetY(float offsetY) {
		throw new MicroRuntimeException("Cannot set offset of AnimatedTiledMapTile.");
	}
	
	public int[] getAnimationIntervals() {
		return animationIntervals;
	}
	
	public void setAnimationIntervals(int[] intervals) {
		if (intervals.length == animationIntervals.length) {
			this.animationIntervals = intervals;
			
			loopDuration = 0;
			for (int interval : intervals)
				loopDuration += interval;
			
		} else
			throw new MicroRuntimeException("Cannot set " + intervals.length + " frame intervals. The given int[] must have a size of " + animationIntervals.length + ".");
	}
	
	@Override
	public MapProperties getProperties() {
		if (properties == null)
			properties = new MapProperties();
		return properties;
	}
	
	@Override
	public MapObjects getObjects() {
		if (objects == null)
			objects = new MapObjects();
		return objects;
	}
	
	public static void updateAnimationBaseTime() {
		lastTiledMapRenderTime = System.currentTimeMillis() - initialTimeOffset;
	}
	
	public AnimatedTiledMapTile(float interval, Array<StaticTiledMapTile> frameTiles) {
		this.frameTiles = new StaticTiledMapTile[frameTiles.size];
		this.loopDuration = frameTiles.size * (int) (interval * 1000f);
		this.animationIntervals = new int[frameTiles.size];
		for (int i = 0; i < frameTiles.size; ++i) {
			this.frameTiles[i] = frameTiles.get(i);
			this.animationIntervals[i] = (int) (interval * 1000f);
		}
	}
	
	public AnimatedTiledMapTile(IntArray intervals, Array<StaticTiledMapTile> frameTiles) {
		this.frameTiles = new StaticTiledMapTile[frameTiles.size];
		
		this.animationIntervals = intervals.toArray();
		this.loopDuration = 0;
		
		for (int i = 0; i < intervals.size; ++i) {
			this.frameTiles[i] = frameTiles.get(i);
			this.loopDuration += intervals.get(i);
		}
	}
	
	public StaticTiledMapTile[] getFrameTiles() {
		return frameTiles;
	}
	
}
