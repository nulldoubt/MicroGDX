/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package me.nulldoubt.micro.backends.lwjgl3.audio;

import me.nulldoubt.micro.audio.Music;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.math.MathUtils;
import me.nulldoubt.micro.utils.FloatArray;
import me.nulldoubt.micro.utils.MicroRuntimeException;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL11;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.openal.AL10.*;

/**
 * @author Nathan Sweet
 */
public abstract class OpenALMusic implements Music {
	
	private static final int bufferSize = 4096 * 10;
	private static final int bufferCount = 3;
	private static final byte[] tempBytes = new byte[bufferSize];
	private static final ByteBuffer tempBuffer = BufferUtils.createByteBuffer(bufferSize);
	protected final FileHandle file;
	private final OpenALLwjgl3Audio audio;
	private final FloatArray renderedSecondsQueue = new FloatArray(bufferCount);
	private IntBuffer buffers;
	private int sourceID = -1;
	private int format, sampleRate;
	private boolean isLooping, isPlaying;
	private float volume = 1;
	private float pan = 0;
	private float renderedSeconds, maxSecondsPerBuffer;
	private OnCompletionListener onCompletionListener;
	
	public OpenALMusic(OpenALLwjgl3Audio audio, FileHandle file) {
		this.audio = audio;
		this.file = file;
		this.onCompletionListener = null;
	}
	
	/**
	 * Prepare our music for playback!
	 *
	 * @param channels   The number of channels for the music. Most commonly 1 (for mono) or 2 (for stereo).
	 * @param bitDepth   The number of bits in each sample. Normally 16. Can also be 8, 32, 64.
	 * @param sampleRate The number of samples to be played each second. Commonly 44100; can be anything within reason.
	 */
	protected void setup(int channels, int bitDepth, int sampleRate) {
		this.format = OpenALUtils.determineFormat(channels, bitDepth);
		this.sampleRate = sampleRate;
		this.maxSecondsPerBuffer = (float) bufferSize / ((bitDepth >> 3) * channels * sampleRate);
	}
	
	public void play() {
		if (audio.noDevice)
			return;
		if (sourceID == -1) {
			sourceID = audio.obtainSource(true);
			if (sourceID == -1)
				return;
			
			audio.music.add(this);
			
			if (buffers == null) {
				buffers = BufferUtils.createIntBuffer(bufferCount);
				alGetError();
				alGenBuffers(buffers);
				int errorCode = alGetError();
				if (errorCode != AL_NO_ERROR)
					throw new MicroRuntimeException("Unable to allocate audio buffers. AL Error: " + errorCode);
			}
			
			alSourcei(sourceID, AL_LOOPING, AL_FALSE);
			setPan(pan, volume);
			
			alGetError();
			
			boolean filled = false; // Check if there's anything to actually play.
			for (int i = 0; i < bufferCount; i++) {
				int bufferID = buffers.get(i);
				if (!fill(bufferID))
					break;
				filled = true;
				alSourceQueueBuffers(sourceID, bufferID);
			}
			if (!filled && onCompletionListener != null)
				onCompletionListener.onCompletion(this);
			
			if (alGetError() != AL_NO_ERROR) {
				stop();
				return;
			}
		}
		if (!isPlaying) {
			alSourcePlay(sourceID);
			isPlaying = true;
		}
	}
	
	public void pause() {
		if (audio.noDevice)
			return;
		if (sourceID != -1)
			alSourcePause(sourceID);
		isPlaying = false;
	}
	
	public void stop() {
		if (audio.noDevice)
			return;
		if (sourceID == -1)
			return;
		audio.music.removeValue(this, true);
		reset();
		audio.freeSource(sourceID);
		sourceID = -1;
		renderedSeconds = 0;
		renderedSecondsQueue.clear();
		isPlaying = false;
	}
	
	public boolean isPlaying() {
		if (audio.noDevice)
			return false;
		if (sourceID == -1)
			return false;
		return isPlaying;
	}
	
	/**
	 * Fills as much of the buffer as possible and returns the number of bytes filled. Returns <= 0 to indicate the end of the
	 * stream.
	 */
	public abstract int read(byte[] buffer);	public void setLooping(boolean isLooping) {
		this.isLooping = isLooping;
	}
	
	/**
	 * Resets the stream to the beginning.
	 */
	public abstract void reset();	public boolean isLooping() {
		return isLooping;
	}
	
	/**
	 * By default, does just the same as reset(). Used to add special behaviour in Ogg.Music.
	 */
	protected void loop() {
		reset();
	}	/**
	 * @param volume Must be > 0.
	 */
	public void setVolume(float volume) {
		if (volume < 0)
			throw new IllegalArgumentException("volume cannot be < 0: " + volume);
		this.volume = volume;
		if (audio.noDevice)
			return;
		if (sourceID != -1)
			alSourcef(sourceID, AL_GAIN, volume);
	}
	
