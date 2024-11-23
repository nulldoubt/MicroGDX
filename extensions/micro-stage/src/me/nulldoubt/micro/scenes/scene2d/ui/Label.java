package me.nulldoubt.micro.scenes.scene2d.ui;

import me.nulldoubt.micro.graphics.Color;
import me.nulldoubt.micro.graphics.g2d.Batch;
import me.nulldoubt.micro.graphics.g2d.BitmapFont;
import me.nulldoubt.micro.graphics.g2d.BitmapFontCache;
import me.nulldoubt.micro.graphics.g2d.GlyphLayout;
import me.nulldoubt.micro.scenes.scene2d.utils.Drawable;
import me.nulldoubt.micro.utils.Align;
import me.nulldoubt.micro.utils.strings.StringBuilder;

public class Label extends Widget {
	
	private static final Color tempColor = new Color();
	private static final GlyphLayout prefSizeLayout = new GlyphLayout();
	
	private LabelStyle style;
	private final GlyphLayout layout = new GlyphLayout();
	private float prefWidth, prefHeight;
	private final StringBuilder text = new StringBuilder();
	private int intValue = Integer.MIN_VALUE;
	private BitmapFontCache cache;
	private int labelAlign = Align.left;
	private int lineAlign = Align.left;
	private boolean wrap;
	private float lastPrefHeight;
	private boolean prefSizeInvalid = true;
	private float fontScaleX = 1, fontScaleY = 1;
	private boolean fontScaleChanged = false;
	private String ellipsis;
	
	public Label(CharSequence text, Skin skin) {
		this(text, skin.get(LabelStyle.class));
	}
	
	public Label(CharSequence text, Skin skin, String styleName) {
		this(text, skin.get(styleName, LabelStyle.class));
	}
	
	public Label(CharSequence text, Skin skin, String fontName, Color color) {
		this(text, new LabelStyle(skin.getFont(fontName), color));
	}
	
	public Label(CharSequence text, Skin skin, String fontName, String colorName) {
		this(text, new LabelStyle(skin.getFont(fontName), skin.getColor(colorName)));
	}
	
	public Label(CharSequence text, LabelStyle style) {
		if (text != null)
			this.text.append(text);
		setStyle(style);
		if (text != null && !text.isEmpty())
			setSize(getPrefWidth(), getPrefHeight());
	}
	
	public void setStyle(LabelStyle style) {
		if (style == null)
			throw new IllegalArgumentException("style cannot be null.");
		if (style.font == null)
			throw new IllegalArgumentException("Missing LabelStyle font.");
		this.style = style;
		
		cache = style.font.newFontCache();
		invalidateHierarchy();
	}
	
	public LabelStyle getStyle() {
		return style;
	}
	
	public boolean setText(int value) {
		if (this.intValue == value)
			return false;
		text.clear();
		text.append(value);
		intValue = value;
		invalidateHierarchy();
		return true;
	}
	
	public void setText(CharSequence newText) {
		if (newText == null) {
			if (text.length == 0)
				return;
			text.clear();
		} else if (newText instanceof StringBuilder) {
			if (text.equals(newText))
				return;
			text.clear();
			text.append((StringBuilder) newText);
		} else {
			if (textEquals(newText))
				return;
			text.clear();
			text.append(newText);
		}
		intValue = Integer.MIN_VALUE;
		invalidateHierarchy();
	}
	
	public boolean textEquals(CharSequence other) {
		int length = text.length;
		char[] chars = text.chars;
		if (length != other.length())
			return false;
		for (int i = 0; i < length; i++)
			if (chars[i] != other.charAt(i))
				return false;
		return true;
	}
	
	public StringBuilder getText() {
		return text;
	}
	
	public void invalidate() {
		super.invalidate();
		prefSizeInvalid = true;
	}
	
	private void scaleAndComputePrefSize() {
		BitmapFont font = cache.getFont();
		float oldScaleX = font.getScaleX();
		float oldScaleY = font.getScaleY();
		if (fontScaleChanged)
			font.getData().setScale(fontScaleX, fontScaleY);
		
		computePrefSize(Label.prefSizeLayout);
		
		if (fontScaleChanged)
			font.getData().setScale(oldScaleX, oldScaleY);
	}
	
	protected void computePrefSize(GlyphLayout layout) {
		prefSizeInvalid = false;
		if (wrap && ellipsis == null) {
			float width = getWidth();
			if (style.background != null) {
				width = Math.max(width, style.background.getMinWidth()) - style.background.getLeftWidth()
						- style.background.getRightWidth();
			}
			layout.setText(cache.getFont(), text, Color.WHITE, width, Align.left, true);
		} else
			layout.setText(cache.getFont(), text);
		prefWidth = layout.width;
		prefHeight = layout.height;
	}
	
