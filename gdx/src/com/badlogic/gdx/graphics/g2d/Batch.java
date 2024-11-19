package com.badlogic.gdx.graphics.g2d;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.Shader;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;

public interface Batch extends Disposable {
	
	void begin();
	
	void end();
	
	void setColor(final Color tint);
	
	void setColor(final float r, final float g, final float b, final float a);
	
	Color getColor();
	
	void setPackedColor(final float packedColor);
	
	float getPackedColor();
	
	void draw(Texture texture, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY);
	
	void draw(Texture texture, float x, float y, float width, float height, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY);
	
	void draw(Texture texture, float x, float y, int srcX, int srcY, int srcWidth, int srcHeight);
	
	void draw(Texture texture, float x, float y, float width, float height, float u, float v, float u2, float v2);
	
	void draw(Texture texture, float x, float y);
	
	void draw(Texture texture, float x, float y, float width, float height);
	
	void draw(Texture texture, float[] spriteVertices, int offset, int count);
	
	void draw(TextureRegion region, float x, float y);
	
	void draw(TextureRegion region, float x, float y, float width, float height);
	
	void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation);
	
	void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation, boolean clockwise);
	
	void draw(TextureRegion region, float width, float height, Affine2 transform);
	
	void flush();
	
	void disableBlending();
	
	void enableBlending();
	
	void setBlendFunction(int srcFunc, int dstFunc);
	
	void setBlendFunctionSeparate(int srcFuncColor, int dstFuncColor, int srcFuncAlpha, int dstFuncAlpha);
	
	int getBlendSrcFunc();
	
	int getBlendDstFunc();
	
	int getBlendSrcFuncAlpha();
	
	int getBlendDstFuncAlpha();
	
	Matrix4 getProjectionMatrix();
	
	Matrix4 getTransformMatrix();
	
	void setProjectionMatrix(final Matrix4 projection);
	
	void setTransformMatrix(final Matrix4 transform);
	
	void setShader(final Shader shader);
	
	Shader getShader();
	
	boolean isBlendingEnabled();
	
	boolean isDrawing();
	
	int X1 = 0;
	int Y1 = 1;
	int C1 = 2;
	int U1 = 3;
	int V1 = 4;
	int X2 = 5;
	int Y2 = 6;
	int C2 = 7;
	int U2 = 8;
	int V2 = 9;
	int X3 = 10;
	int Y3 = 11;
	int C3 = 12;
	int U3 = 13;
	int V3 = 14;
	int X4 = 15;
	int Y4 = 16;
	int C4 = 17;
	int U4 = 18;
	int V4 = 19;
	
}
