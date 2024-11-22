package me.nulldoubt.micro.assets.loaders;

import me.nulldoubt.micro.assets.AssetDescriptor;
import me.nulldoubt.micro.assets.AssetLoaderParameters;
import me.nulldoubt.micro.assets.AssetManager;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.graphics.g2d.TextureAtlas;
import me.nulldoubt.micro.scenes.scene2d.ui.Skin;
import me.nulldoubt.micro.utils.collections.Array;
import me.nulldoubt.micro.utils.collections.ObjectMap;
import me.nulldoubt.micro.utils.collections.ObjectMap.Entry;

public class SkinLoader extends AsynchronousAssetLoader<Skin, SkinLoader.SkinParameter> {
	
	public SkinLoader(FileHandleResolver resolver) {
		super(resolver);
	}
	
	@Override
	public Array<AssetDescriptor<?>> getDependencies(String fileName, FileHandle file, SkinParameter parameter) {
		Array<AssetDescriptor<?>> dependencies = new Array<>();
		if (parameter == null || parameter.textureAtlasPath == null)
			dependencies.add(new AssetDescriptor<>(file.pathWithoutExtension() + ".atlas", TextureAtlas.class));
		else
			dependencies.add(new AssetDescriptor<>(parameter.textureAtlasPath, TextureAtlas.class));
		return dependencies;
	}
	
	@Override
	public void loadAsync(AssetManager manager, String fileName, FileHandle file, SkinParameter parameter) {}
	
	@Override
	public Skin loadSync(AssetManager manager, String fileName, FileHandle file, SkinParameter parameter) {
		String textureAtlasPath = file.pathWithoutExtension() + ".atlas";
		ObjectMap<String, Object> resources = null;
		if (parameter != null) {
			if (parameter.textureAtlasPath != null)
				textureAtlasPath = parameter.textureAtlasPath;
			if (parameter.resources != null)
				resources = parameter.resources;
		}
		TextureAtlas atlas = manager.get(textureAtlasPath, TextureAtlas.class);
		Skin skin = newSkin(atlas);
		if (resources != null)
			for (Entry<String, Object> entry : resources.entries())
				skin.add(entry.key, entry.value);
		skin.load(file);
		return skin;
	}
	
	protected Skin newSkin(TextureAtlas atlas) {
		return new Skin(atlas);
	}
	
	public static class SkinParameter extends AssetLoaderParameters<Skin> {
		
		public final String textureAtlasPath;
		public final ObjectMap<String, Object> resources;
		
		public SkinParameter() {
			this(null, null);
		}
		
		public SkinParameter(ObjectMap<String, Object> resources) {
			this(null, resources);
		}
		
		public SkinParameter(String textureAtlasPath) {
			this(textureAtlasPath, null);
		}
		
		public SkinParameter(String textureAtlasPath, ObjectMap<String, Object> resources) {
			this.textureAtlasPath = textureAtlasPath;
			this.resources = resources;
		}
		
	}
	
}
