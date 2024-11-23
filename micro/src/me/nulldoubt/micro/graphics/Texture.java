package me.nulldoubt.micro.graphics;

import me.nulldoubt.micro.Application;
import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.assets.AssetManager;
import me.nulldoubt.micro.assets.loaders.TextureLoader.TextureParameter;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.graphics.Pixmap.Format;
import me.nulldoubt.micro.graphics.glutils.FileTextureData;
import me.nulldoubt.micro.graphics.glutils.PixmapTextureData;
import me.nulldoubt.micro.utils.collections.Array;

import java.util.HashMap;
import java.util.Map;

public class Texture extends GLTexture {
	
	private static AssetManager assetManager;
	final static Map<Application, Array<Texture>> managedTextures = new HashMap<>();
	
	public enum TextureFilter {
		
		Nearest(GL20.GL_NEAREST),
		Linear(GL20.GL_LINEAR),
		MipMap(GL20.GL_LINEAR_MIPMAP_LINEAR),
		MipMapNearestNearest(GL20.GL_NEAREST_MIPMAP_NEAREST),
		MipMapLinearNearest(GL20.GL_LINEAR_MIPMAP_NEAREST),
		MipMapNearestLinear(GL20.GL_NEAREST_MIPMAP_LINEAR),
		MipMapLinearLinear(GL20.GL_LINEAR_MIPMAP_LINEAR);
		
		final int glEnum;
		
		TextureFilter(int glEnum) {
			this.glEnum = glEnum;
		}
		
		public boolean isMipMap() {
			return glEnum != GL20.GL_NEAREST && glEnum != GL20.GL_LINEAR;
		}
		
		public int getGLEnum() {
			return glEnum;
		}
	}
	
	public enum TextureWrap {
		MirroredRepeat(GL20.GL_MIRRORED_REPEAT), ClampToEdge(GL20.GL_CLAMP_TO_EDGE), Repeat(GL20.GL_REPEAT);
		
		final int glEnum;
		
		TextureWrap(int glEnum) {
			this.glEnum = glEnum;
		}
		
		public int getGLEnum() {
			return glEnum;
		}
	}
	
	TextureData data;
	
	protected Texture() {
		super(0, 0);
	}
	
	public Texture(String internalPath) {
		this(Micro.files.internal(internalPath));
	}
	
	public Texture(FileHandle file) {
		this(file, null, false);
	}
	
	public Texture(FileHandle file, boolean mipmaps) {
		this(file, null, mipmaps);
	}
	
	public Texture(FileHandle file, Format format, boolean useMipMaps) {
		this(TextureData.Factory.loadFromFile(file, format, useMipMaps));
	}
	
	public Texture(Pixmap pixmap) {
		this(new PixmapTextureData(pixmap, null, false, false));
	}
	
	public Texture(Pixmap pixmap, boolean useMipMaps) {
		this(new PixmapTextureData(pixmap, null, useMipMaps, false));
	}
	
	public Texture(Pixmap pixmap, Format format, boolean useMipMaps) {
		this(new PixmapTextureData(pixmap, format, useMipMaps, false));
	}
	
	public Texture(int width, int height, Format format) {
		this(new PixmapTextureData(new Pixmap(width, height, format), null, false, true));
	}
	
	public Texture(TextureData data) {
		this(GL20.GL_TEXTURE_2D, Micro.gl.glGenTexture(), data);
	}
	
	protected Texture(int glTarget, int glHandle, TextureData data) {
		super(glTarget, glHandle);
		load(data);
		if (data.isManaged())
			addManagedTexture(Micro.app, this);
	}
	
	public void load(TextureData data) {
		if (this.data != null && data.isManaged() != this.data.isManaged())
			throw new MicroRuntimeException("New data must have the same managed status as the old data");
		this.data = data;
		
		if (!data.isPrepared())
			data.prepare();
		
		bind();
		uploadImageData(GL20.GL_TEXTURE_2D, data);
		
		unsafeSetFilter(minFilter, magFilter, true);
		unsafeSetWrap(uWrap, vWrap, true);
		unsafeSetAnisotropicFilter(anisotropicFilterLevel, true);
		Micro.gl.glBindTexture(glTarget, 0);
	}
	
	/**
	 * Used internally to reload after context loss. Creates a new GL handle then calls {@link #load(TextureData)}. Use this only
	 * if you know what you do!
	 */
	@Override
	protected void reload() {
		if (!isManaged())
			throw new MicroRuntimeException("Tried to reload unmanaged Texture");
		glHandle = Micro.gl.glGenTexture();
		load(data);
	}
	
