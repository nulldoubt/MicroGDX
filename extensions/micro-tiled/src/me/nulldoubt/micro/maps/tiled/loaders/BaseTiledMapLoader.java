package me.nulldoubt.micro.maps.tiled.loaders;

import me.nulldoubt.micro.assets.AssetDescriptor;
import me.nulldoubt.micro.assets.AssetLoaderParameters;
import me.nulldoubt.micro.assets.loaders.AsynchronousAssetLoader;
import me.nulldoubt.micro.assets.loaders.FileHandleResolver;
import me.nulldoubt.micro.assets.loaders.TextureLoader;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.graphics.Color;
import me.nulldoubt.micro.graphics.Texture;
import me.nulldoubt.micro.graphics.g2d.TextureRegion;
import me.nulldoubt.micro.maps.ImageResolver;
import me.nulldoubt.micro.maps.MapObject;
import me.nulldoubt.micro.maps.tiled.TiledMap;
import me.nulldoubt.micro.maps.tiled.TiledMapTile;
import me.nulldoubt.micro.maps.tiled.TiledMapTileLayer;
import me.nulldoubt.micro.maps.tiled.TiledMapTileSet;
import me.nulldoubt.micro.maps.tiled.tiles.StaticTiledMapTile;
import me.nulldoubt.micro.utils.collections.Array;
import me.nulldoubt.micro.utils.collections.IntMap;

import java.util.StringTokenizer;

public abstract class BaseTiledMapLoader<P extends BaseTiledMapLoader.Parameters> extends AsynchronousAssetLoader<TiledMap, P> {
	
	public static class Parameters extends AssetLoaderParameters<TiledMap> {
		
		public boolean generateMipMaps = false;
		public Texture.TextureFilter textureMinFilter = Texture.TextureFilter.Nearest;
		public Texture.TextureFilter textureMagFilter = Texture.TextureFilter.Nearest;
		public boolean convertObjectToTileSpace = false;
		public boolean flipY = true;
		
	}
	
	protected static final int FLAG_FLIP_HORIZONTALLY = 0x80000000;
	protected static final int FLAG_FLIP_VERTICALLY = 0x40000000;
	protected static final int FLAG_FLIP_DIAGONALLY = 0x20000000;
	protected static final int MASK_CLEAR = 0xE0000000;
	protected boolean convertObjectToTileSpace;
	protected boolean flipY = true;
	protected int mapTileWidth;
	protected int mapTileHeight;
	protected int mapWidthInPixels;
	protected int mapHeightInPixels;
	protected TiledMap map;
	protected IntMap<MapObject> idToObject;
	protected Array<Runnable> runOnEndOfLoadTiled;
	
	public BaseTiledMapLoader(final FileHandleResolver resolver) {
		super(resolver);
	}
	
	protected abstract Array<AssetDescriptor<?>> getDependencyAssetDescriptors(final FileHandle mapFile, final TextureLoader.TextureParameter textureParameter);
	
	protected abstract TiledMap loadTiledMap(final FileHandle mapFile, final P parameter, final ImageResolver imageResolver);
	
	public IntMap<MapObject> getIdToObject() {
		return idToObject;
	}
	
	protected Object castProperty(final String name, final String value, final String type) {
		if (type == null || "string".equals(type)) {
			return value;
		} else if (type.equals("int")) {
			return Integer.valueOf(value);
		} else if (type.equals("float")) {
			return Float.valueOf(value);
		} else if (type.equals("bool")) {
			return Boolean.valueOf(value);
		} else if (type.equals("color")) {
			String opaqueColor = value.substring(3);
			String alpha = value.substring(1, 3);
			return Color.valueOf(opaqueColor + alpha);
		} else
			throw new MicroRuntimeException("Wrong type given for property " + name + ", given : " + type + ", supported : string, bool, int, float, color");
	}
	
	protected TiledMapTileLayer.Cell createTileLayerCell(final boolean flipHorizontally, final boolean flipVertically, final boolean flipDiagonally) {
		final TiledMapTileLayer.Cell cell = new TiledMapTileLayer.Cell();
		if (flipDiagonally) {
			if (flipHorizontally && flipVertically) {
				cell.flipHorizontally = true;
				cell.rotation = TiledMapTileLayer.Cell.ROTATE_270;
			} else if (flipHorizontally)
				cell.rotation = TiledMapTileLayer.Cell.ROTATE_270;
			else if (flipVertically)
				cell.rotation = TiledMapTileLayer.Cell.ROTATE_90;
			else {
				cell.flipVertically = true;
				cell.rotation = TiledMapTileLayer.Cell.ROTATE_270;
			}
		} else {
			cell.flipHorizontally = flipHorizontally;
			cell.flipVertically = flipVertically;
		}
		return cell;
	}
	
	protected static int unsignedByteToInt(final byte b) {
		return b & 0xFF;
	}
	
	protected static FileHandle getRelativeFileHandle(final FileHandle file, final String path) {
		StringTokenizer tokenizer = new StringTokenizer(path, "\\/");
		FileHandle result = file.parent();
		while (tokenizer.hasMoreElements()) {
			String token = tokenizer.nextToken();
			if (token.equals(".."))
				result = result.parent();
			else
				result = result.child(token);
		}
		return result;
	}
	
	protected void addStaticTiledMapTile(final TiledMapTileSet tileSet, final TextureRegion textureRegion, final int tileId, final float offsetX, final float offsetY) {
		final TiledMapTile tile = new StaticTiledMapTile(textureRegion);
		tile.setId(tileId);
		tile.setOffsetX(offsetX);
		tile.setOffsetY(flipY ? -offsetY : offsetY);
		tileSet.putTile(tileId, tile);
	}
	
}