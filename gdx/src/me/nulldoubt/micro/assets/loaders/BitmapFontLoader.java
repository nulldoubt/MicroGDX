package me.nulldoubt.micro.assets.loaders;

import me.nulldoubt.micro.assets.AssetDescriptor;
import me.nulldoubt.micro.assets.AssetLoaderParameters;
import me.nulldoubt.micro.assets.AssetManager;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.graphics.Texture;
import me.nulldoubt.micro.graphics.Texture.TextureFilter;
import me.nulldoubt.micro.graphics.g2d.BitmapFont;
import me.nulldoubt.micro.graphics.g2d.BitmapFont.BitmapFontData;
import me.nulldoubt.micro.graphics.g2d.TextureAtlas;
import me.nulldoubt.micro.graphics.g2d.TextureAtlas.AtlasRegion;
import me.nulldoubt.micro.graphics.g2d.TextureRegion;
import me.nulldoubt.micro.utils.collections.Array;

public class BitmapFontLoader extends AsynchronousAssetLoader<BitmapFont, BitmapFontLoader.BitmapFontParameter> {
	
	public BitmapFontLoader(FileHandleResolver resolver) {
		super(resolver);
	}
	
	BitmapFontData data;
	
	@Override
	public Array<AssetDescriptor<?>> getDependencies(String fileName, FileHandle file, BitmapFontParameter parameter) {
		
		final Array<AssetDescriptor<?>> deps = new Array<>();
		if (parameter != null && parameter.bitmapFontData != null) {
			data = parameter.bitmapFontData;
			return deps;
		}
		
		data = new BitmapFontData(file, parameter != null && parameter.flip);
		if (parameter != null && parameter.atlasName != null)
			deps.add(new AssetDescriptor<>(parameter.atlasName, TextureAtlas.class));
		else {
			for (int i = 0; i < data.getImagePaths().length; i++) {
				String path = data.getImagePath(i);
				FileHandle resolved = resolve(path);
				
				TextureLoader.TextureParameter textureParams = new TextureLoader.TextureParameter();
				
				if (parameter != null) {
					textureParams.genMipMaps = parameter.genMipMaps;
					textureParams.minFilter = parameter.minFilter;
					textureParams.magFilter = parameter.magFilter;
				}
				
				deps.add(new AssetDescriptor<Texture>(resolved, Texture.class, textureParams));
			}
		}
		
		return deps;
	}
	
	@Override
	public void loadAsync(AssetManager manager, String fileName, FileHandle file, BitmapFontParameter parameter) {}
	
	@Override
	public BitmapFont loadSync(AssetManager manager, String fileName, FileHandle file, BitmapFontParameter parameter) {
		if (parameter != null && parameter.atlasName != null) {
			TextureAtlas atlas = manager.get(parameter.atlasName, TextureAtlas.class);
			String name = file.sibling(data.imagePaths[0]).nameWithoutExtension();
			AtlasRegion region = atlas.findRegion(name);
			
			if (region == null)
				throw new MicroRuntimeException("Could not find font region " + name + " in atlas " + parameter.atlasName);
			return new BitmapFont(file, region);
		} else {
			final Array<TextureRegion> regions = new Array<>(data.getImagePaths().length);
			for (int i = 0; i < regions.size; i++)
				regions.add(new TextureRegion(manager.get(data.getImagePath(i), Texture.class)));
			return new BitmapFont(data, regions, true);
		}
	}
	
	public static class BitmapFontParameter extends AssetLoaderParameters<BitmapFont> {
		
		public boolean flip = false;
		public boolean genMipMaps = false;
		public TextureFilter minFilter = TextureFilter.Nearest;
		public TextureFilter magFilter = TextureFilter.Nearest;
		public BitmapFontData bitmapFontData = null;
		public String atlasName = null;
		
	}
	
}
