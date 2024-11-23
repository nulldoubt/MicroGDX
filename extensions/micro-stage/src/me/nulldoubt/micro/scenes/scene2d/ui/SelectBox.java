package me.nulldoubt.micro.scenes.scene2d.ui;

import me.nulldoubt.micro.Input.Keys;
import me.nulldoubt.micro.graphics.Color;
import me.nulldoubt.micro.graphics.g2d.Batch;
import me.nulldoubt.micro.graphics.g2d.BitmapFont;
import me.nulldoubt.micro.graphics.g2d.GlyphLayout;
import me.nulldoubt.micro.math.Interpolation;
import me.nulldoubt.micro.math.Vector2;
import me.nulldoubt.micro.scenes.scene2d.*;
import me.nulldoubt.micro.scenes.scene2d.ui.List.ListStyle;
import me.nulldoubt.micro.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import me.nulldoubt.micro.scenes.scene2d.utils.ArraySelection;
import me.nulldoubt.micro.scenes.scene2d.utils.ClickListener;
import me.nulldoubt.micro.scenes.scene2d.utils.Disableable;
import me.nulldoubt.micro.scenes.scene2d.utils.Drawable;
import me.nulldoubt.micro.utils.Align;
import me.nulldoubt.micro.utils.collections.Array;
import me.nulldoubt.micro.utils.collections.ObjectSet;
import me.nulldoubt.micro.utils.pools.Pool;
import me.nulldoubt.micro.utils.pools.Pools;

import static me.nulldoubt.micro.scenes.scene2d.actions.Actions.*;

public class SelectBox<T> extends Widget implements Disableable {
	
	static final Vector2 temp = new Vector2();
	
	SelectBoxStyle style;
	final Array<T> items = new Array<>();
	SelectBoxScrollPane<T> scrollPane;
	private float prefWidth, prefHeight;
	private final ClickListener clickListener;
	boolean disabled;
	private int alignment = Align.left;
	boolean selectedPrefWidth;
	
	final ArraySelection<T> selection = new ArraySelection(items) {
		public boolean fireChangeEvent() {
			if (selectedPrefWidth)
				invalidateHierarchy();
			return super.fireChangeEvent();
		}
	};
	
	public SelectBox(Skin skin) {
		this(skin.get(SelectBoxStyle.class));
	}
	
	public SelectBox(Skin skin, String styleName) {
		this(skin.get(styleName, SelectBoxStyle.class));
	}
	
