package me.nulldoubt.micro.scenes.scene2d.ui;

import me.nulldoubt.micro.Application.ApplicationType;
import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.graphics.Color;
import me.nulldoubt.micro.graphics.g2d.Batch;
import me.nulldoubt.micro.math.Vector2;
import me.nulldoubt.micro.math.shapes.Rectangle;
import me.nulldoubt.micro.scenes.scene2d.Actor;
import me.nulldoubt.micro.scenes.scene2d.Group;
import me.nulldoubt.micro.scenes.scene2d.InputEvent;
import me.nulldoubt.micro.scenes.scene2d.ui.Tree.Node;
import me.nulldoubt.micro.scenes.scene2d.utils.*;
import me.nulldoubt.micro.utils.collections.Array;

public class Tree<N extends Node, V> extends WidgetGroup {
	
	private static final Vector2 tmp = new Vector2();
	
	TreeStyle style;
	final Array<N> rootNodes = new Array();
	final Selection<N> selection;
	float ySpacing = 4, iconSpacingLeft = 2, iconSpacingRight = 2, paddingLeft, paddingRight, indentSpacing;
	private float prefWidth, prefHeight;
	private boolean sizeInvalid = true;
	private N foundNode, overNode;
	N rangeStart;
	private ClickListener clickListener;
	
	public Tree(Skin skin) {
		this(skin.get(TreeStyle.class));
	}
	
	public Tree(Skin skin, String styleName) {
		this(skin.get(styleName, TreeStyle.class));
	}
	
	public Tree(TreeStyle style) {
		selection = new Selection<>() {
			protected void changed() {
				switch (size()) {
					case 0:
						rangeStart = null;
						break;
					case 1:
						rangeStart = first();
						break;
				}
			}
		};
		selection.setActor(this);
		selection.setMultiple(true);
		setStyle(style);
		initialize();
	}
	
	private void initialize() {
		addListener(clickListener = new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				N node = getNodeAt(y);
				if (node == null)
					return;
				if (node != getNodeAt(getTouchDownY()))
					return;
				if (selection.getMultiple() && selection.notEmpty() && UIUtils.shift()) {
					if (rangeStart == null)
						rangeStart = node;
					N rangeStart = Tree.this.rangeStart;
					if (!UIUtils.ctrl())
						selection.clear();
					float start = rangeStart.actor.getY(), end = node.actor.getY();
					if (start > end)
						selectNodes(rootNodes, end, start);
					else {
						selectNodes(rootNodes, start, end);
						selection.items().orderedItems().reverse();
					}
					
					selection.fireChangeEvent();
					Tree.this.rangeStart = rangeStart;
					return;
				}
				if (node.children.size > 0 && (!selection.getMultiple() || !UIUtils.ctrl())) {
					float rowX = node.actor.getX();
					if (node.icon != null)
						rowX -= iconSpacingRight + node.icon.getMinWidth();
					if (x < rowX) {
						node.setExpanded(!node.expanded);
						return;
					}
				}
				if (!node.isSelectable())
					return;
				selection.choose(node);
				if (!selection.isEmpty())
					rangeStart = node;
			}
			
			public boolean mouseMoved(InputEvent event, float x, float y) {
				setOverNode(getNodeAt(y));
				return false;
			}
			
			public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
				super.enter(event, x, y, pointer, fromActor);
				setOverNode(getNodeAt(y));
			}
			
