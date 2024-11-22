package me.nulldoubt.micro.maps.tiled.loaders;

import me.nulldoubt.micro.assets.AssetDescriptor;
import me.nulldoubt.micro.assets.AssetManager;
import me.nulldoubt.micro.assets.loaders.FileHandleResolver;
import me.nulldoubt.micro.assets.loaders.TextureLoader;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.graphics.Texture;
import me.nulldoubt.micro.graphics.g2d.TextureAtlas;
import me.nulldoubt.micro.graphics.g2d.TextureAtlas.AtlasRegion;
import me.nulldoubt.micro.graphics.g2d.TextureRegion;
import me.nulldoubt.micro.maps.ImageResolver;
import me.nulldoubt.micro.maps.MapProperties;
import me.nulldoubt.micro.maps.tiled.TiledMap;
import me.nulldoubt.micro.maps.tiled.TiledMapTile;
import me.nulldoubt.micro.maps.tiled.TiledMapTileSet;
import me.nulldoubt.micro.utils.collections.Array;
import me.nulldoubt.micro.utils.xml.XmlReader.Element;

public class AtlasTmxMapLoader extends BaseTmxMapLoader<AtlasTmxMapLoader.AtlasTiledMapLoaderParameters> {
	
	public static class AtlasTiledMapLoaderParameters extends BaseTmxMapLoader.Parameters {
		
		public boolean forceTextureFilters = false;
		
	}
	
	protected interface AtlasResolver extends ImageResolver {
		
		TextureAtlas atlas();
		
		record DirectAtlasResolver(TextureAtlas atlas) implements AtlasResolver {
			
			@Override
			public TextureRegion getImage(final String name) {
				return atlas.findRegion(name);
			}
			
		}
		
		class AssetManagerAtlasResolver implements AtlasTmxMapLoader.AtlasResolver {
			
			private final AssetManager assetManager;
			private final String atlasName;
			
			public AssetManagerAtlasResolver(AssetManager assetManager, String atlasName) {
				this.assetManager = assetManager;
				this.atlasName = atlasName;
			}
			
			@Override
			public TextureAtlas atlas() {
				return assetManager.get(atlasName, TextureAtlas.class);
			}
			
			@Override
			public TextureRegion getImage(final String name) {
				return atlas().findRegion(name);
			}
			
		}
		
	}
	
	protected Array<Texture> trackedTextures = new Array<>();
	
	protected AtlasResolver atlasResolver;
	
	public AtlasTmxMapLoader() {
		super(AssetManager.RESOLVER_INTERNAL);
	}
	
	public AtlasTmxMapLoader(FileHandleResolver resolver) {
		super(resolver);
	}
	
	public TiledMap load(String fileName) {
		return load(fileName, new AtlasTiledMapLoaderParameters());
	}
	
	public TiledMap load(String fileName, AtlasTiledMapLoaderParameters parameter) {
		FileHandle tmxFile = resolve(fileName);
		
		this.root = xml.parse(tmxFile);
		
		final FileHandle atlasFileHandle = getAtlasFileHandle(tmxFile);
		TextureAtlas atlas = new TextureAtlas(atlasFileHandle);
		this.atlasResolver = new AtlasResolver.DirectAtlasResolver(atlas);
		
		TiledMap map = loadTiledMap(tmxFile, parameter, atlasResolver);
		map.setOwnedResources(new Array<TextureAtlas>(new TextureAtlas[] {atlas}));
		setTextureFilters(parameter.textureMinFilter, parameter.textureMagFilter);
		return map;
	}
	
	@Override
	public void loadAsync(AssetManager manager, String fileName, FileHandle tmxFile, AtlasTiledMapLoaderParameters parameter) {
		FileHandle atlasHandle = getAtlasFileHandle(tmxFile);
		this.atlasResolver = new AtlasResolver.AssetManagerAtlasResolver(manager, atlasHandle.path());
		
		this.map = loadTiledMap(tmxFile, parameter, atlasResolver);
	}
	
