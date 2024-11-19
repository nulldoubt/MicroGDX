package me.nulldoubt.micro.assets.loaders;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.assets.AssetDescriptor;
import me.nulldoubt.micro.assets.AssetLoaderParameters;
import me.nulldoubt.micro.assets.AssetManager;
import me.nulldoubt.micro.audio.Music;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.utils.Array;

public class MusicLoader extends AsynchronousAssetLoader<Music, MusicLoader.MusicParameter> {
	
	private Music music;
	
	public MusicLoader(FileHandleResolver resolver) {
		super(resolver);
	}
	
	protected Music getLoadedMusic() {
		return music;
	}
	
	@Override
	public void loadAsync(AssetManager manager, String fileName, FileHandle file, MusicParameter parameter) {
		music = Micro.audio.newMusic(file);
	}
	
	@Override
	public Music loadSync(AssetManager manager, String fileName, FileHandle file, MusicParameter parameter) {
		final Music music = this.music;
		this.music = null;
		return music;
	}
	
	@Override
	public Array<AssetDescriptor<?>> getDependencies(String fileName, FileHandle file, MusicParameter parameter) {
		return null;
	}
	
	public static class MusicParameter extends AssetLoaderParameters<Music> {}
	
}
