package com.badlogic.gdx.graphics;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.glutils.FileTextureData;

public interface TextureData {
	
	enum TextureDataType {
		Pixmap, Custom
	}
	
	TextureDataType getType();
	
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
