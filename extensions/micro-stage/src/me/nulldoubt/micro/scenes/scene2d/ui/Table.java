package me.nulldoubt.micro.scenes.scene2d.ui;

import me.nulldoubt.micro.graphics.Color;
import me.nulldoubt.micro.graphics.g2d.Batch;
import me.nulldoubt.micro.graphics.glutils.ShapeRenderer;
import me.nulldoubt.micro.graphics.glutils.ShapeRenderer.ShapeType;
import me.nulldoubt.micro.math.shapes.Rectangle;
import me.nulldoubt.micro.scenes.scene2d.Actor;
import me.nulldoubt.micro.scenes.scene2d.Touchable;
import me.nulldoubt.micro.scenes.scene2d.ui.Label.LabelStyle;
import me.nulldoubt.micro.scenes.scene2d.ui.Value.Fixed;
import me.nulldoubt.micro.scenes.scene2d.utils.Drawable;
import me.nulldoubt.micro.scenes.scene2d.utils.Layout;
import me.nulldoubt.micro.utils.Align;
import me.nulldoubt.micro.utils.collections.Array;
import me.nulldoubt.micro.utils.pools.Pool;
import me.nulldoubt.micro.utils.pools.Pools;

import java.util.Arrays;

public class Table extends WidgetGroup {
	
	public static Color debugTableColor = new Color(0, 0, 1, 1);
	public static Color debugCellColor = new Color(1, 0, 0, 1);
	public static Color debugActorColor = new Color(0, 1, 0, 1);
	
	static final Pool<Cell> cellPool = Pools.get(Cell.class, Cell::new);
	private static float[] columnWeightedWidth, rowWeightedHeight;
	
	private int columns, rows;
	private boolean implicitEndRow;
	
	private final Array<Cell> cells = new Array<>(4);
	private final Cell cellDefaults;
	private final Array<Cell> columnDefaults = new Array<>(2);
	private Cell rowDefaults;
	
	private boolean sizeInvalid = true;
	private float[] columnMinWidth, rowMinHeight;
	private float[] columnPrefWidth, rowPrefHeight;
	private float tableMinWidth, tableMinHeight;
	private float tablePrefWidth, tablePrefHeight;
	private float[] columnWidth, rowHeight;
	private float[] expandWidth, expandHeight;
	
	Value padTop = backgroundTop, padLeft = backgroundLeft, padBottom = backgroundBottom, padRight = backgroundRight;
	int align = Align.center;
	
	Debug debug = Debug.none;
	Array<DebugRect> debugRects;
	
	Drawable background;
	private boolean clip;
	private Skin skin;
	boolean round = true;
	
	public Table() {
		this(null);
	}
	
	public Table(Skin skin) {
		this.skin = skin;
		
		cellDefaults = obtainCell();
		
		setTransform(false);
		setTouchable(Touchable.childrenOnly);
	}
	
	private Cell obtainCell() {
		Cell cell = cellPool.obtain();
		cell.setTable(this);
		return cell;
	}
	
	public void draw(Batch batch, float parentAlpha) {
		validate();
		if (isTransform()) {
			applyTransform(batch, computeTransform());
			drawBackground(batch, parentAlpha, 0, 0);
			if (clip) {
				batch.flush();
				float padLeft = this.padLeft.get(this), padBottom = this.padBottom.get(this);
				if (clipBegin(padLeft, padBottom, getWidth() - padLeft - padRight.get(this),
						getHeight() - padBottom - padTop.get(this))) {
					drawChildren(batch, parentAlpha);
					batch.flush();
					clipEnd();
				}
			} else
				drawChildren(batch, parentAlpha);
			resetTransform(batch);
		} else {
			drawBackground(batch, parentAlpha, getX(), getY());
			super.draw(batch, parentAlpha);
		}
	}
	
	protected void drawBackground(Batch batch, float parentAlpha, float x, float y) {
		if (background == null)
			return;
		Color color = getColor();
		batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
		background.draw(batch, x, y, getWidth(), getHeight());
	}
	
	public void setBackground(String drawableName) {
		if (skin == null)
			throw new IllegalStateException("Table must have a skin set to use this method.");
		setBackground(skin.getDrawable(drawableName));
	}
	
	public void setBackground(Drawable background) {
		if (this.background == background)
			return;
		float padTopOld = getPadTop(), padLeftOld = getPadLeft(), padBottomOld = getPadBottom(), padRightOld = getPadRight();
		this.background = background;
		float padTopNew = getPadTop(), padLeftNew = getPadLeft(), padBottomNew = getPadBottom(), padRightNew = getPadRight();
		if (padTopOld + padBottomOld != padTopNew + padBottomNew || padLeftOld + padRightOld != padLeftNew + padRightNew)
			invalidateHierarchy();
		else if (padTopOld != padTopNew || padLeftOld != padLeftNew || padBottomOld != padBottomNew || padRightOld != padRightNew)
			invalidate();
	}
	
	public Table background(Drawable background) {
		setBackground(background);
		return this;
	}
	
	public Table background(String drawableName) {
		setBackground(drawableName);
		return this;
	}
	
	public Drawable getBackground() {
		return background;
	}
	
