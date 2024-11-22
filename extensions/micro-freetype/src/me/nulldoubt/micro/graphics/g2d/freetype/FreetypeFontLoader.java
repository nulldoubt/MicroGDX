package me.nulldoubt.micro.graphics.g2d.freetype;

import me.nulldoubt.micro.assets.AssetDescriptor;
import me.nulldoubt.micro.assets.AssetLoaderParameters;
import me.nulldoubt.micro.assets.AssetManager;
import me.nulldoubt.micro.assets.loaders.AsynchronousAssetLoader;
import me.nulldoubt.micro.assets.loaders.FileHandleResolver;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.graphics.g2d.BitmapFont;
import me.nulldoubt.micro.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import me.nulldoubt.micro.utils.collections.Array;

public class FreetypeFontLoader extends AsynchronousAssetLoader<BitmapFont, FreetypeFontLoader.FreeTypeFontLoaderParameter> {
	
	public FreetypeFontLoader(FileHandleResolver resolver) {
		super(resolver);
	}
	
	public static class FreeTypeFontLoaderParameter extends AssetLoaderParameters<BitmapFont> {
		
		public String fontFileName;
		public FreeTypeFontParameter fontParameters = new FreeTypeFontParameter();
		
	}
	
	@Override
	public void loadAsync(AssetManager manager, String fileName, FileHandle file, FreeTypeFontLoaderParameter parameter) {
		if (parameter == null)
			throw new RuntimeException("FreetypeFontParameter must be set in AssetManager#load to point at a TTF file!");
	}
	
	@Override
	public BitmapFont loadSync(AssetManager manager, String fileName, FileHandle file, FreeTypeFontLoaderParameter parameter) {
		if (parameter == null)
			throw new RuntimeException("FreetypeFontParameter must be set in AssetManager#load to point at a TTF file!");
		return manager.get(parameter.fontFileName + ".gen", FreeTypeFontGenerator.class).generateFont(parameter.fontParameters);
	}
	
	@Override
	public Array<AssetDescriptor<?>> getDependencies(String fileName, FileHandle file, FreeTypeFontLoaderParameter parameter) {
		final Array<AssetDescriptor<?>> dependencies = new Array<>();
		dependencies.add(new AssetDescriptor<>(parameter.fontFileName + ".gen", FreeTypeFontGenerator.class));
		return dependencies;
	}
	
}
