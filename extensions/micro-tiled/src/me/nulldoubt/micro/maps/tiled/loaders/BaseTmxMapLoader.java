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
import me.nulldoubt.micro.utils.xml.XmlReader;
import me.nulldoubt.micro.utils.xml.XmlReader.Element;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public abstract class BaseTmxMapLoader<P extends BaseTiledMapLoader.Parameters> extends BaseTiledMapLoader<P> {
	
	protected XmlReader xml = new XmlReader();
	protected Element root;
	
	public BaseTmxMapLoader(FileHandleResolver resolver) {
		super(resolver);
	}
	
	@Override
	public Array<AssetDescriptor<?>> getDependencies(String fileName, FileHandle tmxFile, P parameter) {
		this.root = xml.parse(tmxFile);
		
		TextureLoader.TextureParameter textureParameter = new TextureLoader.TextureParameter();
		if (parameter != null) {
			textureParameter.genMipMaps = parameter.generateMipMaps;
			textureParameter.minFilter = parameter.textureMinFilter;
			textureParameter.magFilter = parameter.textureMagFilter;
		}
		
		return getDependencyAssetDescriptors(tmxFile, textureParameter);
	}
	
	@Override
	protected TiledMap loadTiledMap(FileHandle tmxFile, P parameter, ImageResolver imageResolver) {
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
		
		String mapOrientation = root.getAttribute("orientation", null);
		int mapWidth = root.getIntAttribute("width", 0);
		int mapHeight = root.getIntAttribute("height", 0);
		int tileWidth = root.getIntAttribute("tilewidth", 0);
		int tileHeight = root.getIntAttribute("tileheight", 0);
		int hexSideLength = root.getIntAttribute("hexsidelength", 0);
		String staggerAxis = root.getAttribute("staggeraxis", null);
		String staggerIndex = root.getAttribute("staggerindex", null);
		String mapBackgroundColor = root.getAttribute("backgroundcolor", null);
		
		MapProperties mapProperties = map.properties;
		if (mapOrientation != null) {
			mapProperties.put("orientation", mapOrientation);
		}
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
		
		Element properties = root.getChildByName("properties");
		if (properties != null)
			loadProperties(map.properties, properties);
		
		Array<Element> tileSets = root.getChildrenByName("tileset");
		for (Element element : tileSets) {
			loadTileSet(element, tmxFile, imageResolver);
			root.removeChild(element);
		}
		
		for (int i = 0, j = root.getChildCount(); i < j; i++) {
			Element element = root.getChild(i);
			loadLayer(map, map.layers, element, tmxFile, imageResolver);
		}
		
		final Array<MapGroupLayer> groups = map.layers.getByType(MapGroupLayer.class);
		while (groups.notEmpty()) {
			final MapGroupLayer group = groups.first();
			groups.removeIndex(0);
			
			for (MapLayer child : group.layers) {
				child.parallaxX = (child.parallaxX * group.parallaxX);
				child.parallaxY = (child.parallaxY * group.parallaxY);
				if (child instanceof MapGroupLayer) {
					// 2) handle any child groups
					groups.add((MapGroupLayer) child);
				}
			}
		}
		
		for (Runnable runnable : runOnEndOfLoadTiled) {
			runnable.run();
		}
		runOnEndOfLoadTiled = null;
		
		return map;
	}
	
	protected void loadLayer(TiledMap map, MapLayers parentLayers, Element element, FileHandle tmxFile, ImageResolver imageResolver) {
		switch (element.getName()) {
			case "group" -> loadLayerGroup(map, parentLayers, element, tmxFile, imageResolver);
			case "layer" -> loadTileLayer(map, parentLayers, element);
			case "objectgroup" -> loadObjectGroup(map, parentLayers, element);
			case "imagelayer" -> loadImageLayer(map, parentLayers, element, tmxFile, imageResolver);
		}
	}
	
	protected void loadLayerGroup(TiledMap map, MapLayers parentLayers, Element element, FileHandle tmxFile,
								  ImageResolver imageResolver) {
		if (element.getName().equals("group")) {
			MapGroupLayer groupLayer = new MapGroupLayer();
			loadBasicLayerInfo(groupLayer, element);
			
			Element properties = element.getChildByName("properties");
			if (properties != null) {
				loadProperties(groupLayer.properties, properties);
			}
			
			for (int i = 0, j = element.getChildCount(); i < j; i++) {
				Element child = element.getChild(i);
				loadLayer(map, groupLayer.layers, child, tmxFile, imageResolver);
			}
			
			for (MapLayer layer : groupLayer.layers) {
				layer.setParent(groupLayer);
			}
			
			parentLayers.add(groupLayer);
		}
	}
	
	protected void loadTileLayer(TiledMap map, MapLayers parentLayers, Element element) {
		if (element.getName().equals("layer")) {
			int width = element.getIntAttribute("width", 0);
			int height = element.getIntAttribute("height", 0);
			int tileWidth = map.properties.get("tilewidth", Integer.class);
			int tileHeight = map.properties.get("tileheight", Integer.class);
			TiledMapTileLayer layer = new TiledMapTileLayer(width, height, tileWidth, tileHeight);
			
			loadBasicLayerInfo(layer, element);
			
			int[] ids = getTileIds(element, width, height);
			TiledMapTileSets tilesets = map.getTileSets();
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int id = ids[y * width + x];
					boolean flipHorizontally = ((id & FLAG_FLIP_HORIZONTALLY) != 0);
					boolean flipVertically = ((id & FLAG_FLIP_VERTICALLY) != 0);
					boolean flipDiagonally = ((id & FLAG_FLIP_DIAGONALLY) != 0);
					
					TiledMapTile tile = tilesets.getTile(id & ~MASK_CLEAR);
					if (tile != null) {
						Cell cell = createTileLayerCell(flipHorizontally, flipVertically, flipDiagonally);
						cell.tile = tile;
						layer.setCell(x, flipY ? height - 1 - y : y, cell);
					}
				}
			}
			
			Element properties = element.getChildByName("properties");
			if (properties != null) {
				loadProperties(layer.properties, properties);
			}
			parentLayers.add(layer);
		}
	}
	
	protected void loadObjectGroup(TiledMap map, MapLayers parentLayers, Element element) {
		if (element.getName().equals("objectgroup")) {
			MapLayer layer = new MapLayer();
			loadBasicLayerInfo(layer, element);
			Element properties = element.getChildByName("properties");
			if (properties != null) {
				loadProperties(layer.properties, properties);
			}
			
			for (Element objectElement : element.getChildrenByName("object")) {
				loadObject(map, layer, objectElement);
			}
			
			parentLayers.add(layer);
		}
	}
	
	protected void loadImageLayer(TiledMap map, MapLayers parentLayers, Element element, FileHandle tmxFile,
								  ImageResolver imageResolver) {
		if (element.getName().equals("imagelayer")) {
			float x = 0;
			float y = 0;
			if (element.hasAttribute("offsetx")) {
				x = Float.parseFloat(element.getAttribute("offsetx", "0"));
			} else {
				x = Float.parseFloat(element.getAttribute("x", "0"));
			}
			if (element.hasAttribute("offsety")) {
				y = Float.parseFloat(element.getAttribute("offsety", "0"));
			} else {
				y = Float.parseFloat(element.getAttribute("y", "0"));
			}
			if (flipY)
				y = mapHeightInPixels - y;
			
			boolean repeatX = element.getIntAttribute("repeatx", 0) == 1;
			boolean repeatY = element.getIntAttribute("repeaty", 0) == 1;
			
			TextureRegion texture = null;
			
			Element image = element.getChildByName("image");
			
			if (image != null) {
				String source = image.getAttribute("source");
				FileHandle handle = getRelativeFileHandle(tmxFile, source);
				texture = imageResolver.getImage(handle.path());
				y -= texture.getRegionHeight();
			}
			
			TiledMapImageLayer layer = new TiledMapImageLayer(texture, x, y, repeatX, repeatY);
			
			loadBasicLayerInfo(layer, element);
			
			Element properties = element.getChildByName("properties");
			if (properties != null) {
				loadProperties(layer.properties, properties);
			}
			
			parentLayers.add(layer);
		}
	}
	
	protected void loadBasicLayerInfo(MapLayer layer, Element element) {
		String name = element.getAttribute("name", null);
		float opacity = Float.parseFloat(element.getAttribute("opacity", "1.0"));
		boolean visible = element.getIntAttribute("visible", 1) == 1;
		float offsetX = element.getFloatAttribute("offsetx", 0);
		float offsetY = element.getFloatAttribute("offsety", 0);
		float parallaxX = element.getFloatAttribute("parallaxx", 1f);
		float parallaxY = element.getFloatAttribute("parallaxy", 1f);
		
		layer.name = name;
		layer.opacity = opacity;
		layer.visible = visible;
		layer.offsetX = offsetX;
		layer.offsetY = offsetY;
		layer.parallaxX = parallaxX;
		layer.parallaxY = parallaxY;
	}
	
	protected void loadObject(TiledMap map, MapLayer layer, Element element) {
		loadObject(map, layer.objects, element, mapHeightInPixels);
	}
	
	protected void loadObject(TiledMap map, TiledMapTile tile, Element element) {
		loadObject(map, tile.getObjects(), element, tile.getTextureRegion().getRegionHeight());
	}
	
	protected void loadObject(TiledMap map, MapObjects objects, Element element, float heightInPixels) {
		if (element.getName().equals("object")) {
			MapObject object = null;
			
			float scaleX = convertObjectToTileSpace ? 1.0f / mapTileWidth : 1.0f;
			float scaleY = convertObjectToTileSpace ? 1.0f / mapTileHeight : 1.0f;
			
			float x = element.getFloatAttribute("x", 0) * scaleX;
			float y = (flipY ? (heightInPixels - element.getFloatAttribute("y", 0)) : element.getFloatAttribute("y", 0)) * scaleY;
			
			float width = element.getFloatAttribute("width", 0) * scaleX;
			float height = element.getFloatAttribute("height", 0) * scaleY;
			
			if (element.getChildCount() > 0) {
				Element child = null;
				if ((child = element.getChildByName("polygon")) != null) {
					String[] points = child.getAttribute("points").split(" ");
					float[] vertices = new float[points.length * 2];
					for (int i = 0; i < points.length; i++) {
						String[] point = points[i].split(",");
						vertices[i * 2] = Float.parseFloat(point[0]) * scaleX;
						vertices[i * 2 + 1] = Float.parseFloat(point[1]) * scaleY * (flipY ? -1 : 1);
					}
					Polygon polygon = new Polygon(vertices);
					polygon.setPosition(x, y);
					object = new PolygonMapObject(polygon);
				} else if ((child = element.getChildByName("polyline")) != null) {
					String[] points = child.getAttribute("points").split(" ");
					float[] vertices = new float[points.length * 2];
					for (int i = 0; i < points.length; i++) {
						String[] point = points[i].split(",");
						vertices[i * 2] = Float.parseFloat(point[0]) * scaleX;
						vertices[i * 2 + 1] = Float.parseFloat(point[1]) * scaleY * (flipY ? -1 : 1);
					}
					Polyline polyline = new Polyline(vertices);
					polyline.setPosition(x, y);
					object = new PolylineMapObject(polyline);
				} else if ((child = element.getChildByName("ellipse")) != null) {
					object = new EllipseMapObject(x, flipY ? y - height : y, width, height);
				}
			}
			if (object == null) {
				String gid = null;
				if ((gid = element.getAttribute("gid", null)) != null) {
					int id = (int) Long.parseLong(gid);
					boolean flipHorizontally = ((id & FLAG_FLIP_HORIZONTALLY) != 0);
					boolean flipVertically = ((id & FLAG_FLIP_VERTICALLY) != 0);
					
					TiledMapTile tile = map.getTileSets().getTile(id & ~MASK_CLEAR);
					TiledMapTileMapObject tiledMapTileMapObject = new TiledMapTileMapObject(tile, flipHorizontally, flipVertically);
					TextureRegion textureRegion = tiledMapTileMapObject.textureRegion;
					tiledMapTileMapObject.properties.put("gid", id);
					tiledMapTileMapObject.x = x;
					tiledMapTileMapObject.y = (flipY ? y : y - height);
					float objectWidth = element.getFloatAttribute("width", textureRegion.getRegionWidth());
					float objectHeight = element.getFloatAttribute("height", textureRegion.getRegionHeight());
					tiledMapTileMapObject.scaleX = (scaleX * (objectWidth / textureRegion.getRegionWidth()));
					tiledMapTileMapObject.scaleY = (scaleY * (objectHeight / textureRegion.getRegionHeight()));
					tiledMapTileMapObject.rotation = (element.getFloatAttribute("rotation", 0));
					object = tiledMapTileMapObject;
				} else {
					object = new RectangleMapObject(x, flipY ? y - height : y, width, height);
				}
			}
			object.name = (element.getAttribute("name", null));
			String rotation = element.getAttribute("rotation", null);
			if (rotation != null) {
				object.properties.put("rotation", Float.parseFloat(rotation));
			}
			String type = element.getAttribute("type", null);
			if (type != null) {
				object.properties.put("type", type);
			}
			int id = element.getIntAttribute("id", 0);
			if (id != 0) {
				object.properties.put("id", id);
			}
			object.properties.put("x", x);
			
			if (object instanceof TiledMapTileMapObject) {
				object.properties.put("y", y);
			} else {
				object.properties.put("y", (flipY ? y - height : y));
			}
			object.properties.put("width", width);
			object.properties.put("height", height);
			object.visible = element.getIntAttribute("visible", 1) == 1;
			Element properties = element.getChildByName("properties");
			if (properties != null)
				loadProperties(object.properties, properties);
			
			idToObject.put(id, object);
			objects.add(object);
		}
	}
	
	protected void loadProperties(final MapProperties properties, Element element) {
		if (element == null)
			return;
		if (element.getName().equals("properties")) {
			for (Element property : element.getChildrenByName("property")) {
				final String name = property.getAttribute("name", null);
				String value = property.getAttribute("value", null);
				String type = property.getAttribute("type", null);
				if (value == null) {
					value = property.getText();
				}
				if (type != null && type.equals("object")) {
					// Wait until the end of [loadTiledMap] to fetch the object
					try {
						// Value should be the id of the object being pointed to
						final int id = Integer.parseInt(value);
						// Create [Runnable] to fetch object and add it to props
						Runnable fetch = new Runnable() {
							@Override
							public void run() {
								MapObject object = idToObject.get(id);
								properties.put(name, object);
							}
						};
						// [Runnable] should not run until the end of [loadTiledMap]
						runOnEndOfLoadTiled.add(fetch);
					} catch (Exception exception) {
						throw new MicroRuntimeException(
								"Error parsing property [\" + name + \"] of type \"object\" with value: [" + value + "]", exception);
					}
				} else {
					Object castValue = castProperty(name, value, type);
					properties.put(name, castValue);
				}
			}
		}
	}
	
	static public int[] getTileIds(Element element, int width, int height) {
		Element data = element.getChildByName("data");
		String encoding = data.getAttribute("encoding", null);
		if (encoding == null) { // no 'encoding' attribute means that the encoding is XML
			throw new MicroRuntimeException("Unsupported encoding (XML) for TMX Layer Data");
		}
		int[] ids = new int[width * height];
		if (encoding.equals("csv")) {
			String[] array = data.getText().split(",");
			for (int i = 0; i < array.length; i++)
				ids[i] = (int) Long.parseLong(array[i].trim());
		} else {
			if (encoding.equals("base64")) {
				InputStream is = null;
				try {
					String compression = data.getAttribute("compression", null);
					byte[] bytes = Base64.decode(data.getText());
					if (compression == null)
						is = new ByteArrayInputStream(bytes);
					else if (compression.equals("gzip"))
						is = new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes), bytes.length));
					else if (compression.equals("zlib"))
						is = new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(bytes)));
					else
						throw new MicroRuntimeException("Unrecognised compression (" + compression + ") for TMX Layer Data");
					
					byte[] temp = new byte[4];
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
								throw new MicroRuntimeException("Error Reading TMX Layer Data: Premature end of tile data");
							ids[y * width + x] = unsignedByteToInt(temp[0]) | unsignedByteToInt(temp[1]) << 8
									| unsignedByteToInt(temp[2]) << 16 | unsignedByteToInt(temp[3]) << 24;
						}
					}
				} catch (IOException e) {
					throw new MicroRuntimeException("Error Reading TMX Layer Data - IOException: " + e.getMessage());
				} finally {
					Streams.closeQuietly(is);
				}
			} else
				throw new MicroRuntimeException("Unrecognised encoding (" + encoding + ") for TMX Layer Data");
		}
		return ids;
	}
	
	protected void loadTileSet(Element element, FileHandle tmxFile, ImageResolver imageResolver) {
		if (element.getName().equals("tileset")) {
			int firstgid = element.getIntAttribute("firstgid", 1);
			String imageSource = "";
			int imageWidth = 0;
			int imageHeight = 0;
			FileHandle image = null;
			
			String source = element.getAttribute("source", null);
			if (source != null) {
				FileHandle tsx = getRelativeFileHandle(tmxFile, source);
				try {
					element = xml.parse(tsx);
					Element imageElement = element.getChildByName("image");
					if (imageElement != null) {
						imageSource = imageElement.getAttribute("source");
						imageWidth = imageElement.getIntAttribute("width", 0);
						imageHeight = imageElement.getIntAttribute("height", 0);
						image = getRelativeFileHandle(tsx, imageSource);
					}
				} catch (SerializationException e) {
					throw new MicroRuntimeException("Error parsing external tileset.");
				}
			} else {
				Element imageElement = element.getChildByName("image");
				if (imageElement != null) {
					imageSource = imageElement.getAttribute("source");
					imageWidth = imageElement.getIntAttribute("width", 0);
					imageHeight = imageElement.getIntAttribute("height", 0);
					image = getRelativeFileHandle(tmxFile, imageSource);
				}
			}
			String name = element.get("name", null);
			int tilewidth = element.getIntAttribute("tilewidth", 0);
			int tileheight = element.getIntAttribute("tileheight", 0);
			int spacing = element.getIntAttribute("spacing", 0);
			int margin = element.getIntAttribute("margin", 0);
			
			Element offset = element.getChildByName("tileoffset");
			int offsetX = 0;
			int offsetY = 0;
			if (offset != null) {
				offsetX = offset.getIntAttribute("x", 0);
				offsetY = offset.getIntAttribute("y", 0);
			}
			TiledMapTileSet tileSet = new TiledMapTileSet();
			
			// TileSet
			tileSet.name = name;
			final MapProperties tileSetProperties = tileSet.properties;
			Element properties = element.getChildByName("properties");
			if (properties != null) {
				loadProperties(tileSetProperties, properties);
			}
			tileSetProperties.put("firstgid", firstgid);
			
			// Tiles
			Array<Element> tileElements = element.getChildrenByName("tile");
			
			addStaticTiles(tmxFile, imageResolver, tileSet, element, tileElements, name, firstgid, tilewidth, tileheight, spacing,
					margin, source, offsetX, offsetY, imageSource, imageWidth, imageHeight, image);
			
			Array<AnimatedTiledMapTile> animatedTiles = new Array<>();
			
			for (Element tileElement : tileElements) {
				int localtid = tileElement.getIntAttribute("id", 0);
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
	
	protected abstract void addStaticTiles(FileHandle tmxFile, ImageResolver imageResolver, TiledMapTileSet tileset,
										   Element element, Array<Element> tileElements, String name, int firstgid, int tilewidth, int tileheight, int spacing,
										   int margin, String source, int offsetX, int offsetY, String imageSource, int imageWidth, int imageHeight, FileHandle image);
	
	protected void addTileProperties(TiledMapTile tile, Element tileElement) {
		String terrain = tileElement.getAttribute("terrain", null);
		if (terrain != null)
			tile.getProperties().put("terrain", terrain);
		String probability = tileElement.getAttribute("probability", null);
		if (probability != null)
			tile.getProperties().put("probability", probability);
		String type = tileElement.getAttribute("type", null);
		if (type != null)
			tile.getProperties().put("type", type);
		Element properties = tileElement.getChildByName("properties");
		if (properties != null) {
			loadProperties(tile.getProperties(), properties);
		}
	}
	
	protected void addTileObjectGroup(TiledMapTile tile, Element tileElement) {
		Element objectgroupElement = tileElement.getChildByName("objectgroup");
		if (objectgroupElement != null)
			for (Element objectElement : objectgroupElement.getChildrenByName("object"))
				loadObject(map, tile, objectElement);
	}
	
	protected AnimatedTiledMapTile createAnimatedTile(TiledMapTileSet tileSet, TiledMapTile tile, Element tileElement, int firstgid) {
		Element animationElement = tileElement.getChildByName("animation");
		if (animationElement != null) {
			Array<StaticTiledMapTile> staticTiles = new Array<StaticTiledMapTile>();
			IntArray intervals = new IntArray();
			for (Element frameElement : animationElement.getChildrenByName("frame")) {
				staticTiles.add((StaticTiledMapTile) tileSet.getTile(firstgid + frameElement.getIntAttribute("tileid")));
				intervals.add(frameElement.getIntAttribute("duration"));
			}
			
			AnimatedTiledMapTile animatedTile = new AnimatedTiledMapTile(intervals, staticTiles);
			animatedTile.setId(tile.getId());
			return animatedTile;
		}
		return null;
	}
	
}