	/**
	 * Draws the given {@link Pixmap} to the texture at position x, y. No clipping is performed so you have to make sure that you
	 * draw only inside the texture region. Note that this will only draw to mipmap level 0!
	 *
	 * @param pixmap The Pixmap
	 * @param x      The x coordinate in pixels
	 * @param y      The y coordinate in pixels
	 */
	public void draw(Pixmap pixmap, int x, int y) {
		if (data.isManaged())
			throw new MicroRuntimeException("can't draw to a managed texture");
		
		bind();
		Micro.gl.glTexSubImage2D(glTarget, 0, x, y, pixmap.getWidth(), pixmap.getHeight(), pixmap.getGLFormat(), pixmap.getGLType(),
				pixmap.getPixels());
	}
	
	@Override
	public int getWidth() {
		return data.getWidth();
	}
	
	@Override
	public int getHeight() {
		return data.getHeight();
	}
	
	@Override
	public int getDepth() {
		return 0;
	}
	
	public TextureData getTextureData() {
		return data;
	}
	
	/**
	 * @return whether this texture is managed or not.
	 */
	public boolean isManaged() {
		return data.isManaged();
	}
	
	/**
	 * Disposes all resources associated with the texture
	 */
	public void dispose() {
		// this is a hack. reason: we have to set the glHandle to 0 for textures that are
		// reloaded through the asset manager as we first remove (and thus dispose) the texture
		// and then reload it. the glHandle is set to 0 in invalidateAllTextures prior to
		// removal from the asset manager.
		if (glHandle == 0)
			return;
		delete();
		if (data.isManaged())
			if (managedTextures.get(Micro.app) != null)
				managedTextures.get(Micro.app).removeValue(this, true);
	}
	
	public String toString() {
		if (data instanceof FileTextureData)
			return data.toString();
		return super.toString();
	}
	
	private static void addManagedTexture(Application app, Texture texture) {
		Array<Texture> managedTextureArray = managedTextures.get(app);
		if (managedTextureArray == null)
			managedTextureArray = new Array<Texture>();
		managedTextureArray.add(texture);
		managedTextures.put(app, managedTextureArray);
	}
	
	/**
	 * Clears all managed textures. This is an internal method. Do not use it!
	 */
	public static void clearAllTextures(Application app) {
		managedTextures.remove(app);
	}
	
	/**
	 * Invalidate all managed textures. This is an internal method. Do not use it!
	 */
	public static void invalidateAllTextures(Application app) {
		Array<Texture> managedTextureArray = managedTextures.get(app);
		if (managedTextureArray == null)
			return;
		
		if (assetManager == null) {
			for (int i = 0; i < managedTextureArray.size; i++) {
				Texture texture = managedTextureArray.get(i);
				texture.reload();
			}
		} else {
			// first we have to make sure the AssetManager isn't loading anything anymore,
			// otherwise the ref counting trick below wouldn't work (when a texture is
			// currently on the task stack of the manager.)
			assetManager.finishLoading();
			
			// next we go through each texture and reload either directly or via the
			// asset manager.
			Array<Texture> textures = new Array<Texture>(managedTextureArray);
			for (Texture texture : textures) {
				String fileName = assetManager.getAssetFileName(texture);
				if (fileName == null) {
					texture.reload();
				} else {
					// get the ref count of the texture, then set it to 0 so we
					// can actually remove it from the assetmanager. Also set the
					// handle to zero, otherwise we might accidentially dispose
					// already reloaded textures.
					final int refCount = assetManager.getReferenceCount(fileName);
					assetManager.setReferenceCount(fileName, 0);
					texture.glHandle = 0;
					
					// create the parameters, passing the reference to the texture as
					// well as a callback that sets the ref count.
					TextureParameter params = new TextureParameter();
					params.textureData = texture.getTextureData();
					params.minFilter = texture.getMinFilter();
					params.magFilter = texture.getMagFilter();
					params.wrapU = texture.getUWrap();
					params.wrapV = texture.getVWrap();
					params.genMipMaps = texture.data.useMipMaps(); // not sure about this?
					params.texture = texture; // special parameter which will ensure that the references stay the same.
					params.loadedCallback = (assetManager, fileName1, _) -> assetManager.setReferenceCount(fileName1, refCount);
					
					assetManager.unload(fileName);
					texture.glHandle = Micro.gl.glGenTexture();
					assetManager.load(fileName, Texture.class, params);
				}
			}
			managedTextureArray.clear();
			managedTextureArray.addAll(textures);
		}
	}
	
	public static void setAssetManager(AssetManager manager) {
		Texture.assetManager = manager;
	}
	
	public static String getManagedStatus() {
		StringBuilder builder = new StringBuilder();
		builder.append("Managed textures/app: { ");
		for (Application app : managedTextures.keySet()) {
			builder.append(managedTextures.get(app).size);
			builder.append(" ");
		}
		builder.append("}");
		return builder.toString();
	}
	
	/**
	 * @return the number of managed textures currently loaded
	 */
	public static int getNumManagedTextures() {
		return managedTextures.get(Micro.app).size;
	}
	
}