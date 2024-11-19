package me.nulldoubt.micro.assets.loaders;

import me.nulldoubt.micro.assets.AssetDescriptor;
import me.nulldoubt.micro.assets.AssetLoaderParameters;
import me.nulldoubt.micro.assets.AssetManager;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.graphics.Pixmap;
import me.nulldoubt.micro.utils.collections.Array;

public class PixmapLoader extends AsynchronousAssetLoader<Pixmap, PixmapLoader.PixmapParameter> {
	
	public PixmapLoader(FileHandleResolver resolver) {
		super(resolver);
	}
	
	Pixmap pixmap;
	
	@Override
	public void loadAsync(AssetManager manager, String fileName, FileHandle file, PixmapParameter parameter) {
		pixmap = null;
		pixmap = new Pixmap(file);
	}
	
	@Override
	public Pixmap loadSync(AssetManager manager, String fileName, FileHandle file, PixmapParameter parameter) {
		Pixmap pixmap = this.pixmap;
		this.pixmap = null;
		return pixmap;
	}
	
	@Override
	public Array<AssetDescriptor<?>> getDependencies(String fileName, FileHandle file, PixmapParameter parameter) {
		return null;
	}
	
	public static class PixmapParameter extends AssetLoaderParameters<Pixmap> {}
	
}