	public Actor hit(float x, float y, boolean touchable) {
		if (clip) {
			if (touchable && getTouchable() == Touchable.disabled)
				return null;
			if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight())
				return null;
		}
		return super.hit(x, y, touchable);
	}
	
	public Table clip() {
		setClip(true);
		return this;
	}
	
	public Table clip(boolean enabled) {
		setClip(enabled);
		return this;
	}
	
	public void setClip(boolean enabled) {
		clip = enabled;
		setTransform(enabled);
		invalidate();
	}
	
	public boolean getClip() {
		return clip;
	}
	
	public void invalidate() {
		sizeInvalid = true;
		super.invalidate();
	}
	
	public <T extends Actor> Cell<T> add(T actor) {
		Cell<T> cell = obtainCell();
		cell.actor = actor;
		
		if (implicitEndRow) {
			implicitEndRow = false;
			rows--;
			cells.peek().endRow = false;
		}
		
		int cellCount = cells.size;
		if (cellCount > 0) {
			Cell lastCell = cells.peek();
			if (!lastCell.endRow) {
				cell.column = lastCell.column + lastCell.colspan;
				cell.row = lastCell.row;
			} else {
				cell.column = 0;
				cell.row = lastCell.row + 1;
			}
			if (cell.row > 0) {
				Object[] cells = this.cells.items;
				outer:
				for (int i = cellCount - 1; i >= 0; i--) {
					Cell other = (Cell) cells[i];
					for (int column = other.column, nn = column + other.colspan; column < nn; column++) {
						if (column == cell.column) {
							cell.cellAboveIndex = i;
							break outer;
						}
					}
				}
			}
		} else {
			cell.column = 0;
			cell.row = 0;
		}
		cells.add(cell);
		
		cell.set(cellDefaults);
		if (cell.column < columnDefaults.size)
			cell.merge(columnDefaults.get(cell.column));
		cell.merge(rowDefaults);
		
		if (actor != null)
			addActor(actor);
		
		return cell;
	}
	
	public Table add(Actor... actors) {
		for (final Actor actor : actors)
			add(actor);
		return this;
	}
	
	public Cell<Label> add(CharSequence text) {
		if (skin == null)
			throw new IllegalStateException("Table must have a skin set to use this method.");
		return add(new Label(text, skin));
	}
	
	public Cell<Label> add(CharSequence text, String labelStyleName) {
		if (skin == null)
			throw new IllegalStateException("Table must have a skin set to use this method.");
		return add(new Label(text, skin.get(labelStyleName, LabelStyle.class)));
	}
	
	public Cell<Label> add(CharSequence text, String fontName, Color color) {
		if (skin == null)
			throw new IllegalStateException("Table must have a skin set to use this method.");
		return add(new Label(text, new LabelStyle(skin.getFont(fontName), color)));
	}
	
	public Cell<Label> add(CharSequence text, String fontName, String colorName) {
		if (skin == null)
			throw new IllegalStateException("Table must have a skin set to use this method.");
		return add(new Label(text, new LabelStyle(skin.getFont(fontName), skin.getColor(colorName))));
	}
	
	public Cell add() {
		return add((Actor) null);
	}
	
	public Cell<Stack> stack(Actor... actors) {
		final Stack stack = new Stack();
		if (actors != null)
			for (final Actor actor : actors)
				stack.addActor(actor);
		return add(stack);
	}
	
	public boolean removeActor(Actor actor) {
		return removeActor(actor, true);
	}
	
	public boolean removeActor(Actor actor, boolean unfocus) {
		if (!super.removeActor(actor, unfocus))
			return false;
		Cell cell = getCell(actor);
		if (cell != null)
			cell.actor = null;
		return true;
	}
	
	public Actor removeActorAt(int index, boolean unfocus) {
		Actor actor = super.removeActorAt(index, unfocus);
		Cell cell = getCell(actor);
		if (cell != null)
			cell.actor = null;
		return actor;
	}
	
	public void clearChildren(boolean unfocus) {
		Object[] cells = this.cells.items;
		for (int i = this.cells.size - 1; i >= 0; i--) {
			Cell cell = (Cell) cells[i];
			Actor actor = cell.actor;
			if (actor != null)
				actor.remove();
		}
		cellPool.freeAll(this.cells);
		this.cells.clear();
		rows = 0;
		columns = 0;
		if (rowDefaults != null)
			cellPool.free(rowDefaults);
		rowDefaults = null;
		implicitEndRow = false;
		
		super.clearChildren(unfocus);
	}
	
	public void reset() {
		clearChildren();
		padTop = backgroundTop;
		padLeft = backgroundLeft;
		padBottom = backgroundBottom;
		padRight = backgroundRight;
		align = Align.center;
		debug(Debug.none);
		cellDefaults.reset();
		for (int i = 0, n = columnDefaults.size; i < n; i++) {
			Cell columnCell = columnDefaults.get(i);
			if (columnCell != null)
				cellPool.free(columnCell);
		}
		columnDefaults.clear();
	}
	
	public Cell row() {
		if (cells.size > 0) {
			if (!implicitEndRow) {
				if (cells.peek().endRow)
					return rowDefaults; // Row was already ended.
				endRow();
			}
			invalidate();
		}
		implicitEndRow = false;
		if (rowDefaults != null)
			cellPool.free(rowDefaults);
		rowDefaults = obtainCell();
		rowDefaults.clear();
		return rowDefaults;
	}
	
	private void endRow() {
		Object[] cells = this.cells.items;
		int rowColumns = 0;
		for (int i = this.cells.size - 1; i >= 0; i--) {
			Cell cell = (Cell) cells[i];
			if (cell.endRow)
				break;
			rowColumns += cell.colspan;
		}
		columns = Math.max(columns, rowColumns);
		rows++;
		this.cells.peek().endRow = true;
	}
	
	public Cell columnDefaults(int column) {
		Cell cell = columnDefaults.size > column ? columnDefaults.get(column) : null;
		if (cell == null) {
			cell = obtainCell();
			cell.clear();
			if (column >= columnDefaults.size) {
				for (int i = columnDefaults.size; i < column; i++)
					columnDefaults.add(null);
				columnDefaults.add(cell);
			} else
				columnDefaults.set(column, cell);
		}
		return cell;
	}
	
	public <T extends Actor> Cell<T> getCell(T actor) {
		if (actor == null)
			throw new IllegalArgumentException("actor cannot be null.");
		Object[] cells = this.cells.items;
		for (int i = 0, n = this.cells.size; i < n; i++) {
			Cell c = (Cell) cells[i];
			if (c.actor == actor)
				return c;
		}
		return null;
	}
	
	public Array<Cell> getCells() {
		return cells;
	}
	
	public float getPrefWidth() {
		if (sizeInvalid)
			computeSize();
		float width = tablePrefWidth;
		if (background != null)
			return Math.max(width, background.getMinWidth());
		return width;
	}
	
	public float getPrefHeight() {
		if (sizeInvalid)
			computeSize();
		float height = tablePrefHeight;
		if (background != null)
			return Math.max(height, background.getMinHeight());
		return height;
	}
	
	public float getMinWidth() {
		if (sizeInvalid)
			computeSize();
		return tableMinWidth;
	}
	
	public float getMinHeight() {
		if (sizeInvalid)
			computeSize();
		return tableMinHeight;
	}
	
	public Cell defaults() {
		return cellDefaults;
	}
	
	public Table pad(Value pad) {
		if (pad == null)
			throw new IllegalArgumentException("pad cannot be null.");
		padTop = pad;
		padLeft = pad;
		padBottom = pad;
		padRight = pad;
		sizeInvalid = true;
		return this;
	}
	
	public Table pad(Value top, Value left, Value bottom, Value right) {
		if (top == null)
			throw new IllegalArgumentException("top cannot be null.");
		if (left == null)
			throw new IllegalArgumentException("left cannot be null.");
		if (bottom == null)
			throw new IllegalArgumentException("bottom cannot be null.");
		if (right == null)
			throw new IllegalArgumentException("right cannot be null.");
		padTop = top;
		padLeft = left;
		padBottom = bottom;
		padRight = right;
		sizeInvalid = true;
		return this;
	}
	
	public Table padTop(Value padTop) {
		if (padTop == null)
			throw new IllegalArgumentException("padTop cannot be null.");
		this.padTop = padTop;
		sizeInvalid = true;
		return this;
	}
	
	public Table padLeft(Value padLeft) {
		if (padLeft == null)
			throw new IllegalArgumentException("padLeft cannot be null.");
		this.padLeft = padLeft;
		sizeInvalid = true;
		return this;
	}
	
	public Table padBottom(Value padBottom) {
		if (padBottom == null)
			throw new IllegalArgumentException("padBottom cannot be null.");
		this.padBottom = padBottom;
		sizeInvalid = true;
		return this;
	}
	
	public Table padRight(Value padRight) {
		if (padRight == null)
			throw new IllegalArgumentException("padRight cannot be null.");
		this.padRight = padRight;
		sizeInvalid = true;
		return this;
	}
	
	public Table pad(float pad) {
		pad(Fixed.valueOf(pad));
		return this;
	}
	
	public Table pad(float top, float left, float bottom, float right) {
		padTop = Fixed.valueOf(top);
		padLeft = Fixed.valueOf(left);
		padBottom = Fixed.valueOf(bottom);
		padRight = Fixed.valueOf(right);
		sizeInvalid = true;
		return this;
	}
	
	public Table padTop(float padTop) {
		this.padTop = Fixed.valueOf(padTop);
		sizeInvalid = true;
		return this;
	}
	
	public Table padLeft(float padLeft) {
		this.padLeft = Fixed.valueOf(padLeft);
		sizeInvalid = true;
		return this;
	}
	
	public Table padBottom(float padBottom) {
		this.padBottom = Fixed.valueOf(padBottom);
		sizeInvalid = true;
		return this;
	}
	
	public Table padRight(float padRight) {
		this.padRight = Fixed.valueOf(padRight);
		sizeInvalid = true;
		return this;
	}
	
	public Table align(int align) {
		this.align = align;
		return this;
	}
	
	public Table center() {
		align = Align.center;
		return this;
	}
	
	public Table top() {
		align |= Align.top;
		align &= ~Align.bottom;
		return this;
	}
	
	public Table left() {
		align |= Align.left;
		align &= ~Align.right;
		return this;
	}
	
	public Table bottom() {
		align |= Align.bottom;
		align &= ~Align.top;
		return this;
	}
	
	public Table right() {
		align |= Align.right;
		align &= ~Align.left;
		return this;
	}
	
	public void setDebug(boolean enabled) {
		debug(enabled ? Debug.all : Debug.none);
	}
	
	public Table debug() {
		super.debug();
		return this;
	}
	
	public Table debugAll() {
		super.debugAll();
		return this;
	}
	
	public Table debugTable() {
		super.setDebug(true);
		if (debug != Debug.table) {
			this.debug = Debug.table;
			invalidate();
		}
		return this;
	}
	
	public Table debugCell() {
		super.setDebug(true);
		if (debug != Debug.cell) {
			this.debug = Debug.cell;
			invalidate();
		}
		return this;
	}
	
	public Table debugActor() {
		super.setDebug(true);
		if (debug != Debug.actor) {
			this.debug = Debug.actor;
			invalidate();
		}
		return this;
	}
	
	public Table debug(final Debug debug) {
		super.setDebug(debug != Debug.none);
		if (this.debug != debug) {
			this.debug = debug;
			if (debug == Debug.none)
				clearDebugRectangles();
			else
				invalidate();
		}
		return this;
	}
	
	public Debug getTableDebug() {
		return debug;
	}
	
	public Value getPadTopValue() {
		return padTop;
	}
	
	public float getPadTop() {
		return padTop.get(this);
	}
	
	public Value getPadLeftValue() {
		return padLeft;
	}
	
	public float getPadLeft() {
		return padLeft.get(this);
	}
	
	public Value getPadBottomValue() {
		return padBottom;
	}
	
	public float getPadBottom() {
		return padBottom.get(this);
	}
	
	public Value getPadRightValue() {
		return padRight;
	}
	
	public float getPadRight() {
		return padRight.get(this);
	}
	
	public float getPadX() {
		return padLeft.get(this) + padRight.get(this);
	}
	
	public float getPadY() {
		return padTop.get(this) + padBottom.get(this);
	}
	
	public int getAlign() {
		return align;
	}
	
	public int getRow(float y) {
		int n = this.cells.size;
		if (n == 0)
			return -1;
		y += getPadTop();
		Object[] cells = this.cells.items;
		for (int i = 0, row = 0; i < n; ) {
			Cell c = (Cell) cells[i++];
			if (c.actorY + c.computedPadTop < y)
				return row;
			if (c.endRow)
				row++;
		}
		return -1;
	}
	
	public void setSkin(Skin skin) {
		this.skin = skin;
	}
	
	public void setRound(boolean round) {
		this.round = round;
	}
	
	public int getRows() {
		return rows;
	}
	
	public int getColumns() {
		return columns;
	}
	
	public float getRowHeight(int rowIndex) {
		if (rowHeight == null)
			return 0;
		return rowHeight[rowIndex];
	}
	
	public float getRowMinHeight(int rowIndex) {
		if (sizeInvalid)
			computeSize();
		return rowMinHeight[rowIndex];
	}
	
	public float getRowPrefHeight(int rowIndex) {
		if (sizeInvalid)
			computeSize();
		return rowPrefHeight[rowIndex];
	}
	
	public float getColumnWidth(int columnIndex) {
		if (columnWidth == null)
			return 0;
		return columnWidth[columnIndex];
	}
	
	public float getColumnMinWidth(int columnIndex) {
		if (sizeInvalid)
			computeSize();
		return columnMinWidth[columnIndex];
	}
	
	public float getColumnPrefWidth(int columnIndex) {
		if (sizeInvalid)
			computeSize();
		return columnPrefWidth[columnIndex];
	}
	
	private float[] ensureSize(float[] array, int size) {
		if (array == null || array.length < size)
			return new float[size];
		Arrays.fill(array, 0, size, 0);
		return array;
	}
	
	private void computeSize() {
		sizeInvalid = false;
		
		Object[] cells = this.cells.items;
		int cellCount = this.cells.size;
		
		if (cellCount > 0 && !((Cell) cells[cellCount - 1]).endRow) {
			endRow();
			implicitEndRow = true;
		}
		
		int columns = this.columns, rows = this.rows;
		float[] columnMinWidth = this.columnMinWidth = ensureSize(this.columnMinWidth, columns);
		float[] rowMinHeight = this.rowMinHeight = ensureSize(this.rowMinHeight, rows);
		float[] columnPrefWidth = this.columnPrefWidth = ensureSize(this.columnPrefWidth, columns);
		float[] rowPrefHeight = this.rowPrefHeight = ensureSize(this.rowPrefHeight, rows);
		float[] columnWidth = this.columnWidth = ensureSize(this.columnWidth, columns);
		float[] rowHeight = this.rowHeight = ensureSize(this.rowHeight, rows);
		float[] expandWidth = this.expandWidth = ensureSize(this.expandWidth, columns);
		float[] expandHeight = this.expandHeight = ensureSize(this.expandHeight, rows);
		
		float spaceRightLast = 0;
		for (int i = 0; i < cellCount; i++) {
			Cell c = (Cell) cells[i];
			int column = c.column, row = c.row, colspan = c.colspan;
			Actor a = c.actor;
			
			if (c.expandY != 0 && expandHeight[row] == 0)
				expandHeight[row] = c.expandY;
			if (colspan == 1 && c.expandX != 0 && expandWidth[column] == 0)
				expandWidth[column] = c.expandX;
			
			c.computedPadLeft = c.padLeft.get(a) + (column == 0 ? 0 : Math.max(0, c.spaceLeft.get(a) - spaceRightLast));
			c.computedPadTop = c.padTop.get(a);
			if (c.cellAboveIndex != -1) {
				Cell above = (Cell) cells[c.cellAboveIndex];
				c.computedPadTop += Math.max(0, c.spaceTop.get(a) - above.spaceBottom.get(a));
			}
			float spaceRight = c.spaceRight.get(a);
			c.computedPadRight = c.padRight.get(a) + ((column + colspan) == columns ? 0 : spaceRight);
			c.computedPadBottom = c.padBottom.get(a) + (row == rows - 1 ? 0 : c.spaceBottom.get(a));
			spaceRightLast = spaceRight;
			
			float prefWidth = c.prefWidth.get(a), prefHeight = c.prefHeight.get(a);
			float minWidth = c.minWidth.get(a), minHeight = c.minHeight.get(a);
			float maxWidth = c.maxWidth.get(a), maxHeight = c.maxHeight.get(a);
			if (prefWidth < minWidth)
				prefWidth = minWidth;
			if (prefHeight < minHeight)
				prefHeight = minHeight;
			if (maxWidth > 0 && prefWidth > maxWidth)
				prefWidth = maxWidth;
			if (maxHeight > 0 && prefHeight > maxHeight)
				prefHeight = maxHeight;
			if (round) {
				minWidth = (float) Math.ceil(minWidth);
				minHeight = (float) Math.ceil(minHeight);
				prefWidth = (float) Math.ceil(prefWidth);
				prefHeight = (float) Math.ceil(prefHeight);
			}
			
			if (colspan == 1) { // Spanned column min and pref width is added later.
				float hpadding = c.computedPadLeft + c.computedPadRight;
				columnPrefWidth[column] = Math.max(columnPrefWidth[column], prefWidth + hpadding);
				columnMinWidth[column] = Math.max(columnMinWidth[column], minWidth + hpadding);
			}
			float vpadding = c.computedPadTop + c.computedPadBottom;
			rowPrefHeight[row] = Math.max(rowPrefHeight[row], prefHeight + vpadding);
			rowMinHeight[row] = Math.max(rowMinHeight[row], minHeight + vpadding);
		}
		
		float uniformMinWidth = 0, uniformMinHeight = 0;
		float uniformPrefWidth = 0, uniformPrefHeight = 0;
		for (int i = 0; i < cellCount; i++) {
			Cell c = (Cell) cells[i];
			int column = c.column;
			
			int expandX = c.expandX;
			outer:
			if (expandX != 0) {
				int nn = column + c.colspan;
				for (int ii = column; ii < nn; ii++)
					if (expandWidth[ii] != 0)
						break outer;
				for (int ii = column; ii < nn; ii++)
					expandWidth[ii] = expandX;
			}
			
			if (c.uniformX == Boolean.TRUE && c.colspan == 1) {
				float hpadding = c.computedPadLeft + c.computedPadRight;
				uniformMinWidth = Math.max(uniformMinWidth, columnMinWidth[column] - hpadding);
				uniformPrefWidth = Math.max(uniformPrefWidth, columnPrefWidth[column] - hpadding);
			}
			if (c.uniformY == Boolean.TRUE) {
				float vpadding = c.computedPadTop + c.computedPadBottom;
				uniformMinHeight = Math.max(uniformMinHeight, rowMinHeight[c.row] - vpadding);
				uniformPrefHeight = Math.max(uniformPrefHeight, rowPrefHeight[c.row] - vpadding);
			}
		}
		
		if (uniformPrefWidth > 0 || uniformPrefHeight > 0) {
			for (int i = 0; i < cellCount; i++) {
				Cell c = (Cell) cells[i];
				if (uniformPrefWidth > 0 && c.uniformX == Boolean.TRUE && c.colspan == 1) {
					float hpadding = c.computedPadLeft + c.computedPadRight;
					columnMinWidth[c.column] = uniformMinWidth + hpadding;
					columnPrefWidth[c.column] = uniformPrefWidth + hpadding;
				}
				if (uniformPrefHeight > 0 && c.uniformY == Boolean.TRUE) {
					float vpadding = c.computedPadTop + c.computedPadBottom;
					rowMinHeight[c.row] = uniformMinHeight + vpadding;
					rowPrefHeight[c.row] = uniformPrefHeight + vpadding;
				}
			}
		}
		
		for (int i = 0; i < cellCount; i++) {
			Cell c = (Cell) cells[i];
			int colspan = c.colspan;
			if (colspan == 1)
				continue;
			int column = c.column;
			
			Actor a = c.actor;
			float minWidth = c.minWidth.get(a), prefWidth = c.prefWidth.get(a), maxWidth = c.maxWidth.get(a);
			if (prefWidth < minWidth)
				prefWidth = minWidth;
			if (maxWidth > 0 && prefWidth > maxWidth)
				prefWidth = maxWidth;
			if (round) {
				minWidth = (float) Math.ceil(minWidth);
				prefWidth = (float) Math.ceil(prefWidth);
			}
			
			float spannedMinWidth = -(c.computedPadLeft + c.computedPadRight), spannedPrefWidth = spannedMinWidth;
			float totalExpandWidth = 0;
			for (int ii = column, nn = ii + colspan; ii < nn; ii++) {
				spannedMinWidth += columnMinWidth[ii];
				spannedPrefWidth += columnPrefWidth[ii];
				totalExpandWidth += expandWidth[ii];
			}
			
			float extraMinWidth = Math.max(0, minWidth - spannedMinWidth);
			float extraPrefWidth = Math.max(0, prefWidth - spannedPrefWidth);
			for (int ii = column, nn = ii + colspan; ii < nn; ii++) {
				float ratio = totalExpandWidth == 0 ? 1f / colspan : expandWidth[ii] / totalExpandWidth;
				columnMinWidth[ii] += extraMinWidth * ratio;
				columnPrefWidth[ii] += extraPrefWidth * ratio;
			}
		}
		
		float paddingH = padLeft.get(this) + padRight.get(this);
		float paddingV = padTop.get(this) + padBottom.get(this);
		tableMinWidth = paddingH;
		tablePrefWidth = paddingH;
		for (int i = 0; i < columns; i++) {
			tableMinWidth += columnMinWidth[i];
			tablePrefWidth += columnPrefWidth[i];
		}
		tableMinHeight = paddingV;
		tablePrefHeight = paddingV;
		for (int i = 0; i < rows; i++) {
			tableMinHeight += rowMinHeight[i];
			tablePrefHeight += Math.max(rowMinHeight[i], rowPrefHeight[i]);
		}
		tablePrefWidth = Math.max(tableMinWidth, tablePrefWidth);
		tablePrefHeight = Math.max(tableMinHeight, tablePrefHeight);
	}
	
	public void layout() {
		if (sizeInvalid)
			computeSize();
		
		float layoutWidth = getWidth(), layoutHeight = getHeight();
		int columns = this.columns, rows = this.rows;
		float[] columnWidth = this.columnWidth, rowHeight = this.rowHeight;
		float padLeft = this.padLeft.get(this), paddingH = padLeft + padRight.get(this);
		float padTop = this.padTop.get(this), paddingV = padTop + padBottom.get(this);
		
		float[] columnWeightedWidth;
		float totalGrowWidth = tablePrefWidth - tableMinWidth;
		if (totalGrowWidth == 0)
			columnWeightedWidth = columnMinWidth;
		else {
			float extraWidth = Math.min(totalGrowWidth, Math.max(0, layoutWidth - tableMinWidth));
			columnWeightedWidth = Table.columnWeightedWidth = ensureSize(Table.columnWeightedWidth, columns);
			float[] columnMinWidth = this.columnMinWidth, columnPrefWidth = this.columnPrefWidth;
			for (int i = 0; i < columns; i++) {
				float growWidth = columnPrefWidth[i] - columnMinWidth[i];
				float growRatio = growWidth / totalGrowWidth;
				columnWeightedWidth[i] = columnMinWidth[i] + extraWidth * growRatio;
			}
		}
		
		float[] rowWeightedHeight;
		float totalGrowHeight = tablePrefHeight - tableMinHeight;
		if (totalGrowHeight == 0)
			rowWeightedHeight = rowMinHeight;
		else {
			rowWeightedHeight = Table.rowWeightedHeight = ensureSize(Table.rowWeightedHeight, rows);
			float extraHeight = Math.min(totalGrowHeight, Math.max(0, layoutHeight - tableMinHeight));
			float[] rowMinHeight = this.rowMinHeight, rowPrefHeight = this.rowPrefHeight;
			for (int i = 0; i < rows; i++) {
				float growHeight = rowPrefHeight[i] - rowMinHeight[i];
				float growRatio = growHeight / totalGrowHeight;
				rowWeightedHeight[i] = rowMinHeight[i] + extraHeight * growRatio;
			}
		}
		
		Object[] cells = this.cells.items;
		int cellCount = this.cells.size;
		for (int i = 0; i < cellCount; i++) {
			Cell c = (Cell) cells[i];
			int column = c.column, row = c.row;
			Actor a = c.actor;
			
			float spannedWeightedWidth = 0;
			int colspan = c.colspan;
			for (int ii = column, nn = ii + colspan; ii < nn; ii++)
				spannedWeightedWidth += columnWeightedWidth[ii];
			float weightedHeight = rowWeightedHeight[row];
			
			float prefWidth = c.prefWidth.get(a), prefHeight = c.prefHeight.get(a);
			float minWidth = c.minWidth.get(a), minHeight = c.minHeight.get(a);
			float maxWidth = c.maxWidth.get(a), maxHeight = c.maxHeight.get(a);
			if (prefWidth < minWidth)
				prefWidth = minWidth;
			if (prefHeight < minHeight)
				prefHeight = minHeight;
			if (maxWidth > 0 && prefWidth > maxWidth)
				prefWidth = maxWidth;
			if (maxHeight > 0 && prefHeight > maxHeight)
				prefHeight = maxHeight;
			
			c.actorWidth = Math.min(spannedWeightedWidth - c.computedPadLeft - c.computedPadRight, prefWidth);
			c.actorHeight = Math.min(weightedHeight - c.computedPadTop - c.computedPadBottom, prefHeight);
			
			if (colspan == 1)
				columnWidth[column] = Math.max(columnWidth[column], spannedWeightedWidth);
			rowHeight[row] = Math.max(rowHeight[row], weightedHeight);
		}
		
		float[] expandWidth = this.expandWidth, expandHeight = this.expandHeight;
		float totalExpand = 0;
		for (int i = 0; i < columns; i++)
			totalExpand += expandWidth[i];
		if (totalExpand > 0) {
			float extra = layoutWidth - paddingH;
			for (int i = 0; i < columns; i++)
				extra -= columnWidth[i];
			if (extra > 0) {
				float used = 0;
				int lastIndex = 0;
				for (int i = 0; i < columns; i++) {
					if (expandWidth[i] == 0)
						continue;
					float amount = extra * expandWidth[i] / totalExpand;
					columnWidth[i] += amount;
					used += amount;
					lastIndex = i;
				}
				columnWidth[lastIndex] += extra - used;
			}
		}
		
		totalExpand = 0;
		for (int i = 0; i < rows; i++)
			totalExpand += expandHeight[i];
		if (totalExpand > 0) {
			float extra = layoutHeight - paddingV;
			for (int i = 0; i < rows; i++)
				extra -= rowHeight[i];
			if (extra > 0) {
				float used = 0;
				int lastIndex = 0;
				for (int i = 0; i < rows; i++) {
					if (expandHeight[i] == 0)
						continue;
					float amount = extra * expandHeight[i] / totalExpand;
					rowHeight[i] += amount;
					used += amount;
					lastIndex = i;
				}
				rowHeight[lastIndex] += extra - used;
			}
		}
		
		for (int i = 0; i < cellCount; i++) {
			Cell c = (Cell) cells[i];
			int colspan = c.colspan;
			if (colspan == 1)
				continue;
			
			float extraWidth = 0;
			for (int column = c.column, nn = column + colspan; column < nn; column++)
				extraWidth += columnWeightedWidth[column] - columnWidth[column];
			extraWidth -= Math.max(0, c.computedPadLeft + c.computedPadRight);
			
			extraWidth /= colspan;
			if (extraWidth > 0) {
				for (int column = c.column, nn = column + colspan; column < nn; column++)
					columnWidth[column] += extraWidth;
			}
		}
		
		float tableWidth = paddingH, tableHeight = paddingV;
		for (int i = 0; i < columns; i++)
			tableWidth += columnWidth[i];
		for (int i = 0; i < rows; i++)
			tableHeight += rowHeight[i];
		
		int align = this.align;
		float x = padLeft;
		if ((align & Align.right) != 0)
			x += layoutWidth - tableWidth;
		else if ((align & Align.left) == 0)
			x += (layoutWidth - tableWidth) / 2;
		
		float y = padTop;
		if ((align & Align.bottom) != 0)
			y += layoutHeight - tableHeight;
		else if ((align & Align.top) == 0)
			y += (layoutHeight - tableHeight) / 2;
		
		float currentX = x, currentY = y;
		for (int i = 0; i < cellCount; i++) {
			Cell c = (Cell) cells[i];
			
			float spannedCellWidth = 0;
			for (int column = c.column, nn = column + c.colspan; column < nn; column++)
				spannedCellWidth += columnWidth[column];
			spannedCellWidth -= c.computedPadLeft + c.computedPadRight;
			
			currentX += c.computedPadLeft;
			
			float fillX = c.fillX, fillY = c.fillY;
			if (fillX > 0) {
				c.actorWidth = Math.max(spannedCellWidth * fillX, c.minWidth.get(c.actor));
				float maxWidth = c.maxWidth.get(c.actor);
				if (maxWidth > 0)
					c.actorWidth = Math.min(c.actorWidth, maxWidth);
			}
			if (fillY > 0) {
				c.actorHeight = Math.max(rowHeight[c.row] * fillY - c.computedPadTop - c.computedPadBottom, c.minHeight.get(c.actor));
				float maxHeight = c.maxHeight.get(c.actor);
				if (maxHeight > 0)
					c.actorHeight = Math.min(c.actorHeight, maxHeight);
			}
			
			align = c.align;
			if ((align & Align.left) != 0)
				c.actorX = currentX;
			else if ((align & Align.right) != 0)
				c.actorX = currentX + spannedCellWidth - c.actorWidth;
			else
				c.actorX = currentX + (spannedCellWidth - c.actorWidth) / 2;
			
			if ((align & Align.top) != 0)
				c.actorY = c.computedPadTop;
			else if ((align & Align.bottom) != 0)
				c.actorY = rowHeight[c.row] - c.actorHeight - c.computedPadBottom;
			else
				c.actorY = (rowHeight[c.row] - c.actorHeight + c.computedPadTop - c.computedPadBottom) / 2;
			c.actorY = layoutHeight - currentY - c.actorY - c.actorHeight;
			
			if (round) {
				c.actorWidth = (float) Math.ceil(c.actorWidth);
				c.actorHeight = (float) Math.ceil(c.actorHeight);
				c.actorX = (float) Math.floor(c.actorX);
				c.actorY = (float) Math.floor(c.actorY);
			}
			
			if (c.actor != null)
				c.actor.setBounds(c.actorX, c.actorY, c.actorWidth, c.actorHeight);
			
			if (c.endRow) {
				currentX = x;
				currentY += rowHeight[c.row];
			} else
				currentX += spannedCellWidth + c.computedPadRight;
		}
		
		Array<Actor> childrenArray = getChildren();
		Actor[] children = childrenArray.items;
		for (int i = 0, n = childrenArray.size; i < n; i++) {
			Object child = children[i];
			if (child instanceof Layout)
				((Layout) child).validate();
		}
		
		if (debug != Debug.none)
			addDebugRects(x, y, tableWidth - paddingH, tableHeight - paddingV);
	}
	
	private void addDebugRects(float currentX, float currentY, float width, float height) {
		clearDebugRectangles();
		if (debug == Debug.table || debug == Debug.all) {
			addDebugRectangle(0, 0, getWidth(), getHeight(), debugTableColor);
			addDebugRectangle(currentX, getHeight() - currentY, width, -height, debugTableColor);
		}
		float x = currentX;
		for (int i = 0, n = cells.size; i < n; i++) {
			Cell c = cells.get(i);
			
			if (debug == Debug.actor || debug == Debug.all)
				addDebugRectangle(c.actorX, c.actorY, c.actorWidth, c.actorHeight, debugActorColor);
			
			float spannedCellWidth = 0;
			for (int column = c.column, nn = column + c.colspan; column < nn; column++)
				spannedCellWidth += columnWidth[column];
			spannedCellWidth -= c.computedPadLeft + c.computedPadRight;
			currentX += c.computedPadLeft;
			if (debug == Debug.cell || debug == Debug.all) {
				float h = rowHeight[c.row] - c.computedPadTop - c.computedPadBottom;
				float y = currentY + c.computedPadTop;
				addDebugRectangle(currentX, getHeight() - y, spannedCellWidth, -h, debugCellColor);
			}
			
			if (c.endRow) {
				currentX = x;
				currentY += rowHeight[c.row];
			} else
				currentX += spannedCellWidth + c.computedPadRight;
		}
	}
	
	private void clearDebugRectangles() {
		if (debugRects == null)
			debugRects = new Array<>();
		DebugRect.pool.freeAll(debugRects);
		debugRects.clear();
	}
	
	private void addDebugRectangle(float x, float y, float w, float h, Color color) {
		DebugRect rect = DebugRect.pool.obtain();
		rect.color = color;
		rect.set(x, y, w, h);
		debugRects.add(rect);
	}
	
	public void drawDebug(ShapeRenderer shapes) {
		if (isTransform()) {
			applyTransform(shapes, computeTransform());
			drawDebugRects(shapes);
			if (clip) {
				shapes.flush();
				float x = 0, y = 0, width = getWidth(), height = getHeight();
				if (background != null) {
					x = padLeft.get(this);
					y = padBottom.get(this);
					width -= x + padRight.get(this);
					height -= y + padTop.get(this);
				}
				if (clipBegin(x, y, width, height)) {
					drawDebugChildren(shapes);
					clipEnd();
				}
			} else
				drawDebugChildren(shapes);
			resetTransform(shapes);
		} else {
			drawDebugRects(shapes);
			super.drawDebug(shapes);
		}
	}
	
	private void drawDebugRects(ShapeRenderer shapes) {
		if (debugRects == null || !getDebug())
			return;
		shapes.set(ShapeType.Line);
		if (getStage() != null)
			shapes.setColor(getStage().getDebugColor());
		float x = 0, y = 0;
		if (!isTransform()) {
			x = getX();
			y = getY();
		}
		for (int i = 0, n = debugRects.size; i < n; i++) {
			DebugRect debugRect = debugRects.get(i);
			shapes.setColor(debugRect.color);
			shapes.rect(x + debugRect.x, y + debugRect.y, debugRect.width, debugRect.height);
		}
	}
	
	public Skin getSkin() {
		return skin;
	}
	
	public static class DebugRect extends Rectangle {
		
		static Pool<DebugRect> pool = Pools.get(DebugRect.class, DebugRect::new);
		Color color;
		
	}
	
	public enum Debug {
		none, all, table, cell, actor
	}
	
	public static Value backgroundTop = new Value() {
		public float get(Actor context) {
			Drawable background = ((Table) context).background;
			return background == null ? 0 : background.getTopHeight();
		}
	};
	
	public static Value backgroundLeft = new Value() {
		public float get(Actor context) {
			Drawable background = ((Table) context).background;
			return background == null ? 0 : background.getLeftWidth();
		}
	};
	
	public static Value backgroundBottom = new Value() {
		public float get(Actor context) {
			Drawable background = ((Table) context).background;
			return background == null ? 0 : background.getBottomHeight();
		}
	};
	
	public static Value backgroundRight = new Value() {
		public float get(Actor context) {
			Drawable background = ((Table) context).background;
			return background == null ? 0 : background.getRightWidth();
		}
	};
	
}
