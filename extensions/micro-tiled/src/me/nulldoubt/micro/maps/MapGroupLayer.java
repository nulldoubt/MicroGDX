package me.nulldoubt.micro.maps;

public class MapGroupLayer extends MapLayer {
	
	public final MapLayers layers;
	
	public MapGroupLayer() {
		layers = new MapLayers();
	}
	
	@Override
	public void invalidateRenderOffset() {
		super.invalidateRenderOffset();
		for (int i = 0; i < layers.size(); i++)
			layers.get(i).invalidateRenderOffset();
	}
	
}
