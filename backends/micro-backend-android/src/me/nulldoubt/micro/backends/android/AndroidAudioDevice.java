package me.nulldoubt.micro.backends.android;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import me.nulldoubt.micro.audio.AudioDevice;

class AndroidAudioDevice implements AudioDevice {
	
	private final AudioTrack track;
	
	private short[] buffer = new short[1024];
	
	private final boolean isMono;
	
	private final int latency;
	
	AndroidAudioDevice(int samplingRate, boolean isMono) {
		this.isMono = isMono;
		int minSize = AudioTrack.getMinBufferSize(samplingRate, isMono ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
		track = new AudioTrack(AudioManager.STREAM_MUSIC, samplingRate, isMono ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, minSize, AudioTrack.MODE_STREAM);
		track.play();
		latency = minSize / (isMono ? 1 : 2);
	}
	
	@Override
	public void dispose() {
		track.stop();
		track.release();
	}
	
	@Override
	public boolean isMono() {
		return isMono;
	}
	
	@Override
	public void writeSamples(short[] samples, int offset, int numSamples) {
		int writtenSamples = track.write(samples, offset, numSamples);
		while (writtenSamples != numSamples)
			writtenSamples += track.write(samples, offset + writtenSamples, numSamples - writtenSamples);
	}
	
	@Override
	public void writeSamples(float[] samples, int offset, int numSamples) {
		if (buffer.length < samples.length)
			buffer = new short[samples.length];
		
		int bound = offset + numSamples;
		for (int i = offset, j = 0; i < bound; i++, j++) {
			float fValue = samples[i];
			if (fValue > 1)
				fValue = 1;
			if (fValue < -1)
				fValue = -1;
			short value = (short) (fValue * Short.MAX_VALUE);
			buffer[j] = value;
		}
		
		int writtenSamples = track.write(buffer, 0, numSamples);
		while (writtenSamples != numSamples)
			writtenSamples += track.write(buffer, writtenSamples, numSamples - writtenSamples);
	}
	
	@Override
	public int getLatency() {
		return latency;
	}
	
	@Override
	public void setVolume(float volume) {
		track.setStereoVolume(volume, volume);
	}
	
	@Override
	public void pause() {
		if (track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING)
			track.pause();
	}
	
	@Override
	public void resume() {
		if (track.getPlayState() == AudioTrack.PLAYSTATE_PAUSED)
			track.play();
	}
	
}
