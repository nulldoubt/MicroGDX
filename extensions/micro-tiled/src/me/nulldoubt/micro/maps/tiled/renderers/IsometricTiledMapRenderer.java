package me.nulldoubt.micro.maps.tiled.renderers;

import me.nulldoubt.micro.graphics.Color;
import me.nulldoubt.micro.graphics.g2d.Batch;
import me.nulldoubt.micro.graphics.g2d.TextureRegion;
import me.nulldoubt.micro.maps.tiled.TiledMap;
import me.nulldoubt.micro.maps.tiled.TiledMapImageLayer;
import me.nulldoubt.micro.maps.tiled.TiledMapTile;
import me.nulldoubt.micro.maps.tiled.TiledMapTileLayer;
import me.nulldoubt.micro.maps.tiled.TiledMapTileLayer.Cell;
import me.nulldoubt.micro.math.Matrix4;
import me.nulldoubt.micro.math.Vector2;
import me.nulldoubt.micro.math.Vector3;

import static me.nulldoubt.micro.graphics.g2d.Batch.*;

public class IsometricTiledMapRenderer extends BatchTiledMapRenderer {
	
	private Matrix4 invIsoTransform;
	private final Vector3 screenPos = new Vector3();
	
	private final Vector2 topRight = new Vector2();
	private final Vector2 bottomLeft = new Vector2();
	private final Vector2 topLeft = new Vector2();
	private final Vector2 bottomRight = new Vector2();
	
	public IsometricTiledMapRenderer(TiledMap map) {
		super(map);
		init();
	}
	
	public IsometricTiledMapRenderer(TiledMap map, Batch batch) {
		super(map, batch);
		init();
	}
	
	public IsometricTiledMapRenderer(TiledMap map, float unitScale) {
		super(map, unitScale);
		init();
	}
	
	public IsometricTiledMapRenderer(TiledMap map, float unitScale, Batch batch) {
		super(map, unitScale, batch);
		init();
	}
	
	private void init() {
		// create the isometric transform
		Matrix4 isoTransform = new Matrix4();
		isoTransform.idt();
		
		// isoTransform.translate(0, 32, 0);
		isoTransform.scale((float) (Math.sqrt(2.0) / 2.0), (float) (Math.sqrt(2.0) / 4.0), 1.0f);
		isoTransform.rotate(0.0f, 0.0f, 1.0f, -45);
		
		// ... and the inverse matrix
		invIsoTransform = new Matrix4(isoTransform);
		invIsoTransform.inv();
	}
	
	private Vector3 translateScreenToIso(Vector2 vec) {
		screenPos.set(vec.x, vec.y, 0);
		screenPos.mul(invIsoTransform);
		return screenPos;
	}
	
