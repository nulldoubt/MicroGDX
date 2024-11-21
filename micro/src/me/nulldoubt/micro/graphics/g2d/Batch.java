package me.nulldoubt.micro.graphics.g2d;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.graphics.*;
import me.nulldoubt.micro.graphics.Mesh.VertexDataType;
import me.nulldoubt.micro.graphics.VertexAttributes.Usage;
import me.nulldoubt.micro.graphics.glutils.Shader;
import me.nulldoubt.micro.math.Affine2;
import me.nulldoubt.micro.math.MathUtils;
import me.nulldoubt.micro.math.Matrix4;

import java.nio.Buffer;

public class Batch {
	
	public static final int X1 = 0;
	public static final int Y1 = 1;
	public static final int C1 = 2;
	public static final int U1 = 3;
	public static final int V1 = 4;
	public static final int X2 = 5;
	public static final int Y2 = 6;
	public static final int C2 = 7;
	public static final int U2 = 8;
	public static final int V2 = 9;
	public static final int X3 = 10;
	public static final int Y3 = 11;
	public static final int C3 = 12;
	public static final int U3 = 13;
	public static final int V3 = 14;
	public static final int X4 = 15;
	public static final int Y4 = 16;
	public static final int C4 = 17;
	public static final int U4 = 18;
	public static final int V4 = 19;
	
	private final VertexDataType currentDataType;
	private final Mesh mesh;
	
	private final float[] vertices;
	private int idx = 0;
	private Texture lastTexture = null;
	private float invTexWidth = 0, invTexHeight = 0;
	
	private boolean drawing = false;
	
	private final Matrix4 transformMatrix = new Matrix4();
	private final Matrix4 projectionMatrix = new Matrix4();
	private final Matrix4 combinedMatrix = new Matrix4();
	
	private boolean blendingDisabled = false;
	private int blendSrcFunc = GL20.GL_SRC_ALPHA;
	private int blendDstFunc = GL20.GL_ONE_MINUS_SRC_ALPHA;
	private int blendSrcFuncAlpha = GL20.GL_SRC_ALPHA;
	private int blendDstFuncAlpha = GL20.GL_ONE_MINUS_SRC_ALPHA;
	
	private final Shader shader;
	private Shader customShader = null;
	private boolean ownsShader;
	
	private final Color color = new Color(1, 1, 1, 1);
	private float colorPacked = Color.WHITE_FLOAT_BITS;
	
	public int renderCalls = 0;
	public int totalRenderCalls = 0;
	public int maxSpritesInBatch = 0;
	
	public Batch() {
		this(1000, null);
	}
	
	public Batch(int size) {
		this(size, null);
	}
	
	public Batch(int size, Shader defaultShader) {
		// 32767 is max vertex index, so 32767 / 4 vertices per sprite = 8191 sprites max.
		if (size > 8191)
			throw new IllegalArgumentException("Can't have more than 8191 sprites per batch: " + size);
		
		currentDataType = (Micro.gl30 != null) ? VertexDataType.VertexBufferObjectWithVAO : VertexDataType.VertexBufferObject;
		mesh = new Mesh(currentDataType, false, size * 4, size * 6,
				new VertexAttribute(Usage.Position, 2, Shader.POSITION_ATTRIBUTE),
				new VertexAttribute(Usage.ColorPacked, 4, Shader.COLOR_ATTRIBUTE),
				new VertexAttribute(Usage.TextureCoordinates, 2, Shader.TEXCOORD_ATTRIBUTE + "0"));
		
		projectionMatrix.setToOrtho2D(0, 0, Micro.graphics.getWidth(), Micro.graphics.getHeight());
		
		vertices = new float[size * Sprite.SPRITE_SIZE];
		
		int len = size * 6;
		short[] indices = new short[len];
		short j = 0;
		for (int i = 0; i < len; i += 6, j += 4) {
			indices[i] = j;
			indices[i + 1] = (short) (j + 1);
			indices[i + 2] = (short) (j + 2);
			indices[i + 3] = (short) (j + 2);
			indices[i + 4] = (short) (j + 3);
			indices[i + 5] = j;
		}
		mesh.setIndices(indices);
		
		if (defaultShader == null) {
			shader = createDefaultShader();
			ownsShader = true;
		} else
			shader = defaultShader;
		
		mesh.getIndexData().bind();
		mesh.getIndexData().unbind();
	}
	
