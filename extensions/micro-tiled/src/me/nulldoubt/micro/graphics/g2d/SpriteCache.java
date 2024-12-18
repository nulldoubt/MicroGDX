package me.nulldoubt.micro.graphics.g2d;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.graphics.*;
import me.nulldoubt.micro.graphics.VertexAttributes.Usage;
import me.nulldoubt.micro.graphics.glutils.Shader;
import me.nulldoubt.micro.math.MathUtils;
import me.nulldoubt.micro.math.Matrix4;
import me.nulldoubt.micro.utils.Disposable;
import me.nulldoubt.micro.utils.collections.Array;
import me.nulldoubt.micro.utils.collections.IntArray;

import java.nio.Buffer;
import java.nio.FloatBuffer;

import static me.nulldoubt.micro.graphics.g2d.Sprite.SPRITE_SIZE;
import static me.nulldoubt.micro.graphics.g2d.Sprite.VERTEX_SIZE;

public class SpriteCache implements Disposable {
	
	private static final float[] tempVertices = new float[VERTEX_SIZE * 6];
	
	private final Mesh mesh;
	private boolean drawing;
	private final Matrix4 transformMatrix = new Matrix4();
	private final Matrix4 projectionMatrix = new Matrix4();
	private Array<Cache> caches = new Array();
	
	private final Matrix4 combinedMatrix = new Matrix4();
	private final Shader shader;
	
	private Cache currentCache;
	private final Array<Texture> textures = new Array(8);
	private final IntArray counts = new IntArray(8);
	
	private final Color color = new Color(1, 1, 1, 1);
	private float colorPacked = Color.WHITE_FLOAT_BITS;
	
	private Shader customShader = null;
	
	public int renderCalls = 0;
	
	public int totalRenderCalls = 0;
	
	public SpriteCache() {
		this(1000, false);
	}
	
	public SpriteCache(int size, boolean useIndices) {
		this(size, createDefaultShader(), useIndices);
	}
	
	public SpriteCache(int size, Shader shader, boolean useIndices) {
		this.shader = shader;
		
		if (useIndices && size > 8191)
			throw new IllegalArgumentException("Can't have more than 8191 sprites per batch: " + size);
		
		mesh = new Mesh(true, size * (useIndices ? 4 : 6), useIndices ? size * 6 : 0,
				new VertexAttribute(Usage.Position, 2, Shader.POSITION_ATTRIBUTE),
				new VertexAttribute(Usage.ColorPacked, 4, Shader.COLOR_ATTRIBUTE),
				new VertexAttribute(Usage.TextureCoordinates, 2, Shader.TEXCOORD_ATTRIBUTE + "0"));
		mesh.setAutoBind(false);
		
		if (useIndices) {
			int length = size * 6;
			short[] indices = new short[length];
			short j = 0;
			for (int i = 0; i < length; i += 6, j += 4) {
				indices[i + 0] = j;
				indices[i + 1] = (short) (j + 1);
				indices[i + 2] = (short) (j + 2);
				indices[i + 3] = (short) (j + 2);
				indices[i + 4] = (short) (j + 3);
				indices[i + 5] = j;
			}
			mesh.setIndices(indices);
		}
		
		projectionMatrix.setToOrtho2D(0, 0, Micro.graphics.getWidth(), Micro.graphics.getHeight());
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
		colorPacked = packedColor;
	}
	
	public float getPackedColor() {
		return colorPacked;
	}
	
	public void beginCache() {
		if (drawing)
			throw new IllegalStateException("end must be called before beginCache");
		if (currentCache != null)
			throw new IllegalStateException("endCache must be called before begin.");
		int verticesPerImage = mesh.getNumIndices() > 0 ? 4 : 6;
		FloatBuffer verticesBuffer = mesh.getVerticesBuffer(true);
		currentCache = new Cache(caches.size, verticesBuffer.limit());
		caches.add(currentCache);
		verticesBuffer.compact();
	}
	
	public void beginCache(int cacheID) {
		if (drawing)
			throw new IllegalStateException("end must be called before beginCache");
		if (currentCache != null)
			throw new IllegalStateException("endCache must be called before begin.");
		Buffer verticesBuffer = (Buffer) mesh.getVerticesBuffer(true);
		if (cacheID == caches.size - 1) {
			Cache oldCache = caches.removeIndex(cacheID);
			verticesBuffer.limit(oldCache.offset);
			beginCache();
			return;
		}
		currentCache = caches.get(cacheID);
		verticesBuffer.position(currentCache.offset);
	}
	
