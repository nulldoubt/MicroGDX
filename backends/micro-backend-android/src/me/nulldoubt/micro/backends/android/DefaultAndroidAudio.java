package me.nulldoubt.micro.backends.android;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import me.nulldoubt.micro.Files.FileType;
import me.nulldoubt.micro.audio.AudioDevice;
import me.nulldoubt.micro.audio.AudioRecorder;
import me.nulldoubt.micro.audio.Music;
import me.nulldoubt.micro.audio.Sound;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.files.FileHandle;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DefaultAndroidAudio implements AndroidAudio {
	
	private final SoundPool soundPool;
	private final AudioManager manager;
	private final List<AndroidMusic> musics = new ArrayList<>();
	
	public DefaultAndroidAudio(Context context, AndroidApplicationConfiguration config) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			AudioAttributes audioAttrib = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
					.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
			soundPool = new SoundPool.Builder().setAudioAttributes(audioAttrib).setMaxStreams(config.maxSimultaneousSounds).build();
		} else
			soundPool = new SoundPool(config.maxSimultaneousSounds, AudioManager.STREAM_MUSIC, 0);// srcQuality: the sample-rate
		manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		if (context instanceof Activity)
			((Activity) context).setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}
	
	@Override
	public void pause() {
		synchronized (musics) {
			for (AndroidMusic music : musics) {
				if (music.isPlaying()) {
					music.pause();
					music.wasPlaying = true;
				} else
					music.wasPlaying = false;
			}
		}
		this.soundPool.autoPause();
	}
	
	@Override
	public void resume() {
		synchronized (musics) {
			for (int i = 0; i < musics.size(); i++) {
				if (musics.get(i).wasPlaying)
					musics.get(i).play();
			}
		}
		this.soundPool.autoResume();
	}
	
	@Override
	public AudioDevice newAudioDevice(int samplingRate, boolean mono) {
		return new AndroidAudioDevice(samplingRate, mono);
	}
	
	@Override
	public Music newMusic(FileHandle file) {
		AndroidFileHandle aHandle = (AndroidFileHandle) file;
		
		MediaPlayer mediaPlayer = createMediaPlayer();
		
		if (aHandle.type() == FileType.Internal) {
			try {
				AssetFileDescriptor descriptor = aHandle.getAssetFileDescriptor();
				mediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
				descriptor.close();
				mediaPlayer.prepare();
				AndroidMusic music = new AndroidMusic(this, mediaPlayer);
				synchronized (musics) {
					musics.add(music);
				}
				return music;
			} catch (Exception e) {
				throw new MicroRuntimeException("Error loading audio file: " + file + "\nNote: Internal audio files must be placed in the assets directory.", e);
			}
		} else {
			try {
				mediaPlayer.setDataSource(aHandle.file().getPath());
				mediaPlayer.prepare();
				AndroidMusic music = new AndroidMusic(this, mediaPlayer);
				synchronized (musics) {
					musics.add(music);
				}
				return music;
			} catch (Exception ex) {
				throw new MicroRuntimeException("Error loading audio file: " + file, ex);
			}
		}
		
	}
	
	@Override
	public boolean switchOutputDevice(String device) {
		return true;
	}
	
	@Override
	public String[] getAvailableOutputDevices() {
		return new String[0];
	}
	
	public Music newMusic(FileDescriptor fd) {
		MediaPlayer mediaPlayer = createMediaPlayer();
		
		try {
			mediaPlayer.setDataSource(fd);
			mediaPlayer.prepare();
			
			AndroidMusic music = new AndroidMusic(this, mediaPlayer);
			synchronized (musics) {
				musics.add(music);
			}
			return music;
		} catch (Exception e) {
			throw new MicroRuntimeException("Error loading audio from FileDescriptor", e);
		}
	}
	
	@Override
	public Sound newSound(FileHandle file) {
		AndroidSound androidSound;
		AndroidFileHandle aHandle = (AndroidFileHandle) file;
		if (aHandle.type() == FileType.Internal) {
			try {
				AssetFileDescriptor descriptor = aHandle.getAssetFileDescriptor();
				androidSound = new AndroidSound(soundPool, manager, soundPool.load(descriptor, 1));
				descriptor.close();
			} catch (IOException ex) {
				throw new MicroRuntimeException(
						"Error loading audio file: " + file + "\nNote: Internal audio files must be placed in the assets directory.", ex);
			}
		} else {
			try {
				androidSound = new AndroidSound(soundPool, manager, soundPool.load(aHandle.file().getPath(), 1));
			} catch (Exception ex) {
				throw new MicroRuntimeException("Error loading audio file: " + file, ex);
			}
		}
		return androidSound;
	}
	
	@Override
	public AudioRecorder newAudioRecorder(int samplingRate, boolean mono) {
		return new AndroidAudioRecorder(samplingRate, mono);
	}
	
	@Override
	public void dispose() {
		synchronized (musics) {
			final ArrayList<AndroidMusic> musicsCopy = new ArrayList<>(musics);
			for (AndroidMusic music : musicsCopy)
				music.dispose();
		}
		soundPool.release();
	}
	
	@Override
	public void notifyMusicDisposed(AndroidMusic music) {
		synchronized (musics) {
			musics.remove(this);
		}
	}
	
	protected MediaPlayer createMediaPlayer() {
		final MediaPlayer mediaPlayer = new MediaPlayer();
		mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_GAME).build());
		return mediaPlayer;
	}
	
}
