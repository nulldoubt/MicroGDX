package me.nulldoubt.micro.graphics;

import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.graphics.Pixmap.Format;
import me.nulldoubt.micro.graphics.glutils.FileTextureData;

public interface TextureData {
	
	default boolean isCustom() {
		return false;
	}
	
	boolean isPrepared();
	
	void prepare();
	
	Pixmap consumePixmap();
	
	boolean disposePixmap();
	
	void consumeCustomData(int target);
	
	int getWidth();
	
	int getHeight();
	
	Format getFormat();
	
	boolean useMipMaps();
	
	boolean isManaged();
	
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