	@Override
	public void renderTileLayer(TiledMapTileLayer layer) {
		final Color batchColor = batch.getColor();
		final float color = Color.toFloatBits(batchColor.r, batchColor.g, batchColor.b, batchColor.a * layer.opacity);
		
		float tileWidth = layer.tileWidth * unitScale;
		float tileHeight = layer.tileHeight * unitScale;
		
		final float layerOffsetX = layer.getRenderOffsetX() * unitScale - viewBounds.x * (layer.parallaxX - 1);
		// offset in tiled is y down, so we flip it
		final float layerOffsetY = -layer.getRenderOffsetY() * unitScale - viewBounds.y * (layer.parallaxY - 1);
		
		float halfTileWidth = tileWidth * 0.5f;
		float halfTileHeight = tileHeight * 0.5f;
		
		// setting up the screen points
		// COL1
		topRight.set(viewBounds.x + viewBounds.width - layerOffsetX, viewBounds.y - layerOffsetY);
		// COL2
		bottomLeft.set(viewBounds.x - layerOffsetX, viewBounds.y + viewBounds.height - layerOffsetY);
		// ROW1
		topLeft.set(viewBounds.x - layerOffsetX, viewBounds.y - layerOffsetY);
		// ROW2
		bottomRight.set(viewBounds.x + viewBounds.width - layerOffsetX, viewBounds.y + viewBounds.height - layerOffsetY);
		
		// transforming screen coordinates to iso coordinates
		int row1 = (int) (translateScreenToIso(topLeft).y / tileWidth) - 2;
		int row2 = (int) (translateScreenToIso(bottomRight).y / tileWidth) + 2;
		
		int col1 = (int) (translateScreenToIso(bottomLeft).x / tileWidth) - 2;
		int col2 = (int) (translateScreenToIso(topRight).x / tileWidth) + 2;
		
		for (int row = row2; row >= row1; row--) {
			for (int col = col1; col <= col2; col++) {
				float x = (col * halfTileWidth) + (row * halfTileWidth);
				float y = (row * halfTileHeight) - (col * halfTileHeight);
				
				final TiledMapTileLayer.Cell cell = layer.getCell(col, row);
				if (cell == null)
					continue;
				final TiledMapTile tile = cell.tile;
				
				if (tile != null) {
					final boolean flipX = cell.flipHorizontally;
					final boolean flipY = cell.flipVertically;
					final int rotations = cell.rotation;
					
					TextureRegion region = tile.getTextureRegion();
					
					float x1 = x + tile.getOffsetX() * unitScale + layerOffsetX;
					float y1 = y + tile.getOffsetY() * unitScale + layerOffsetY;
					float x2 = x1 + region.getRegionWidth() * unitScale;
					float y2 = y1 + region.getRegionHeight() * unitScale;
					
					float u1 = region.getU();
					float v1 = region.getV2();
					float u2 = region.getU2();
					float v2 = region.getV();
					
					vertices[X1] = x1;
					vertices[Y1] = y1;
					vertices[C1] = color;
					vertices[U1] = u1;
					vertices[V1] = v1;
					
					vertices[X2] = x1;
					vertices[Y2] = y2;
					vertices[C2] = color;
					vertices[U2] = u1;
					vertices[V2] = v2;
					
					vertices[X3] = x2;
					vertices[Y3] = y2;
					vertices[C3] = color;
					vertices[U3] = u2;
					vertices[V3] = v2;
					
					vertices[X4] = x2;
					vertices[Y4] = y1;
					vertices[C4] = color;
					vertices[U4] = u2;
					vertices[V4] = v1;
					
					if (flipX) {
						float temp = vertices[U1];
						vertices[U1] = vertices[U3];
						vertices[U3] = temp;
						temp = vertices[U2];
						vertices[U2] = vertices[U4];
						vertices[U4] = temp;
					}
					if (flipY) {
						float temp = vertices[V1];
						vertices[V1] = vertices[V3];
						vertices[V3] = temp;
						temp = vertices[V2];
						vertices[V2] = vertices[V4];
						vertices[V4] = temp;
					}
					if (rotations != 0) {
						switch (rotations) {
							case Cell.ROTATE_90: {
								float tempV = vertices[V1];
								vertices[V1] = vertices[V2];
								vertices[V2] = vertices[V3];
								vertices[V3] = vertices[V4];
								vertices[V4] = tempV;
								
								float tempU = vertices[U1];
								vertices[U1] = vertices[U2];
								vertices[U2] = vertices[U3];
								vertices[U3] = vertices[U4];
								vertices[U4] = tempU;
								break;
							}
							case Cell.ROTATE_180: {
								float tempU = vertices[U1];
								vertices[U1] = vertices[U3];
								vertices[U3] = tempU;
								tempU = vertices[U2];
								vertices[U2] = vertices[U4];
								vertices[U4] = tempU;
								float tempV = vertices[V1];
								vertices[V1] = vertices[V3];
								vertices[V3] = tempV;
								tempV = vertices[V2];
								vertices[V2] = vertices[V4];
								vertices[V4] = tempV;
								break;
							}
							case Cell.ROTATE_270: {
								float tempV = vertices[V1];
								vertices[V1] = vertices[V4];
								vertices[V4] = vertices[V3];
								vertices[V3] = vertices[V2];
								vertices[V2] = tempV;
								
								float tempU = vertices[U1];
								vertices[U1] = vertices[U4];
								vertices[U4] = vertices[U3];
								vertices[U3] = vertices[U2];
								vertices[U2] = tempU;
								break;
							}
						}
					}
					batch.draw(region.getTexture(), vertices, 0, NUM_VERTICES);
				}
			}
		}
	}
	
