package me.nulldoubt.micro.scenes.scene2d.utils;

import me.nulldoubt.micro.graphics.Color;
import me.nulldoubt.micro.graphics.g2d.Batch;
import me.nulldoubt.micro.graphics.g2d.Sprite;
import me.nulldoubt.micro.graphics.g2d.TextureAtlas.AtlasSprite;

public class SpriteDrawable extends BaseDrawable implements TransformDrawable {
	
	private Sprite sprite;
	
	public SpriteDrawable() {}
	
	public SpriteDrawable(Sprite sprite) {
		setSprite(sprite);
	}
	
	public SpriteDrawable(SpriteDrawable drawable) {
		super(drawable);
		setSprite(drawable.sprite);
	}
	
	public void draw(Batch batch, float x, float y, float width, float height) {
		Color spriteColor = sprite.getColor();
		float oldColor = sprite.getPackedColor();
		sprite.setColor(spriteColor.mul(batch.getColor()));
		
		sprite.setRotation(0);
		sprite.setScale(1, 1);
		sprite.setBounds(x, y, width, height);
		sprite.draw(batch);
		
		sprite.setPackedColor(oldColor);
	}
	
	public void draw(Batch batch, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
		Color spriteColor = sprite.getColor();
		float oldColor = sprite.getPackedColor();
		sprite.setColor(spriteColor.mul(batch.getColor()));
		
		sprite.setOrigin(originX, originY);
		sprite.setRotation(rotation);
		sprite.setScale(scaleX, scaleY);
		sprite.setBounds(x, y, width, height);
		sprite.draw(batch);
		
		sprite.setPackedColor(oldColor);
	}
	
	public void setSprite(Sprite sprite) {
		this.sprite = sprite;
		setMinWidth(sprite.getWidth());
		setMinHeight(sprite.getHeight());
	}
	
	public Sprite getSprite() {
		return sprite;
	}
	
	public SpriteDrawable tint(Color tint) {
		Sprite newSprite;
		if (sprite instanceof AtlasSprite atlasSprite)
			newSprite = new AtlasSprite(atlasSprite);
		else
			newSprite = new Sprite(sprite);
		newSprite.setColor(tint);
		newSprite.setSize(getMinWidth(), getMinHeight());
		SpriteDrawable drawable = new SpriteDrawable(newSprite);
		drawable.setLeftWidth(getLeftWidth());
		drawable.setRightWidth(getRightWidth());
		drawable.setTopHeight(getTopHeight());
		drawable.setBottomHeight(getBottomHeight());
		return drawable;
	}
	
}