	public void layout() {
		BitmapFont font = cache.getFont();
		float oldScaleX = font.getScaleX();
		float oldScaleY = font.getScaleY();
		if (fontScaleChanged)
			font.getData().setScale(fontScaleX, fontScaleY);
		
		boolean wrap = this.wrap && ellipsis == null;
		if (wrap) {
			float prefHeight = getPrefHeight();
			if (prefHeight != lastPrefHeight) {
				lastPrefHeight = prefHeight;
				invalidateHierarchy();
			}
		}
		
		float width = getWidth(), height = getHeight();
		Drawable background = style.background;
		float x = 0, y = 0;
		if (background != null) {
			x = background.getLeftWidth();
			y = background.getBottomHeight();
			width -= background.getLeftWidth() + background.getRightWidth();
			height -= background.getBottomHeight() + background.getTopHeight();
		}
		
		GlyphLayout layout = this.layout;
		float textWidth, textHeight;
		if (wrap || text.indexOf("\n") != -1) {
			layout.setText(font, text, 0, text.length, Color.WHITE, width, lineAlign, wrap, ellipsis);
			textWidth = layout.width;
			textHeight = layout.height;
			if ((labelAlign & Align.left) == 0) {
				if ((labelAlign & Align.right) != 0)
					x += width - textWidth;
				else
					x += (width - textWidth) / 2;
			}
		} else {
			textWidth = width;
			textHeight = font.getData().capHeight;
		}
		
		if ((labelAlign & Align.top) != 0) {
			y += cache.getFont().isFlipped() ? 0 : height - textHeight;
			y += style.font.getDescent();
		} else if ((labelAlign & Align.bottom) != 0) {
			y += cache.getFont().isFlipped() ? height - textHeight : 0;
			y -= style.font.getDescent();
		} else {
			y += (height - textHeight) / 2;
		}
		if (!cache.getFont().isFlipped())
			y += textHeight;
		
		layout.setText(font, text, 0, text.length, Color.WHITE, textWidth, lineAlign, wrap, ellipsis);
		cache.setText(layout, x, y);
		
		if (fontScaleChanged)
			font.getData().setScale(oldScaleX, oldScaleY);
	}
	
	public void draw(Batch batch, float parentAlpha) {
		validate();
		Color color = tempColor.set(getColor());
		color.a *= parentAlpha;
		if (style.background != null) {
			batch.setColor(color.r, color.g, color.b, color.a);
			style.background.draw(batch, getX(), getY(), getWidth(), getHeight());
		}
		if (style.fontColor != null)
			color.mul(style.fontColor);
		cache.tint(color);
		cache.setPosition(getX(), getY());
		cache.draw(batch);
	}
	
	public float getPrefWidth() {
		if (wrap)
			return 0;
		if (prefSizeInvalid)
			scaleAndComputePrefSize();
		float width = prefWidth;
		Drawable background = style.background;
		if (background != null)
			width = Math.max(width + background.getLeftWidth() + background.getRightWidth(), background.getMinWidth());
		return width;
	}
	
	public float getPrefHeight() {
		if (prefSizeInvalid)
			scaleAndComputePrefSize();
		float descentScaleCorrection = 1;
		if (fontScaleChanged)
			descentScaleCorrection = fontScaleY / style.font.getScaleY();
		float height = prefHeight - style.font.getDescent() * descentScaleCorrection * 2;
		Drawable background = style.background;
		if (background != null)
			height = Math.max(height + background.getTopHeight() + background.getBottomHeight(), background.getMinHeight());
		return height;
	}
	
	public GlyphLayout getGlyphLayout() {
		return layout;
	}
	
	public void setWrap(boolean wrap) {
		this.wrap = wrap;
		invalidateHierarchy();
	}
	
	public boolean getWrap() {
		return wrap;
	}
	
	public int getLabelAlign() {
		return labelAlign;
	}
	
	public int getLineAlign() {
		return lineAlign;
	}
	
	public void setAlignment(int alignment) {
		setAlignment(alignment, alignment);
	}
	
	public void setAlignment(int labelAlign, int lineAlign) {
		this.labelAlign = labelAlign;
		
		if ((lineAlign & Align.left) != 0)
			this.lineAlign = Align.left;
		else if ((lineAlign & Align.right) != 0)
			this.lineAlign = Align.right;
		else
			this.lineAlign = Align.center;
		
		invalidate();
	}
	
	public void setFontScale(float fontScale) {
		setFontScale(fontScale, fontScale);
	}
	
	public void setFontScale(float fontScaleX, float fontScaleY) {
		fontScaleChanged = true;
		this.fontScaleX = fontScaleX;
		this.fontScaleY = fontScaleY;
		invalidateHierarchy();
	}
	
	public float getFontScaleX() {
		return fontScaleX;
	}
	
	public void setFontScaleX(float fontScaleX) {
		setFontScale(fontScaleX, fontScaleY);
	}
	
	public float getFontScaleY() {
		return fontScaleY;
	}
	
	public void setFontScaleY(float fontScaleY) {
		setFontScale(fontScaleX, fontScaleY);
	}
	
	public void setEllipsis(String ellipsis) {
		this.ellipsis = ellipsis;
	}
	
	public void setEllipsis(boolean ellipsis) {
		if (ellipsis)
			this.ellipsis = "...";
		else
			this.ellipsis = null;
	}
	
	protected BitmapFontCache getBitmapFontCache() {
		return cache;
	}
	
	public String toString() {
		String name = getName();
		if (name != null)
			return name;
		String className = getClass().getName();
		int dotIndex = className.lastIndexOf('.');
		if (dotIndex != -1)
			className = className.substring(dotIndex + 1);
		return (className.indexOf('$') != -1 ? "Label " : "") + className + ": " + text;
	}
	
	public static class LabelStyle {
		
		public BitmapFont font;
		public Color fontColor;
		public Drawable background;
		
		public LabelStyle() {}
		
		public LabelStyle(BitmapFont font, Color fontColor) {
			this.font = font;
			this.fontColor = fontColor;
		}
		
		public LabelStyle(LabelStyle style) {
			font = style.font;
			if (style.fontColor != null)
				fontColor = new Color(style.fontColor);
			background = style.background;
		}
		
	}
	
}