	public int endCache() {
		if (currentCache == null)
			throw new IllegalStateException("beginCache must be called before endCache.");
		Cache cache = currentCache;
		int cacheCount = mesh.getVerticesBuffer(false).position() - cache.offset;
		if (cache.textures == null) {
			// New cache.
			cache.maxCount = cacheCount;
			cache.textureCount = textures.size;
			cache.textures = textures.toArray(Texture.class);
			cache.counts = new int[cache.textureCount];
			for (int i = 0, n = counts.size; i < n; i++)
				cache.counts[i] = counts.get(i);
			
			((Buffer) mesh.getVerticesBuffer(true)).flip();
		} else {
			// Redefine existing cache.
			if (cacheCount > cache.maxCount)
				throw new MicroRuntimeException("If a cache is not the last created, it cannot be redefined with more entries than when it was first created: " + cacheCount + " (" + cache.maxCount + " max)");
			
			cache.textureCount = textures.size;
			
			if (cache.textures.length < cache.textureCount)
				cache.textures = new Texture[cache.textureCount];
			for (int i = 0, n = cache.textureCount; i < n; i++)
				cache.textures[i] = textures.get(i);
			
			if (cache.counts.length < cache.textureCount)
				cache.counts = new int[cache.textureCount];
			for (int i = 0, n = cache.textureCount; i < n; i++)
				cache.counts[i] = counts.get(i);
			
			FloatBuffer vertices = mesh.getVerticesBuffer(true);
			((Buffer) vertices).position(0);
			Cache lastCache = caches.get(caches.size - 1);
			((Buffer) vertices).limit(lastCache.offset + lastCache.maxCount);
		}
		
		currentCache = null;
		textures.clear();
		counts.clear();
		
		return cache.id;
	}
	
	public void clear() {
		caches.clear();
		((Buffer) mesh.getVerticesBuffer(true)).clear().flip();
	}
	
	public void add(Texture texture, float[] vertices, int offset, int length) {
		if (currentCache == null)
			throw new IllegalStateException("beginCache must be called before add.");
		
		int verticesPerImage = mesh.getNumIndices() > 0 ? 4 : 6;
		int count = length / (verticesPerImage * VERTEX_SIZE) * 6;
		int lastIndex = textures.size - 1;
		if (lastIndex < 0 || textures.get(lastIndex) != texture) {
			textures.add(texture);
			counts.add(count);
		} else
			counts.incr(lastIndex, count);
		
		mesh.getVerticesBuffer(true).put(vertices, offset, length);
	}
	
	public void add(Texture texture, float x, float y) {
		final float fx2 = x + texture.getWidth();
		final float fy2 = y + texture.getHeight();
		
		tempVertices[0] = x;
		tempVertices[1] = y;
		tempVertices[2] = colorPacked;
		tempVertices[3] = 0;
		tempVertices[4] = 1;
		
		tempVertices[5] = x;
		tempVertices[6] = fy2;
		tempVertices[7] = colorPacked;
		tempVertices[8] = 0;
		tempVertices[9] = 0;
		
		tempVertices[10] = fx2;
		tempVertices[11] = fy2;
		tempVertices[12] = colorPacked;
		tempVertices[13] = 1;
		tempVertices[14] = 0;
		
		if (mesh.getNumIndices() > 0) {
			tempVertices[15] = fx2;
			tempVertices[16] = y;
			tempVertices[17] = colorPacked;
			tempVertices[18] = 1;
			tempVertices[19] = 1;
			add(texture, tempVertices, 0, 20);
		} else {
			tempVertices[15] = fx2;
			tempVertices[16] = fy2;
			tempVertices[17] = colorPacked;
			tempVertices[18] = 1;
			tempVertices[19] = 0;
			
			tempVertices[20] = fx2;
			tempVertices[21] = y;
			tempVertices[22] = colorPacked;
			tempVertices[23] = 1;
			tempVertices[24] = 1;
			
			tempVertices[25] = x;
			tempVertices[26] = y;
			tempVertices[27] = colorPacked;
			tempVertices[28] = 0;
			tempVertices[29] = 1;
			add(texture, tempVertices, 0, 30);
		}
	}
	