	public int getChannels() {
		return format == AL_FORMAT_STEREO16 ? 2 : 1;
	}	public float getVolume() {
		return this.volume;
	}
	
	public int getRate() {
		return sampleRate;
	}	public void setPan(float pan, float volume) {
		this.volume = volume;
		this.pan = pan;
		if (audio.noDevice)
			return;
		if (sourceID == -1)
			return;
		alSource3f(sourceID, AL_POSITION, MathUtils.cos((pan - 1) * MathUtils.HALF_PI), 0,
				MathUtils.sin((pan + 1) * MathUtils.HALF_PI));
		alSourcef(sourceID, AL_GAIN, volume);
	}
	
	public void update() {
		if (audio.noDevice)
			return;
		if (sourceID == -1)
			return;
		
		boolean end = false;
		int buffers = alGetSourcei(sourceID, AL_BUFFERS_PROCESSED);
		while (buffers-- > 0) {
			int bufferID = alSourceUnqueueBuffers(sourceID);
			if (bufferID == AL_INVALID_VALUE)
				break;
			if (renderedSecondsQueue.size > 0)
				renderedSeconds = renderedSecondsQueue.pop();
			if (end)
				continue;
			if (fill(bufferID))
				alSourceQueueBuffers(sourceID, bufferID);
			else
				end = true;
		}
		if (end && alGetSourcei(sourceID, AL_BUFFERS_QUEUED) == 0) {
			stop();
			if (onCompletionListener != null)
				onCompletionListener.onCompletion(this);
		}
		
		// A buffer underflow will cause the source to stop.
		if (isPlaying && alGetSourcei(sourceID, AL_SOURCE_STATE) != AL_PLAYING)
			alSourcePlay(sourceID);
	}	public void setPosition(float position) {
		if (audio.noDevice)
			return;
		if (sourceID == -1)
			return;
		boolean wasPlaying = isPlaying;
		isPlaying = false;
		alSourceStop(sourceID);
		alSourceUnqueueBuffers(sourceID, buffers);
		while (renderedSecondsQueue.size > 0) {
			renderedSeconds = renderedSecondsQueue.pop();
		}
		if (position <= renderedSeconds) {
			reset();
			renderedSeconds = 0;
		}
		while (renderedSeconds < (position - maxSecondsPerBuffer)) {
			int length = read(tempBytes);
			if (length <= 0)
				break;
			float currentBufferSeconds = maxSecondsPerBuffer * (float) length / (float) bufferSize;
			renderedSeconds += currentBufferSeconds;
		}
		renderedSecondsQueue.add(renderedSeconds);
		boolean filled = false;
		for (int i = 0; i < bufferCount; i++) {
			int bufferID = buffers.get(i);
			if (!fill(bufferID))
				break;
			filled = true;
			alSourceQueueBuffers(sourceID, bufferID);
		}
		renderedSecondsQueue.pop();
		if (!filled) {
			stop();
			if (onCompletionListener != null)
				onCompletionListener.onCompletion(this);
		}
		alSourcef(sourceID, AL11.AL_SEC_OFFSET, position - renderedSeconds);
		if (wasPlaying) {
			alSourcePlay(sourceID);
			isPlaying = true;
		}
	}
	
	private boolean fill(int bufferID) {
		((Buffer) tempBuffer).clear();
		int length = read(tempBytes);
		if (length <= 0) {
			if (isLooping) {
				loop();
				length = read(tempBytes);
				if (length <= 0)
					return false;
				if (renderedSecondsQueue.size > 0) {
					renderedSecondsQueue.set(0, 0);
				}
			} else
				return false;
		}
		float previousLoadedSeconds = renderedSecondsQueue.size > 0 ? renderedSecondsQueue.first() : 0;
		float currentBufferSeconds = maxSecondsPerBuffer * (float) length / (float) bufferSize;
		renderedSecondsQueue.insert(0, previousLoadedSeconds + currentBufferSeconds);
		
		((Buffer) tempBuffer.put(tempBytes, 0, length)).flip();
		alBufferData(bufferID, format, tempBuffer, sampleRate);
		return true;
	}	public float getPosition() {
		if (audio.noDevice)
			return 0;
		if (sourceID == -1)
			return 0;
		return renderedSeconds + alGetSourcef(sourceID, AL11.AL_SEC_OFFSET);
	}
	
	public int getSourceId() {
		return sourceID;
	}
	

	

	

	

	

	

	
	public void dispose() {
		stop();
		if (audio.noDevice)
			return;
		if (buffers == null)
			return;
		alDeleteBuffers(buffers);
		buffers = null;
		onCompletionListener = null;
	}
	
	public void setOnCompletionListener(OnCompletionListener listener) {
		onCompletionListener = listener;
	}
	

	
}
