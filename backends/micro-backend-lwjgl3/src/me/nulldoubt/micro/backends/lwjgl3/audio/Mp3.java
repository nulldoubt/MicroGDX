package me.nulldoubt.micro.backends.lwjgl3.audio;

import javazoom.jl.decoder.*;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.files.FileHandle;

import java.io.ByteArrayOutputStream;

public class Mp3 {
	
	public static class Music extends OpenALMusic {
		
		private Bitstream bitstream;
		private OutputBuffer outputBuffer;
		private MP3Decoder decoder;
		
		public Music(OpenALLwjgl3Audio audio, FileHandle file) {
			super(audio, file);
			if (audio.noDevice)
				return;
			bitstream = new Bitstream(file.read());
			decoder = new MP3Decoder();
			try {
				Header header = bitstream.readFrame();
				if (header == null)
					throw new MicroRuntimeException("Empty MP3");
				int channels = header.mode() == Header.SINGLE_CHANNEL ? 1 : 2;
				outputBuffer = new OutputBuffer(channels, false);
				decoder.setOutputBuffer(outputBuffer);
				setup(channels, 16, header.getSampleRate());
			} catch (BitstreamException e) {
				throw new MicroRuntimeException("Error while preloading MP3", e);
			}
		}
		
		public int read(byte[] buffer) {
			try {
				boolean setup = bitstream == null;
				if (setup) {
					bitstream = new Bitstream(file.read());
					decoder = new MP3Decoder();
				}
				
				int totalLength = 0;
				int minRequiredLength = buffer.length - OutputBuffer.BUFFERSIZE * 2;
				while (totalLength <= minRequiredLength) {
					Header header = bitstream.readFrame();
					if (header == null)
						break;
					if (setup) {
						int channels = header.mode() == Header.SINGLE_CHANNEL ? 1 : 2;
						outputBuffer = new OutputBuffer(channels, false);
						decoder.setOutputBuffer(outputBuffer);
						setup(channels, 16, header.getSampleRate());
						setup = false;
					}
					try {
						decoder.decodeFrame(header, bitstream);
					} catch (Exception _) {}
					bitstream.closeFrame();
					
					int length = outputBuffer.reset();
					System.arraycopy(outputBuffer.getBuffer(), 0, buffer, totalLength, length);
					totalLength += length;
				}
				return totalLength;
			} catch (Throwable ex) {
				reset();
				throw new MicroRuntimeException("Error reading audio data.", ex);
			}
		}
		
		public void reset() {
			if (bitstream == null)
				return;
			try {
				bitstream.close();
			} catch (BitstreamException _) {}
			bitstream = null;
		}
		
	}
	
	public static class Sound extends OpenALSound {
		// Note: This uses a slightly modified version of JLayer.
		
		public Sound(OpenALLwjgl3Audio audio, FileHandle file) {
			super(audio);
			if (audio.noDevice)
				return;
			ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
			
			Bitstream bitstream = new Bitstream(file.read());
			MP3Decoder decoder = new MP3Decoder();
			
			try {
				OutputBuffer outputBuffer = null;
				int sampleRate = -1, channels = -1;
				while (true) {
					Header header = bitstream.readFrame();
					if (header == null)
						break;
					if (outputBuffer == null) {
						channels = header.mode() == Header.SINGLE_CHANNEL ? 1 : 2;
						outputBuffer = new OutputBuffer(channels, false);
						decoder.setOutputBuffer(outputBuffer);
						sampleRate = header.getSampleRate();
					}
					try {
						decoder.decodeFrame(header, bitstream);
					} catch (Exception _) {}
					bitstream.closeFrame();
					output.write(outputBuffer.getBuffer(), 0, outputBuffer.reset());
				}
				bitstream.close();
				setup(output.toByteArray(), channels, 16, sampleRate);
			} catch (Throwable ex) {
				throw new MicroRuntimeException("Error reading audio data.", ex);
			}
		}
		
	}
	
}