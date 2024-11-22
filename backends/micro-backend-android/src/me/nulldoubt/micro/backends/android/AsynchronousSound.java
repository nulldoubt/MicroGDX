package me.nulldoubt.micro.backends.android;

import android.os.Handler;
import me.nulldoubt.micro.audio.Sound;

public class AsynchronousSound implements Sound {
	
	private final Sound sound;
	private final Handler handler;
	
	public AsynchronousSound(Sound sound, Handler handler) {
		this.sound = sound;
		this.handler = handler;
	}
	
	@Override
	public long play() {
		handler.post(sound::play);
		return 0;
	}
	
	@Override
	public long play(final float volume) {
		handler.post(() -> sound.play(volume));
		return 0;
	}
	
	@Override
	public long play(final float volume, final float pitch, final float pan) {
		handler.post(() -> sound.play(volume, pitch, pan));
		return 0;
	}
	
	@Override
	public long loop() {
		handler.post(sound::loop);
		return 0;
	}
	
	@Override
	public long loop(final float volume) {
		handler.post(() -> sound.loop(volume));
		return 0;
	}
	
	@Override
	public long loop(final float volume, final float pitch, final float pan) {
		handler.post(() -> sound.loop(volume, pitch, pan));
		return 0;
	}
	
	@Override
	public void stop() {
		handler.post(sound::stop);
	}
	
	@Override
	public void pause() {
		sound.pause();
	}
	
	@Override
	public void resume() {
		sound.resume();
	}
	
	@Override
	public void dispose() {
		sound.dispose();
	}
	
	@Override
	public void stop(long soundId) {
		throw new UnsupportedOperationException("Asynchronous audio doesn't support sound id based operations.");
	}
	
	@Override
	public void pause(long soundId) {
		throw new UnsupportedOperationException("Asynchronous audio doesn't support sound id based operations.");
	}
	
	@Override
	public void resume(long soundId) {
		throw new UnsupportedOperationException("Asynchronous audio doesn't support sound id based operations.");
	}
	
	@Override
	public void setLooping(long soundId, boolean looping) {
		throw new UnsupportedOperationException("Asynchronous audio doesn't support sound id based operations.");
	}
	
	@Override
	public void setPitch(long soundId, float pitch) {
		throw new UnsupportedOperationException("Asynchronous audio doesn't support sound id based operations.");
	}
	
	@Override
	public void setVolume(long soundId, float volume) {
		throw new UnsupportedOperationException("Asynchronous audio doesn't support sound id based operations.");
	}
	
	@Override
	public void setPan(long soundId, float pan, float volume) {
		throw new UnsupportedOperationException("Asynchronous audio doesn't support sound id based operations.");
	}
	
}
