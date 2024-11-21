
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

/** Creates {@link BitmapFont} instances from FreeType font files. Requires a {@link FreeTypeFontLoaderParameter} to be passed to
 * {@link AssetManager#load(String, Class, AssetLoaderParameters)} which specifies the name of the TTF file as well the parameters
 * used to generate the BitmapFont (size, characters, etc.) */
public class FreetypeFontLoader extends AsynchronousAssetLoader<BitmapFont, FreetypeFontLoader.FreeTypeFontLoaderParameter> {
	public FreetypeFontLoader (FileHandleResolver resolver) {
		super(resolver);
	}

	public static class FreeTypeFontLoaderParameter extends AssetLoaderParameters<BitmapFont> {
		/** the name of the TTF file to be used to load the font **/
		public String fontFileName;
		/** the parameters used to generate the font, e.g. size, characters, etc. **/
		public FreeTypeFontParameter fontParameters = new FreeTypeFontParameter();
	}

	@Override
	public void loadAsync (AssetManager manager, String fileName, FileHandle file, FreeTypeFontLoaderParameter parameter) {
		if (parameter == null)
			throw new RuntimeException("FreetypeFontParameter must be set in AssetManager#load to point at a TTF file!");
	}

	@Override
	public BitmapFont loadSync (AssetManager manager, String fileName, FileHandle file, FreeTypeFontLoaderParameter parameter) {
		if (parameter == null)
			throw new RuntimeException("FreetypeFontParameter must be set in AssetManager#load to point at a TTF file!");
		FreeTypeFontGenerator generator = manager.get(parameter.fontFileName + ".gen", FreeTypeFontGenerator.class);
		BitmapFont font = generator.generateFont(parameter.fontParameters);
		return font;
	}

	@Override
	public Array<AssetDescriptor> getDependencies (String fileName, FileHandle file, FreeTypeFontLoaderParameter parameter) {
		Array<AssetDescriptor> deps = new Array<AssetDescriptor>();
		deps.add(new AssetDescriptor<FreeTypeFontGenerator>(parameter.fontFileName + ".gen", FreeTypeFontGenerator.class));
		return deps;
	}
}