	@Override
	public void renderImageLayer(TiledMapImageLayer layer) {
		final Color batchColor = batch.getColor();
		final float color = Color.toFloatBits(batchColor.r, batchColor.g, batchColor.b, batchColor.a * layer.opacity);
		
		final float[] vertices = this.vertices;
		
		TextureRegion region = layer.getTextureRegion();
		
		if (region == null)
			return;
		
		int tileHeight = getMap().properties.get("tileheight", Integer.class);
		int mapHeight = getMap().properties.get("height", Integer.class);
		float mapHeightPixels = (mapHeight * tileHeight) * unitScale;
		float halfTileHeight = (tileHeight * 0.5f) * unitScale;
		
		float x = layer.getX();
		float y = layer.getY();
		final float x1 = x * unitScale - viewBounds.x * (layer.parallaxX - 1);
		final float y1 = y * unitScale - viewBounds.y * (layer.parallaxY - 1) - (mapHeightPixels * 0.5f) + halfTileHeight;
		float x2 = x1 + region.getRegionWidth() * unitScale;
		float y2 = y1 + region.getRegionHeight() * unitScale;
		
		imageBounds.set(x1, y1, x2 - x1, y2 - y1);
		
		if (!layer.isRepeatX() && !layer.isRepeatY()) {
			if (viewBounds.contains(imageBounds) || viewBounds.overlaps(imageBounds)) {
				final float u1 = region.getU();
				final float v1 = region.getV2();
				final float u2 = region.getU2();
				final float v2 = region.getV();
				
				vertices[X1] = x1;
				vertices[Y1] = y1;
				vertices[C1] = color;
				vertices[U1] = u1;
				vertices[V1] = v1;
				
				vertices[X2] = x1;
				vertices[Y2] = y2;
				vertices[C2] = color;
				vertices[U2] = u1;
				vertices[V2] = v2;
				
				vertices[X3] = x2;
				vertices[Y3] = y2;
				vertices[C3] = color;
				vertices[U3] = u2;
				vertices[V3] = v2;
				
				vertices[X4] = x2;
				vertices[Y4] = y1;
				vertices[C4] = color;
				vertices[U4] = u2;
				vertices[V4] = v1;
				
				batch.draw(region.getTexture(), vertices, 0, NUM_VERTICES);
			}
		} else {
			
			// Determine number of times to repeat image across X and Y, + 4 for padding to avoid pop in/out
			int repeatX = layer.isRepeatX() ? (int) Math.ceil((viewBounds.width / imageBounds.width) + 4) : 0;
			int repeatY = layer.isRepeatY() ? (int) Math.ceil((viewBounds.height / imageBounds.height) + 4) : 0;
			
			// Calculate the offset of the first image to align with the camera
			float startX = viewBounds.x;
			float startY = viewBounds.y;
			startX = startX - (startX % imageBounds.width);
			startY = startY - (startY % imageBounds.height);
			
			for (int i = 0; i <= repeatX; i++) {
				for (int j = 0; j <= repeatY; j++) {
					float rx1 = x1;
					float ry1 = y1;
					float rx2 = x2;
					float ry2 = y2;
					
					// Use (i -2)/(j-2) to begin placing our repeating images outside the camera.
					// In case the image is offset, we must negate this using + (x1% imageBounds.width)
					// It's a way to get the remainder of how many images would fit between its starting position and 0
					if (layer.isRepeatX()) {
						rx1 = startX + ((i - 2) * imageBounds.width) + (x1 % imageBounds.width);
						rx2 = rx1 + imageBounds.width;
					}
					
					if (layer.isRepeatY()) {
						ry1 = startY + ((j - 2) * imageBounds.height) + (y1 % imageBounds.height);
						ry2 = ry1 + imageBounds.height;
					}
					
					repeatedImageBounds.set(rx1, ry1, rx2 - rx1, ry2 - ry1);
					
					if (viewBounds.contains(repeatedImageBounds) || viewBounds.overlaps(repeatedImageBounds)) {
						final float ru1 = region.getU();
						final float rv1 = region.getV2();
						final float ru2 = region.getU2();
						final float rv2 = region.getV();
						
						vertices[X1] = rx1;
						vertices[Y1] = ry1;
						vertices[C1] = color;
						vertices[U1] = ru1;
						vertices[V1] = rv1;
						
						vertices[X2] = rx1;
						vertices[Y2] = ry2;
						vertices[C2] = color;
						vertices[U2] = ru1;
						vertices[V2] = rv2;
						
						vertices[X3] = rx2;
						vertices[Y3] = ry2;
						vertices[C3] = color;
						vertices[U3] = ru2;
						vertices[V3] = rv2;
						
						vertices[X4] = rx2;
						vertices[Y4] = ry1;
						vertices[C4] = color;
						vertices[U4] = ru2;
						vertices[V4] = rv1;
						
						batch.draw(region.getTexture(), vertices, 0, NUM_VERTICES);
					}
				}
			}
		}
	}
	
}