	public void add(Texture texture, float x, float y, int srcWidth, int srcHeight, float u, float v, float u2, float v2,
					float color) {
		final float fx2 = x + srcWidth;
		final float fy2 = y + srcHeight;
		
		tempVertices[0] = x;
		tempVertices[1] = y;
		tempVertices[2] = color;
		tempVertices[3] = u;
		tempVertices[4] = v;
		
		tempVertices[5] = x;
		tempVertices[6] = fy2;
		tempVertices[7] = color;
		tempVertices[8] = u;
		tempVertices[9] = v2;
		
		tempVertices[10] = fx2;
		tempVertices[11] = fy2;
		tempVertices[12] = color;
		tempVertices[13] = u2;
		tempVertices[14] = v2;
		
		if (mesh.getNumIndices() > 0) {
			tempVertices[15] = fx2;
			tempVertices[16] = y;
			tempVertices[17] = color;
			tempVertices[18] = u2;
			tempVertices[19] = v;
			add(texture, tempVertices, 0, 20);
		} else {
			tempVertices[15] = fx2;
			tempVertices[16] = fy2;
			tempVertices[17] = color;
			tempVertices[18] = u2;
			tempVertices[19] = v2;
			
			tempVertices[20] = fx2;
			tempVertices[21] = y;
			tempVertices[22] = color;
			tempVertices[23] = u2;
			tempVertices[24] = v;
			
			tempVertices[25] = x;
			tempVertices[26] = y;
			tempVertices[27] = color;
			tempVertices[28] = u;
			tempVertices[29] = v;
			add(texture, tempVertices, 0, 30);
		}
	}
	
	public void add(Texture texture, float x, float y, int srcX, int srcY, int srcWidth, int srcHeight) {
		float invTexWidth = 1.0f / texture.getWidth();
		float invTexHeight = 1.0f / texture.getHeight();
		final float u = srcX * invTexWidth;
		final float v = (srcY + srcHeight) * invTexHeight;
		final float u2 = (srcX + srcWidth) * invTexWidth;
		final float v2 = srcY * invTexHeight;
		final float fx2 = x + srcWidth;
		final float fy2 = y + srcHeight;
		
		tempVertices[0] = x;
		tempVertices[1] = y;
		tempVertices[2] = colorPacked;
		tempVertices[3] = u;
		tempVertices[4] = v;
		
		tempVertices[5] = x;
		tempVertices[6] = fy2;
		tempVertices[7] = colorPacked;
		tempVertices[8] = u;
		tempVertices[9] = v2;
		
		tempVertices[10] = fx2;
		tempVertices[11] = fy2;
		tempVertices[12] = colorPacked;
		tempVertices[13] = u2;
		tempVertices[14] = v2;
		
		if (mesh.getNumIndices() > 0) {
			tempVertices[15] = fx2;
			tempVertices[16] = y;
			tempVertices[17] = colorPacked;
			tempVertices[18] = u2;
			tempVertices[19] = v;
			add(texture, tempVertices, 0, 20);
		} else {
			tempVertices[15] = fx2;
			tempVertices[16] = fy2;
			tempVertices[17] = colorPacked;
			tempVertices[18] = u2;
			tempVertices[19] = v2;
			
			tempVertices[20] = fx2;
			tempVertices[21] = y;
			tempVertices[22] = colorPacked;
			tempVertices[23] = u2;
			tempVertices[24] = v;
			
			tempVertices[25] = x;
			tempVertices[26] = y;
			tempVertices[27] = colorPacked;
			tempVertices[28] = u;
			tempVertices[29] = v;
			add(texture, tempVertices, 0, 30);
		}
	}
	