	public static Shader createDefaultShader() {
		String vertexShader = "attribute vec4 " + Shader.POSITION_ATTRIBUTE + ";\n" //
				+ "attribute vec4 " + Shader.COLOR_ATTRIBUTE + ";\n" //
				+ "attribute vec2 " + Shader.TEXCOORD_ATTRIBUTE + "0;\n" //
				+ "uniform mat4 u_projTrans;\n" //
				+ "varying vec4 v_color;\n" //
				+ "varying vec2 v_texCoords;\n" //
				+ "\n" //
				+ "void main()\n" //
				+ "{\n" //
				+ "   v_color = " + Shader.COLOR_ATTRIBUTE + ";\n" //
				+ "   v_color.a = v_color.a * (255.0/254.0);\n" //
				+ "   v_texCoords = " + Shader.TEXCOORD_ATTRIBUTE + "0;\n" //
				+ "   gl_Position =  u_projTrans * " + Shader.POSITION_ATTRIBUTE + ";\n" //
				+ "}\n";
		String fragmentShader = "#ifdef GL_ES\n" //
				+ "#define LOWP lowp\n" //
				+ "precision mediump float;\n" //
				+ "#else\n" //
				+ "#define LOWP \n" //
				+ "#endif\n" //
				+ "varying LOWP vec4 v_color;\n" //
				+ "varying vec2 v_texCoords;\n" //
				+ "uniform sampler2D u_texture;\n" //
				+ "void main()\n"//
				+ "{\n" //
				+ "  gl_FragColor = v_color * texture2D(u_texture, v_texCoords);\n" //
				+ "}";
		
		Shader shader = new Shader(vertexShader, fragmentShader);
		if (!shader.isCompiled())
			throw new IllegalArgumentException("Error compiling shader: " + shader.getLog());
		return shader;
	}
	
	public void begin() {
		if (drawing)
			throw new IllegalStateException("SpriteBatch.end must be called before begin.");
		renderCalls = 0;
		
		Micro.gl.glDepthMask(false);
		if (customShader != null)
			customShader.bind();
		else
			shader.bind();
		setupMatrices();
		
		drawing = true;
	}
	
	public void end() {
		if (!drawing)
			throw new IllegalStateException("SpriteBatch.begin must be called before end.");
		if (idx > 0)
			flush();
		lastTexture = null;
		drawing = false;
		
		GL20 gl = Micro.gl;
		gl.glDepthMask(true);
		if (isBlendingEnabled())
			gl.glDisable(GL20.GL_BLEND);
	}
	
	public void setColor(Color tint) {
		color.set(tint);
		colorPacked = tint.toFloatBits();
	}
	
	public void setColor(float r, float g, float b, float a) {
		color.set(r, g, b, a);
		colorPacked = color.toFloatBits();
	}
	
	public Color getColor() {
		return color;
	}
	
	public void setPackedColor(float packedColor) {
		Color.abgr8888ToColor(color, packedColor);
		this.colorPacked = packedColor;
	}
	
	public float getPackedColor() {
		return colorPacked;
	}
	
