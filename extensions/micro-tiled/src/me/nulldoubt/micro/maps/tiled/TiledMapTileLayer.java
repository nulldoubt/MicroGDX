package me.nulldoubt.micro.maps.tiled;

import me.nulldoubt.micro.maps.MapLayer;

public class TiledMapTileLayer extends MapLayer {
	
	public final int width;
	public final int height;
	
	public final int tileWidth;
	public final int tileHeight;
	
	private final Cell[][] cells;
	
	public TiledMapTileLayer(int width, int height, int tileWidth, int tileHeight) {
		super();
		this.width = width;
		this.height = height;
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		this.cells = new Cell[width][height];
	}
	
	public Cell getCell(int x, int y) {
		if (x < 0 || x >= width)
			return null;
		if (y < 0 || y >= height)
			return null;
		return cells[x][y];
	}
	
	public void setCell(int x, int y, Cell cell) {
		if (x < 0 || x >= width)
			return;
		if (y < 0 || y >= height)
			return;
		cells[x][y] = cell;
	}
	
	public static class Cell {
		
		public TiledMapTile tile;
		public boolean flipHorizontally;
		public boolean flipVertically;
		public int rotation;
		
		public static final int ROTATE_0 = 0;
		public static final int ROTATE_90 = 1;
		public static final int ROTATE_180 = 2;
		public static final int ROTATE_270 = 3;
		
	}
	
}
