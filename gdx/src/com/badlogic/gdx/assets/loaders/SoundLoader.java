package com.badlogic.gdx.assets.loaders;

import com.badlogic.gdx.Micro;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;

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
