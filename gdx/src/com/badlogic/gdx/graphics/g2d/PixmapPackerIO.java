package com.badlogic.gdx.graphics.g2d;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.PixmapPacker.Page;

import java.io.IOException;
import java.io.Writer;
import java.util.regex.Matcher;

import static com.badlogic.gdx.graphics.g2d.PixmapPacker.indexPattern;

public class PixmapPackerIO {
	
	public static class SaveParameters {
		
		public TextureFilter minFilter = TextureFilter.Nearest;
		public TextureFilter magFilter = TextureFilter.Nearest;
		public boolean useIndexes;
		
	}
	
	public void save(FileHandle file, PixmapPacker packer) throws IOException {
		save(file, packer, new SaveParameters());
	}
	
	public void save(FileHandle file, PixmapPacker packer, SaveParameters parameters) throws IOException {
		Writer writer = file.writer(false);
		int index = 0;
		for (Page page : packer.pages) {
			if (page.rects.size > 0) {
				FileHandle pageFile = file.sibling(file.nameWithoutExtension() + "_" + (++index) + ".png");
				PixmapIO.writePNG(pageFile, page.image);
				writer.write("\n");
				writer.write(pageFile.name() + "\n");
				writer.write("size: " + page.image.getWidth() + "," + page.image.getHeight() + "\n");
				writer.write("format: " + packer.pageFormat.name() + "\n");
				writer.write("filter: " + parameters.minFilter.name() + "," + parameters.magFilter.name() + "\n");
				writer.write("repeat: none" + "\n");
				for (String name : page.rects.keys()) {
					int imageIndex = -1;
					String imageName = name;
					if (parameters.useIndexes) {
						Matcher matcher = indexPattern.matcher(imageName);
						if (matcher.matches()) {
							imageName = matcher.group(1);
							imageIndex = Integer.parseInt(matcher.group(2));
						}
					}
					writer.write(imageName + "\n");
					PixmapPacker.PixmapPackerRectangle rect = page.rects.get(name);
					writer.write("  rotate: false" + "\n");
					writer.write("  xy: " + (int) rect.x + "," + (int) rect.y + "\n");
					writer.write("  size: " + (int) rect.width + "," + (int) rect.height + "\n");
					if (rect.splits != null) {
						writer.write(
								"  split: " + rect.splits[0] + ", " + rect.splits[1] + ", " + rect.splits[2] + ", " + rect.splits[3] + "\n");
						if (rect.pads != null) {
							writer
									.write("  pad: " + rect.pads[0] + ", " + rect.pads[1] + ", " + rect.pads[2] + ", " + rect.pads[3] + "\n");
						}
					}
					writer.write("  orig: " + rect.originalWidth + ", " + rect.originalHeight + "\n");
					writer.write("  offset: " + rect.offsetX + ", " + (int) (rect.originalHeight - rect.height - rect.offsetY) + "\n");
					
					writer.write("  index: " + imageIndex + "\n");
				}
			}
		}
		writer.close();
	}
	
}
