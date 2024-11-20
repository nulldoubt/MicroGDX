package me.nulldoubt.micro.backends.lwjgl3.audio;

import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.utils.Streams;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;

public class Wav {
	
	public static class Music extends OpenALMusic {
		
		private WavInputStream input;
		
		public Music(OpenALLwjgl3Audio audio, FileHandle file) {
			super(audio, file);
			input = new WavInputStream(file);
			if (audio.noDevice)
				return;
			setup(input.channels, input.bitDepth, input.sampleRate);
		}
		
		public int read(byte[] buffer) {
			if (input == null) {
				input = new WavInputStream(file);
				setup(input.channels, input.bitDepth, input.sampleRate);
			}
			try {
				return input.read(buffer);
			} catch (IOException ex) {
				throw new MicroRuntimeException("Error reading WAV file: " + file, ex);
			}
		}
		
		public void reset() {
			Streams.closeQuietly(input);
			input = null;
		}
		
	}
	
	public static class Sound extends OpenALSound {
		
		public Sound(OpenALLwjgl3Audio audio, FileHandle file) {
			super(audio);
			if (audio.noDevice)
				return;
			
			WavInputStream input = null;
			try {
				input = new WavInputStream(file);
				if (input.type == 0x0055) {
					setType("mp3");
					return;
				}
				setup(Streams.copyStreamToByteArray(input, input.dataRemaining), input.channels, input.bitDepth,
						input.sampleRate);
			} catch (IOException ex) {
				throw new MicroRuntimeException("Error reading WAV file: " + file, ex);
			} finally {
				Streams.closeQuietly(input);
			}
		}
		
	}
	
	/**
	 * @author Nathan Sweet
	 */
	public static class WavInputStream extends FilterInputStream {
		
		public int channels, bitDepth, sampleRate, dataRemaining, type;
		
		public WavInputStream(FileHandle file) {
			super(file.read());
			try {
				if (read() != 'R' || read() != 'I' || read() != 'F' || read() != 'F')
					throw new MicroRuntimeException("RIFF header not found: " + file);
				
				skipFully(4);
				
				if (read() != 'W' || read() != 'A' || read() != 'V' || read() != 'E')
					throw new MicroRuntimeException("Invalid wave file header: " + file);
				
				int fmtChunkLength = seekToChunk('f', 'm', 't', ' ');
				
				// http://www-mmsp.ece.mcgill.ca/Documents/AudioFormats/WAVE/WAVE.html
				// http://soundfile.sapp.org/doc/WaveFormat/
				type = read() & 0xff | (read() & 0xff) << 8;
				
				if (type == 0x0055)
					return; // Handle MP3 in constructor instead
				if (type != 0x0001 && type != 0x0003)
					throw new MicroRuntimeException(
							"WAV files must be PCM, unsupported format: " + getCodecName(type) + " (" + type + ")");
				
				channels = read() & 0xff | (read() & 0xff) << 8;
				sampleRate = read() & 0xff | (read() & 0xff) << 8 | (read() & 0xff) << 16 | (read() & 0xff) << 24;
				
				skipFully(6);
				
				bitDepth = read() & 0xff | (read() & 0xff) << 8;
				if (type == 0x0001) { // PCM
					if (bitDepth != 8 && bitDepth != 16)
						throw new MicroRuntimeException("PCM WAV files must be 8 or 16-bit: " + bitDepth);
				} else if (type == 0x0003) { // Float
					if (bitDepth != 32 && bitDepth != 64)
						throw new MicroRuntimeException("Floating-point WAV files must be 32 or 64-bit: " + bitDepth);
				}
				
				skipFully(fmtChunkLength - 16);
				
				dataRemaining = seekToChunk('d', 'a', 't', 'a');
			} catch (Throwable ex) {
				Streams.closeQuietly(this);
				throw new MicroRuntimeException("Error reading WAV file: " + file, ex);
			}
		}
		
		private int seekToChunk(char c1, char c2, char c3, char c4) throws IOException {
			while (true) {
				boolean found = read() == c1;
				found &= read() == c2;
				found &= read() == c3;
				found &= read() == c4;
				int chunkLength = read() & 0xff | (read() & 0xff) << 8 | (read() & 0xff) << 16 | (read() & 0xff) << 24;
				if (chunkLength == -1)
					throw new IOException("Chunk not found: " + c1 + c2 + c3 + c4);
				if (found)
					return chunkLength;
				skipFully(chunkLength);
			}
		}
		
		private void skipFully(int count) throws IOException {
			while (count > 0) {
				long skipped = in.skip(count);
				if (skipped <= 0)
					throw new EOFException("Unable to skip.");
				count -= (int) skipped;
			}
		}
		
		public int read(byte[] buffer) throws IOException {
			if (dataRemaining == 0)
				return -1;
			int offset = 0;
			do {
				int length = Math.min(super.read(buffer, offset, buffer.length - offset), dataRemaining);
				if (length == -1) {
					if (offset > 0)
						return offset;
					return -1;
				}
				offset += length;
				dataRemaining -= length;
			} while (offset < buffer.length);
			return offset;
		}
		
		private String getCodecName(int type) {
			return switch (type) { // @off
				case 0x0002 -> "Microsoft ADPCM";
				case 0x0006 -> "ITU-T G.711 A-law";
				case 0x0007 -> "ITU-T G.711 u-law";
				case 0x0011 -> "IMA ADPCM";
				case 0x0022 -> "DSP Group TrueSpeech";
				case 0x0031 -> "Microsoft GSM 6.10";
				case 0x0040 -> "Antex G.721 ADPCM";
				case 0x0070 -> "Lernout & Hauspie CELP 4.8kbps";
				case 0x0072 -> "Lernout & Hauspie CBS 12kbps";
				case 0xfffe -> "Extensible";
				default -> "Unknown"; // @on
			};
		}
		
	}
	
}