	public void add(Texture texture, float x, float y, float width, float height, int srcX, int srcY, int srcWidth, int srcHeight,
					boolean flipX, boolean flipY) {
		
		float invTexWidth = 1.0f / texture.getWidth();
		float invTexHeight = 1.0f / texture.getHeight();
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
		
		tempVertices[0] = x;
		tempVertices[1] = y;
		tempVertices[2] = colorPacked;
		tempVertices[3] = u;
		tempVertices[4] = v;
		
		tempVertices[5] = x;
		tempVertices[6] = fy2;
		tempVertices[7] = colorPacked;
		tempVertices[8] = u;
		tempVertices[9] = v2;
		
		tempVertices[10] = fx2;
		tempVertices[11] = fy2;
		tempVertices[12] = colorPacked;
		tempVertices[13] = u2;
		tempVertices[14] = v2;
		
		if (mesh.getNumIndices() > 0) {
			tempVertices[15] = fx2;
			tempVertices[16] = y;
			tempVertices[17] = colorPacked;
			tempVertices[18] = u2;
			tempVertices[19] = v;
			add(texture, tempVertices, 0, 20);
		} else {
			tempVertices[15] = fx2;
			tempVertices[16] = fy2;
			tempVertices[17] = colorPacked;
			tempVertices[18] = u2;
			tempVertices[19] = v2;
			
			tempVertices[20] = fx2;
			tempVertices[21] = y;
			tempVertices[22] = colorPacked;
			tempVertices[23] = u2;
			tempVertices[24] = v;
			
			tempVertices[25] = x;
			tempVertices[26] = y;
			tempVertices[27] = colorPacked;
			tempVertices[28] = u;
			tempVertices[29] = v;
			add(texture, tempVertices, 0, 30);
		}
	}
	
	public void add(Texture texture, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
		
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
		
		float invTexWidth = 1.0f / texture.getWidth();
		float invTexHeight = 1.0f / texture.getHeight();
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
		
		tempVertices[0] = x1;
		tempVertices[1] = y1;
		tempVertices[2] = colorPacked;
		tempVertices[3] = u;
		tempVertices[4] = v;
		
		tempVertices[5] = x2;
		tempVertices[6] = y2;
		tempVertices[7] = colorPacked;
		tempVertices[8] = u;
		tempVertices[9] = v2;
		
		tempVertices[10] = x3;
		tempVertices[11] = y3;
		tempVertices[12] = colorPacked;
		tempVertices[13] = u2;
		tempVertices[14] = v2;
		
		if (mesh.getNumIndices() > 0) {
			tempVertices[15] = x4;
			tempVertices[16] = y4;
			tempVertices[17] = colorPacked;
			tempVertices[18] = u2;
			tempVertices[19] = v;
			add(texture, tempVertices, 0, 20);
		} else {
			tempVertices[15] = x3;
			tempVertices[16] = y3;
			tempVertices[17] = colorPacked;
			tempVertices[18] = u2;
			tempVertices[19] = v2;
			
			tempVertices[20] = x4;
			tempVertices[21] = y4;
			tempVertices[22] = colorPacked;
			tempVertices[23] = u2;
			tempVertices[24] = v;
			
			tempVertices[25] = x1;
			tempVertices[26] = y1;
			tempVertices[27] = colorPacked;
			tempVertices[28] = u;
			tempVertices[29] = v;
			add(texture, tempVertices, 0, 30);
		}
	}
	
	public void add(TextureRegion region, float x, float y) {
		add(region, x, y, region.getRegionWidth(), region.getRegionHeight());
	}
	
	public void add(TextureRegion region, float x, float y, float width, float height) {
		final float fx2 = x + width;
		final float fy2 = y + height;
		final float u = region.u;
		final float v = region.v2;
		final float u2 = region.u2;
		final float v2 = region.v;
		
		tempVertices[0] = x;
		tempVertices[1] = y;
		tempVertices[2] = colorPacked;
		tempVertices[3] = u;
		tempVertices[4] = v;
		
		tempVertices[5] = x;
		tempVertices[6] = fy2;
		tempVertices[7] = colorPacked;
		tempVertices[8] = u;
		tempVertices[9] = v2;
		
		tempVertices[10] = fx2;
		tempVertices[11] = fy2;
		tempVertices[12] = colorPacked;
		tempVertices[13] = u2;
		tempVertices[14] = v2;
		
		if (mesh.getNumIndices() > 0) {
			tempVertices[15] = fx2;
			tempVertices[16] = y;
			tempVertices[17] = colorPacked;
			tempVertices[18] = u2;
			tempVertices[19] = v;
			add(region.texture, tempVertices, 0, 20);
		} else {
			tempVertices[15] = fx2;
			tempVertices[16] = fy2;
			tempVertices[17] = colorPacked;
			tempVertices[18] = u2;
			tempVertices[19] = v2;
			
			tempVertices[20] = fx2;
			tempVertices[21] = y;
			tempVertices[22] = colorPacked;
			tempVertices[23] = u2;
			tempVertices[24] = v;
			
			tempVertices[25] = x;
			tempVertices[26] = y;
			tempVertices[27] = colorPacked;
			tempVertices[28] = u;
			tempVertices[29] = v;
			add(region.texture, tempVertices, 0, 30);
		}
	}
	
