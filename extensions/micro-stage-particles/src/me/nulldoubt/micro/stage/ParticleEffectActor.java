package me.nulldoubt.micro.stage;

import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.graphics.g2d.Batch;
import me.nulldoubt.micro.graphics.g2d.ParticleEffect;
import me.nulldoubt.micro.graphics.g2d.TextureAtlas;
import me.nulldoubt.micro.scenes.scene2d.Actor;
import me.nulldoubt.micro.utils.Disposable;

public class ParticleEffectActor extends Actor implements Disposable {
	
	private final ParticleEffect particleEffect;
	protected float lastDelta;
	protected boolean isRunning;
	protected boolean ownsEffect;
	private boolean resetOnStart;
	private boolean autoRemove;
	
	public ParticleEffectActor(ParticleEffect particleEffect, boolean resetOnStart) {
		super();
		this.particleEffect = particleEffect;
		this.resetOnStart = resetOnStart;
	}
	
	public ParticleEffectActor(FileHandle particleFile, TextureAtlas atlas) {
		super();
		particleEffect = new ParticleEffect();
		particleEffect.load(particleFile, atlas);
		ownsEffect = true;
	}
	
	public ParticleEffectActor(FileHandle particleFile, FileHandle imagesDir) {
		super();
		particleEffect = new ParticleEffect();
		particleEffect.load(particleFile, imagesDir);
		ownsEffect = true;
	}
	
	@Override
	public void draw(Batch batch, float parentAlpha) {
		particleEffect.setPosition(getX(), getY());
		if (lastDelta > 0) {
			particleEffect.update(lastDelta);
			lastDelta = 0;
		}
		if (isRunning) {
			particleEffect.draw(batch);
			isRunning = !particleEffect.isComplete();
		}
	}
	
	@Override
	public void act(float delta) {
		super.act(delta);
		lastDelta += delta;
		if (autoRemove && particleEffect.isComplete())
			remove();
	}
	
	public void start() {
		isRunning = true;
		if (resetOnStart)
			particleEffect.reset(false);
		particleEffect.start();
	}
	
	public boolean isResetOnStart() {
		return resetOnStart;
	}
	
	public ParticleEffectActor setResetOnStart(boolean resetOnStart) {
		this.resetOnStart = resetOnStart;
		return this;
	}
	
	public boolean isAutoRemove() {
		return autoRemove;
	}
	
	public ParticleEffectActor setAutoRemove(boolean autoRemove) {
		this.autoRemove = autoRemove;
		return this;
	}
	
	public boolean isRunning() {
		return isRunning;
	}
	
	public ParticleEffect getEffect() {
		return this.particleEffect;
	}
	
	@Override
	protected void scaleChanged() {
		super.scaleChanged();
		particleEffect.scaleEffect(getScaleX(), getScaleY(), getScaleY());
	}
	
	public void cancel() {
		isRunning = true;
	}
	
	public void allowCompletion() {
		particleEffect.allowCompletion();
	}
	
	@Override
	public void dispose() {
		if (ownsEffect)
			particleEffect.dispose();
	}
	
}
