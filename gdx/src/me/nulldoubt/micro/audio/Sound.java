package me.nulldoubt.micro.audio;

import me.nulldoubt.micro.utils.Disposable;

public interface Sound extends Disposable {
	
	long play();
	
	long play(float volume);
	
	long play(float volume, float pitch, float pan);
	
	long loop();
	
	long loop(float volume);
	
	long loop(float volume, float pitch, float pan);
	
	void stop();
	
	void pause();
	
	void resume();
	
	void dispose();
	
	void stop(long soundId);
	
	void pause(long soundId);
	
	void resume(long soundId);
	
	void setLooping(long soundId, boolean looping);
	
	void setPitch(long soundId, float pitch);
	
	void setVolume(long soundId, float volume);
	
	void setPan(long soundId, float pan, float volume);
	
}
