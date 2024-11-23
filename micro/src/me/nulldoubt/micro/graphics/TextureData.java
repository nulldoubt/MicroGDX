package me.nulldoubt.micro.graphics;

import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.graphics.Pixmap.Format;
import me.nulldoubt.micro.graphics.glutils.FileTextureData;

public interface TextureData {
	
	boolean isPrepared();
	
	void prepare();
	
	void consume(final int target, final int mipMapLevel);
	
	int getWidth();
	
	int getHeight();
	
	boolean isManaged();
	
	default boolean useMipMaps() {
		return false;
	}
	
	class Factory {
		
		public static TextureData loadFromFile(FileHandle file, boolean useMipMaps) {
			return loadFromFile(file, null, useMipMaps);
		}
		
		public static TextureData loadFromFile(FileHandle file, Format format, boolean useMipMaps) {
			if (file == null)
				return null;
			return new FileTextureData(file, new Pixmap(file), format, useMipMaps);
		}
		
	}
	
}
