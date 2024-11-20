package me.nulldoubt.micro.backends.lwjgl3.audio;

import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.utils.Buffers;
import me.nulldoubt.micro.utils.Streams;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class Ogg {
	
	public static class Music extends OpenALMusic {
		
		private OggInputStream input;
		private OggInputStream previousInput;
		
		public Music(OpenALLwjgl3Audio audio, FileHandle file) {
			super(audio, file);
			if (audio.noDevice)
				return;
			input = new OggInputStream(file.read());
			setup(input.getChannels(), 16, input.getSampleRate());
		}
		
		public int read(byte[] buffer) {
			if (input == null) {
				input = new OggInputStream(file.read(), previousInput);
				setup(input.getChannels(), 16, input.getSampleRate());
				previousInput = null; // release this reference
			}
			return input.read(buffer);
		}
		
		public void reset() {
			Streams.closeQuietly(input);
			previousInput = null;
			input = null;
		}
		
		@Override
		protected void loop() {
			Streams.closeQuietly(input);
			previousInput = input;
			input = null;
		}
		
	}
	
	public static class Sound extends OpenALSound {
		
		public Sound(OpenALLwjgl3Audio audio, FileHandle file) {
			super(audio);
			if (audio.noDevice)
				return;
			
			// put the encoded audio data in a ByteBuffer
			byte[] streamData = file.readBytes();
			ByteBuffer encodedData = Buffers.newByteBuffer(streamData.length);
			encodedData.put(streamData);
			encodedData.flip();
			
			try (MemoryStack stack = MemoryStack.stackPush()) {
				final IntBuffer channelsBuffer = stack.mallocInt(1);
				final IntBuffer sampleRateBuffer = stack.mallocInt(1);
				
				// decode
				final ShortBuffer decodedData = STBVorbis.stb_vorbis_decode_memory(encodedData, channelsBuffer, sampleRateBuffer);
				int channels = channelsBuffer.get(0);
				int sampleRate = sampleRateBuffer.get(0);
				if (decodedData == null) {
					throw new MicroRuntimeException("Error decoding OGG file: " + file);
				}
				
				setup(decodedData, channels, 16, sampleRate);
			}
		}
		
	}
	
}
