package com.badlogic.gdx.assets;

public interface AssetErrorListener {
	
	void error(AssetDescriptor<?> asset, Throwable throwable);
	
}
