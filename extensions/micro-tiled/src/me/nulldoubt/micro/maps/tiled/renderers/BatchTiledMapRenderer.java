package me.nulldoubt.micro.maps.tiled.renderers;

import me.nulldoubt.micro.graphics.Camera;
import me.nulldoubt.micro.graphics.Color;
import me.nulldoubt.micro.graphics.g2d.Batch;
import me.nulldoubt.micro.graphics.g2d.TextureRegion;
import me.nulldoubt.micro.maps.MapGroupLayer;
import me.nulldoubt.micro.maps.MapLayer;
import me.nulldoubt.micro.maps.MapLayers;
import me.nulldoubt.micro.maps.MapObject;
import me.nulldoubt.micro.maps.tiled.TiledMap;
import me.nulldoubt.micro.maps.tiled.TiledMapImageLayer;
import me.nulldoubt.micro.maps.tiled.TiledMapRenderer;
import me.nulldoubt.micro.maps.tiled.TiledMapTileLayer;
import me.nulldoubt.micro.maps.tiled.tiles.AnimatedTiledMapTile;
import me.nulldoubt.micro.math.Matrix4;
import me.nulldoubt.micro.math.shapes.Rectangle;
import me.nulldoubt.micro.utils.Disposable;

import static me.nulldoubt.micro.graphics.g2d.Batch.*;

public abstract class BatchTiledMapRenderer implements TiledMapRenderer, Disposable {
	
	protected static final int NUM_VERTICES = 20;
	
	protected TiledMap map;
	
	protected float unitScale;
	
	protected Batch batch;
	
	protected Rectangle viewBounds;
	protected Rectangle imageBounds = new Rectangle();
	protected Rectangle repeatedImageBounds = new Rectangle();
	
	protected boolean ownsBatch;
	
	protected float[] vertices = new float[NUM_VERTICES];
	
	public TiledMap getMap() {
		return map;
	}
	
	public void setMap(TiledMap map) {
		this.map = map;
	}
	
	public float getUnitScale() {
		return unitScale;
	}
	
	public Batch getBatch() {
		return batch;
	}
	
	public Rectangle getViewBounds() {
		return viewBounds;
	}
	
	public BatchTiledMapRenderer(TiledMap map) {
		this(map, 1.0f);
	}
	
	public BatchTiledMapRenderer(TiledMap map, float unitScale) {
		this.map = map;
		this.unitScale = unitScale;
		this.viewBounds = new Rectangle();
		this.batch = new Batch();
		this.ownsBatch = true;
	}
	
	public BatchTiledMapRenderer(TiledMap map, Batch batch) {
		this(map, 1.0f, batch);
	}
	
	public BatchTiledMapRenderer(TiledMap map, float unitScale, Batch batch) {
		this.map = map;
		this.unitScale = unitScale;
		this.viewBounds = new Rectangle();
		this.batch = batch;
		this.ownsBatch = false;
	}
	
	@Override
	public void setView(Camera camera) {
		batch.setProjectionMatrix(camera.combined);
		float width = camera.viewportWidth * camera.zoom;
		float height = camera.viewportHeight * camera.zoom;
		float w = width * Math.abs(camera.up.y) + height * Math.abs(camera.up.x);
		float h = height * Math.abs(camera.up.y) + width * Math.abs(camera.up.x);
		viewBounds.set(camera.position.x - w / 2, camera.position.y - h / 2, w, h);
	}
	
	@Override
	public void setView(Matrix4 projection, float viewBoundsX, float viewBoundsY, float viewBoundsWidth, float viewBoundsHeight) {
		batch.setProjectionMatrix(projection);
		viewBounds.set(viewBoundsX, viewBoundsY, viewBoundsWidth, viewBoundsHeight);
	}
	
	@Override
	public void render() {
		beginRender();
		for (MapLayer layer : map.layers)
			renderMapLayer(layer);
		endRender();
	}
	
	@Override
	public void render(int[] layers) {
		beginRender();
		for (int layerIdx : layers) {
			MapLayer layer = map.layers.get(layerIdx);
			renderMapLayer(layer);
		}
		endRender();
	}
	
	protected void renderMapLayer(MapLayer layer) {
		if (!layer.visible)
			return;
		if (layer instanceof MapGroupLayer) {
			MapLayers childLayers = ((MapGroupLayer) layer).layers;
			for (int i = 0; i < childLayers.size(); i++) {
				MapLayer childLayer = childLayers.get(i);
				if (!childLayer.visible)
					continue;
				renderMapLayer(childLayer);
			}
		} else {
			if (layer instanceof TiledMapTileLayer) {
				renderTileLayer((TiledMapTileLayer) layer);
			} else if (layer instanceof TiledMapImageLayer) {
				renderImageLayer((TiledMapImageLayer) layer);
			} else {
				renderObjects(layer);
			}
		}
	}
	
	@Override
	public void renderObjects(MapLayer layer) {
		for (MapObject object : layer.objects)
			renderObject(object);
	}
	
	@Override
	public void renderObject(MapObject object) {}
	
	@Override
	public void renderImageLayer(TiledMapImageLayer layer) {
		final Color batchColor = batch.getColor();
		final float color = Color.toFloatBits(batchColor.r, batchColor.g, batchColor.b, batchColor.a * layer.opacity);
		
		final float[] vertices = this.vertices;
		
		TextureRegion region = layer.getTextureRegion();
		
		if (region == null) {
			return;
		}
		
		final float x = layer.getX();
		final float y = layer.getY();
		final float x1 = x * unitScale - viewBounds.x * (layer.parallaxX - 1);
		final float y1 = y * unitScale - viewBounds.y * (layer.parallaxY - 1);
		final float x2 = x1 + region.getRegionWidth() * unitScale;
		final float y2 = y1 + region.getRegionHeight() * unitScale;
		
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
	
	/**
	 * Called before the rendering of all layers starts.
	 */
	protected void beginRender() {
		AnimatedTiledMapTile.updateAnimationBaseTime();
		batch.begin();
	}
	
	/**
	 * Called after the rendering of all layers ended.
	 */
	protected void endRender() {
		batch.end();
	}
	
	@Override
	public void dispose() {
		if (ownsBatch) {
			batch.dispose();
		}
	}
	
}