	public void draw(Texture texture, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
		if (!drawing)
			throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
		
		float[] vertices = this.vertices;
		
		if (texture != lastTexture)
			switchTexture(texture);
		else if (idx == vertices.length) //
			flush();
		
		// bottom left and top right corner points relative to origin
		final float worldOriginX = x + originX;
		final float worldOriginY = y + originY;
		float fx = -originX;
		float fy = -originY;
		float fx2 = width - originX;
		float fy2 = height - originY;
		
		// scale
		if (scaleX != 1 || scaleY != 1) {
			fx *= scaleX;
			fy *= scaleY;
			fx2 *= scaleX;
			fy2 *= scaleY;
		}
		
		// construct corner points, start from top left and go counter clockwise
		final float p1x = fx;
		final float p1y = fy;
		final float p2x = fx;
		final float p2y = fy2;
		final float p3x = fx2;
		final float p3y = fy2;
		final float p4x = fx2;
		final float p4y = fy;
		
		float x1;
		float y1;
		float x2;
		float y2;
		float x3;
		float y3;
		float x4;
		float y4;
		
		// rotate
		if (rotation != 0) {
			final float cos = MathUtils.cosDeg(rotation);
			final float sin = MathUtils.sinDeg(rotation);
			
			x1 = cos * p1x - sin * p1y;
			y1 = sin * p1x + cos * p1y;
			
			x2 = cos * p2x - sin * p2y;
			y2 = sin * p2x + cos * p2y;
			
			x3 = cos * p3x - sin * p3y;
			y3 = sin * p3x + cos * p3y;
			
			x4 = x1 + (x3 - x2);
			y4 = y3 - (y2 - y1);
		} else {
			x1 = p1x;
			y1 = p1y;
			
			x2 = p2x;
			y2 = p2y;
			
			x3 = p3x;
			y3 = p3y;
			
			x4 = p4x;
			y4 = p4y;
		}
		
		x1 += worldOriginX;
		y1 += worldOriginY;
		x2 += worldOriginX;
		y2 += worldOriginY;
		x3 += worldOriginX;
		y3 += worldOriginY;
		x4 += worldOriginX;
		y4 += worldOriginY;
		
		float u = srcX * invTexWidth;
		float v = (srcY + srcHeight) * invTexHeight;
		float u2 = (srcX + srcWidth) * invTexWidth;
		float v2 = srcY * invTexHeight;
		
		if (flipX) {
			float tmp = u;
			u = u2;
			u2 = tmp;
		}
		
		if (flipY) {
			float tmp = v;
			v = v2;
			v2 = tmp;
		}
		
		float color = this.colorPacked;
		int idx = this.idx;
		vertices[idx] = x1;
		vertices[idx + 1] = y1;
		vertices[idx + 2] = color;
		vertices[idx + 3] = u;
		vertices[idx + 4] = v;
		
		vertices[idx + 5] = x2;
		vertices[idx + 6] = y2;
		vertices[idx + 7] = color;
		vertices[idx + 8] = u;
		vertices[idx + 9] = v2;
		
		vertices[idx + 10] = x3;
		vertices[idx + 11] = y3;
		vertices[idx + 12] = color;
		vertices[idx + 13] = u2;
		vertices[idx + 14] = v2;
		
		vertices[idx + 15] = x4;
		vertices[idx + 16] = y4;
		vertices[idx + 17] = color;
		vertices[idx + 18] = u2;
		vertices[idx + 19] = v;
		this.idx = idx + 20;
	}
	
	public void draw(Texture texture, float x, float y, float width, float height, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
		if (!drawing)
			throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
		
		float[] vertices = this.vertices;
		
		if (texture != lastTexture)
			switchTexture(texture);
		else if (idx == vertices.length) //
			flush();
		
		float u = srcX * invTexWidth;
		float v = (srcY + srcHeight) * invTexHeight;
		float u2 = (srcX + srcWidth) * invTexWidth;
		float v2 = srcY * invTexHeight;
		final float fx2 = x + width;
		final float fy2 = y + height;
		
		if (flipX) {
			float tmp = u;
			u = u2;
			u2 = tmp;
		}
		
		if (flipY) {
			float tmp = v;
			v = v2;
			v2 = tmp;
		}
		
		float color = this.colorPacked;
		int idx = this.idx;
		vertices[idx] = x;
		vertices[idx + 1] = y;
		vertices[idx + 2] = color;
		vertices[idx + 3] = u;
		vertices[idx + 4] = v;
		
		vertices[idx + 5] = x;
		vertices[idx + 6] = fy2;
		vertices[idx + 7] = color;
		vertices[idx + 8] = u;
		vertices[idx + 9] = v2;
		
		vertices[idx + 10] = fx2;
		vertices[idx + 11] = fy2;
		vertices[idx + 12] = color;
		vertices[idx + 13] = u2;
		vertices[idx + 14] = v2;
		
		vertices[idx + 15] = fx2;
		vertices[idx + 16] = y;
		vertices[idx + 17] = color;
		vertices[idx + 18] = u2;
		vertices[idx + 19] = v;
		this.idx = idx + 20;
	}
	
