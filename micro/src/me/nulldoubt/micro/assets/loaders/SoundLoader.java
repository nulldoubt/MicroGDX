package me.nulldoubt.micro.assets.loaders;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.assets.AssetDescriptor;
import me.nulldoubt.micro.assets.AssetLoaderParameters;
import me.nulldoubt.micro.assets.AssetManager;
import me.nulldoubt.micro.audio.Sound;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.utils.collections.Array;

public class SoundLoader extends AsynchronousAssetLoader<Sound, SoundLoader.SoundParameter> {
	
	private Sound sound;
	
	public SoundLoader(final FileHandleResolver resolver) {
		super(resolver);
	}
	
	protected Sound getLoadedSound() {
		return sound;
	}
	
	@Override
	public void loadAsync(AssetManager manager, String fileName, FileHandle file, SoundParameter parameter) {
		sound = Micro.audio.newSound(file);
	}
	
	@Override
	public Sound loadSync(AssetManager manager, String fileName, FileHandle file, SoundParameter parameter) {
		final Sound sound = this.sound;
		this.sound = null;
		return sound;
	}
	
	@Override
	public Array<AssetDescriptor<?>> getDependencies(String fileName, FileHandle file, SoundParameter parameter) {
		return null;
	}
	
	public static class SoundParameter extends AssetLoaderParameters<Sound> {}
	
}
