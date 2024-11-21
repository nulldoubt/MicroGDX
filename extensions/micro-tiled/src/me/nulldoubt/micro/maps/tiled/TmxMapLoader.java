package me.nulldoubt.micro.maps.tiled;

import me.nulldoubt.micro.assets.AssetDescriptor;
import me.nulldoubt.micro.assets.AssetManager;
import me.nulldoubt.micro.assets.loaders.FileHandleResolver;
import me.nulldoubt.micro.assets.loaders.TextureLoader;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.graphics.Texture;
import me.nulldoubt.micro.graphics.g2d.TextureRegion;
import me.nulldoubt.micro.maps.ImageResolver;
import me.nulldoubt.micro.maps.ImageResolver.AssetManagerImageResolver;
import me.nulldoubt.micro.maps.ImageResolver.DirectImageResolver;
import me.nulldoubt.micro.maps.MapProperties;
import me.nulldoubt.micro.utils.XmlReader.Element;
import me.nulldoubt.micro.utils.collections.Array;
import me.nulldoubt.micro.utils.collections.ObjectMap;

public class TmxMapLoader extends BaseTmxMapLoader<TmxMapLoader.Parameters> {
	
	public static class Parameters extends BaseTmxMapLoader.Parameters {}
	
	public TmxMapLoader() {
		super(AssetManager.RESOLVER_INTERNAL);
	}
	
	public TmxMapLoader(final FileHandleResolver resolver) {
		super(resolver);
	}
	
	public TiledMap load(String fileName) {
		return load(fileName, new TmxMapLoader.Parameters());
	}
	
	public TiledMap load(final String fileName, final TmxMapLoader.Parameters parameter) {
		final FileHandle tmxFile = resolve(fileName);
		root = xml.parse(tmxFile);
		
		ObjectMap<String, Texture> textures = new ObjectMap<>();
		
		final Array<FileHandle> textureFiles = getDependencyFileHandles(tmxFile);
		for (FileHandle textureFile : textureFiles) {
			Texture texture = new Texture(textureFile, parameter.generateMipMaps);
			texture.setFilter(parameter.textureMinFilter, parameter.textureMagFilter);
			textures.put(textureFile.path(), texture);
		}
		
		TiledMap map = loadTiledMap(tmxFile, parameter, new DirectImageResolver(textures));
		map.setOwnedResources(textures.values().toArray());
		return map;
	}
	
	@Override
	public void loadAsync(AssetManager manager, String fileName, FileHandle tmxFile, Parameters parameter) {
		this.map = loadTiledMap(tmxFile, parameter, new AssetManagerImageResolver(manager));
	}
	
	@Override
	public TiledMap loadSync(AssetManager manager, String fileName, FileHandle file, Parameters parameter) {
		return map;
	}
	
	@Override
	protected Array<AssetDescriptor<?>> getDependencyAssetDescriptors(FileHandle tmxFile, TextureLoader.TextureParameter textureParameter) {
		Array<AssetDescriptor<?>> descriptors = new Array<>();
		
		final Array<FileHandle> fileHandles = getDependencyFileHandles(tmxFile);
		for (FileHandle handle : fileHandles)
			descriptors.add(new AssetDescriptor<>(handle, Texture.class, textureParameter));
		
		return descriptors;
	}
	
	protected Array<FileHandle> getDependencyFileHandles(FileHandle tmxFile) {
		Array<FileHandle> fileHandles = new Array<>();
		
		// TileSet descriptors
		for (Element tileset : root.getChildrenByNameRecursively("tileset")) {
			String source = tileset.getAttribute("source", null);
			if (source != null) {
				FileHandle tsxFile = getRelativeFileHandle(tmxFile, source);
				tileset = xml.parse(tsxFile);
				Element imageElement = tileset.getChildByName("image");
				if (imageElement != null) {
					String imageSource = tileset.getChildByName("image").getAttribute("source");
					FileHandle image = getRelativeFileHandle(tsxFile, imageSource);
					fileHandles.add(image);
				} else {
					for (Element tile : tileset.getChildrenByName("tile")) {
						String imageSource = tile.getChildByName("image").getAttribute("source");
						FileHandle image = getRelativeFileHandle(tsxFile, imageSource);
						fileHandles.add(image);
					}
				}
			} else {
				Element imageElement = tileset.getChildByName("image");
				if (imageElement != null) {
					String imageSource = tileset.getChildByName("image").getAttribute("source");
					FileHandle image = getRelativeFileHandle(tmxFile, imageSource);
					fileHandles.add(image);
				} else {
					for (Element tile : tileset.getChildrenByName("tile")) {
						String imageSource = tile.getChildByName("image").getAttribute("source");
						FileHandle image = getRelativeFileHandle(tmxFile, imageSource);
						fileHandles.add(image);
					}
				}
			}
		}
		
		// ImageLayer descriptors
		for (Element imageLayer : root.getChildrenByNameRecursively("imagelayer")) {
			Element image = imageLayer.getChildByName("image");
			String source = image.getAttribute("source", null);
			
			if (source != null) {
				FileHandle handle = getRelativeFileHandle(tmxFile, source);
				fileHandles.add(handle);
			}
		}
		
		return fileHandles;
	}
	
	@Override
	protected void addStaticTiles(FileHandle tmxFile, ImageResolver imageResolver, TiledMapTileSet tileSet, Element element,
								  Array<Element> tileElements, String name, int firstgid, int tilewidth, int tileheight, int spacing, int margin,
								  String source, int offsetX, int offsetY, String imageSource, int imageWidth, int imageHeight, FileHandle image) {
		
		MapProperties props = tileSet.properties;
		if (image != null) {
			// One image for the whole tileSet
			TextureRegion texture = imageResolver.getImage(image.path());
			
			props.put("imagesource", imageSource);
			props.put("imagewidth", imageWidth);
			props.put("imageheight", imageHeight);
			props.put("tilewidth", tilewidth);
			props.put("tileheight", tileheight);
			props.put("margin", margin);
			props.put("spacing", spacing);
			
			int stopWidth = texture.getRegionWidth() - tilewidth;
			int stopHeight = texture.getRegionHeight() - tileheight;
			
			int id = firstgid;
			
			for (int y = margin; y <= stopHeight; y += tileheight + spacing) {
				for (int x = margin; x <= stopWidth; x += tilewidth + spacing) {
					TextureRegion tileRegion = new TextureRegion(texture, x, y, tilewidth, tileheight);
					int tileId = id++;
					addStaticTiledMapTile(tileSet, tileRegion, tileId, offsetX, offsetY);
				}
			}
		} else {
			// Every tile has its own image source
			for (Element tileElement : tileElements) {
				Element imageElement = tileElement.getChildByName("image");
				if (imageElement != null) {
					imageSource = imageElement.getAttribute("source");
					
					if (source != null) {
						image = getRelativeFileHandle(getRelativeFileHandle(tmxFile, source), imageSource);
					} else {
						image = getRelativeFileHandle(tmxFile, imageSource);
					}
				}
				TextureRegion texture = imageResolver.getImage(image.path());
				int tileId = firstgid + tileElement.getIntAttribute("id");
				addStaticTiledMapTile(tileSet, texture, tileId, offsetX, offsetY);
			}
		}
	}
	
}
