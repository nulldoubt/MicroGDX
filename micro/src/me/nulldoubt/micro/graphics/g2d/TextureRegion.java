package me.nulldoubt.micro.graphics.g2d;

import me.nulldoubt.micro.graphics.Texture;

public class TextureRegion {
	
	Texture texture;
	float u, v;
	float u2, v2;
	int regionWidth, regionHeight;
	
	public TextureRegion() {}
	
	public TextureRegion(final Texture texture) {
		if (texture == null)
			throw new IllegalArgumentException("texture cannot be null.");
		this.texture = texture;
		setRegion(0, 0, texture.getWidth(), texture.getHeight());
	}
	
	public TextureRegion(Texture texture, int width, int height) {
		this.texture = texture;
		setRegion(0, 0, width, height);
	}
	
	public TextureRegion(Texture texture, int x, int y, int width, int height) {
		this.texture = texture;
		setRegion(x, y, width, height);
	}
	
	public TextureRegion(Texture texture, float u, float v, float u2, float v2) {
		this.texture = texture;
		setRegion(u, v, u2, v2);
	}
	
	public TextureRegion(TextureRegion region) {
		setRegion(region);
	}
	
	public TextureRegion(TextureRegion region, int x, int y, int width, int height) {
		setRegion(region, x, y, width, height);
	}
	
	public void setRegion(Texture texture) {
		this.texture = texture;
		setRegion(0, 0, texture.getWidth(), texture.getHeight());
	}
	
	public void setRegion(int x, int y, int width, int height) {
		float invTexWidth = 1f / texture.getWidth();
		float invTexHeight = 1f / texture.getHeight();
		setRegion(x * invTexWidth, y * invTexHeight, (x + width) * invTexWidth, (y + height) * invTexHeight);
		regionWidth = Math.abs(width);
		regionHeight = Math.abs(height);
	}
	
	public void setRegion(float u, float v, float u2, float v2) {
		int texWidth = texture.getWidth(), texHeight = texture.getHeight();
		regionWidth = Math.round(Math.abs(u2 - u) * texWidth);
		regionHeight = Math.round(Math.abs(v2 - v) * texHeight);
		
		// For a 1x1 region, adjust UVs toward pixel center to avoid filtering artifacts on AMD GPUs when drawing very stretched.
		if (regionWidth == 1 && regionHeight == 1) {
			float adjustX = 0.25f / texWidth;
			u += adjustX;
			u2 -= adjustX;
			float adjustY = 0.25f / texHeight;
			v += adjustY;
			v2 -= adjustY;
		}
		
		this.u = u;
		this.v = v;
		this.u2 = u2;
		this.v2 = v2;
	}
	
	public void setRegion(TextureRegion region) {
		texture = region.texture;
		setRegion(region.u, region.v, region.u2, region.v2);
	}
	
	public void setRegion(TextureRegion region, int x, int y, int width, int height) {
		texture = region.texture;
		setRegion(region.getRegionX() + x, region.getRegionY() + y, width, height);
	}
	
	public Texture getTexture() {
		return texture;
	}
	
	public void setTexture(Texture texture) {
		this.texture = texture;
	}
	
	public float getU() {
		return u;
	}
	
	public void setU(float u) {
		this.u = u;
		regionWidth = Math.round(Math.abs(u2 - u) * texture.getWidth());
	}
	
	public float getV() {
		return v;
	}
	
	public void setV(float v) {
		this.v = v;
		regionHeight = Math.round(Math.abs(v2 - v) * texture.getHeight());
	}
	
	public float getU2() {
		return u2;
	}
	
	public void setU2(float u2) {
		this.u2 = u2;
		regionWidth = Math.round(Math.abs(u2 - u) * texture.getWidth());
	}
	
	public float getV2() {
		return v2;
	}
	
	public void setV2(float v2) {
		this.v2 = v2;
		regionHeight = Math.round(Math.abs(v2 - v) * texture.getHeight());
	}
	
	public int getRegionX() {
		return Math.round(u * texture.getWidth());
	}
	
	public void setRegionX(int x) {
		setU(x / (float) texture.getWidth());
	}
	
	public int getRegionY() {
		return Math.round(v * texture.getHeight());
	}
	
	public void setRegionY(int y) {
		setV(y / (float) texture.getHeight());
	}
	
	public int getRegionWidth() {
		return regionWidth;
	}
	
	public void setRegionWidth(int width) {
		if (isFlipX()) {
			setU(u2 + width / (float) texture.getWidth());
		} else {
			setU2(u + width / (float) texture.getWidth());
		}
	}
	
	public int getRegionHeight() {
		return regionHeight;
	}
	
	public void setRegionHeight(int height) {
		if (isFlipY()) {
			setV(v2 + height / (float) texture.getHeight());
		} else {
			setV2(v + height / (float) texture.getHeight());
		}
	}
	
	public void flip(boolean x, boolean y) {
		if (x) {
			float temp = u;
			u = u2;
			u2 = temp;
		}
		if (y) {
			float temp = v;
			v = v2;
			v2 = temp;
		}
	}
	
	public boolean isFlipX() {
		return u > u2;
	}
	
	public boolean isFlipY() {
		return v > v2;
	}
	
	public void scroll(float xAmount, float yAmount) {
		if (xAmount != 0) {
			float width = (u2 - u) * texture.getWidth();
			u = (u + xAmount) % 1;
			u2 = u + width / texture.getWidth();
		}
		if (yAmount != 0) {
			float height = (v2 - v) * texture.getHeight();
			v = (v + yAmount) % 1;
			v2 = v + height / texture.getHeight();
		}
	}
	
	public TextureRegion[][] split(int tileWidth, int tileHeight) {
		int x = getRegionX();
		int y = getRegionY();
		int width = regionWidth;
		int height = regionHeight;
		
		int rows = height / tileHeight;
		int cols = width / tileWidth;
		
		int startX = x;
		TextureRegion[][] tiles = new TextureRegion[rows][cols];
		for (int row = 0; row < rows; row++, y += tileHeight) {
			x = startX;
			for (int col = 0; col < cols; col++, x += tileWidth) {
				tiles[row][col] = new TextureRegion(texture, x, y, tileWidth, tileHeight);
			}
		}
		
		return tiles;
	}
	
	public static TextureRegion[][] split(Texture texture, int tileWidth, int tileHeight) {
		TextureRegion region = new TextureRegion(texture);
		return region.split(tileWidth, tileHeight);
	}
	
}
