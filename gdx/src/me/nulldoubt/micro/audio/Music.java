package me.nulldoubt.micro.audio;

import me.nulldoubt.micro.utils.Disposable;

public interface Music extends Disposable {
	
	void play();
	
	void pause();
	
	void stop();
	
	boolean isPlaying();
	
	void setLooping(boolean isLooping);
	
	boolean isLooping();
	
	void setVolume(float volume);
	
	float getVolume();
	
	void setPan(float pan, float volume);
	
	void setPosition(float position);
	
	float getPosition();
	
	void dispose();
	
	void setOnCompletionListener(OnCompletionListener listener);
	
	interface OnCompletionListener {
		
		void onCompletion(Music music);
		
	}
	
}
