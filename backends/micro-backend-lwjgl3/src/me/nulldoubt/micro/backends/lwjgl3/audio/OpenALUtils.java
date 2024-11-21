package me.nulldoubt.micro.backends.lwjgl3.audio;

import me.nulldoubt.micro.exceptions.MicroRuntimeException;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.EXTDouble.AL_FORMAT_MONO_DOUBLE_EXT;
import static org.lwjgl.openal.EXTDouble.AL_FORMAT_STEREO_DOUBLE_EXT;
import static org.lwjgl.openal.EXTFloat32.AL_FORMAT_MONO_FLOAT32;
import static org.lwjgl.openal.EXTFloat32.AL_FORMAT_STEREO_FLOAT32;
import static org.lwjgl.openal.EXTMCFormats.*;

public class OpenALUtils {
	
	static int determineFormat(int channels, int bitDepth) { // @off
		int format;
		switch (channels) {
			case 1:
				format = switch (bitDepth) {
					case 8 -> AL_FORMAT_MONO8;
					case 16 -> AL_FORMAT_MONO16;
					case 32 -> AL_FORMAT_MONO_FLOAT32;
					case 64 -> AL_FORMAT_MONO_DOUBLE_EXT;
					default -> throw new MicroRuntimeException("Audio: Bit depth must be 8, 16, 32 or 64.");
				};
				break;
			case 2: // Doesn't work on mono devices (#6631)
				format = switch (bitDepth) {
					case 8 -> AL_FORMAT_STEREO8;
					case 16 -> AL_FORMAT_STEREO16;
					case 32 -> AL_FORMAT_STEREO_FLOAT32;
					case 64 -> AL_FORMAT_STEREO_DOUBLE_EXT;
					default -> throw new MicroRuntimeException("Audio: Bit depth must be 8, 16, 32 or 64.");
				};
				break;
			case 4:
				format = AL_FORMAT_QUAD16;
				break; // Works on stereo devices but not mono as above
			case 6:
				format = AL_FORMAT_51CHN16;
				break;
			case 7:
				format = AL_FORMAT_61CHN16;
				break;
			case 8:
				format = AL_FORMAT_71CHN16;
				break;
			default:
				throw new MicroRuntimeException("Audio: Invalid number of channels. " +
						"Must be mono, stereo, quad, 5.1, 6.1 or 7.1.");
		}
		if (channels >= 4) {
			if (bitDepth == 8)
				format--; // Use 8-bit AL_FORMAT instead
			else if (bitDepth == 32)
				format++; // Use 32-bit AL_FORMAT instead
			else if (bitDepth != 16)
				throw new MicroRuntimeException("Audio: Bit depth must be 8, 16 or 32 when 4+ channels are present.");
		}
		return format; // @on
	}
	
}
