package me.nulldoubt.micro.graphics;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.graphics.Pixmap.Blending;
import me.nulldoubt.micro.graphics.Pixmap.Format;
import me.nulldoubt.micro.graphics.Texture.TextureFilter;
import com.nulldoubt.micro.graphics.g2d.PixmapPacker.SkylineStrategy.SkylinePage.Row;
import me.nulldoubt.micro.graphics.glutils.PixmapTextureData;
import me.nulldoubt.micro.math.shapes.Rectangle;
import me.nulldoubt.micro.utils.collections.Array;
import me.nulldoubt.micro.utils.Disposable;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.utils.collections.OrderedMap;

import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PixmapPacker implements Disposable {
	
	boolean packToTexture;
	boolean disposed;
	int pageWidth, pageHeight;
	Format pageFormat;
	int padding;
	boolean duplicateBorder;
	boolean stripWhitespaceX, stripWhitespaceY;
	int alphaThreshold;
	Color transparentColor = new Color(0f, 0f, 0f, 0f);
	final Array<Page> pages = new Array<>();
	PackStrategy packStrategy;
	
	static Pattern indexPattern = Pattern.compile("(.+)_(\\d+)$");
	
	public PixmapPacker(int pageWidth, int pageHeight, Format pageFormat, int padding, boolean duplicateBorder) {
		this(pageWidth, pageHeight, pageFormat, padding, duplicateBorder, false, false, new GuillotineStrategy());
	}
	
	public PixmapPacker(int pageWidth, int pageHeight, Format pageFormat, int padding, boolean duplicateBorder, PackStrategy packStrategy) {
		this(pageWidth, pageHeight, pageFormat, padding, duplicateBorder, false, false, packStrategy);
	}
	
	public PixmapPacker(int pageWidth, int pageHeight, Format pageFormat, int padding, boolean duplicateBorder, boolean stripWhitespaceX, boolean stripWhitespaceY, PackStrategy packStrategy) {
		this.pageWidth = pageWidth;
		this.pageHeight = pageHeight;
		this.pageFormat = pageFormat;
		this.padding = padding;
		this.duplicateBorder = duplicateBorder;
		this.stripWhitespaceX = stripWhitespaceX;
		this.stripWhitespaceY = stripWhitespaceY;
		this.packStrategy = packStrategy;
	}
	
	public void sort(Array<Pixmap> images) {
		packStrategy.sort(images);
	}
	
	public synchronized PixmapPackerRectangle pack(Pixmap image) {
		return pack(null, image);
	}
	
	public synchronized PixmapPackerRectangle pack(String name, Pixmap image) {
		if (disposed)
			return null;
		if (name != null && getRect(name) != null)
			throw new MicroRuntimeException("Pixmap has already been packed with name: " + name);
		
		PixmapPackerRectangle rect;
		Pixmap pixmapToDispose = null;
		if (name != null && name.endsWith(".9")) {
			rect = new PixmapPackerRectangle(0, 0, image.getWidth() - 2, image.getHeight() - 2);
			pixmapToDispose = new Pixmap(image.getWidth() - 2, image.getHeight() - 2, image.getFormat());
			pixmapToDispose.setBlending(Blending.None);
			rect.splits = getSplits(image);
			rect.pads = getPads(image, rect.splits);
			pixmapToDispose.drawPixmap(image, 0, 0, 1, 1, image.getWidth() - 1, image.getHeight() - 1);
			image = pixmapToDispose;
			name = name.split("\\.")[0];
		} else {
			if (stripWhitespaceX || stripWhitespaceY) {
				int originalWidth = image.getWidth();
				int originalHeight = image.getHeight();
				// Strip whitespace, manipulate the pixmap and return corrected Rect
				int top = 0;
				int bottom = image.getHeight();
				if (stripWhitespaceY) {
					outer:
					for (int y = 0; y < image.getHeight(); y++) {
						for (int x = 0; x < image.getWidth(); x++) {
							int pixel = image.getPixel(x, y);
							int alpha = ((pixel & 0x000000ff));
							if (alpha > alphaThreshold)
								break outer;
						}
						top++;
					}
					outer:
					for (int y = image.getHeight(); --y >= top; ) {
						for (int x = 0; x < image.getWidth(); x++) {
							int pixel = image.getPixel(x, y);
							int alpha = ((pixel & 0x000000ff));
							if (alpha > alphaThreshold)
								break outer;
						}
						bottom--;
					}
				}
				int left = 0;
				int right = image.getWidth();
				if (stripWhitespaceX) {
					outer:
					for (int x = 0; x < image.getWidth(); x++) {
						for (int y = top; y < bottom; y++) {
							int pixel = image.getPixel(x, y);
							int alpha = ((pixel & 0x000000ff));
							if (alpha > alphaThreshold)
								break outer;
						}
						left++;
					}
					outer:
					for (int x = image.getWidth(); --x >= left; ) {
						for (int y = top; y < bottom; y++) {
							int pixel = image.getPixel(x, y);
							int alpha = ((pixel & 0x000000ff));
							if (alpha > alphaThreshold)
								break outer;
						}
						right--;
					}
				}
				
				int newWidth = right - left;
				int newHeight = bottom - top;
				
				pixmapToDispose = new Pixmap(newWidth, newHeight, image.getFormat());
				pixmapToDispose.setBlending(Blending.None);
				pixmapToDispose.drawPixmap(image, 0, 0, left, top, newWidth, newHeight);
				image = pixmapToDispose;
				
				rect = new PixmapPackerRectangle(0, 0, newWidth, newHeight, left, top, originalWidth, originalHeight);
			} else {
				rect = new PixmapPackerRectangle(0, 0, image.getWidth(), image.getHeight());
			}
		}
		
		if (rect.getWidth() > pageWidth || rect.getHeight() > pageHeight) {
			if (name == null)
				throw new MicroRuntimeException("Page size too small for pixmap.");
			throw new MicroRuntimeException("Page size too small for pixmap: " + name);
		}
		
		Page page = packStrategy.pack(this, name, rect);
		if (name != null) {
			page.rects.put(name, rect);
			page.addedRects.add(name);
		}
		
		int rectX = (int) rect.x, rectY = (int) rect.y, rectWidth = (int) rect.width, rectHeight = (int) rect.height;
		
		if (packToTexture && !duplicateBorder && page.texture != null && !page.dirty) {
			page.texture.bind();
			Micro.gl.glTexSubImage2D(page.texture.glTarget, 0, rectX, rectY, rectWidth, rectHeight, image.getGLFormat(),
					image.getGLType(), image.getPixels());
		} else
			page.dirty = true;
		
		page.image.drawPixmap(image, rectX, rectY);
		
		if (duplicateBorder) {
			int imageWidth = image.getWidth(), imageHeight = image.getHeight();
			// Copy corner pixels to fill corners of the padding.
			page.image.drawPixmap(image, 0, 0, 1, 1, rectX - 1, rectY - 1, 1, 1);
			page.image.drawPixmap(image, imageWidth - 1, 0, 1, 1, rectX + rectWidth, rectY - 1, 1, 1);
			page.image.drawPixmap(image, 0, imageHeight - 1, 1, 1, rectX - 1, rectY + rectHeight, 1, 1);
			page.image.drawPixmap(image, imageWidth - 1, imageHeight - 1, 1, 1, rectX + rectWidth, rectY + rectHeight, 1, 1);
			// Copy edge pixels into padding.
			page.image.drawPixmap(image, 0, 0, imageWidth, 1, rectX, rectY - 1, rectWidth, 1);
			page.image.drawPixmap(image, 0, imageHeight - 1, imageWidth, 1, rectX, rectY + rectHeight, rectWidth, 1);
			page.image.drawPixmap(image, 0, 0, 1, imageHeight, rectX - 1, rectY, 1, rectHeight);
			page.image.drawPixmap(image, imageWidth - 1, 0, 1, imageHeight, rectX + rectWidth, rectY, 1, rectHeight);
		}
		
		if (pixmapToDispose != null) {
			pixmapToDispose.dispose();
		}
		
		rect.page = page;
		return rect;
	}
	
	public Array<Page> getPages() {
		return pages;
	}
	
	public synchronized Rectangle getRect(String name) {
		for (Page page : pages) {
			Rectangle rect = page.rects.get(name);
			if (rect != null)
				return rect;
		}
		return null;
	}
	
	public synchronized Page getPage(String name) {
		for (Page page : pages) {
			Rectangle rect = page.rects.get(name);
			if (rect != null)
				return page;
		}
		return null;
	}
	
	public synchronized int getPageIndex(String name) {
		for (int i = 0; i < pages.size; i++) {
			Rectangle rect = pages.get(i).rects.get(name);
			if (rect != null)
				return i;
		}
		return -1;
	}
	
	public synchronized void dispose() {
		for (Page page : pages) {
			if (page.texture == null)
				page.image.dispose();
		}
		disposed = true;
	}
	
	public synchronized TextureAtlas generateTextureAtlas(TextureFilter minFilter, TextureFilter magFilter, boolean useMipMaps) {
		TextureAtlas atlas = new TextureAtlas();
		updateTextureAtlas(atlas, minFilter, magFilter, useMipMaps);
		return atlas;
	}
	
	public synchronized void updateTextureAtlas(TextureAtlas atlas, TextureFilter minFilter, TextureFilter magFilter, boolean useMipMaps) {
		updateTextureAtlas(atlas, minFilter, magFilter, useMipMaps, true);
	}
	
	public synchronized void updateTextureAtlas(TextureAtlas atlas, TextureFilter minFilter, TextureFilter magFilter, boolean useMipMaps, boolean useIndexes) {
		updatePageTextures(minFilter, magFilter, useMipMaps);
		for (Page page : pages) {
			if (page.addedRects.size > 0) {
				for (String name : page.addedRects) {
					PixmapPackerRectangle rect = page.rects.get(name);
					TextureAtlas.AtlasRegion region = new TextureAtlas.AtlasRegion(page.texture, (int) rect.x, (int) rect.y,
							(int) rect.width, (int) rect.height);
					
					if (rect.splits != null) {
						region.names = new String[] {"split", "pad"};
						region.values = new int[][] {rect.splits, rect.pads};
					}
					
					int imageIndex = -1;
					String imageName = name;
					
					if (useIndexes) {
						Matcher matcher = indexPattern.matcher(imageName);
						if (matcher.matches()) {
							imageName = matcher.group(1);
							imageIndex = Integer.parseInt(matcher.group(2));
						}
					}
					
					region.name = imageName;
					region.index = imageIndex;
					region.offsetX = rect.offsetX;
					region.offsetY = (int) (rect.originalHeight - rect.height - rect.offsetY);
					region.originalWidth = rect.originalWidth;
					region.originalHeight = rect.originalHeight;
					
					atlas.getRegions().add(region);
				}
				page.addedRects.clear();
				atlas.getTextures().add(page.texture);
			}
		}
	}
	
	public synchronized void updateTextureRegions(Array<TextureRegion> regions, TextureFilter minFilter, TextureFilter magFilter, boolean useMipMaps) {
		updatePageTextures(minFilter, magFilter, useMipMaps);
		while (regions.size < pages.size)
			regions.add(new TextureRegion(pages.get(regions.size).texture));
	}
	
	public synchronized void updatePageTextures(TextureFilter minFilter, TextureFilter magFilter, boolean useMipMaps) {
		for (Page page : pages)
			page.updateTexture(minFilter, magFilter, useMipMaps);
	}
	
	public int getPageWidth() {
		return pageWidth;
	}
	
	public void setPageWidth(int pageWidth) {
		this.pageWidth = pageWidth;
	}
	
	public int getPageHeight() {
		return pageHeight;
	}
	
	public void setPageHeight(int pageHeight) {
		this.pageHeight = pageHeight;
	}
	
	public Format getPageFormat() {
		return pageFormat;
	}
	
	public void setPageFormat(Format pageFormat) {
		this.pageFormat = pageFormat;
	}
	
	public int getPadding() {
		return padding;
	}
	
	public void setPadding(int padding) {
		this.padding = padding;
	}
	
	public boolean getDuplicateBorder() {
		return duplicateBorder;
	}
	
	public void setDuplicateBorder(boolean duplicateBorder) {
		this.duplicateBorder = duplicateBorder;
	}
	
	public boolean getPackToTexture() {
		return packToTexture;
	}
	
	public void setPackToTexture(boolean packToTexture) {
		this.packToTexture = packToTexture;
	}
	
	public static class Page {
		
		OrderedMap<String, PixmapPackerRectangle> rects = new OrderedMap<>();
		Pixmap image;
		Texture texture;
		final Array<String> addedRects = new Array<>();
		boolean dirty;
		
		public Page(PixmapPacker packer) {
			image = new Pixmap(packer.pageWidth, packer.pageHeight, packer.pageFormat);
			image.setBlending(Blending.None);
			image.setColor(packer.getTransparentColor());
			image.fill();
		}
		
		public Pixmap getPixmap() {
			return image;
		}
		
		public OrderedMap<String, PixmapPackerRectangle> getRects() {
			return rects;
		}
		
		public Texture getTexture() {
			return texture;
		}
		
		public boolean updateTexture(TextureFilter minFilter, TextureFilter magFilter, boolean useMipMaps) {
			if (texture != null) {
				if (!dirty)
					return false;
				texture.load(texture.getTextureData());
			} else {
				texture = new Texture(new PixmapTextureData(image, image.getFormat(), useMipMaps, false, true)) {
					@Override
					public void dispose() {
						super.dispose();
						image.dispose();
					}
				};
				texture.setFilter(minFilter, magFilter);
			}
			dirty = false;
			return true;
		}
		
	}
	
	public interface PackStrategy {
		
		void sort(Array<Pixmap> images);
		
		Page pack(PixmapPacker packer, String name, Rectangle rect);
		
	}
	
	public static class GuillotineStrategy implements PackStrategy {
		
		Comparator<Pixmap> comparator;
		
		public void sort(Array<Pixmap> pixmaps) {
			if (comparator == null) {
				comparator = new Comparator<Pixmap>() {
					public int compare(Pixmap o1, Pixmap o2) {
						return Math.max(o1.getWidth(), o1.getHeight()) - Math.max(o2.getWidth(), o2.getHeight());
					}
				};
			}
			pixmaps.sort(comparator);
		}
		
		public Page pack(PixmapPacker packer, String name, Rectangle rect) {
			GuillotinePage page;
			if (packer.pages.size == 0) {
				// Add a page if empty.
				page = new GuillotinePage(packer);
				packer.pages.add(page);
			} else {
				// Always try to pack into the last page.
				page = (GuillotinePage) packer.pages.peek();
			}
			
			int padding = packer.padding;
			rect.width += padding;
			rect.height += padding;
			Node node = insert(page.root, rect);
			if (node == null) {
				// Didn't fit, pack into a new page.
				page = new GuillotinePage(packer);
				packer.pages.add(page);
				node = insert(page.root, rect);
			}
			node.full = true;
			rect.set(node.rect.x, node.rect.y, node.rect.width - padding, node.rect.height - padding);
			return page;
		}
		
		private Node insert(Node node, Rectangle rect) {
			if (!node.full && node.leftChild != null && node.rightChild != null) {
				Node newNode = insert(node.leftChild, rect);
				if (newNode == null)
					newNode = insert(node.rightChild, rect);
				return newNode;
			} else {
				if (node.full)
					return null;
				if (node.rect.width == rect.width && node.rect.height == rect.height)
					return node;
				if (node.rect.width < rect.width || node.rect.height < rect.height)
					return null;
				
				node.leftChild = new Node();
				node.rightChild = new Node();
				
				int deltaWidth = (int) node.rect.width - (int) rect.width;
				int deltaHeight = (int) node.rect.height - (int) rect.height;
				node.leftChild.rect.x = node.rect.x;
				node.leftChild.rect.y = node.rect.y;
				if (deltaWidth > deltaHeight) {
					node.leftChild.rect.width = rect.width;
					node.leftChild.rect.height = node.rect.height;
					
					node.rightChild.rect.x = node.rect.x + rect.width;
					node.rightChild.rect.y = node.rect.y;
					node.rightChild.rect.width = node.rect.width - rect.width;
					node.rightChild.rect.height = node.rect.height;
				} else {
					node.leftChild.rect.width = node.rect.width;
					node.leftChild.rect.height = rect.height;
					
					node.rightChild.rect.x = node.rect.x;
					node.rightChild.rect.y = node.rect.y + rect.height;
					node.rightChild.rect.width = node.rect.width;
					node.rightChild.rect.height = node.rect.height - rect.height;
				}
				
				return insert(node.leftChild, rect);
			}
		}
		
		static final class Node {
			
			public Node leftChild;
			public Node rightChild;
			public final Rectangle rect = new Rectangle();
			public boolean full;
			
		}
		
		static class GuillotinePage extends Page {
			
			Node root;
			
			public GuillotinePage(PixmapPacker packer) {
				super(packer);
				root = new Node();
				root.rect.x = packer.padding;
				root.rect.y = packer.padding;
				root.rect.width = packer.pageWidth - packer.padding * 2;
				root.rect.height = packer.pageHeight - packer.padding * 2;
			}
			
		}
		
	}
	
	/**
	 * Does bin packing by inserting in rows. This is good at packing images that have similar heights.
	 *
	 * @author Nathan Sweet
	 */
	public static class SkylineStrategy implements PackStrategy {
		
		Comparator<Pixmap> comparator;
		
		public void sort(Array<Pixmap> images) {
			if (comparator == null) {
				comparator = new Comparator<Pixmap>() {
					public int compare(Pixmap o1, Pixmap o2) {
						return o1.getHeight() - o2.getHeight();
					}
				};
			}
			images.sort(comparator);
		}
		
		public Page pack(PixmapPacker packer, String name, Rectangle rect) {
			int padding = packer.padding;
			int pageWidth = packer.pageWidth - padding * 2, pageHeight = packer.pageHeight - padding * 2;
			int rectWidth = (int) rect.width + padding, rectHeight = (int) rect.height + padding;
			for (int i = 0, n = packer.pages.size; i < n; i++) {
				SkylinePage page = (SkylinePage) packer.pages.get(i);
				Row bestRow = null;
				// Fit in any row before the last.
				for (int ii = 0, nn = page.rows.size - 1; ii < nn; ii++) {
					Row row = page.rows.get(ii);
					if (row.x + rectWidth >= pageWidth)
						continue;
					if (row.y + rectHeight >= pageHeight)
						continue;
					if (rectHeight > row.height)
						continue;
					if (bestRow == null || row.height < bestRow.height)
						bestRow = row;
				}
				if (bestRow == null) {
					// Fit in last row, increasing height.
					Row row = page.rows.peek();
					if (row.y + rectHeight >= pageHeight)
						continue;
					if (row.x + rectWidth < pageWidth) {
						row.height = Math.max(row.height, rectHeight);
						bestRow = row;
					} else if (row.y + row.height + rectHeight < pageHeight) {
						// Fit in new row.
						bestRow = new Row();
						bestRow.y = row.y + row.height;
						bestRow.height = rectHeight;
						page.rows.add(bestRow);
					}
				}
				if (bestRow != null) {
					rect.x = bestRow.x;
					rect.y = bestRow.y;
					bestRow.x += rectWidth;
					return page;
				}
			}
			// Fit in new page.
			SkylinePage page = new SkylinePage(packer);
			packer.pages.add(page);
			Row row = new Row();
			row.x = padding + rectWidth;
			row.y = padding;
			row.height = rectHeight;
			page.rows.add(row);
			rect.x = padding;
			rect.y = padding;
			return page;
		}
		
		static class SkylinePage extends Page {
			
			Array<Row> rows = new Array<>();
			
			public SkylinePage(PixmapPacker packer) {
				super(packer);
				
			}
			
			static class Row {
				
				int x, y, height;
				
			}
			
		}
		
	}
	
	public Color getTransparentColor() {
		return this.transparentColor;
	}
	
	public void setTransparentColor(Color color) {
		this.transparentColor.set(color);
	}
	
	private int[] getSplits(Pixmap raster) {
		
		int startX = getSplitPoint(raster, 1, 0, true, true);
		int endX = getSplitPoint(raster, startX, 0, false, true);
		int startY = getSplitPoint(raster, 0, 1, true, false);
		int endY = getSplitPoint(raster, 0, startY, false, false);
		
		// Ensure pixels after the end are not invalid.
		getSplitPoint(raster, endX + 1, 0, true, true);
		getSplitPoint(raster, 0, endY + 1, true, false);
		
		// No splits, or all splits.
		if (startX == 0 && endX == 0 && startY == 0 && endY == 0)
			return null;
		
		// Subtraction here is because the coordinates were computed before the 1px border was stripped.
		if (startX != 0) {
			startX--;
			endX = raster.getWidth() - 2 - (endX - 1);
		} else {
			// If no start point was ever found, we assume full stretch.
			endX = raster.getWidth() - 2;
		}
		if (startY != 0) {
			startY--;
			endY = raster.getHeight() - 2 - (endY - 1);
		} else {
			// If no start point was ever found, we assume full stretch.
			endY = raster.getHeight() - 2;
		}
		
		return new int[] {startX, endX, startY, endY};
	}
	
	private int[] getPads(Pixmap raster, int[] splits) {
		
		int bottom = raster.getHeight() - 1;
		int right = raster.getWidth() - 1;
		
		int startX = getSplitPoint(raster, 1, bottom, true, true);
		int startY = getSplitPoint(raster, right, 1, true, false);
		
		// No need to hunt for the end if a start was never found.
		int endX = 0;
		int endY = 0;
		if (startX != 0)
			endX = getSplitPoint(raster, startX + 1, bottom, false, true);
		if (startY != 0)
			endY = getSplitPoint(raster, right, startY + 1, false, false);
		
		// Ensure pixels after the end are not invalid.
		getSplitPoint(raster, endX + 1, bottom, true, true);
		getSplitPoint(raster, right, endY + 1, true, false);
		
		// No pads.
		if (startX == 0 && startY == 0) {
			return null;
		}
		
		// -2 here is because the coordinates were computed before the 1px border was stripped.
		if (startX == 0) {
			startX = -1;
			endX = -1;
		} else {
			if (startX > 0) {
				startX--;
				endX = raster.getWidth() - 2 - (endX - 1);
			} else {
				// If no start point was ever found, we assume full stretch.
				endX = raster.getWidth() - 2;
			}
		}
		if (startY == 0) {
			startY = -1;
			endY = -1;
		} else {
			if (startY > 0) {
				startY--;
				endY = raster.getHeight() - 2 - (endY - 1);
			} else {
				// If no start point was ever found, we assume full stretch.
				endY = raster.getHeight() - 2;
			}
		}
		
		int[] pads = new int[] {startX, endX, startY, endY};
		
		if (splits != null && Arrays.equals(pads, splits)) {
			return null;
		}
		
		return pads;
	}
	
	private final Color c = new Color();
	
	private int getSplitPoint(Pixmap raster, int startX, int startY, boolean startPoint, boolean xAxis) {
		int[] rgba = new int[4];
		
		int next = xAxis ? startX : startY;
		int end = xAxis ? raster.getWidth() : raster.getHeight();
		int breakA = startPoint ? 255 : 0;
		
		int x = startX;
		int y = startY;
		while (next != end) {
			if (xAxis)
				x = next;
			else
				y = next;
			
			int colint = raster.getPixel(x, y);
			c.set(colint);
			rgba[0] = (int) (c.r * 255);
			rgba[1] = (int) (c.g * 255);
			rgba[2] = (int) (c.b * 255);
			rgba[3] = (int) (c.a * 255);
			if (rgba[3] == breakA)
				return next;
			
			if (!startPoint && (rgba[0] != 0 || rgba[1] != 0 || rgba[2] != 0 || rgba[3] != 255))
				System.out.println(x + "  " + y + " " + Arrays.toString(rgba) + " ");
			
			next++;
		}
		
		return 0;
	}
	
	public static class PixmapPackerRectangle extends Rectangle {
		
		public Page page;
		public int[] splits;
		public int[] pads;
		public int offsetX, offsetY;
		public int originalWidth, originalHeight;
		
		PixmapPackerRectangle(int x, int y, int width, int height) {
			super(x, y, width, height);
			this.offsetX = 0;
			this.offsetY = 0;
			this.originalWidth = width;
			this.originalHeight = height;
		}
		
		PixmapPackerRectangle(int x, int y, int width, int height, int left, int top, int originalWidth, int originalHeight) {
			super(x, y, width, height);
			this.offsetX = left;
			this.offsetY = top;
			this.originalWidth = originalWidth;
			this.originalHeight = originalHeight;
		}
		
	}
	
}
