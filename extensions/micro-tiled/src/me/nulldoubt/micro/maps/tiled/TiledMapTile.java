package me.nulldoubt.micro.maps.tiled;

import me.nulldoubt.micro.graphics.g2d.TextureRegion;
import me.nulldoubt.micro.maps.MapObjects;
import me.nulldoubt.micro.maps.MapProperties;

public interface TiledMapTile {
	
	enum BlendMode {
		NONE, ALPHA
	}
	
	int getId();
	
	void setId(int id);
	
	BlendMode getBlendMode();
	
	void setBlendMode(BlendMode blendMode);
	
	TextureRegion getTextureRegion();
	
	void setTextureRegion(TextureRegion textureRegion);
	
	float getOffsetX();
	
	void setOffsetX(float offsetX);
	
	float getOffsetY();
	
	void setOffsetY(float offsetY);
	
	MapProperties getProperties();
	
	MapObjects getObjects();
	
}