	public void add(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
		
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
		
		tempVertices[0] = x1;
		tempVertices[1] = y1;
		tempVertices[2] = colorPacked;
		tempVertices[3] = u;
		tempVertices[4] = v;
		
		tempVertices[5] = x2;
		tempVertices[6] = y2;
		tempVertices[7] = colorPacked;
		tempVertices[8] = u;
		tempVertices[9] = v2;
		
		tempVertices[10] = x3;
		tempVertices[11] = y3;
		tempVertices[12] = colorPacked;
		tempVertices[13] = u2;
		tempVertices[14] = v2;
		
		if (mesh.getNumIndices() > 0) {
			tempVertices[15] = x4;
			tempVertices[16] = y4;
			tempVertices[17] = colorPacked;
			tempVertices[18] = u2;
			tempVertices[19] = v;
			add(region.texture, tempVertices, 0, 20);
		} else {
			tempVertices[15] = x3;
			tempVertices[16] = y3;
			tempVertices[17] = colorPacked;
			tempVertices[18] = u2;
			tempVertices[19] = v2;
			
			tempVertices[20] = x4;
			tempVertices[21] = y4;
			tempVertices[22] = colorPacked;
			tempVertices[23] = u2;
			tempVertices[24] = v;
			
			tempVertices[25] = x1;
			tempVertices[26] = y1;
			tempVertices[27] = colorPacked;
			tempVertices[28] = u;
			tempVertices[29] = v;
			add(region.texture, tempVertices, 0, 30);
		}
	}
	
	public void add(Sprite sprite) {
		if (mesh.getNumIndices() > 0) {
			add(sprite.getTexture(), sprite.getVertices(), 0, SPRITE_SIZE);
			return;
		}
		
		float[] spriteVertices = sprite.getVertices();
		System.arraycopy(spriteVertices, 0, tempVertices, 0, 3 * VERTEX_SIZE); // temp0,1,2=sprite0,1,2
		System.arraycopy(spriteVertices, 2 * VERTEX_SIZE, tempVertices, 3 * VERTEX_SIZE, VERTEX_SIZE); // temp3=sprite2
		System.arraycopy(spriteVertices, 3 * VERTEX_SIZE, tempVertices, 4 * VERTEX_SIZE, VERTEX_SIZE); // temp4=sprite3
		System.arraycopy(spriteVertices, 0, tempVertices, 5 * VERTEX_SIZE, VERTEX_SIZE); // temp5=sprite0
		add(sprite.getTexture(), tempVertices, 0, 30);
	}
	
	public void begin() {
		if (drawing)
			throw new IllegalStateException("end must be called before begin.");
		if (currentCache != null)
			throw new IllegalStateException("endCache must be called before begin");
		renderCalls = 0;
		combinedMatrix.set(projectionMatrix).mul(transformMatrix);
		
		Micro.gl20.glDepthMask(false);
		
		if (customShader != null) {
			customShader.bind();
			customShader.setUniformMatrix("u_proj", projectionMatrix);
			customShader.setUniformMatrix("u_trans", transformMatrix);
			customShader.setUniformMatrix("u_projTrans", combinedMatrix);
			customShader.setUniformi("u_texture", 0);
			mesh.bind(customShader);
		} else {
			shader.bind();
			shader.setUniformMatrix("u_projectionViewMatrix", combinedMatrix);
			shader.setUniformi("u_texture", 0);
			mesh.bind(shader);
		}
		drawing = true;
	}
	
	public void end() {
		if (!drawing)
			throw new IllegalStateException("begin must be called before end.");
		drawing = false;
		
		GL20 gl = Micro.gl20;
		gl.glDepthMask(true);
		if (customShader != null)
			mesh.unbind(customShader);
		else
			mesh.unbind(shader);
	}
	