	public SelectBox(SelectBoxStyle style) {
		setStyle(style);
		setSize(getPrefWidth(), getPrefHeight());
		
		selection.setActor(this);
		selection.setRequired(true);
		
		scrollPane = newScrollPane();
		
		addListener(clickListener = new ClickListener() {
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if (pointer == 0 && button != 0)
					return false;
				if (isDisabled())
					return false;
				if (scrollPane.hasParent())
					hideScrollPane();
				else
					showScrollPane();
				return true;
			}
		});
	}
	
	protected SelectBoxScrollPane<T> newScrollPane() {
		return new SelectBoxScrollPane(this);
	}
	
	public void setMaxListCount(int maxListCount) {
		scrollPane.maxListCount = maxListCount;
	}
	
	public int getMaxListCount() {
		return scrollPane.maxListCount;
	}
	
	protected void setStage(Stage stage) {
		if (stage == null)
			scrollPane.hide();
		super.setStage(stage);
	}
	
	public void setStyle(SelectBoxStyle style) {
		if (style == null)
			throw new IllegalArgumentException("style cannot be null.");
		this.style = style;
		
		if (scrollPane != null) {
			scrollPane.setStyle(style.scrollStyle);
			scrollPane.list.setStyle(style.listStyle);
		}
		invalidateHierarchy();
	}
	
	public SelectBoxStyle getStyle() {
		return style;
	}
	
	public void setItems(T... newItems) {
		if (newItems == null)
			throw new IllegalArgumentException("newItems cannot be null.");
		float oldPrefWidth = getPrefWidth();
		
		items.clear();
		items.addAll(newItems);
		selection.validate();
		scrollPane.list.setItems(items);
		
		invalidate();
		if (oldPrefWidth != getPrefWidth())
			invalidateHierarchy();
	}
	
	public void setItems(Array<T> newItems) {
		if (newItems == null)
			throw new IllegalArgumentException("newItems cannot be null.");
		float oldPrefWidth = getPrefWidth();
		
		if (newItems != items) {
			items.clear();
			items.addAll(newItems);
		}
		selection.validate();
		scrollPane.list.setItems(items);
		
		invalidate();
		if (oldPrefWidth != getPrefWidth())
			invalidateHierarchy();
	}
	
	public void clearItems() {
		if (items.size == 0)
			return;
		items.clear();
		selection.clear();
		scrollPane.list.clearItems();
		invalidateHierarchy();
	}
	
	public Array<T> getItems() {
		return items;
	}
	
	public void layout() {
		Drawable bg = style.background;
		BitmapFont font = style.font;
		
		if (bg != null) {
			prefHeight = Math.max(bg.getTopHeight() + bg.getBottomHeight() + font.getCapHeight() - font.getDescent() * 2,
					bg.getMinHeight());
		} else
			prefHeight = font.getCapHeight() - font.getDescent() * 2;
		
		Pool<GlyphLayout> layoutPool = Pools.get(GlyphLayout.class, GlyphLayout::new);
		GlyphLayout layout = layoutPool.obtain();
		if (selectedPrefWidth) {
			prefWidth = 0;
			if (bg != null)
				prefWidth = bg.getLeftWidth() + bg.getRightWidth();
			T selected = getSelected();
			if (selected != null) {
				layout.setText(font, toString(selected));
				prefWidth += layout.width;
			}
		} else {
			float maxItemWidth = 0;
			for (int i = 0; i < items.size; i++) {
				layout.setText(font, toString(items.get(i)));
				maxItemWidth = Math.max(layout.width, maxItemWidth);
			}
			
			prefWidth = maxItemWidth;
			if (bg != null)
				prefWidth = Math.max(prefWidth + bg.getLeftWidth() + bg.getRightWidth(), bg.getMinWidth());
			
			ListStyle listStyle = style.listStyle;
			ScrollPaneStyle scrollStyle = style.scrollStyle;
			float scrollWidth = maxItemWidth + listStyle.selection.getLeftWidth() + listStyle.selection.getRightWidth();
			bg = scrollStyle.background;
			if (bg != null)
				scrollWidth = Math.max(scrollWidth + bg.getLeftWidth() + bg.getRightWidth(), bg.getMinWidth());
			if (scrollPane == null || !scrollPane.disableY) {
				scrollWidth += Math.max(style.scrollStyle.vScroll != null ? style.scrollStyle.vScroll.getMinWidth() : 0,
						style.scrollStyle.vScrollKnob != null ? style.scrollStyle.vScrollKnob.getMinWidth() : 0);
			}
			prefWidth = Math.max(prefWidth, scrollWidth);
		}
		layoutPool.free(layout);
	}
	
	protected Drawable getBackgroundDrawable() {
		if (isDisabled() && style.backgroundDisabled != null)
			return style.backgroundDisabled;
		if (scrollPane.hasParent() && style.backgroundOpen != null)
			return style.backgroundOpen;
		if (isOver() && style.backgroundOver != null)
			return style.backgroundOver;
		return style.background;
	}
	
	protected Color getFontColor() {
		if (isDisabled() && style.disabledFontColor != null)
			return style.disabledFontColor;
		if (style.overFontColor != null && (isOver() || scrollPane.hasParent()))
			return style.overFontColor;
		return style.fontColor;
	}
	
	public void draw(Batch batch, float parentAlpha) {
		validate();
		
		Drawable background = getBackgroundDrawable();
		Color fontColor = getFontColor();
		BitmapFont font = style.font;
		
		Color color = getColor();
		float x = getX(), y = getY();
		float width = getWidth(), height = getHeight();
		
		batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
		if (background != null)
			background.draw(batch, x, y, width, height);
		
		T selected = selection.first();
		if (selected != null) {
			if (background != null) {
				width -= background.getLeftWidth() + background.getRightWidth();
				height -= background.getBottomHeight() + background.getTopHeight();
				x += background.getLeftWidth();
				y += (int) (height / 2 + background.getBottomHeight() + font.getData().capHeight / 2);
			} else {
				y += (int) (height / 2 + font.getData().capHeight / 2);
			}
			font.setColor(fontColor.r, fontColor.g, fontColor.b, fontColor.a * parentAlpha);
			drawItem(batch, font, selected, x, y, width);
		}
	}
	
	protected GlyphLayout drawItem(Batch batch, BitmapFont font, T item, float x, float y, float width) {
		String string = toString(item);
		return font.draw(batch, string, x, y, 0, string.length(), width, alignment, false, "...");
	}
	
	public void setAlignment(int alignment) {
		this.alignment = alignment;
	}
	
	public ArraySelection<T> getSelection() {
		return selection;
	}
	
	public T getSelected() {
		return selection.first();
	}
	
	public void setSelected(T item) {
		if (items.contains(item, false))
			selection.set(item);
		else if (items.size > 0)
			selection.set(items.first());
		else
			selection.clear();
	}
	
	public int getSelectedIndex() {
		ObjectSet<T> selected = selection.items();
		return selected.size == 0 ? -1 : items.indexOf(selected.first(), false);
	}
	
	public void setSelectedIndex(int index) {
		selection.set(items.get(index));
	}
	
	public void setSelectedPrefWidth(boolean selectedPrefWidth) {
		this.selectedPrefWidth = selectedPrefWidth;
	}
	
	public boolean getSelectedPrefWidth() {
		return selectedPrefWidth;
	}
	
	public float getMaxSelectedPrefWidth() {
		Pool<GlyphLayout> layoutPool = Pools.get(GlyphLayout.class, GlyphLayout::new);
		GlyphLayout layout = layoutPool.obtain();
		float width = 0;
		for (int i = 0; i < items.size; i++) {
			layout.setText(style.font, toString(items.get(i)));
			width = Math.max(layout.width, width);
		}
		Drawable bg = style.background;
		if (bg != null)
			width = Math.max(width + bg.getLeftWidth() + bg.getRightWidth(), bg.getMinWidth());
		return width;
	}
	
	public void setDisabled(boolean disabled) {
		if (disabled && !this.disabled)
			hideScrollPane();
		this.disabled = disabled;
	}
	
	public boolean isDisabled() {
		return disabled;
	}
	
	public float getPrefWidth() {
		validate();
		return prefWidth;
	}
	
	public float getPrefHeight() {
		validate();
		return prefHeight;
	}
	
	protected String toString(T item) {
		return item.toString();
	}
	
	/**
	 * @deprecated Use {@link #showScrollPane()}.
	 */
	@Deprecated
	public void showList() {
		showScrollPane();
	}
	
	public void showScrollPane() {
		if (items.size == 0)
			return;
		if (getStage() != null)
			scrollPane.show(getStage());
	}
	
	public void hideScrollPane() {
		scrollPane.hide();
	}
	
	public List<T> getList() {
		return scrollPane.list;
	}
	
	public void setScrollingDisabled(boolean y) {
		scrollPane.setScrollingDisabled(true, y);
		invalidateHierarchy();
	}
	
	public SelectBoxScrollPane<T> getScrollPane() {
		return scrollPane;
	}
	
	public boolean isOver() {
		return clickListener.isOver();
	}
	
	public ClickListener getClickListener() {
		return clickListener;
	}
	
	protected void onShow(Actor scrollPane, boolean below) {
		scrollPane.getColor().a = 0;
		scrollPane.addAction(fadeIn(0.3f, Interpolation.fade));
	}
	
	protected void onHide(Actor scrollPane) {
		scrollPane.getColor().a = 1;
		scrollPane.addAction(sequence(fadeOut(0.15f, Interpolation.fade), removeActor()));
	}
	
	public static class SelectBoxScrollPane<T> extends ScrollPane {
		
		final SelectBox<T> selectBox;
		int maxListCount;
		private final Vector2 stagePosition = new Vector2();
		final List<T> list;
		private final InputListener hideListener;
		private Actor previousScrollFocus;
		
		public SelectBoxScrollPane(final SelectBox<T> selectBox) {
			super(null, selectBox.style.scrollStyle);
			this.selectBox = selectBox;
			
			setOverscroll(false, false);
			setFadeScrollBars(false);
			setScrollingDisabled(true, false);
			
			list = newList();
			list.setTouchable(Touchable.disabled);
			list.setTypeToSelect(true);
			setActor(list);
			
			list.addListener(new ClickListener() {
				public void clicked(InputEvent event, float x, float y) {
					T selected = list.getSelected();
					// Force clicking the already selected item to trigger a change event.
					if (selected != null)
						selectBox.selection.items().clear(51);
					selectBox.selection.choose(selected);
					hide();
				}
				
				public boolean mouseMoved(InputEvent event, float x, float y) {
					int index = list.getItemIndexAt(y);
					if (index != -1)
						list.setSelectedIndex(index);
					return true;
				}
			});
			
			addListener(new InputListener() {
				public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
					if (toActor == null || !isAscendantOf(toActor)) {
						T selected = selectBox.getSelected();
						if (selected != null)
							list.selection.set(selected);
					}
				}
			});
			
			hideListener = new InputListener() {
				public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
					Actor target = event.getTarget();
					if (isAscendantOf(target))
						return false;
					list.selection.set(selectBox.getSelected());
					hide();
					return false;
				}
				
				public boolean keyDown(InputEvent event, int keycode) {
					switch (keycode) {
						case Keys.NUMPAD_ENTER:
						case Keys.ENTER:
							selectBox.selection.choose(list.getSelected());
							// Fall thru.
						case Keys.ESCAPE:
							hide();
							event.stop();
							return true;
					}
					return false;
				}
			};
		}
		
		protected List<T> newList() {
			return new List<T>(selectBox.style.listStyle) {
				public String toString(T obj) {
					return selectBox.toString(obj);
				}
			};
		}
		
		public void show(Stage stage) {
			if (list.isTouchable())
				return;
			
			stage.addActor(this);
			stage.addCaptureListener(hideListener);
			stage.addListener(list.getKeyListener());
			
			selectBox.localToStageCoordinates(stagePosition.set(0, 0));
			
			// Show the list above or below the select box, limited to a number of items and the available height in the stage.
			float itemHeight = list.getItemHeight();
			float height = itemHeight * (maxListCount <= 0 ? selectBox.items.size : Math.min(maxListCount, selectBox.items.size));
			Drawable scrollPaneBackground = getStyle().background;
			if (scrollPaneBackground != null)
				height += scrollPaneBackground.getTopHeight() + scrollPaneBackground.getBottomHeight();
			Drawable listBackground = list.getStyle().background;
			if (listBackground != null)
				height += listBackground.getTopHeight() + listBackground.getBottomHeight();
			
			float heightBelow = stagePosition.y;
			float heightAbove = stage.getHeight() - heightBelow - selectBox.getHeight();
			boolean below = true;
			if (height > heightBelow) {
				if (heightAbove > heightBelow) {
					below = false;
					height = Math.min(height, heightAbove);
				} else
					height = heightBelow;
			}
			
			if (below)
				setY(stagePosition.y - height);
			else
				setY(stagePosition.y + selectBox.getHeight());
			
			setHeight(height);
			validate();
			float width = Math.max(getPrefWidth(), selectBox.getWidth());
			setWidth(width);
			
			float x = stagePosition.x;
			if (x + width > stage.getWidth()) {
				x -= getWidth() - selectBox.getWidth() - 1;
				if (x < 0)
					x = 0;
			}
			setX(x);
			
			validate();
			scrollTo(0, list.getHeight() - selectBox.getSelectedIndex() * itemHeight - itemHeight / 2, 0, 0, true, true);
			updateVisualScroll();
			
			previousScrollFocus = null;
			Actor actor = stage.getScrollFocus();
			if (actor != null && !actor.isDescendantOf(this))
				previousScrollFocus = actor;
			stage.setScrollFocus(this);
			
			list.selection.set(selectBox.getSelected());
			list.setTouchable(Touchable.enabled);
			clearActions();
			selectBox.onShow(this, below);
		}
		
		public void hide() {
			if (!list.isTouchable() || !hasParent())
				return;
			list.setTouchable(Touchable.disabled);
			
			Stage stage = getStage();
			if (stage != null) {
				stage.removeCaptureListener(hideListener);
				stage.removeListener(list.getKeyListener());
				if (previousScrollFocus != null && previousScrollFocus.getStage() == null)
					previousScrollFocus = null;
				Actor actor = stage.getScrollFocus();
				if (actor == null || isAscendantOf(actor))
					stage.setScrollFocus(previousScrollFocus);
			}
			
			clearActions();
			selectBox.onHide(this);
		}
		
		public void draw(Batch batch, float parentAlpha) {
			selectBox.localToStageCoordinates(temp.set(0, 0));
			if (!temp.equals(stagePosition))
				hide();
			super.draw(batch, parentAlpha);
		}
		
		public void act(float delta) {
			super.act(delta);
			toFront();
		}
		
		protected void setStage(Stage stage) {
			Stage oldStage = getStage();
			if (oldStage != null) {
				oldStage.removeCaptureListener(hideListener);
				oldStage.removeListener(list.getKeyListener());
			}
			super.setStage(stage);
		}
		
		public List<T> getList() {
			return list;
		}
		
		public SelectBox<T> getSelectBox() {
			return selectBox;
		}
		
	}
	
	public static class SelectBoxStyle {
		
		public BitmapFont font;
		public Color fontColor = new Color(1, 1, 1, 1);
		public Color overFontColor, disabledFontColor;
		public Drawable background;
		public ScrollPaneStyle scrollStyle;
		public ListStyle listStyle;
		public Drawable backgroundOver, backgroundOpen, backgroundDisabled;
		
		public SelectBoxStyle() {
		}
		
		public SelectBoxStyle(BitmapFont font, Color fontColor, Drawable background, ScrollPaneStyle scrollStyle,
							  ListStyle listStyle) {
			this.font = font;
			this.fontColor.set(fontColor);
			this.background = background;
			this.scrollStyle = scrollStyle;
			this.listStyle = listStyle;
		}
		
		public SelectBoxStyle(SelectBoxStyle style) {
			font = style.font;
			fontColor.set(style.fontColor);
			
			if (style.overFontColor != null)
				overFontColor = new Color(style.overFontColor);
			if (style.disabledFontColor != null)
				disabledFontColor = new Color(style.disabledFontColor);
			
			background = style.background;
			scrollStyle = new ScrollPaneStyle(style.scrollStyle);
			listStyle = new ListStyle(style.listStyle);
			
			backgroundOver = style.backgroundOver;
			backgroundOpen = style.backgroundOpen;
			backgroundDisabled = style.backgroundDisabled;
		}
		
	}
	
}