			public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
				super.exit(event, x, y, pointer, toActor);
				if (toActor == null || !toActor.isDescendantOf(Tree.this))
					setOverNode(null);
			}
		});
	}
	
	public void setStyle(TreeStyle style) {
		this.style = style;
		if (indentSpacing == 0)
			indentSpacing = plusMinusWidth();
	}
	
	public void add(N node) {
		insert(rootNodes.size, node);
	}
	
	public void insert(int index, N node) {
		if (node.parent != null) {
			node.parent.remove(node);
			node.parent = null;
		} else {
			int existingIndex = rootNodes.indexOf(node, true);
			if (existingIndex != -1) {
				if (existingIndex == index)
					return;
				if (existingIndex < index)
					index--;
				rootNodes.removeIndex(existingIndex);
				int actorIndex = node.actor.getZIndex();
				if (actorIndex != -1)
					node.removeFromTree(this, actorIndex);
			}
		}
		
		rootNodes.insert(index, node);
		
		int actorIndex;
		if (index == 0)
			actorIndex = 0;
		else if (index < rootNodes.size - 1)
			actorIndex = rootNodes.get(index + 1).actor.getZIndex();
		else {
			N before = rootNodes.get(index - 1);
			actorIndex = before.actor.getZIndex() + before.countActors();
		}
		node.addToTree(this, actorIndex);
	}
	
	public void remove(N node) {
		if (node.parent != null) {
			node.parent.remove(node);
			return;
		}
		if (!rootNodes.removeValue(node, true))
			return;
		int actorIndex = node.actor.getZIndex();
		if (actorIndex != -1)
			node.removeFromTree(this, actorIndex);
	}
	
	public void clearChildren(boolean unfocus) {
		super.clearChildren(unfocus);
		setOverNode(null);
		rootNodes.clear();
		selection.clear();
	}
	
	public void invalidate() {
		super.invalidate();
		sizeInvalid = true;
	}
	
	private float plusMinusWidth() {
		float width = Math.max(style.plus.getMinWidth(), style.minus.getMinWidth());
		if (style.plusOver != null)
			width = Math.max(width, style.plusOver.getMinWidth());
		if (style.minusOver != null)
			width = Math.max(width, style.minusOver.getMinWidth());
		return width;
	}
	
	private void computeSize() {
		sizeInvalid = false;
		prefWidth = plusMinusWidth();
		prefHeight = 0;
		computeSize(rootNodes, 0, prefWidth);
		prefWidth += paddingLeft + paddingRight;
	}
	
	private void computeSize(Array<N> nodes, float indent, float plusMinusWidth) {
		float ySpacing = this.ySpacing;
		float spacing = iconSpacingLeft + iconSpacingRight;
		for (int i = 0, n = nodes.size; i < n; i++) {
			N node = nodes.get(i);
			float rowWidth = indent + plusMinusWidth;
			Actor actor = node.actor;
			if (actor instanceof Layout) {
				Layout layout = (Layout) actor;
				rowWidth += layout.getPrefWidth();
				node.height = layout.getPrefHeight();
			} else {
				rowWidth += actor.getWidth();
				node.height = actor.getHeight();
			}
			if (node.icon != null) {
				rowWidth += spacing + node.icon.getMinWidth();
				node.height = Math.max(node.height, node.icon.getMinHeight());
			}
			prefWidth = Math.max(prefWidth, rowWidth);
			prefHeight += node.height + ySpacing;
			if (node.expanded)
				computeSize(node.children, indent + indentSpacing, plusMinusWidth);
		}
	}
	
	public void layout() {
		if (sizeInvalid)
			computeSize();
		layout(rootNodes, paddingLeft, getHeight() - ySpacing / 2, plusMinusWidth());
	}
	
	private float layout(Array<N> nodes, float indent, float y, float plusMinusWidth) {
		float ySpacing = this.ySpacing;
		float iconSpacingLeft = this.iconSpacingLeft;
		float spacing = iconSpacingLeft + iconSpacingRight;
		for (int i = 0, n = nodes.size; i < n; i++) {
			N node = nodes.get(i);
			float x = indent + plusMinusWidth;
			if (node.icon != null)
				x += spacing + node.icon.getMinWidth();
			else
				x += iconSpacingLeft;
			if (node.actor instanceof Layout)
				((Layout) node.actor).pack();
			y -= node.getHeight();
			node.actor.setPosition(x, y);
			y -= ySpacing;
			if (node.expanded)
				y = layout(node.children, indent + indentSpacing, y, plusMinusWidth);
		}
		return y;
	}
	
	public void draw(Batch batch, float parentAlpha) {
		drawBackground(batch, parentAlpha);
		Color color = getColor();
		float a = color.a * parentAlpha;
		batch.setColor(color.r, color.g, color.b, a);
		drawIcons(batch, color.r, color.g, color.b, a, rootNodes, paddingLeft, plusMinusWidth());
		super.draw(batch, parentAlpha); // Draw node actors.
	}
	
	protected void drawBackground(Batch batch, float parentAlpha) {
		if (style.background != null) {
			Color color = getColor();
			batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
			style.background.draw(batch, getX(), getY(), getWidth(), getHeight());
		}
	}
	
	protected float drawIcons(Batch batch, float r, float g, float b, float a, Array<N> nodes, float indent, float plusMinusWidth) {
		Rectangle cullingArea = getCullingArea();
		float cullBottom = 0, cullTop = 0;
		if (cullingArea != null) {
			cullBottom = cullingArea.y;
			cullTop = cullBottom + cullingArea.height;
		}
		TreeStyle style = this.style;
		float x = getX(), y = getY(), expandX = x + indent, iconX = expandX + plusMinusWidth + iconSpacingLeft, actorY = 0;
		for (int i = 0, n = nodes.size; i < n; i++) {
			N node = nodes.get(i);
			Actor actor = node.actor;
			actorY = actor.getY();
			float height = node.height;
			if (cullingArea == null || (actorY + height >= cullBottom && actorY <= cullTop)) {
				if (selection.contains(node) && style.selection != null) {
					drawSelection(node, style.selection, batch, x, y + actorY - ySpacing / 2, getWidth(), height + ySpacing);
				} else if (node == overNode && style.over != null) {
					drawOver(node, style.over, batch, x, y + actorY - ySpacing / 2, getWidth(), height + ySpacing);
				}
				
				if (node.icon != null) {
					float iconY = y + actorY + Math.round((height - node.icon.getMinHeight()) / 2);
					Color actorColor = actor.getColor();
					batch.setColor(actorColor.r, actorColor.g, actorColor.b, actorColor.a * a);
					drawIcon(node, node.icon, batch, iconX, iconY);
					batch.setColor(r, g, b, a);
				}
				
				if (node.children.size > 0) {
					Drawable expandIcon = getExpandIcon(node, iconX);
					float iconY = y + actorY + Math.round((height - expandIcon.getMinHeight()) / 2);
					drawExpandIcon(node, expandIcon, batch, expandX, iconY);
				}
			} else if (actorY < cullBottom) //
				break;
			if (node.expanded && node.children.size > 0)
				drawIcons(batch, r, g, b, a, node.children, indent + indentSpacing, plusMinusWidth);
		}
		return actorY;
	}
	
	protected void drawSelection(N node, Drawable selection, Batch batch, float x, float y, float width, float height) {
		selection.draw(batch, x, y, width, height);
	}
	
	protected void drawOver(N node, Drawable over, Batch batch, float x, float y, float width, float height) {
		over.draw(batch, x, y, width, height);
	}
	
	protected void drawExpandIcon(N node, Drawable expandIcon, Batch batch, float x, float y) {
		expandIcon.draw(batch, x, y, expandIcon.getMinWidth(), expandIcon.getMinHeight());
	}
	
	protected void drawIcon(N node, Drawable icon, Batch batch, float x, float y) {
		icon.draw(batch, x, y, icon.getMinWidth(), icon.getMinHeight());
	}
	
	protected Drawable getExpandIcon(N node, float iconX) {
		if (node == overNode //
				&& Micro.app.getType() == ApplicationType.Desktop //
				&& (!selection.getMultiple() || (!UIUtils.ctrl() && !UIUtils.shift())) //
		) {
			float mouseX = screenToLocalCoordinates(tmp.set(Micro.input.getX(), 0)).x + getX();
			if (mouseX >= 0 && mouseX < iconX) {
				Drawable icon = node.expanded ? style.minusOver : style.plusOver;
				if (icon != null)
					return icon;
			}
		}
		return node.expanded ? style.minus : style.plus;
	}
	
	public N getNodeAt(float y) {
		foundNode = null;
		getNodeAt(rootNodes, y, getHeight());
		try {
			return foundNode;
		} finally {
			foundNode = null;
		}
	}
	
	private float getNodeAt(Array<N> nodes, float y, float rowY) {
		for (int i = 0, n = nodes.size; i < n; i++) {
			N node = nodes.get(i);
			float height = node.height;
			rowY -= node.getHeight() - height;
			if (y >= rowY - height - ySpacing && y < rowY) {
				foundNode = node;
				return -1;
			}
			rowY -= height + ySpacing;
			if (node.expanded) {
				rowY = getNodeAt(node.children, y, rowY);
				if (rowY == -1)
					return -1;
			}
		}
		return rowY;
	}
	
	void selectNodes(Array<N> nodes, float low, float high) {
		for (int i = 0, n = nodes.size; i < n; i++) {
			N node = nodes.get(i);
			if (node.actor.getY() < low)
				break;
			if (!node.isSelectable())
				continue;
			if (node.actor.getY() <= high)
				selection.add(node);
			if (node.expanded)
				selectNodes(node.children, low, high);
		}
	}
	
	public Selection<N> getSelection() {
		return selection;
	}
	
	public N getSelectedNode() {
		return selection.first();
	}
	
	public V getSelectedValue() {
		N node = selection.first();
		return node == null ? null : (V) node.getValue();
	}
	
	public TreeStyle getStyle() {
		return style;
	}
	
	public Array<N> getRootNodes() {
		return rootNodes;
	}
	
	public void updateRootNodes() {
		for (int i = 0, n = rootNodes.size; i < n; i++) {
			N node = rootNodes.get(i);
			int actorIndex = node.actor.getZIndex();
			if (actorIndex != -1)
				node.removeFromTree(this, actorIndex);
		}
		for (int i = 0, n = rootNodes.size, actorIndex = 0; i < n; i++)
			actorIndex += rootNodes.get(i).addToTree(this, actorIndex);
	}
	
	public N getOverNode() {
		return overNode;
	}
	
	public V getOverValue() {
		if (overNode == null)
			return null;
		return (V) overNode.getValue();
	}
	
	public void setOverNode(N overNode) {
		this.overNode = overNode;
	}
	
	public void setPadding(float padding) {
		paddingLeft = padding;
		paddingRight = padding;
	}
	
	public void setPadding(float left, float right) {
		this.paddingLeft = left;
		this.paddingRight = right;
	}
	
	public void setIndentSpacing(float indentSpacing) {
		this.indentSpacing = indentSpacing;
	}
	
	public float getIndentSpacing() {
		return indentSpacing;
	}
	
	public void setYSpacing(float ySpacing) {
		this.ySpacing = ySpacing;
	}
	
	public float getYSpacing() {
		return ySpacing;
	}
	
	public void setIconSpacing(float left, float right) {
		this.iconSpacingLeft = left;
		this.iconSpacingRight = right;
	}
	
	public float getPrefWidth() {
		if (sizeInvalid)
			computeSize();
		return prefWidth;
	}
	
	public float getPrefHeight() {
		if (sizeInvalid)
			computeSize();
		return prefHeight;
	}
	
	public void findExpandedValues(Array<V> values) {
		findExpandedValues(rootNodes, values);
	}
	
	public void restoreExpandedValues(Array<V> values) {
		for (int i = 0, n = values.size; i < n; i++) {
			N node = findNode(values.get(i));
			if (node != null) {
				node.setExpanded(true);
				node.expandTo();
			}
		}
	}
	
	static boolean findExpandedValues(Array<? extends Node> nodes, Array values) {
		boolean expanded = false;
		for (int i = 0, n = nodes.size; i < n; i++) {
			Node node = nodes.get(i);
			if (node.expanded && !findExpandedValues(node.children, values))
				values.add(node.value);
		}
		return expanded;
	}
	
	public N findNode(V value) {
		if (value == null)
			throw new IllegalArgumentException("value cannot be null.");
		return (N) findNode(rootNodes, value);
	}
	
	static Node findNode(Array<? extends Node> nodes, Object value) {
		for (int i = 0, n = nodes.size; i < n; i++) {
			Node node = nodes.get(i);
			if (value.equals(node.value))
				return node;
		}
		for (int i = 0, n = nodes.size; i < n; i++) {
			Node node = nodes.get(i);
			Node found = findNode(node.children, value);
			if (found != null)
				return found;
		}
		return null;
	}
	
	public void collapseAll() {
		collapseAll(rootNodes);
	}
	
	static void collapseAll(Array<? extends Node> nodes) {
		for (int i = 0, n = nodes.size; i < n; i++) {
			Node node = nodes.get(i);
			node.setExpanded(false);
			collapseAll(node.children);
		}
	}
	
	public void expandAll() {
		expandAll(rootNodes);
	}
	
	static void expandAll(Array<? extends Node> nodes) {
		for (int i = 0, n = nodes.size; i < n; i++)
			nodes.get(i).expandAll();
	}
	
	public ClickListener getClickListener() {
		return clickListener;
	}
	
	public static abstract class Node<N extends Node, V, A extends Actor> {
		
		A actor;
		N parent;
		final Array<N> children = new Array<>(0);
		boolean selectable = true;
		boolean expanded;
		Drawable icon;
		float height;
		V value;
		
		public Node(A actor) {
			if (actor == null)
				throw new IllegalArgumentException("actor cannot be null.");
			this.actor = actor;
		}
		
		public Node() {}
		
		public void setExpanded(boolean expanded) {
			if (expanded == this.expanded)
				return;
			this.expanded = expanded;
			if (children.size == 0)
				return;
			Tree tree = getTree();
			if (tree == null)
				return;
			Object[] children = this.children.items;
			int actorIndex = actor.getZIndex() + 1;
			if (expanded) {
				for (int i = 0, n = this.children.size; i < n; i++)
					actorIndex += ((N) children[i]).addToTree(tree, actorIndex);
			} else {
				for (int i = 0, n = this.children.size; i < n; i++)
					((N) children[i]).removeFromTree(tree, actorIndex);
			}
		}
		
		protected int addToTree(Tree<N, V> tree, int actorIndex) {
			tree.addActorAt(actorIndex, actor);
			if (!expanded)
				return 1;
			int childIndex = actorIndex + 1;
			Object[] children = this.children.items;
			for (int i = 0, n = this.children.size; i < n; i++)
				childIndex += ((N) children[i]).addToTree(tree, childIndex);
			return childIndex - actorIndex;
		}
		
		protected void removeFromTree(Tree<N, V> tree, int actorIndex) {
			if (!expanded)
				return;
			Object[] children = this.children.items;
			for (int i = 0, n = this.children.size; i < n; i++)
				((N) children[i]).removeFromTree(tree, actorIndex);
		}
		
		public void add(N node) {
			insert(children.size, node);
		}
		
		public void addAll(Array<N> nodes) {
			for (int i = 0, n = nodes.size; i < n; i++)
				insert(children.size, nodes.get(i));
		}
		
		public void insert(int childIndex, N node) {
			node.parent = this;
			children.insert(childIndex, node);
			if (!expanded)
				return;
			Tree tree = getTree();
			if (tree != null) {
				int actorIndex;
				if (childIndex == 0)
					actorIndex = actor.getZIndex() + 1;
				else if (childIndex < children.size - 1)
					actorIndex = children.get(childIndex + 1).actor.getZIndex();
				else {
					N before = children.get(childIndex - 1);
					actorIndex = before.actor.getZIndex() + before.countActors();
				}
				node.addToTree(tree, actorIndex);
			}
		}
		
		int countActors() {
			if (!expanded)
				return 1;
			int count = 1;
			Object[] children = this.children.items;
			for (int i = 0, n = this.children.size; i < n; i++)
				count += ((N) children[i]).countActors();
			return count;
		}
		
		public void remove() {
			Tree tree = getTree();
			if (tree != null)
				tree.remove(this);
			else if (parent != null) //
				parent.remove(this);
		}
		
		public void remove(N node) {
			if (!children.removeValue(node, true))
				return;
			if (!expanded)
				return;
			Tree tree = getTree();
			if (tree != null)
				node.removeFromTree(tree, node.actor.getZIndex());
		}
		
		public void clearChildren() {
			if (expanded) {
				Tree tree = getTree();
				if (tree != null) {
					int actorIndex = actor.getZIndex() + 1;
					Object[] children = this.children.items;
					for (int i = 0, n = this.children.size; i < n; i++)
						((N) children[i]).removeFromTree(tree, actorIndex);
				}
			}
			children.clear();
		}
		
		public Tree<N, V> getTree() {
			Group parent = actor.getParent();
			if (parent instanceof Tree)
				return (Tree) parent;
			return null;
		}
		
		public void setActor(A newActor) {
			if (actor != null) {
				Tree<N, V> tree = getTree();
				if (tree != null) {
					int index = actor.getZIndex();
					tree.removeActorAt(index, true);
					tree.addActorAt(index, newActor);
				}
			}
			actor = newActor;
		}
		
		public A getActor() {
			return actor;
		}
		
		public boolean isExpanded() {
			return expanded;
		}
		
		public Array<N> getChildren() {
			return children;
		}
		
		public boolean hasChildren() {
			return children.size > 0;
		}
		
		public void updateChildren() {
			if (!expanded)
				return;
			Tree tree = getTree();
			if (tree == null)
				return;
			Object[] children = this.children.items;
			int n = this.children.size;
			int actorIndex = actor.getZIndex() + 1;
			for (int i = 0; i < n; i++)
				((N) children[i]).removeFromTree(tree, actorIndex);
			for (int i = 0; i < n; i++)
				actorIndex += ((N) children[i]).addToTree(tree, actorIndex);
		}
		
		public N getParent() {
			return parent;
		}
		
		public void setIcon(Drawable icon) {
			this.icon = icon;
		}
		
		public V getValue() {
			return value;
		}
		
		public void setValue(V value) {
			this.value = value;
		}
		
		public Drawable getIcon() {
			return icon;
		}
		
		public int getLevel() {
			int level = 0;
			Node current = this;
			do {
				level++;
				current = current.getParent();
			} while (current != null);
			return level;
		}
		
		public N findNode(V value) {
			if (value == null)
				throw new IllegalArgumentException("value cannot be null.");
			if (value.equals(this.value))
				return (N) this;
			return (N) Tree.findNode(children, value);
		}
		
		public void collapseAll() {
			setExpanded(false);
			Tree.collapseAll(children);
		}
		
		public void expandAll() {
			setExpanded(true);
			if (children.size > 0)
				Tree.expandAll(children);
		}
		
		public void expandTo() {
			Node node = parent;
			while (node != null) {
				node.setExpanded(true);
				node = node.parent;
			}
		}
		
		public boolean isSelectable() {
			return selectable;
		}
		
		public void setSelectable(boolean selectable) {
			this.selectable = selectable;
		}
		
		public void findExpandedValues(Array<V> values) {
			if (expanded && !Tree.findExpandedValues(children, values))
				values.add(value);
		}
		
		public void restoreExpandedValues(Array<V> values) {
			for (int i = 0, n = values.size; i < n; i++) {
				N node = findNode(values.get(i));
				if (node != null) {
					node.setExpanded(true);
					node.expandTo();
				}
			}
		}
		
		public float getHeight() {
			return height;
		}
		
		public boolean isAscendantOf(N node) {
			if (node == null)
				throw new IllegalArgumentException("node cannot be null.");
			Node current = node;
			do {
				if (current == this)
					return true;
				current = current.parent;
			} while (current != null);
			return false;
		}
		
		public boolean isDescendantOf(N node) {
			if (node == null)
				throw new IllegalArgumentException("node cannot be null.");
			Node parent = this;
			do {
				if (parent == node)
					return true;
				parent = parent.parent;
			} while (parent != null);
			return false;
		}
		
	}
	
	public static class TreeStyle {
		
		public Drawable plus, minus;
		public Drawable plusOver, minusOver;
		public Drawable over, selection, background;
		
		public TreeStyle() {}
		
		public TreeStyle(Drawable plus, Drawable minus, Drawable selection) {
			this.plus = plus;
			this.minus = minus;
			this.selection = selection;
		}
		
		public TreeStyle(TreeStyle style) {
			plus = style.plus;
			minus = style.minus;
			
			plusOver = style.plusOver;
			minusOver = style.minusOver;
			
			over = style.over;
			selection = style.selection;
			background = style.background;
		}
		
	}
	
}
