package me.nulldoubt.micro.maps.tiled.loaders;

import me.nulldoubt.micro.assets.AssetDescriptor;
import me.nulldoubt.micro.assets.loaders.FileHandleResolver;
import me.nulldoubt.micro.assets.loaders.TextureLoader;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.exceptions.SerializationException;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.graphics.g2d.TextureRegion;
import me.nulldoubt.micro.maps.*;
import me.nulldoubt.micro.maps.tiled.*;
import me.nulldoubt.micro.maps.tiled.TiledMapTileLayer.Cell;
import me.nulldoubt.micro.maps.tiled.objects.TiledMapTileMapObject;
import me.nulldoubt.micro.maps.tiled.tiles.AnimatedTiledMapTile;
import me.nulldoubt.micro.maps.tiled.tiles.StaticTiledMapTile;
import me.nulldoubt.micro.math.shapes.Polygon;
import me.nulldoubt.micro.math.shapes.Polyline;
import me.nulldoubt.micro.objects.EllipseMapObject;
import me.nulldoubt.micro.objects.PolygonMapObject;
import me.nulldoubt.micro.objects.PolylineMapObject;
import me.nulldoubt.micro.objects.RectangleMapObject;
import me.nulldoubt.micro.utils.Streams;
import me.nulldoubt.micro.utils.base64.Base64;
import me.nulldoubt.micro.utils.collections.Array;
import me.nulldoubt.micro.utils.collections.IntArray;
import me.nulldoubt.micro.utils.collections.IntMap;
import me.nulldoubt.micro.utils.json.JsonReader;
import me.nulldoubt.micro.utils.json.JsonValue;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public abstract class BaseTmjMapLoader<P extends BaseTiledMapLoader.Parameters> extends BaseTiledMapLoader<P> {
	
	protected JsonReader json = new JsonReader();
	protected JsonValue root;
	
	public BaseTmjMapLoader(final FileHandleResolver resolver) {
		super(resolver);
	}
	
	@Override
	public Array<AssetDescriptor<?>> getDependencies(String fileName, FileHandle tmjFile, P parameter) {
		this.root = json.parse(tmjFile);
		
		TextureLoader.TextureParameter textureParameter = new TextureLoader.TextureParameter();
		if (parameter != null) {
			textureParameter.genMipMaps = parameter.generateMipMaps;
			textureParameter.minFilter = parameter.textureMinFilter;
			textureParameter.magFilter = parameter.textureMagFilter;
		}
		
		return getDependencyAssetDescriptors(tmjFile, textureParameter);
	}
	
	protected TiledMap loadTiledMap(FileHandle tmjFile, P parameter, ImageResolver imageResolver) {
		this.map = new TiledMap();
		this.idToObject = new IntMap<>();
		this.runOnEndOfLoadTiled = new Array<>();
		
		if (parameter != null) {
			this.convertObjectToTileSpace = parameter.convertObjectToTileSpace;
			this.flipY = parameter.flipY;
		} else {
			this.convertObjectToTileSpace = false;
			this.flipY = true;
		}
		String mapOrientation = root.getString("orientation", null);
		int mapWidth = root.getInt("width", 0);
		int mapHeight = root.getInt("height", 0);
		int tileWidth = root.getInt("tilewidth", 0);
		int tileHeight = root.getInt("tileheight", 0);
		int hexSideLength = root.getInt("hexsidelength", 0);
		String staggerAxis = root.getString("staggeraxis", null);
		String staggerIndex = root.getString("staggerindex", null);
		String mapBackgroundColor = root.getString("backgroundcolor", null);
		
		MapProperties mapProperties = map.properties;
		if (mapOrientation != null)
			mapProperties.put("orientation", mapOrientation);
		
		mapProperties.put("width", mapWidth);
		mapProperties.put("height", mapHeight);
		mapProperties.put("tilewidth", tileWidth);
		mapProperties.put("tileheight", tileHeight);
		mapProperties.put("hexsidelength", hexSideLength);
		if (staggerAxis != null) {
			mapProperties.put("staggeraxis", staggerAxis);
		}
		if (staggerIndex != null) {
			mapProperties.put("staggerindex", staggerIndex);
		}
		if (mapBackgroundColor != null) {
			mapProperties.put("backgroundcolor", mapBackgroundColor);
		}
		this.mapTileWidth = tileWidth;
		this.mapTileHeight = tileHeight;
		this.mapWidthInPixels = mapWidth * tileWidth;
		this.mapHeightInPixels = mapHeight * tileHeight;
		
		if (mapOrientation != null) {
			if ("staggered".equals(mapOrientation)) {
				if (mapHeight > 1) {
					this.mapWidthInPixels += tileWidth / 2;
					this.mapHeightInPixels = mapHeightInPixels / 2 + tileHeight / 2;
				}
			}
		}
		
		JsonValue properties = root.get("properties");
		if (properties != null)
			loadProperties(map.properties, properties);
		
		JsonValue tileSets = root.get("tilesets");
		for (JsonValue element : tileSets)
			loadTileSet(element, tmjFile, imageResolver);
		
		JsonValue layers = root.get("layers");
		for (JsonValue element : layers)
			loadLayer(map, map.layers, element, tmjFile, imageResolver);
		
		final Array<MapGroupLayer> groups = map.layers.getByType(MapGroupLayer.class);
		while (groups.notEmpty()) {
			final MapGroupLayer group = groups.first();
			groups.removeIndex(0);
			
			for (MapLayer child : group.layers) {
				child.parallaxX = (child.parallaxX * group.parallaxX);
				child.parallaxY = (child.parallaxY * group.parallaxY);
				if (child instanceof MapGroupLayer mapGroupLayer)
					groups.add(mapGroupLayer);
			}
		}
		
		for (Runnable runnable : runOnEndOfLoadTiled) {
			runnable.run();
		}
		runOnEndOfLoadTiled = null;
		
		return map;
	}
	
	protected void loadLayer(TiledMap map, MapLayers parentLayers, JsonValue element, FileHandle tmjFile,
							 ImageResolver imageResolver) {
		String type = element.getString("type", "");
		switch (type) {
			case "group":
				loadLayerGroup(map, parentLayers, element, tmjFile, imageResolver);
				break;
			case "tilelayer":
				loadTileLayer(map, parentLayers, element);
				break;
			case "objectgroup":
				loadObjectGroup(map, parentLayers, element);
				break;
			case "imagelayer":
				loadImageLayer(map, parentLayers, element, tmjFile, imageResolver);
				break;
		}
	}
	
	protected void loadLayerGroup(TiledMap map, MapLayers parentLayers, JsonValue element, FileHandle tmjFile,
								  ImageResolver imageResolver) {
		if (element.getString("type", "").equals("group")) {
			MapGroupLayer groupLayer = new MapGroupLayer();
			loadBasicLayerInfo(groupLayer, element);
			
			JsonValue properties = element.get("properties");
			if (properties != null) {
				loadProperties(groupLayer.properties, properties);
			}
			
			JsonValue layers = element.get("layers");
			if (layers != null) {
				for (JsonValue child : layers) {
					loadLayer(map, groupLayer.layers, child, tmjFile, imageResolver);
				}
			}
			
			for (MapLayer layer : groupLayer.layers) {
				layer.setParent(groupLayer);
			}
			
			parentLayers.add(groupLayer);
		}
	}
	
	protected void loadTileLayer(TiledMap map, MapLayers parentLayers, JsonValue element) {
		
		if (element.getString("type", "").equals("tilelayer")) {
			int width = element.getInt("width", 0);
			int height = element.getInt("height", 0);
			int tileWidth = map.properties.get("tilewidth", Integer.class);
			int tileHeight = map.properties.get("tileheight", Integer.class);
			TiledMapTileLayer layer = new TiledMapTileLayer(width, height, tileWidth, tileHeight);
			
			loadBasicLayerInfo(layer, element);
			
			int[] ids = getTileIds(element, width, height);
			TiledMapTileSets tileSets = map.getTileSets();
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int id = ids[y * width + x];
					boolean flipHorizontally = ((id & FLAG_FLIP_HORIZONTALLY) != 0);
					boolean flipVertically = ((id & FLAG_FLIP_VERTICALLY) != 0);
					boolean flipDiagonally = ((id & FLAG_FLIP_DIAGONALLY) != 0);
					
					TiledMapTile tile = tileSets.getTile(id & ~MASK_CLEAR);
					if (tile != null) {
						Cell cell = createTileLayerCell(flipHorizontally, flipVertically, flipDiagonally);
						cell.tile = tile;
						layer.setCell(x, flipY ? height - 1 - y : y, cell);
					}
				}
			}
			JsonValue properties = element.get("properties");
			if (properties != null) {
				loadProperties(layer.properties, properties);
			}
			parentLayers.add(layer);
		}
	}
	
	protected void loadObjectGroup(TiledMap map, MapLayers parentLayers, JsonValue element) {
		if (element.getString("type", "").equals("objectgroup")) {
			MapLayer layer = new MapLayer();
			loadBasicLayerInfo(layer, element);
			JsonValue properties = element.get("properties");
			if (properties != null) {
				loadProperties(layer.properties, properties);
			}
			
			for (JsonValue objectElement : element.get("objects")) {
				loadObject(map, layer, objectElement);
			}
			
			parentLayers.add(layer);
		}
	}
	
	protected void loadImageLayer(TiledMap map, MapLayers parentLayers, JsonValue element, FileHandle tmjFile,
								  ImageResolver imageResolver) {
		if (element.getString("type", "").equals("imagelayer")) {
			float x = element.getFloat("offsetx", 0);
			float y = element.getFloat("offsety", 0);
			if (flipY)
				y = mapHeightInPixels - y;
			
			String imageSrc = element.getString("image", "");
			
			boolean repeatX = element.getInt("repeatx", 0) == 1;
			boolean repeatY = element.getInt("repeaty", 0) == 1;
			
			TextureRegion texture = null;
			
			if (!imageSrc.isEmpty()) {
				FileHandle handle = getRelativeFileHandle(tmjFile, imageSrc);
				texture = imageResolver.getImage(handle.path());
				y -= texture.getRegionHeight();
			}
			
			TiledMapImageLayer layer = new TiledMapImageLayer(texture, x, y, repeatX, repeatY);
			loadBasicLayerInfo(layer, element);
			
			JsonValue properties = element.get("properties");
			if (properties != null)
				loadProperties(layer.properties, properties);
			
			parentLayers.add(layer);
		}
	}
	
	protected void loadBasicLayerInfo(MapLayer layer, JsonValue element) {
		String name = element.getString("name");
		float opacity = element.getFloat("opacity", 1.0f);
		boolean visible = element.getBoolean("visible", true);
		float offsetX = element.getFloat("offsetx", 0);
		float offsetY = element.getFloat("offsety", 0);
		float parallaxX = element.getFloat("parallaxx", 1f);
		float parallaxY = element.getFloat("parallaxy", 1f);
		
		layer.name = name;
		layer.opacity = opacity;
		layer.visible = visible;
		layer.offsetX = offsetX;
		layer.offsetY = offsetY;
		layer.parallaxX = parallaxX;
		layer.parallaxY = parallaxY;
	}
	
	protected void loadObject(TiledMap map, MapLayer layer, JsonValue element) {
		loadObject(map, layer.objects, element, mapHeightInPixels);
	}
	
	protected void loadObject(TiledMap map, TiledMapTile tile, JsonValue element) {
		loadObject(map, tile.getObjects(), element, tile.getTextureRegion().getRegionHeight());
	}
	
	protected void loadObject(TiledMap map, MapObjects objects, JsonValue element, float heightInPixels) {
		
		MapObject object = null;
		
		float scaleX = convertObjectToTileSpace ? 1.0f / mapTileWidth : 1.0f;
		float scaleY = convertObjectToTileSpace ? 1.0f / mapTileHeight : 1.0f;
		
		float x = element.getFloat("x", 0) * scaleX;
		float y = (flipY ? (heightInPixels - element.getFloat("y", 0)) : element.getFloat("y", 0)) * scaleY;
		
		float width = element.getFloat("width", 0) * scaleX;
		float height = element.getFloat("height", 0) * scaleY;
		
		JsonValue child;
		if ((child = element.get("polygon")) != null) {
			float[] vertices = new float[child.size * 2];
			int index = 0;
			for (JsonValue point : child) {
				vertices[index++] = point.getFloat("x", 0) * scaleX; // Scaled X
				vertices[index++] = point.getFloat("y", 0) * scaleY * (flipY ? -1 : 1); // Scaled/flipped Y
			}
			Polygon polygon = new Polygon(vertices);
			polygon.setPosition(x, y);
			object = new PolygonMapObject(polygon);
		} else if ((child = element.get("polyline")) != null) {
			float[] vertices = new float[child.size * 2];
			int index = 0;
			for (JsonValue point : child) {
				// Apply scale and flip transformations
				vertices[index++] = point.getFloat("x", 0) * scaleX; // Scaled X
				vertices[index++] = point.getFloat("y", 0) * scaleY * (flipY ? -1 : 1); // Scaled/flipped Y
			}
			Polyline polyline = new Polyline(vertices);
			polyline.setPosition(x, y);
			object = new PolylineMapObject(polyline);
		} else if (element.get("ellipse") != null)
			object = new EllipseMapObject(x, flipY ? y - height : y, width, height);
		
		if (object == null) {
			String gid;
			if ((gid = element.getString("gid", null)) != null) {
				int id = (int) Long.parseLong(gid);
				boolean flipHorizontally = ((id & FLAG_FLIP_HORIZONTALLY) != 0);
				boolean flipVertically = ((id & FLAG_FLIP_VERTICALLY) != 0);
				
				TiledMapTile tile = map.getTileSets().getTile(id & ~MASK_CLEAR);
				TiledMapTileMapObject tiledMapTileMapObject = new TiledMapTileMapObject(tile, flipHorizontally, flipVertically);
				TextureRegion textureRegion = tiledMapTileMapObject.textureRegion;
				tiledMapTileMapObject.properties.put("gid", id);
				tiledMapTileMapObject.x = x;
				tiledMapTileMapObject.y = (flipY ? y : y - height);
				float objectWidth = element.getFloat("width", textureRegion.getRegionWidth());
				float objectHeight = element.getFloat("height", textureRegion.getRegionHeight());
				tiledMapTileMapObject.scaleX = (scaleX * (objectWidth / textureRegion.getRegionWidth()));
				tiledMapTileMapObject.scaleY = (scaleY * (objectHeight / textureRegion.getRegionHeight()));
				tiledMapTileMapObject.rotation = (element.getFloat("rotation", 0));
				object = tiledMapTileMapObject;
			} else {
				object = new RectangleMapObject(x, flipY ? y - height : y, width, height);
			}
		}
		object.name = (element.getString("name", null));
		String rotation = element.getString("rotation", null);
		if (rotation != null)
			object.properties.put("rotation", Float.parseFloat(rotation));
		
		String type = element.getString("type", null);
		if (type != null)
			object.properties.put("type", type);
		
		int id = element.getInt("id", 0);
		if (id != 0)
			object.properties.put("id", id);
		
		object.properties.put("x", x);
		
		if (object instanceof TiledMapTileMapObject) {
			object.properties.put("y", y);
		} else {
			object.properties.put("y", (flipY ? y - height : y));
		}
		object.properties.put("width", width);
		object.properties.put("height", height);
		object.visible = (element.getBoolean("visible", true));
		JsonValue properties = element.get("properties");
		if (properties != null)
			loadProperties(object.properties, properties);
		
		idToObject.put(id, object);
		objects.add(object);
		
	}
	
	private void loadProperties(final MapProperties properties, JsonValue element) {
		if (element == null)
			return;
		
		if (element.name() != null && element.name().equals("properties")) {
			for (JsonValue property : element) {
				final String name = property.getString("name", null);
				String value = property.getString("value", null);
				String type = property.getString("type", null);
				if (value == null)
					value = property.asString();
				if (type != null && type.equals("object")) {
					try {
						final int id = Integer.parseInt(value);
						runOnEndOfLoadTiled.add(() -> properties.put(name, idToObject.get(id)));
					} catch (Exception exception) {
						throw new MicroRuntimeException("Error parsing property [\" + name + \"] of type \"object\" with value: [" + value + "]", exception);
					}
				} else {
					Object castValue = castProperty(name, value, type);
					properties.put(name, castValue);
				}
			}
		}
	}
	
	static public int[] getTileIds(JsonValue element, int width, int height) {
		JsonValue data = element.get("data");
		String encoding = element.getString("encoding", null);
		
		int[] ids;
		if (encoding == null || encoding.isEmpty() || encoding.equals("csv")) {
			ids = data.asIntArray();
		} else if (encoding.equals("base64")) {
			InputStream is = null;
			try {
				String compression = element.getString("compression", null);
				byte[] bytes = Base64.decode(data.asString());
				if (compression == null || compression.isEmpty())
					is = new ByteArrayInputStream(bytes);
				else if (compression.equals("gzip"))
					is = new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes), bytes.length));
				else if (compression.equals("zlib"))
					is = new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(bytes)));
				else
					throw new MicroRuntimeException("Unrecognised compression (" + compression + ") for TMJ Layer Data");
				
				byte[] temp = new byte[4];
				ids = new int[width * height];
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						int read = is.read(temp);
						while (read < temp.length) {
							int curr = is.read(temp, read, temp.length - read);
							if (curr == -1)
								break;
							read += curr;
						}
						if (read != temp.length)
							throw new MicroRuntimeException("Error Reading TMJ Layer Data: Premature end of tile data");
						ids[y * width + x] = unsignedByteToInt(temp[0]) | unsignedByteToInt(temp[1]) << 8
								| unsignedByteToInt(temp[2]) << 16 | unsignedByteToInt(temp[3]) << 24;
					}
				}
			} catch (IOException e) {
				throw new MicroRuntimeException("Error Reading TMJ Layer Data - IOException: " + e.getMessage());
			} finally {
				Streams.closeQuietly(is);
			}
		} else {
			throw new MicroRuntimeException("Unrecognised encoding (" + encoding + ") for TMJ Layer Data");
		}
		
		return ids;
	}
	
	protected void loadTileSet(JsonValue element, FileHandle tmjFile, ImageResolver imageResolver) {
		if (element.getString("firstgid") != null) {
			int firstgid = element.getInt("firstgid", 1);
			String imageSource = "";
			int imageWidth = 0;
			int imageHeight = 0;
			FileHandle image = null;
			
			String source = element.getString("source", null);
			if (source != null) {
				FileHandle tsj = getRelativeFileHandle(tmjFile, source);
				try {
					element = json.parse(tsj);
					if (element.has("image")) {
						imageSource = element.getString("image");
						imageWidth = element.getInt("imagewidth", 0);
						imageHeight = element.getInt("imageheight", 0);
						image = getRelativeFileHandle(tsj, imageSource);
					}
				} catch (SerializationException e) {
					throw new MicroRuntimeException("Error parsing external tileSet.");
				}
			} else {
				if (element.has("image")) {
					imageSource = element.getString("image");
					imageWidth = element.getInt("imagewidth", 0);
					imageHeight = element.getInt("imageheight", 0);
					image = getRelativeFileHandle(tmjFile, imageSource);
				}
			}
			String name = element.getString("name", null);
			int tilewidth = element.getInt("tilewidth", 0);
			int tileheight = element.getInt("tileheight", 0);
			int spacing = element.getInt("spacing", 0);
			int margin = element.getInt("margin", 0);
			
			JsonValue offset = element.get("tileoffset");
			int offsetX = 0;
			int offsetY = 0;
			if (offset != null) {
				offsetX = offset.getInt("x", 0);
				offsetY = offset.getInt("y", 0);
			}
			TiledMapTileSet tileSet = new TiledMapTileSet();
			
			// TileSet
			tileSet.name = name;
			final MapProperties tileSetProperties = tileSet.properties;
			JsonValue properties = element.get("properties");
			if (properties != null) {
				loadProperties(tileSetProperties, properties);
			}
			tileSetProperties.put("firstgid", firstgid);
			
			// Tiles
			JsonValue tiles = element.get("tiles");
			
			if (tiles == null) {
				tiles = new JsonValue(JsonValue.ValueType.array);
			}
			
			addStaticTiles(tmjFile, imageResolver, tileSet, element, tiles, name, firstgid, tilewidth, tileheight, spacing, margin,
					source, offsetX, offsetY, imageSource, imageWidth, imageHeight, image);
			
			Array<AnimatedTiledMapTile> animatedTiles = new Array<>();
			
			for (JsonValue tileElement : tiles) {
				int localtid = tileElement.getInt("id", 0);
				TiledMapTile tile = tileSet.getTile(firstgid + localtid);
				if (tile != null) {
					AnimatedTiledMapTile animatedTile = createAnimatedTile(tileSet, tile, tileElement, firstgid);
					if (animatedTile != null) {
						animatedTiles.add(animatedTile);
						tile = animatedTile;
					}
					addTileProperties(tile, tileElement);
					addTileObjectGroup(tile, tileElement);
				}
			}
			// replace original static tiles by animated tiles
			for (AnimatedTiledMapTile animatedTile : animatedTiles) {
				tileSet.putTile(animatedTile.getId(), animatedTile);
			}
			
			map.getTileSets().addTileSet(tileSet);
			
		}
	}
	
	protected abstract void addStaticTiles(FileHandle tmjFile, ImageResolver imageResolver, TiledMapTileSet tileSet,
										   JsonValue element, JsonValue tiles, String name, int firstgid, int tilewidth, int tileheight, int spacing, int margin,
										   String source, int offsetX, int offsetY, String imageSource, int imageWidth, int imageHeight, FileHandle image);
	
	private void addTileProperties(TiledMapTile tile, JsonValue tileElement) {
		String terrain = tileElement.getString("terrain", null);
		if (terrain != null)
			tile.getProperties().put("terrain", terrain);
		
		String probability = tileElement.getString("probability", null);
		if (probability != null)
			tile.getProperties().put("probability", probability);
		
		String type = tileElement.getString("type", null);
		if (type != null)
			tile.getProperties().put("type", type);
		
		JsonValue properties = tileElement.get("properties");
		if (properties != null)
			loadProperties(tile.getProperties(), properties);
	}
	
	private void addTileObjectGroup(TiledMapTile tile, JsonValue tileElement) {
		JsonValue objectGroupElement = tileElement.get("objectgroup");
		if (objectGroupElement != null)
			for (JsonValue objectElement : objectGroupElement.get("objects"))
				loadObject(this.map, tile, objectElement);
	}
	
	private AnimatedTiledMapTile createAnimatedTile(TiledMapTileSet tileSet, TiledMapTile tile, JsonValue tileElement, int firstgid) {
		JsonValue animationElement = tileElement.get("animation");
		if (animationElement != null) {
			Array<StaticTiledMapTile> staticTiles = new Array<>();
			IntArray intervals = new IntArray();
			for (JsonValue frameValue : animationElement) {
				staticTiles.add((StaticTiledMapTile) tileSet.getTile(firstgid + frameValue.getInt("tileid")));
				intervals.add(frameValue.getInt("duration"));
			}
			
			AnimatedTiledMapTile animatedTile = new AnimatedTiledMapTile(intervals, staticTiles);
			animatedTile.setId(tile.getId());
			return animatedTile;
		}
		return null;
	}
	
}