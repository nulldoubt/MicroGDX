package me.nulldoubt.micro.maps;

import me.nulldoubt.micro.assets.AssetManager;
import me.nulldoubt.micro.graphics.Texture;
import me.nulldoubt.micro.graphics.g2d.TextureAtlas;
import me.nulldoubt.micro.graphics.g2d.TextureRegion;
import me.nulldoubt.micro.utils.collections.ObjectMap;

public interface ImageResolver {
	
	TextureRegion getImage(String name);
	
	class DirectImageResolver implements ImageResolver {
		
		private final ObjectMap<String, Texture> images;
		
		public DirectImageResolver(ObjectMap<String, Texture> images) {
			this.images = images;
		}
		
		@Override
		public TextureRegion getImage(String name) {
			return new TextureRegion(images.get(name));
		}
		
	}
	
	class AssetManagerImageResolver implements ImageResolver {
		
		private final AssetManager assetManager;
		
		public AssetManagerImageResolver(AssetManager assetManager) {
			this.assetManager = assetManager;
		}
		
		@Override
		public TextureRegion getImage(String name) {
			return new TextureRegion(assetManager.get(name, Texture.class));
		}
		
	}
	
	class TextureAtlasImageResolver implements ImageResolver {
		
		private final TextureAtlas atlas;
		
		public TextureAtlasImageResolver(TextureAtlas atlas) {
			this.atlas = atlas;
		}
		
		@Override
		public TextureRegion getImage(String name) {
			return atlas.findRegion(name);
		}
		
	}
	
}