	public void draw(int cacheID) {
		if (!drawing)
			throw new IllegalStateException("SpriteCache.begin must be called before draw.");
		
		Cache cache = caches.get(cacheID);
		int verticesPerImage = mesh.getNumIndices() > 0 ? 4 : 6;
		int offset = cache.offset / (verticesPerImage * VERTEX_SIZE) * 6;
		Texture[] textures = cache.textures;
		int[] counts = cache.counts;
		int textureCount = cache.textureCount;
		for (int i = 0; i < textureCount; i++) {
			int count = counts[i];
			textures[i].bind();
			if (customShader != null)
				mesh.render(customShader, GL20.GL_TRIANGLES, offset, count);
			else
				mesh.render(shader, GL20.GL_TRIANGLES, offset, count);
			offset += count;
		}
		renderCalls += textureCount;
		totalRenderCalls += textureCount;
	}
	
	public void draw(int cacheID, int offset, int length) {
		if (!drawing)
			throw new IllegalStateException("SpriteCache.begin must be called before draw.");
		
		Cache cache = caches.get(cacheID);
		int verticesPerImage = mesh.getNumIndices() > 0 ? 4 : 6;
		offset = cache.offset / (verticesPerImage * VERTEX_SIZE) * 6 + offset * 6;
		length *= 6;
		Texture[] textures = cache.textures;
		int[] counts = cache.counts;
		int textureCount = cache.textureCount;
		for (int i = 0; i < textureCount; i++) {
			textures[i].bind();
			int count = counts[i];
			if (count > length) {
				i = textureCount;
				count = length;
			} else
				length -= count;
			if (customShader != null)
				mesh.render(customShader, GL20.GL_TRIANGLES, offset, count);
			else
				mesh.render(shader, GL20.GL_TRIANGLES, offset, count);
			offset += count;
		}
		renderCalls += cache.textureCount;
		totalRenderCalls += textureCount;
	}
	
	public void dispose() {
		mesh.dispose();
		if (shader != null)
			shader.dispose();
	}
	
	public Matrix4 getProjectionMatrix() {
		return projectionMatrix;
	}
	
	public void setProjectionMatrix(Matrix4 projection) {
		if (drawing)
			throw new IllegalStateException("Can't set the matrix within begin/end.");
		projectionMatrix.set(projection);
	}
	
	public Matrix4 getTransformMatrix() {
		return transformMatrix;
	}
	
	public void setTransformMatrix(Matrix4 transform) {
		if (drawing)
			throw new IllegalStateException("Can't set the matrix within begin/end.");
		transformMatrix.set(transform);
	}
	
	private static class Cache {
		
		final int id;
		final int offset;
		int maxCount;
		int textureCount;
		Texture[] textures;
		int[] counts;
		
		public Cache(int id, int offset) {
			this.id = id;
			this.offset = offset;
		}
		
	}
	
	static Shader createDefaultShader() {
		String vertexShader = "attribute vec4 " + Shader.POSITION_ATTRIBUTE + ";\n" //
				+ "attribute vec4 " + Shader.COLOR_ATTRIBUTE + ";\n" //
				+ "attribute vec2 " + Shader.TEXCOORD_ATTRIBUTE + "0;\n" //
				+ "uniform mat4 u_projectionViewMatrix;\n" //
				+ "varying vec4 v_color;\n" //
				+ "varying vec2 v_texCoords;\n" //
				+ "\n" //
				+ "void main()\n" //
				+ "{\n" //
				+ "   v_color = " + Shader.COLOR_ATTRIBUTE + ";\n" //
				+ "   v_color.a = v_color.a * (255.0/254.0);\n" //
				+ "   v_texCoords = " + Shader.TEXCOORD_ATTRIBUTE + "0;\n" //
				+ "   gl_Position =  u_projectionViewMatrix * " + Shader.POSITION_ATTRIBUTE + ";\n" //
				+ "}\n";
		String fragmentShader = "#ifdef GL_ES\n" //
				+ "precision mediump float;\n" //
				+ "#endif\n" //
				+ "varying vec4 v_color;\n" //
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
	
	public void setShader(Shader shader) {
		customShader = shader;
	}
	
	public Shader getCustomShader() {
		return customShader;
	}
	
	public boolean isDrawing() {
		return drawing;
	}
	
}
