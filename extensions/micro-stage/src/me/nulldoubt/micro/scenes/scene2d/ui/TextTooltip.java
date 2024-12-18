package me.nulldoubt.micro.scenes.scene2d.ui;

import me.nulldoubt.micro.scenes.scene2d.ui.Label.LabelStyle;
import me.nulldoubt.micro.scenes.scene2d.utils.Drawable;

public class TextTooltip extends Tooltip<Label> {
	
	public TextTooltip(String text, Skin skin) {
		this(text, TooltipManager.getInstance(), skin.get(TextTooltipStyle.class));
	}
	
	public TextTooltip(String text, Skin skin, String styleName) {
		this(text, TooltipManager.getInstance(), skin.get(styleName, TextTooltipStyle.class));
	}
	
	public TextTooltip(String text, TextTooltipStyle style) {
		this(text, TooltipManager.getInstance(), style);
	}
	
	public TextTooltip(String text, TooltipManager manager, Skin skin) {
		this(text, manager, skin.get(TextTooltipStyle.class));
	}
	
	public TextTooltip(String text, TooltipManager manager, Skin skin, String styleName) {
		this(text, manager, skin.get(styleName, TextTooltipStyle.class));
	}
	
	public TextTooltip(String text, final TooltipManager manager, TextTooltipStyle style) {
		super(null, manager);
		
		container.setActor(newLabel(text, style.label));
		
		setStyle(style);
	}
	
	protected Label newLabel(String text, LabelStyle style) {
		return new Label(text, style);
	}
	
	public void setStyle(TextTooltipStyle style) {
		if (style == null)
			throw new NullPointerException("style cannot be null");
		container.setBackground(style.background);
		container.maxWidth(style.wrapWidth);
		
		boolean wrap = style.wrapWidth != 0;
		container.fill(wrap);
		
		Label label = container.getActor();
		label.setStyle(style.label);
		label.setWrap(wrap);
	}
	
	/**
	 * The style for a text tooltip, see {@link TextTooltip}.
	 *
	 * @author Nathan Sweet
	 */
	public static class TextTooltipStyle {
		
		public LabelStyle label;
		public Drawable background;
		/**
		 * 0 means don't wrap.
		 */
		public float wrapWidth;
		
		public TextTooltipStyle() {
		}
		
		public TextTooltipStyle(LabelStyle label, Drawable background) {
			this.label = label;
			this.background = background;
		}
		
		public TextTooltipStyle(TextTooltipStyle style) {
			label = new LabelStyle(style.label);
			background = style.background;
			wrapWidth = style.wrapWidth;
		}
		
	}
	
}