	public void draw(Texture texture, float x, float y, int srcX, int srcY, int srcWidth, int srcHeight) {
		if (!drawing)
			throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
		
		float[] vertices = this.vertices;
		
		if (texture != lastTexture)
			switchTexture(texture);
		else if (idx == vertices.length) //
			flush();
		
		final float u = srcX * invTexWidth;
		final float v = (srcY + srcHeight) * invTexHeight;
		final float u2 = (srcX + srcWidth) * invTexWidth;
		final float v2 = srcY * invTexHeight;
		final float fx2 = x + srcWidth;
		final float fy2 = y + srcHeight;
		
		float color = this.colorPacked;
		int idx = this.idx;
		vertices[idx] = x;
		vertices[idx + 1] = y;
		vertices[idx + 2] = color;
		vertices[idx + 3] = u;
		vertices[idx + 4] = v;
		
		vertices[idx + 5] = x;
		vertices[idx + 6] = fy2;
		vertices[idx + 7] = color;
		vertices[idx + 8] = u;
		vertices[idx + 9] = v2;
		
		vertices[idx + 10] = fx2;
		vertices[idx + 11] = fy2;
		vertices[idx + 12] = color;
		vertices[idx + 13] = u2;
		vertices[idx + 14] = v2;
		
		vertices[idx + 15] = fx2;
		vertices[idx + 16] = y;
		vertices[idx + 17] = color;
		vertices[idx + 18] = u2;
		vertices[idx + 19] = v;
		this.idx = idx + 20;
	}
	
	public void draw(Texture texture, float x, float y, float width, float height, float u, float v, float u2, float v2) {
		if (!drawing)
			throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
		
		float[] vertices = this.vertices;
		
		if (texture != lastTexture)
			switchTexture(texture);
		else if (idx == vertices.length) //
			flush();
		
		final float fx2 = x + width;
		final float fy2 = y + height;
		
		float color = this.colorPacked;
		int idx = this.idx;
		vertices[idx] = x;
		vertices[idx + 1] = y;
		vertices[idx + 2] = color;
		vertices[idx + 3] = u;
		vertices[idx + 4] = v;
		
		vertices[idx + 5] = x;
		vertices[idx + 6] = fy2;
		vertices[idx + 7] = color;
		vertices[idx + 8] = u;
		vertices[idx + 9] = v2;
		
		vertices[idx + 10] = fx2;
		vertices[idx + 11] = fy2;
		vertices[idx + 12] = color;
		vertices[idx + 13] = u2;
		vertices[idx + 14] = v2;
		
		vertices[idx + 15] = fx2;
		vertices[idx + 16] = y;
		vertices[idx + 17] = color;
		vertices[idx + 18] = u2;
		vertices[idx + 19] = v;
		this.idx = idx + 20;
	}
	
	public void draw(Texture texture, float x, float y) {
		draw(texture, x, y, texture.getWidth(), texture.getHeight());
	}
	
	public void draw(Texture texture, float x, float y, float width, float height) {
		if (!drawing)
			throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
		
		float[] vertices = this.vertices;
		
		if (texture != lastTexture)
			switchTexture(texture);
		else if (idx == vertices.length) //
			flush();
		
		final float fx2 = x + width;
		final float fy2 = y + height;
		final float u = 0;
		final float v = 1;
		final float u2 = 1;
		final float v2 = 0;
		
		float color = this.colorPacked;
		int idx = this.idx;
		vertices[idx] = x;
		vertices[idx + 1] = y;
		vertices[idx + 2] = color;
		vertices[idx + 3] = u;
		vertices[idx + 4] = v;
		
		vertices[idx + 5] = x;
		vertices[idx + 6] = fy2;
		vertices[idx + 7] = color;
		vertices[idx + 8] = u;
		vertices[idx + 9] = v2;
		
		vertices[idx + 10] = fx2;
		vertices[idx + 11] = fy2;
		vertices[idx + 12] = color;
		vertices[idx + 13] = u2;
		vertices[idx + 14] = v2;
		
		vertices[idx + 15] = fx2;
		vertices[idx + 16] = y;
		vertices[idx + 17] = color;
		vertices[idx + 18] = u2;
		vertices[idx + 19] = v;
		this.idx = idx + 20;
	}
	
