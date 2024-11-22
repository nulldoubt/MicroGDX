package me.nulldoubt.micro.maps.tiled.loaders;

import me.nulldoubt.micro.assets.AssetDescriptor;
import me.nulldoubt.micro.assets.AssetManager;
import me.nulldoubt.micro.assets.loaders.FileHandleResolver;
import me.nulldoubt.micro.assets.loaders.TextureLoader;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.graphics.Texture;
import me.nulldoubt.micro.graphics.g2d.TextureRegion;
import me.nulldoubt.micro.maps.ImageResolver;
import me.nulldoubt.micro.maps.MapProperties;
import me.nulldoubt.micro.maps.tiled.TiledMap;
import me.nulldoubt.micro.maps.tiled.TiledMapTileSet;
import me.nulldoubt.micro.utils.collections.Array;
import me.nulldoubt.micro.utils.collections.ObjectMap;
import me.nulldoubt.micro.utils.json.JsonValue;

public class TmjMapLoader extends BaseTmjMapLoader<BaseTmjMapLoader.Parameters> {
	
	public static class Parameters extends BaseTmjMapLoader.Parameters {}
	
	public TmjMapLoader() {
		super(AssetManager.RESOLVER_INTERNAL);
	}
	
	public TmjMapLoader(final FileHandleResolver resolver) {
		super(resolver);
	}
	
	public TiledMap load(String fileName) {
		return load(fileName, new TmjMapLoader.Parameters());
	}
	
	public TiledMap load(String fileName, TmjMapLoader.Parameters parameter) {
		final FileHandle tmjFile = resolve(fileName);
		
		this.root = json.parse(tmjFile);
		
		ObjectMap<String, Texture> textures = new ObjectMap<>();
		
		final Array<FileHandle> textureFiles = getDependencyFileHandles(tmjFile);
		for (FileHandle textureFile : textureFiles) {
			Texture texture = new Texture(textureFile, parameter.generateMipMaps);
			texture.setFilter(parameter.textureMinFilter, parameter.textureMagFilter);
			textures.put(textureFile.path(), texture);
		}
		
		TiledMap map = loadTiledMap(tmjFile, parameter, new ImageResolver.DirectImageResolver(textures));
		map.setOwnedResources(textures.values().toArray());
		return map;
	}
	
	@Override
	public void loadAsync(AssetManager manager, String fileName, FileHandle tmjFile, BaseTmjMapLoader.Parameters parameter) {
		this.map = loadTiledMap(tmjFile, parameter, new ImageResolver.AssetManagerImageResolver(manager));
	}
	
	@Override
	public TiledMap loadSync(AssetManager manager, String fileName, FileHandle file, BaseTmjMapLoader.Parameters parameter) {
		return map;
	}
	
	@Override
	protected Array<AssetDescriptor<?>> getDependencyAssetDescriptors(FileHandle tmjFile, TextureLoader.TextureParameter textureParameter) {
		final Array<AssetDescriptor<?>> descriptors = new Array<>();
		final Array<FileHandle> fileHandles = getDependencyFileHandles(tmjFile);
		for (FileHandle handle : fileHandles)
			descriptors.add(new AssetDescriptor<>(handle, Texture.class, textureParameter));
		return descriptors;
	}
	
	protected Array<FileHandle> getDependencyFileHandles(FileHandle tmjFile) {
		
		final Array<FileHandle> fileHandles = new Array<>();
		for (JsonValue tileSet : root.get("tileSets"))
			getTileSetDependencyFileHandle(fileHandles, tmjFile, tileSet);
		
		collectImageLayerFileHandles(root.get("layers"), tmjFile, fileHandles);
		
		return fileHandles;
	}
	
	private void collectImageLayerFileHandles(JsonValue layers, FileHandle tmjFile, Array<FileHandle> fileHandles) {
		if (layers == null)
			return;
		
		for (JsonValue layer : layers) {
			String type = layer.getString("type", "");
			if (type.equals("imagelayer")) {
				String source = layer.getString("image", null);
				
				if (source != null) {
					FileHandle handle = getRelativeFileHandle(tmjFile, source);
					fileHandles.add(handle);
				}
			} else if (type.equals("group")) {
				// Recursively process group layers
				collectImageLayerFileHandles(layer.get("layers"), tmjFile, fileHandles);
			}
			
		}
	}
	
	protected Array<FileHandle> getTileSetDependencyFileHandle(FileHandle tmjFile, JsonValue tileSet) {
		Array<FileHandle> fileHandles = new Array<>();
		return getTileSetDependencyFileHandle(fileHandles, tmjFile, tileSet);
	}
	
	protected Array<FileHandle> getTileSetDependencyFileHandle(Array<FileHandle> fileHandles, FileHandle tmjFile,
															   JsonValue tileSet) {
		String source = tileSet.getString("source", null);
		if (source != null) {
			FileHandle tsxFile = getRelativeFileHandle(tmjFile, source);
			tileSet = json.parse(tsxFile);
			if (tileSet.has("image")) {
				String imageSource = tileSet.getString("image");
				FileHandle image = getRelativeFileHandle(tsxFile, imageSource);
				fileHandles.add(image);
			} else {
				for (JsonValue tile : tileSet.get("tile")) {
					String imageSource = tile.getString("image");
					FileHandle image = getRelativeFileHandle(tsxFile, imageSource);
					fileHandles.add(image);
				}
			}
		} else {
			if (tileSet.has("image")) {
				String imageSource = tileSet.getString("image");
				FileHandle image = getRelativeFileHandle(tmjFile, imageSource);
				fileHandles.add(image);
			} else {
				JsonValue tiles = tileSet.get("tiles");
				if (tiles != null) {
					for (JsonValue tile : tiles) {
						String imageSource = tile.getString("image");
						FileHandle image = getRelativeFileHandle(tmjFile, imageSource);
						fileHandles.add(image);
					}
				}
			}
		}
		return fileHandles;
	}
	
	@Override
	protected void addStaticTiles(FileHandle tmjFile, ImageResolver imageResolver, TiledMapTileSet tileSet, JsonValue element,
								  JsonValue tiles, String name, int firstgid, int tilewidth, int tileheight, int spacing, int margin, String source,
								  int offsetX, int offsetY, String imageSource, int imageWidth, int imageHeight, FileHandle image) {
		
		MapProperties props = tileSet.properties;
		if (image != null) {
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
			for (JsonValue tile : tiles) {
				if (tile.has("image")) {
					imageSource = tile.getString("image");
					if (source != null)
						image = getRelativeFileHandle(getRelativeFileHandle(tmjFile, source), imageSource);
					else
						image = getRelativeFileHandle(tmjFile, imageSource);
				}
				TextureRegion texture = imageResolver.getImage(image.path());
				int tileId = firstgid + tile.getInt("id");
				addStaticTiledMapTile(tileSet, texture, tileId, offsetX, offsetY);
			}
		}
	}
	
}