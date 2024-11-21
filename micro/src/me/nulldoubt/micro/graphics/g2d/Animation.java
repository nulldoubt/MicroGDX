package me.nulldoubt.micro.graphics.g2d;

import me.nulldoubt.micro.math.MathUtils;
import me.nulldoubt.micro.utils.collections.Array;

public class Animation<T> {
	
	public enum PlayMode {
		NORMAL, REVERSED, LOOP, LOOP_REVERSED, LOOP_PINGPONG, LOOP_RANDOM,
	}
	
	T[] keyFrames;
	private float frameDuration;
	private float animationDuration;
	private int lastFrameNumber;
	private float lastStateTime;
	
	private PlayMode playMode = PlayMode.NORMAL;
	
	public Animation(float frameDuration, Array<? extends T> keyFrames) {
		this.frameDuration = frameDuration;
		final Class<?> arrayType = keyFrames.items.getClass().getComponentType();
		final T[] frames = (T[]) java.lang.reflect.Array.newInstance(arrayType, keyFrames.size);
		for (int i = 0, n = keyFrames.size; i < n; i++)
			frames[i] = keyFrames.get(i);
		setKeyFrames(frames);
	}
	
	public Animation(float frameDuration, Array<? extends T> keyFrames, PlayMode playMode) {
		this(frameDuration, keyFrames);
		setPlayMode(playMode);
	}
	
	@SafeVarargs
	public Animation(float frameDuration, T... keyFrames) {
		this.frameDuration = frameDuration;
		setKeyFrames(keyFrames);
	}
	
	public T getKeyFrame(float stateTime, boolean looping) {
		PlayMode oldPlayMode = playMode;
		if (looping && (playMode == PlayMode.NORMAL || playMode == PlayMode.REVERSED)) {
			if (playMode == PlayMode.NORMAL)
				playMode = PlayMode.LOOP;
			else
				playMode = PlayMode.LOOP_REVERSED;
		} else if (!looping && !(playMode == PlayMode.NORMAL || playMode == PlayMode.REVERSED)) {
			if (playMode == PlayMode.LOOP_REVERSED)
				playMode = PlayMode.REVERSED;
			else
				playMode = PlayMode.LOOP;
		}
		T frame = getKeyFrame(stateTime);
		playMode = oldPlayMode;
		return frame;
	}
	
	public T getKeyFrame(float stateTime) {
		int frameNumber = getKeyFrameIndex(stateTime);
		return keyFrames[frameNumber];
	}
	
	public int getKeyFrameIndex(float stateTime) {
		if (keyFrames.length == 1)
			return 0;
		
		int frameNumber = (int) (stateTime / frameDuration);
		switch (playMode) {
			case NORMAL:
				frameNumber = Math.min(keyFrames.length - 1, frameNumber);
				break;
			case LOOP:
				frameNumber = frameNumber % keyFrames.length;
				break;
			case LOOP_PINGPONG:
				frameNumber = frameNumber % ((keyFrames.length * 2) - 2);
				if (frameNumber >= keyFrames.length)
					frameNumber = keyFrames.length - 2 - (frameNumber - keyFrames.length);
				break;
			case LOOP_RANDOM:
				int lastFrameNumber = (int) ((lastStateTime) / frameDuration);
				if (lastFrameNumber != frameNumber) {
					frameNumber = MathUtils.random(keyFrames.length - 1);
				} else {
					frameNumber = this.lastFrameNumber;
				}
				break;
			case REVERSED:
				frameNumber = Math.max(keyFrames.length - frameNumber - 1, 0);
				break;
			case LOOP_REVERSED:
				frameNumber = frameNumber % keyFrames.length;
				frameNumber = keyFrames.length - frameNumber - 1;
				break;
		}
		
		lastFrameNumber = frameNumber;
		lastStateTime = stateTime;
		
		return frameNumber;
	}
	
	public T[] getKeyFrames() {
		return keyFrames;
	}
	
	@SafeVarargs
	protected final void setKeyFrames(T... keyFrames) {
		this.keyFrames = keyFrames;
		this.animationDuration = keyFrames.length * frameDuration;
	}
	
	public PlayMode getPlayMode() {
		return playMode;
	}
	
	public void setPlayMode(PlayMode playMode) {
		this.playMode = playMode;
	}
	
	public boolean isAnimationFinished(float stateTime) {
		int frameNumber = (int) (stateTime / frameDuration);
		return keyFrames.length - 1 < frameNumber;
	}
	
	public void setFrameDuration(float frameDuration) {
		this.frameDuration = frameDuration;
		this.animationDuration = keyFrames.length * frameDuration;
	}
	
	public float getFrameDuration() {
		return frameDuration;
	}
	
	public float getAnimationDuration() {
		return animationDuration;
	}
	
}
