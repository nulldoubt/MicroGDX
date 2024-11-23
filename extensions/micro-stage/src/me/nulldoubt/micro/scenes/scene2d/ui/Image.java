package me.nulldoubt.micro.scenes.scene2d.ui;

import me.nulldoubt.micro.graphics.Color;
import me.nulldoubt.micro.graphics.Texture;
import me.nulldoubt.micro.graphics.g2d.Batch;
import me.nulldoubt.micro.graphics.g2d.NinePatch;
import me.nulldoubt.micro.graphics.g2d.TextureRegion;
import me.nulldoubt.micro.math.Vector2;
import me.nulldoubt.micro.scenes.scene2d.utils.Drawable;
import me.nulldoubt.micro.scenes.scene2d.utils.NinePatchDrawable;
import me.nulldoubt.micro.scenes.scene2d.utils.TextureRegionDrawable;
import me.nulldoubt.micro.scenes.scene2d.utils.TransformDrawable;
import me.nulldoubt.micro.utils.Align;
import me.nulldoubt.micro.utils.Scaling;

public class Image extends Widget {
	
	private Scaling scaling;
	private int align = Align.center;
	private float imageX, imageY, imageWidth, imageHeight;
	private Drawable drawable;
	
	public Image() {
		this((Drawable) null);
	}
	
	public Image(NinePatch patch) {
		this(new NinePatchDrawable(patch), Scaling.stretch, Align.center);
	}
	
	public Image(TextureRegion region) {
		this(new TextureRegionDrawable(region), Scaling.stretch, Align.center);
	}
	
	public Image(Texture texture) {
		this(new TextureRegionDrawable(new TextureRegion(texture)));
	}
	
	public Image(Skin skin, String drawableName) {
		this(skin.getDrawable(drawableName), Scaling.stretch, Align.center);
	}
	
	public Image(Drawable drawable) {
		this(drawable, Scaling.stretch, Align.center);
	}
	
	public Image(Drawable drawable, Scaling scaling) {
		this(drawable, scaling, Align.center);
	}
	
	public Image(Drawable drawable, Scaling scaling, int align) {
		setDrawable(drawable);
		this.scaling = scaling;
		this.align = align;
		setSize(getPrefWidth(), getPrefHeight());
	}
	
	public void layout() {
		if (drawable == null)
			return;
		
		float regionWidth = drawable.getMinWidth();
		float regionHeight = drawable.getMinHeight();
		float width = getWidth();
		float height = getHeight();
		
		Vector2 size = scaling.apply(regionWidth, regionHeight, width, height);
		imageWidth = size.x;
		imageHeight = size.y;
		
		if ((align & Align.left) != 0)
			imageX = 0;
		else if ((align & Align.right) != 0)
			imageX = (int) (width - imageWidth);
		else
			imageX = (int) (width / 2 - imageWidth / 2);
		
		if ((align & Align.top) != 0)
			imageY = (int) (height - imageHeight);
		else if ((align & Align.bottom) != 0)
			imageY = 0;
		else
			imageY = (int) (height / 2 - imageHeight / 2);
	}
	
	public void draw(Batch batch, float parentAlpha) {
		validate();
		
		Color color = getColor();
		batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
		
		float x = getX();
		float y = getY();
		float scaleX = getScaleX();
		float scaleY = getScaleY();
		
		if (drawable instanceof TransformDrawable) {
			float rotation = getRotation();
			if (scaleX != 1 || scaleY != 1 || rotation != 0) {
				((TransformDrawable) drawable).draw(batch, x + imageX, y + imageY, getOriginX() - imageX, getOriginY() - imageY,
						imageWidth, imageHeight, scaleX, scaleY, rotation);
				return;
			}
		}
		if (drawable != null)
			drawable.draw(batch, x + imageX, y + imageY, imageWidth * scaleX, imageHeight * scaleY);
	}
	
	public void setDrawable(Skin skin, String drawableName) {
		setDrawable(skin.getDrawable(drawableName));
	}
	
	public void setDrawable(Drawable drawable) {
		if (this.drawable == drawable)
			return;
		if (drawable != null) {
			if (getPrefWidth() != drawable.getMinWidth() || getPrefHeight() != drawable.getMinHeight())
				invalidateHierarchy();
		} else
			invalidateHierarchy();
		this.drawable = drawable;
	}
	
	public Drawable getDrawable() {
		return drawable;
	}
	
	public void setScaling(Scaling scaling) {
		if (scaling == null)
			throw new IllegalArgumentException("scaling cannot be null.");
		this.scaling = scaling;
		invalidate();
	}
	
	public void setAlign(int align) {
		this.align = align;
		invalidate();
	}
	
	public int getAlign() {
		return align;
	}
	
	public float getMinWidth() {
		return 0;
	}
	
	public float getMinHeight() {
		return 0;
	}
	
	public float getPrefWidth() {
		if (drawable != null)
			return drawable.getMinWidth();
		return 0;
	}
	
	public float getPrefHeight() {
		if (drawable != null)
			return drawable.getMinHeight();
		return 0;
	}
	
	public float getImageX() {
		return imageX;
	}
	
	public float getImageY() {
		return imageY;
	}
	
	public float getImageWidth() {
		return imageWidth;
	}
	
	public float getImageHeight() {
		return imageHeight;
	}
	
	public String toString() {
		String name = getName();
		if (name != null)
			return name;
		String className = getClass().getName();
		int dotIndex = className.lastIndexOf('.');
		if (dotIndex != -1)
			className = className.substring(dotIndex + 1);
		return (className.indexOf('$') != -1 ? "Image " : "") + className + ": " + drawable;
	}
	
}