	public void draw(Texture texture, float[] spriteVertices, int offset, int count) {
		if (!drawing)
			throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
		
		int verticesLength = vertices.length;
		int remainingVertices = verticesLength;
		if (texture != lastTexture)
			switchTexture(texture);
		else {
			remainingVertices -= idx;
			if (remainingVertices == 0) {
				flush();
				remainingVertices = verticesLength;
			}
		}
		int copyCount = Math.min(remainingVertices, count);
		
		System.arraycopy(spriteVertices, offset, vertices, idx, copyCount);
		idx += copyCount;
		count -= copyCount;
		while (count > 0) {
			offset += copyCount;
			flush();
			copyCount = Math.min(verticesLength, count);
			System.arraycopy(spriteVertices, offset, vertices, 0, copyCount);
			idx += copyCount;
			count -= copyCount;
		}
	}
	
	public void draw(TextureRegion region, float x, float y) {
		draw(region, x, y, region.getRegionWidth(), region.getRegionHeight());
	}
	
	public void draw(TextureRegion region, float x, float y, float width, float height) {
		if (!drawing)
			throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
		
		float[] vertices = this.vertices;
		
		Texture texture = region.texture;
		if (texture != lastTexture) {
			switchTexture(texture);
		} else if (idx == vertices.length) //
			flush();
		
		final float fx2 = x + width;
		final float fy2 = y + height;
		final float u = region.u;
		final float v = region.v2;
		final float u2 = region.u2;
		final float v2 = region.v;
		
		float color = this.colorPacked;
		int idx = this.idx;
		vertices[idx] = x;
		vertices[idx + 1] = y;
		vertices[idx + 2] = color;
		vertices[idx + 3] = u;
		vertices[idx + 4] = v;
		
		vertices[idx + 5] = x;
		vertices[idx + 6] = fy2;
		vertices[idx + 7] = color;
		vertices[idx + 8] = u;
		vertices[idx + 9] = v2;
		
		vertices[idx + 10] = fx2;
		vertices[idx + 11] = fy2;
		vertices[idx + 12] = color;
		vertices[idx + 13] = u2;
		vertices[idx + 14] = v2;
		
		vertices[idx + 15] = fx2;
		vertices[idx + 16] = y;
		vertices[idx + 17] = color;
		vertices[idx + 18] = u2;
		vertices[idx + 19] = v;
		this.idx = idx + 20;
	}
	
	public void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
		if (!drawing)
			throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
		
		float[] vertices = this.vertices;
		
		Texture texture = region.texture;
		if (texture != lastTexture) {
			switchTexture(texture);
		} else if (idx == vertices.length) //
			flush();
		
		// bottom left and top right corner points relative to origin
		final float worldOriginX = x + originX;
		final float worldOriginY = y + originY;
		float fx = -originX;
		float fy = -originY;
		float fx2 = width - originX;
		float fy2 = height - originY;
		
		// scale
		if (scaleX != 1 || scaleY != 1) {
			fx *= scaleX;
			fy *= scaleY;
			fx2 *= scaleX;
			fy2 *= scaleY;
		}
		
		// construct corner points, start from top left and go counter clockwise
		final float p1x = fx;
		final float p1y = fy;
		final float p2x = fx;
		final float p2y = fy2;
		final float p3x = fx2;
		final float p3y = fy2;
		final float p4x = fx2;
		final float p4y = fy;
		
		float x1;
		float y1;
		float x2;
		float y2;
		float x3;
		float y3;
		float x4;
		float y4;
		
