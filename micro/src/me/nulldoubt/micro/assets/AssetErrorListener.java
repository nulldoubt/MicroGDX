package me.nulldoubt.micro.assets;

public interface AssetErrorListener {
	
	void error(AssetDescriptor<?> asset, Throwable throwable);
	
}
