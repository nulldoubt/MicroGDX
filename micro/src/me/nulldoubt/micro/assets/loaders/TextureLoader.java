package me.nulldoubt.micro.assets.loaders;

import me.nulldoubt.micro.assets.AssetDescriptor;
import me.nulldoubt.micro.assets.AssetLoaderParameters;
import me.nulldoubt.micro.assets.AssetManager;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.graphics.Pixmap.Format;
import me.nulldoubt.micro.graphics.Texture;
import me.nulldoubt.micro.graphics.Texture.TextureFilter;
import me.nulldoubt.micro.graphics.Texture.TextureWrap;
import me.nulldoubt.micro.graphics.TextureData;
import me.nulldoubt.micro.utils.collections.Array;

public class TextureLoader extends AsynchronousAssetLoader<Texture, TextureLoader.TextureParameter> {
	
	public static class TextureLoaderInfo {
		
		String filename;
		TextureData data;
		Texture texture;
		
	}
	
	TextureLoaderInfo info = new TextureLoaderInfo();
	
	public TextureLoader(FileHandleResolver resolver) {
		super(resolver);
	}
	
	@Override
	public void loadAsync(AssetManager manager, String fileName, FileHandle file, TextureParameter parameter) {
		info.filename = fileName;
		if (parameter == null || parameter.textureData == null) {
			Format format = null;
			boolean genMipMaps = false;
			info.texture = null;
			
			if (parameter != null) {
				format = parameter.format;
				genMipMaps = parameter.genMipMaps;
				info.texture = parameter.texture;
			}
			
			info.data = TextureData.Factory.loadFromFile(file, format, genMipMaps);
		} else {
			info.data = parameter.textureData;
			info.texture = parameter.texture;
		}
		if (!info.data.isPrepared())
			info.data.prepare();
	}
	
	@Override
	public Texture loadSync(AssetManager manager, String fileName, FileHandle file, TextureParameter parameter) {
		if (info == null)
			return null;
		Texture texture = info.texture;
		if (texture != null) {
			texture.load(info.data);
		} else {
			texture = new Texture(info.data);
		}
		if (parameter != null) {
			texture.setFilter(parameter.minFilter, parameter.magFilter);
			texture.setWrap(parameter.wrapU, parameter.wrapV);
		}
		return texture;
	}
	
	@Override
	public Array<AssetDescriptor<?>> getDependencies(String fileName, FileHandle file, TextureParameter parameter) {
		return null;
	}
	
	public static class TextureParameter extends AssetLoaderParameters<Texture> {
		
		public Format format = null;
		public boolean genMipMaps = false;
		
		public Texture texture = null;
		/**
		 *
		 */
		public TextureData textureData = null;
		public TextureFilter minFilter = TextureFilter.Nearest;
		public TextureFilter magFilter = TextureFilter.Nearest;
		public TextureWrap wrapU = TextureWrap.ClampToEdge;
		public TextureWrap wrapV = TextureWrap.ClampToEdge;
		
	}
	
}