		// rotate
		if (rotation != 0) {
			final float cos = MathUtils.cosDeg(rotation);
			final float sin = MathUtils.sinDeg(rotation);
			
			x1 = cos * p1x - sin * p1y;
			y1 = sin * p1x + cos * p1y;
			
			x2 = cos * p2x - sin * p2y;
			y2 = sin * p2x + cos * p2y;
			
			x3 = cos * p3x - sin * p3y;
			y3 = sin * p3x + cos * p3y;
			
			x4 = x1 + (x3 - x2);
			y4 = y3 - (y2 - y1);
		} else {
			x1 = p1x;
			y1 = p1y;
			
			x2 = p2x;
			y2 = p2y;
			
			x3 = p3x;
			y3 = p3y;
			
			x4 = p4x;
			y4 = p4y;
		}
		
		x1 += worldOriginX;
		y1 += worldOriginY;
		x2 += worldOriginX;
		y2 += worldOriginY;
		x3 += worldOriginX;
		y3 += worldOriginY;
		x4 += worldOriginX;
		y4 += worldOriginY;
		
		final float u = region.u;
		final float v = region.v2;
		final float u2 = region.u2;
		final float v2 = region.v;
		
		float color = this.colorPacked;
		int idx = this.idx;
		vertices[idx] = x1;
		vertices[idx + 1] = y1;
		vertices[idx + 2] = color;
		vertices[idx + 3] = u;
		vertices[idx + 4] = v;
		
		vertices[idx + 5] = x2;
		vertices[idx + 6] = y2;
		vertices[idx + 7] = color;
		vertices[idx + 8] = u;
		vertices[idx + 9] = v2;
		
		vertices[idx + 10] = x3;
		vertices[idx + 11] = y3;
		vertices[idx + 12] = color;
		vertices[idx + 13] = u2;
		vertices[idx + 14] = v2;
		
