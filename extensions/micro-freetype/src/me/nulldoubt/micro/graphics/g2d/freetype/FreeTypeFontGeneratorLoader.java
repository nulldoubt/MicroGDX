package me.nulldoubt.micro.graphics.g2d.freetype;

import me.nulldoubt.micro.assets.AssetDescriptor;
import me.nulldoubt.micro.assets.AssetLoaderParameters;
import me.nulldoubt.micro.assets.AssetManager;
import me.nulldoubt.micro.assets.loaders.FileHandleResolver;
import me.nulldoubt.micro.assets.loaders.SynchronousAssetLoader;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.utils.collections.Array;

public class FreeTypeFontGeneratorLoader extends SynchronousAssetLoader<FreeTypeFontGenerator, FreeTypeFontGeneratorLoader.FreeTypeFontGeneratorParameters> {
	
	public FreeTypeFontGeneratorLoader(final FileHandleResolver resolver) {
		super(resolver);
	}
	
	@Override
	public FreeTypeFontGenerator load(final AssetManager assetManager, final String fileName, final FileHandle file, final FreeTypeFontGeneratorParameters parameter) {
		return (file.extension().equals("gen") ? new FreeTypeFontGenerator(file.sibling(file.nameWithoutExtension())) : new FreeTypeFontGenerator(file));
	}
	
	@Override
	public Array<AssetDescriptor<?>> getDependencies(final String fileName, final FileHandle file, final FreeTypeFontGeneratorParameters parameter) {
		return null;
	}
	
	public static class FreeTypeFontGeneratorParameters extends AssetLoaderParameters<FreeTypeFontGenerator> {}
	
}