	@Override
	public TiledMap loadSync(AssetManager manager, String fileName, FileHandle file, AtlasTiledMapLoaderParameters parameter) {
		if (parameter != null) {
			setTextureFilters(parameter.textureMinFilter, parameter.textureMagFilter);
		}
		
		return map;
	}
	
	@Override
	protected Array<AssetDescriptor<?>> getDependencyAssetDescriptors(FileHandle tmxFile, TextureLoader.TextureParameter textureParameter) {
		final Array<AssetDescriptor<?>> descriptors = new Array<>();
		
		final FileHandle atlasFileHandle = getAtlasFileHandle(tmxFile);
		if (atlasFileHandle != null)
			descriptors.add(new AssetDescriptor<>(atlasFileHandle, TextureAtlas.class));
		
		return descriptors;
	}
	
	@Override
	protected void addStaticTiles(FileHandle tmxFile, ImageResolver imageResolver, TiledMapTileSet tileSet, Element element,
								  Array<Element> tileElements, String name, int firstgid, int tilewidth, int tileheight, int spacing, int margin,
								  String source, int offsetX, int offsetY, String imageSource, int imageWidth, int imageHeight, FileHandle image) {
		
		TextureAtlas atlas = atlasResolver.atlas();
		
		for (Texture texture : atlas.getTextures())
			trackedTextures.add(texture);
		
		MapProperties props = tileSet.properties;
		props.put("imagesource", imageSource);
		props.put("imagewidth", imageWidth);
		props.put("imageheight", imageHeight);
		props.put("tilewidth", tilewidth);
		props.put("tileheight", tileheight);
		props.put("margin", margin);
		props.put("spacing", spacing);
		
		if (imageSource != null && !imageSource.isEmpty()) {
			int lastgid = firstgid + ((imageWidth / tilewidth) * (imageHeight / tileheight)) - 1;
			for (AtlasRegion region : atlas.findRegions(name)) {
				// Handle unused tileIds
				if (region != null) {
					int tileId = firstgid + region.index;
					if (tileId >= firstgid && tileId <= lastgid) {
						addStaticTiledMapTile(tileSet, region, tileId, offsetX, offsetY);
					}
				}
			}
		}
		
		// Add tiles with individual image sources
		for (Element tileElement : tileElements) {
			int tileId = firstgid + tileElement.getIntAttribute("id", 0);
			TiledMapTile tile = tileSet.getTile(tileId);
			if (tile == null) {
				Element imageElement = tileElement.getChildByName("image");
				if (imageElement != null) {
					String regionName = imageElement.getAttribute("source");
					regionName = regionName.substring(0, regionName.lastIndexOf('.'));
					AtlasRegion region = atlas.findRegion(regionName);
					if (region == null)
						throw new MicroRuntimeException("Tileset atlasRegion not found: " + regionName);
					addStaticTiledMapTile(tileSet, region, tileId, offsetX, offsetY);
				}
			}
		}
	}
	
	protected FileHandle getAtlasFileHandle(FileHandle tmxFile) {
		Element properties = root.getChildByName("properties");
		
		String atlasFilePath = null;
		if (properties != null) {
			for (Element property : properties.getChildrenByName("property")) {
				String name = property.getAttribute("name");
				if (name.startsWith("atlas")) {
					atlasFilePath = property.getAttribute("value");
					break;
				}
			}
		}
		if (atlasFilePath == null) {
			throw new MicroRuntimeException("The map is missing the 'atlas' property");
		} else {
			final FileHandle fileHandle = getRelativeFileHandle(tmxFile, atlasFilePath);
			if (!fileHandle.exists()) {
				throw new MicroRuntimeException("The 'atlas' file could not be found: '" + atlasFilePath + "'");
			}
			return fileHandle;
		}
	}
	
	protected void setTextureFilters(Texture.TextureFilter min, Texture.TextureFilter mag) {
		for (Texture texture : trackedTextures)
			texture.setFilter(min, mag);
		trackedTextures.clear();
	}
	
}