		vertices[idx + 15] = x4;
		vertices[idx + 16] = y4;
		vertices[idx + 17] = color;
		vertices[idx + 18] = u2;
		vertices[idx + 19] = v;
		this.idx = idx + 20;
	}
	
	public void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation, boolean clockwise) {
		if (!drawing)
			throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
		
		float[] vertices = this.vertices;
		
		Texture texture = region.texture;
		if (texture != lastTexture) {
			switchTexture(texture);
		} else if (idx == vertices.length) //
			flush();
		
		// bottom left and top right corner points relative to origin
		final float worldOriginX = x + originX;
		final float worldOriginY = y + originY;
		float fx = -originX;
		float fy = -originY;
		float fx2 = width - originX;
		float fy2 = height - originY;
		
		// scale
		if (scaleX != 1 || scaleY != 1) {
			fx *= scaleX;
			fy *= scaleY;
			fx2 *= scaleX;
			fy2 *= scaleY;
		}
		
		// construct corner points, start from top left and go counter clockwise
		final float p1x = fx;
		final float p1y = fy;
		final float p2x = fx;
		final float p2y = fy2;
		final float p3x = fx2;
		final float p3y = fy2;
		final float p4x = fx2;
		final float p4y = fy;
		
		float x1;
		float y1;
		float x2;
		float y2;
		float x3;
		float y3;
		float x4;
		float y4;
		
		// rotate
		if (rotation != 0) {
			final float cos = MathUtils.cosDeg(rotation);
			final float sin = MathUtils.sinDeg(rotation);
			
			x1 = cos * p1x - sin * p1y;
			y1 = sin * p1x + cos * p1y;
			
			x2 = cos * p2x - sin * p2y;
			y2 = sin * p2x + cos * p2y;
			
			x3 = cos * p3x - sin * p3y;
			y3 = sin * p3x + cos * p3y;
			
			x4 = x1 + (x3 - x2);
			y4 = y3 - (y2 - y1);
		} else {
			x1 = p1x;
			y1 = p1y;
			
			x2 = p2x;
			y2 = p2y;
			
			x3 = p3x;
			y3 = p3y;
			
			x4 = p4x;
			y4 = p4y;
		}
		
		x1 += worldOriginX;
		y1 += worldOriginY;
		x2 += worldOriginX;
		y2 += worldOriginY;
		x3 += worldOriginX;
		y3 += worldOriginY;
		x4 += worldOriginX;
		y4 += worldOriginY;
		
		float u1, v1, u2, v2, u3, v3, u4, v4;
		if (clockwise) {
			u1 = region.u2;
			v1 = region.v2;
			u2 = region.u;
			v2 = region.v2;
			u3 = region.u;
			v3 = region.v;
			u4 = region.u2;
			v4 = region.v;
		} else {
			u1 = region.u;
			v1 = region.v;
			u2 = region.u2;
			v2 = region.v;
			u3 = region.u2;
			v3 = region.v2;
			u4 = region.u;
			v4 = region.v2;
		}
		
		float color = this.colorPacked;
		int idx = this.idx;
		vertices[idx] = x1;
		vertices[idx + 1] = y1;
		vertices[idx + 2] = color;
		vertices[idx + 3] = u1;
		vertices[idx + 4] = v1;
		
		vertices[idx + 5] = x2;
		vertices[idx + 6] = y2;
		vertices[idx + 7] = color;
		vertices[idx + 8] = u2;
		vertices[idx + 9] = v2;
		
		vertices[idx + 10] = x3;
		vertices[idx + 11] = y3;
		vertices[idx + 12] = color;
		vertices[idx + 13] = u3;
		vertices[idx + 14] = v3;
		
		vertices[idx + 15] = x4;
		vertices[idx + 16] = y4;
		vertices[idx + 17] = color;
		vertices[idx + 18] = u4;
		vertices[idx + 19] = v4;
		this.idx = idx + 20;
	}
	
	public void draw(TextureRegion region, float width, float height, Affine2 transform) {
		if (!drawing)
			throw new IllegalStateException("SpriteBatch.begin must be called before draw.");
		
		float[] vertices = this.vertices;
		
		Texture texture = region.texture;
		if (texture != lastTexture) {
			switchTexture(texture);
		} else if (idx == vertices.length) {
			flush();
		}
		
		// construct corner points
		float x1 = transform.m02;
		float y1 = transform.m12;
		float x2 = transform.m01 * height + transform.m02;
		float y2 = transform.m11 * height + transform.m12;
		float x3 = transform.m00 * width + transform.m01 * height + transform.m02;
		float y3 = transform.m10 * width + transform.m11 * height + transform.m12;
		float x4 = transform.m00 * width + transform.m02;
		float y4 = transform.m10 * width + transform.m12;
		
		float u = region.u;
		float v = region.v2;
		float u2 = region.u2;
		float v2 = region.v;
		
		float color = this.colorPacked;
		int idx = this.idx;
		vertices[idx] = x1;
		vertices[idx + 1] = y1;
		vertices[idx + 2] = color;
		vertices[idx + 3] = u;
		vertices[idx + 4] = v;
		
		vertices[idx + 5] = x2;
		vertices[idx + 6] = y2;
		vertices[idx + 7] = color;
		vertices[idx + 8] = u;
		vertices[idx + 9] = v2;
		
		vertices[idx + 10] = x3;
		vertices[idx + 11] = y3;
		vertices[idx + 12] = color;
		vertices[idx + 13] = u2;
		vertices[idx + 14] = v2;
		
		vertices[idx + 15] = x4;
		vertices[idx + 16] = y4;
		vertices[idx + 17] = color;
		vertices[idx + 18] = u2;
		vertices[idx + 19] = v;
		this.idx = idx + 20;
	}
	
	public void flush() {
		if (idx == 0)
			return;
		
		renderCalls++;
		totalRenderCalls++;
		int spritesInBatch = idx / 20;
		if (spritesInBatch > maxSpritesInBatch)
			maxSpritesInBatch = spritesInBatch;
		int count = spritesInBatch * 6;
		
		lastTexture.bind();
		Mesh mesh = this.mesh;
		mesh.setVertices(vertices, 0, idx);
		
		// Only upload indices for the vertex array type
		if (currentDataType == VertexDataType.VertexArray) {
			Buffer indicesBuffer = (Buffer) mesh.getIndicesBuffer(true);
			indicesBuffer.position(0);
			indicesBuffer.limit(count);
		}
		
		if (blendingDisabled) {
			Micro.gl.glDisable(GL20.GL_BLEND);
		} else {
			Micro.gl.glEnable(GL20.GL_BLEND);
			if (blendSrcFunc != -1)
				Micro.gl.glBlendFuncSeparate(blendSrcFunc, blendDstFunc, blendSrcFuncAlpha, blendDstFuncAlpha);
		}
		
		mesh.render(customShader != null ? customShader : shader, GL20.GL_TRIANGLES, 0, count);
		
		idx = 0;
	}
	
	public void disableBlending() {
		if (blendingDisabled)
			return;
		flush();
		blendingDisabled = true;
	}
	
	public void enableBlending() {
		if (!blendingDisabled)
			return;
		flush();
		blendingDisabled = false;
	}
	
	public void setBlendFunction(int srcFunc, int dstFunc) {
		setBlendFunctionSeparate(srcFunc, dstFunc, srcFunc, dstFunc);
	}
	
	public void setBlendFunctionSeparate(int srcFuncColor, int dstFuncColor, int srcFuncAlpha, int dstFuncAlpha) {
		if (blendSrcFunc == srcFuncColor && blendDstFunc == dstFuncColor && blendSrcFuncAlpha == srcFuncAlpha
				&& blendDstFuncAlpha == dstFuncAlpha)
			return;
		flush();
		blendSrcFunc = srcFuncColor;
		blendDstFunc = dstFuncColor;
		blendSrcFuncAlpha = srcFuncAlpha;
		blendDstFuncAlpha = dstFuncAlpha;
	}
	
	public int getBlendSrcFunc() {
		return blendSrcFunc;
	}
	
	public int getBlendDstFunc() {
		return blendDstFunc;
	}
	
	public int getBlendSrcFuncAlpha() {
		return blendSrcFuncAlpha;
	}
	
	public int getBlendDstFuncAlpha() {
		return blendDstFuncAlpha;
	}
	
	public void dispose() {
		mesh.dispose();
		if (ownsShader && shader != null)
			shader.dispose();
	}
	
	public Matrix4 getProjectionMatrix() {
		return projectionMatrix;
	}
	
	public Matrix4 getTransformMatrix() {
		return transformMatrix;
	}
	
	public void setProjectionMatrix(Matrix4 projection) {
		if (drawing)
			flush();
		projectionMatrix.set(projection);
		if (drawing)
			setupMatrices();
	}
	
	public void setTransformMatrix(Matrix4 transform) {
		if (drawing)
			flush();
		transformMatrix.set(transform);
		if (drawing)
			setupMatrices();
	}
	
	protected void setupMatrices() {
		combinedMatrix.set(projectionMatrix).mul(transformMatrix);
		if (customShader != null) {
			customShader.setUniformMatrix("u_projTrans", combinedMatrix);
			customShader.setUniformi("u_texture", 0);
		} else {
			shader.setUniformMatrix("u_projTrans", combinedMatrix);
			shader.setUniformi("u_texture", 0);
		}
	}
	
	protected void switchTexture(Texture texture) {
		flush();
		lastTexture = texture;
		invTexWidth = 1.0f / texture.getWidth();
		invTexHeight = 1.0f / texture.getHeight();
	}
	
	public void setShader(Shader shader) {
		if (shader == customShader) // avoid unnecessary flushing in case we are drawing
			return;
		if (drawing)
			flush();
		customShader = shader;
		if (drawing) {
			if (customShader != null)
				customShader.bind();
			else
				this.shader.bind();
			setupMatrices();
		}
	}
	
	public Shader getShader() {
		if (customShader == null)
			return shader;
		return customShader;
	}
	
	public boolean isBlendingEnabled() {
		return !blendingDisabled;
	}
	
	public boolean isDrawing() {
		return drawing;
	}
	
}
