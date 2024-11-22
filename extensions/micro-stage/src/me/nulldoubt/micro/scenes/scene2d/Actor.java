package me.nulldoubt.micro.scenes.scene2d;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.graphics.Color;
import me.nulldoubt.micro.graphics.g2d.Batch;
import me.nulldoubt.micro.graphics.glutils.ShapeRenderer;
import me.nulldoubt.micro.graphics.glutils.ShapeRenderer.ShapeType;
import me.nulldoubt.micro.math.MathUtils;
import me.nulldoubt.micro.math.Vector2;
import me.nulldoubt.micro.math.shapes.Rectangle;
import me.nulldoubt.micro.utils.Align;
import me.nulldoubt.micro.utils.Scissors;
import me.nulldoubt.micro.utils.collections.Array;
import me.nulldoubt.micro.utils.collections.DelayedRemovalArray;
import me.nulldoubt.micro.utils.pools.Pools;

import static me.nulldoubt.micro.utils.Align.*;

public class Actor {
	
	private Stage stage;
	Group parent;
	private final DelayedRemovalArray<EventListener> listeners = new DelayedRemovalArray<>(0);
	private final DelayedRemovalArray<EventListener> captureListeners = new DelayedRemovalArray<>(0);
	private final Array<Action> actions = new Array<>(0);
	
	private String name;
	private Touchable touchable = Touchable.enabled;
	private boolean visible = true, debug;
	float x, y;
	float width, height;
	float originX, originY;
	float scaleX = 1, scaleY = 1;
	float rotation;
	final Color color = new Color(1, 1, 1, 1);
	private Object userObject;
	
	public void draw(Batch batch, float parentAlpha) {}
	
	public void act(float delta) {
		Array<Action> actions = this.actions;
		if (actions.size == 0)
			return;
		if (stage != null && stage.getActionsRequestRendering())
			Micro.graphics.requestRendering();
		try {
			for (int i = 0; i < actions.size; i++) {
				Action action = actions.get(i);
				if (action.act(delta) && i < actions.size) {
					Action current = actions.get(i);
					int actionIndex = current == action ? i : actions.indexOf(action, true);
					if (actionIndex != -1) {
						actions.removeIndex(actionIndex);
						action.setActor(null);
						i--;
					}
				}
			}
		} catch (RuntimeException ex) {
			String context = toString();
			throw new RuntimeException("Actor: " + context.substring(0, Math.min(context.length(), 128)), ex);
		}
	}
	
	public boolean fire(Event event) {
		if (event.getStage() == null)
			event.setStage(getStage());
		event.setTarget(this);
		
		Array<Group> ascendants = Pools.obtain(Array.class, Array::new);
		Group parent = this.parent;
		while (parent != null) {
			ascendants.add(parent);
			parent = parent.parent;
		}
		
		try {
			// Notify ascendants' capture listeners, starting at the root. Ascendants may stop an event before children receive it.
			Object[] ascendantsArray = ascendants.items;
			for (int i = ascendants.size - 1; i >= 0; i--) {
				Group currentTarget = (Group) ascendantsArray[i];
				currentTarget.notify(event, true);
				if (event.isStopped())
					return event.isCancelled();
			}
			
			// Notify the target capture listeners.
			notify(event, true);
			if (event.isStopped())
				return event.isCancelled();
			
			// Notify the target listeners.
			notify(event, false);
			if (!event.getBubbles())
				return event.isCancelled();
			if (event.isStopped())
				return event.isCancelled();
			
			// Notify ascendants' actor listeners, starting at the target. Children may stop an event before ascendants receive it.
			for (int i = 0, n = ascendants.size; i < n; i++) {
				((Group) ascendantsArray[i]).notify(event, false);
				if (event.isStopped())
					return event.isCancelled();
			}
			
			return event.isCancelled();
		} finally {
			ascendants.clear();
			Pools.free(ascendants);
		}
	}
	
	public boolean notify(Event event, boolean capture) {
		if (event.getTarget() == null)
			throw new IllegalArgumentException("The event target cannot be null.");
		
		DelayedRemovalArray<EventListener> listeners = capture ? captureListeners : this.listeners;
		if (listeners.size == 0)
			return event.isCancelled();
		
		event.setListenerActor(this);
		event.setCapture(capture);
		if (event.getStage() == null)
			event.setStage(stage);
		
		try {
			listeners.begin();
			for (int i = 0, n = listeners.size; i < n; i++)
				if (listeners.get(i).handle(event))
					event.handle();
			listeners.end();
		} catch (RuntimeException ex) {
			String context = toString();
			throw new RuntimeException("Actor: " + context.substring(0, Math.min(context.length(), 128)), ex);
		}
		
		return event.isCancelled();
	}
	
	public Actor hit(float x, float y, boolean touchable) {
		if (touchable && this.touchable != Touchable.enabled)
			return null;
		if (!isVisible())
			return null;
		return x >= 0 && x < width && y >= 0 && y < height ? this : null;
	}
	
	public boolean remove() {
		if (parent != null)
			return parent.removeActor(this, true);
		return false;
	}
	
	public boolean addListener(EventListener listener) {
		if (listener == null)
			throw new IllegalArgumentException("listener cannot be null.");
		if (!listeners.contains(listener, true)) {
			listeners.add(listener);
			return true;
		}
		return false;
	}
	
	public boolean removeListener(EventListener listener) {
		if (listener == null)
			throw new IllegalArgumentException("listener cannot be null.");
		return listeners.removeValue(listener, true);
	}
	
	public DelayedRemovalArray<EventListener> getListeners() {
		return listeners;
	}
	
	public boolean addCaptureListener(EventListener listener) {
		if (listener == null)
			throw new IllegalArgumentException("listener cannot be null.");
		if (!captureListeners.contains(listener, true))
			captureListeners.add(listener);
		return true;
	}
	
	public boolean removeCaptureListener(EventListener listener) {
		if (listener == null)
			throw new IllegalArgumentException("listener cannot be null.");
		return captureListeners.removeValue(listener, true);
	}
	
	public DelayedRemovalArray<EventListener> getCaptureListeners() {
		return captureListeners;
	}
	
	public void addAction(Action action) {
		action.setActor(this);
		actions.add(action);
		
		if (stage != null && stage.getActionsRequestRendering())
			Micro.graphics.requestRendering();
	}
	
	public void removeAction(Action action) {
		if (action != null && actions.removeValue(action, true))
			action.setActor(null);
	}
	
	public Array<Action> getActions() {
		return actions;
	}
	
	public boolean hasActions() {
		return actions.size > 0;
	}
	
	public void clearActions() {
		for (int i = actions.size - 1; i >= 0; i--)
			actions.get(i).setActor(null);
		actions.clear();
	}
	
	public void clearListeners() {
		listeners.clear();
		captureListeners.clear();
	}
	
	public void clear() {
		clearActions();
		clearListeners();
	}
	
	public Stage getStage() {
		return stage;
	}
	
	protected void setStage(Stage stage) {
		this.stage = stage;
	}
	
	public boolean isDescendantOf(Actor actor) {
		if (actor == null)
			throw new IllegalArgumentException("actor cannot be null.");
		Actor parent = this;
		do {
			if (parent == actor)
				return true;
			parent = parent.parent;
		} while (parent != null);
		return false;
	}
	
	public boolean isAscendantOf(Actor actor) {
		if (actor == null)
			throw new IllegalArgumentException("actor cannot be null.");
		do {
			if (actor == this)
				return true;
			actor = actor.parent;
		} while (actor != null);
		return false;
	}
	
	public <T extends Actor> T firstAscendant(Class<T> type) {
		if (type == null)
			throw new IllegalArgumentException("actor cannot be null.");
		Actor actor = this;
		do {
			if (type.isInstance(actor))
				return (T) actor;
			actor = actor.parent;
		} while (actor != null);
		return null;
	}
	
	public boolean hasParent() {
		return parent != null;
	}
	
	public Group getParent() {
		return parent;
	}
	
	protected void setParent(Group parent) {
		this.parent = parent;
	}
	
	public boolean isTouchable() {
		return touchable == Touchable.enabled;
	}
	
	public Touchable getTouchable() {
		return touchable;
	}
	
	public void setTouchable(Touchable touchable) {
		this.touchable = touchable;
	}
	
	public boolean isVisible() {
		return visible;
	}
	
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	public boolean ascendantsVisible() {
		Actor actor = this;
		do {
			if (!actor.isVisible())
				return false;
			actor = actor.parent;
		} while (actor != null);
		return true;
	}
	
	public boolean hasKeyboardFocus() {
		Stage stage = getStage();
		return stage != null && stage.getKeyboardFocus() == this;
	}
	
	public boolean hasScrollFocus() {
		Stage stage = getStage();
		return stage != null && stage.getScrollFocus() == this;
	}
	
	public boolean isTouchFocusTarget() {
		Stage stage = getStage();
		if (stage == null)
			return false;
		for (int i = 0, n = stage.touchFocuses.size; i < n; i++)
			if (stage.touchFocuses.get(i).target == this)
				return true;
		return false;
	}
	
	public boolean isTouchFocusListener() {
		Stage stage = getStage();
		if (stage == null)
			return false;
		for (int i = 0, n = stage.touchFocuses.size; i < n; i++)
			if (stage.touchFocuses.get(i).listenerActor == this)
				return true;
		return false;
	}
	
	public Object getUserObject() {
		return userObject;
	}
	
	/**
	 * Sets an application specific object for convenience.
	 */
	public void setUserObject(Object userObject) {
		this.userObject = userObject;
	}
	
	/**
	 * Returns the X position of the actor's left edge.
	 */
	public float getX() {
		return x;
	}
	
	/**
	 * Returns the X position of the specified {@link Align alignment}.
	 */
	public float getX(int alignment) {
		float x = this.x;
		if ((alignment & right) != 0)
			x += width;
		else if ((alignment & left) == 0) //
			x += width / 2;
		return x;
	}
	
	public void setX(float x) {
		if (this.x != x) {
			this.x = x;
			positionChanged();
		}
	}
	
	/**
	 * Sets the x position using the specified {@link Align alignment}. Note this may set the position to non-integer
	 * coordinates.
	 */
	public void setX(float x, int alignment) {
		
		if ((alignment & right) != 0)
			x -= width;
		else if ((alignment & left) == 0) //
			x -= width / 2;
		
		if (this.x != x) {
			this.x = x;
			positionChanged();
		}
	}
	
	/**
	 * Returns the Y position of the actor's bottom edge.
	 */
	public float getY() {
		return y;
	}
	
	public void setY(float y) {
		if (this.y != y) {
			this.y = y;
			positionChanged();
		}
	}
	
	/**
	 * Sets the y position using the specified {@link Align alignment}. Note this may set the position to non-integer
	 * coordinates.
	 */
	public void setY(float y, int alignment) {
		
		if ((alignment & top) != 0)
			y -= height;
		else if ((alignment & bottom) == 0) //
			y -= height / 2;
		
		if (this.y != y) {
			this.y = y;
			positionChanged();
		}
	}
	
	/**
	 * Returns the Y position of the specified {@link Align alignment}.
	 */
	public float getY(int alignment) {
		float y = this.y;
		if ((alignment & top) != 0)
			y += height;
		else if ((alignment & bottom) == 0) //
			y += height / 2;
		return y;
	}
	
	/**
	 * Sets the position of the actor's bottom left corner.
	 */
	public void setPosition(float x, float y) {
		if (this.x != x || this.y != y) {
			this.x = x;
			this.y = y;
			positionChanged();
		}
	}
	
	/**
	 * Sets the position using the specified {@link Align alignment}. Note this may set the position to non-integer
	 * coordinates.
	 */
	public void setPosition(float x, float y, int alignment) {
		if ((alignment & right) != 0)
			x -= width;
		else if ((alignment & left) == 0) //
			x -= width / 2;
		
		if ((alignment & top) != 0)
			y -= height;
		else if ((alignment & bottom) == 0) //
			y -= height / 2;
		
		if (this.x != x || this.y != y) {
			this.x = x;
			this.y = y;
			positionChanged();
		}
	}
	
	/**
	 * Add x and y to current position
	 */
	public void moveBy(float x, float y) {
		if (x != 0 || y != 0) {
			this.x += x;
			this.y += y;
			positionChanged();
		}
	}
	
	public float getWidth() {
		return width;
	}
	
	public void setWidth(float width) {
		if (this.width != width) {
			this.width = width;
			sizeChanged();
		}
	}
	
	public float getHeight() {
		return height;
	}
	
	public void setHeight(float height) {
		if (this.height != height) {
			this.height = height;
			sizeChanged();
		}
	}
	
	/**
	 * Returns y plus height.
	 */
	public float getTop() {
		return y + height;
	}
	
	/**
	 * Returns x plus width.
	 */
	public float getRight() {
		return x + width;
	}
	
	/**
	 * Called when the actor's position has been changed.
	 */
	protected void positionChanged() {
	}
	
	/**
	 * Called when the actor's size has been changed.
	 */
	protected void sizeChanged() {
	}
	
	/**
	 * Called when the actor's scale has been changed.
	 */
	protected void scaleChanged() {
	}
	
	/**
	 * Called when the actor's rotation has been changed.
	 */
	protected void rotationChanged() {
	}
	
	/**
	 * Sets the width and height.
	 */
	public void setSize(float width, float height) {
		if (this.width != width || this.height != height) {
			this.width = width;
			this.height = height;
			sizeChanged();
		}
	}
	
	/**
	 * Adds the specified size to the current size.
	 */
	public void sizeBy(float size) {
		if (size != 0) {
			width += size;
			height += size;
			sizeChanged();
		}
	}
	
	/**
	 * Adds the specified size to the current size.
	 */
	public void sizeBy(float width, float height) {
		if (width != 0 || height != 0) {
			this.width += width;
			this.height += height;
			sizeChanged();
		}
	}
	
	/**
	 * Set bounds the x, y, width, and height.
	 */
	public void setBounds(float x, float y, float width, float height) {
		if (this.x != x || this.y != y) {
			this.x = x;
			this.y = y;
			positionChanged();
		}
		if (this.width != width || this.height != height) {
			this.width = width;
			this.height = height;
			sizeChanged();
		}
	}
	
	public float getOriginX() {
		return originX;
	}
	
	public void setOriginX(float originX) {
		this.originX = originX;
	}
	
	public float getOriginY() {
		return originY;
	}
	
	public void setOriginY(float originY) {
		this.originY = originY;
	}
	
	/**
	 * Sets the origin position which is relative to the actor's bottom left corner.
	 */
	public void setOrigin(float originX, float originY) {
		this.originX = originX;
		this.originY = originY;
	}
	
	/**
	 * Sets the origin position to the specified {@link Align alignment}.
	 */
	public void setOrigin(int alignment) {
		if ((alignment & left) != 0)
			originX = 0;
		else if ((alignment & right) != 0)
			originX = width;
		else
			originX = width / 2;
		
		if ((alignment & bottom) != 0)
			originY = 0;
		else if ((alignment & top) != 0)
			originY = height;
		else
			originY = height / 2;
	}
	
	public float getScaleX() {
		return scaleX;
	}
	
	public void setScaleX(float scaleX) {
		if (this.scaleX != scaleX) {
			this.scaleX = scaleX;
			scaleChanged();
		}
	}
	
	public float getScaleY() {
		return scaleY;
	}
	
	public void setScaleY(float scaleY) {
		if (this.scaleY != scaleY) {
			this.scaleY = scaleY;
			scaleChanged();
		}
	}
	
	/**
	 * Sets the scale for both X and Y
	 */
	public void setScale(float scaleXY) {
		if (this.scaleX != scaleXY || this.scaleY != scaleXY) {
			this.scaleX = scaleXY;
			this.scaleY = scaleXY;
			scaleChanged();
		}
	}
	
	/**
	 * Sets the scale X and scale Y.
	 */
	public void setScale(float scaleX, float scaleY) {
		if (this.scaleX != scaleX || this.scaleY != scaleY) {
			this.scaleX = scaleX;
			this.scaleY = scaleY;
			scaleChanged();
		}
	}
	
	/**
	 * Adds the specified scale to the current scale.
	 */
	public void scaleBy(float scale) {
		if (scale != 0) {
			scaleX += scale;
			scaleY += scale;
			scaleChanged();
		}
	}
	
	/**
	 * Adds the specified scale to the current scale.
	 */
	public void scaleBy(float scaleX, float scaleY) {
		if (scaleX != 0 || scaleY != 0) {
			this.scaleX += scaleX;
			this.scaleY += scaleY;
			scaleChanged();
		}
	}
	
	public float getRotation() {
		return rotation;
	}
	
	public void setRotation(float degrees) {
		if (this.rotation != degrees) {
			this.rotation = degrees;
			rotationChanged();
		}
	}
	
	/**
	 * Adds the specified rotation to the current rotation.
	 */
	public void rotateBy(float amountInDegrees) {
		if (amountInDegrees != 0) {
			rotation = (rotation + amountInDegrees) % 360;
			rotationChanged();
		}
	}
	
	public void setColor(Color color) {
		this.color.set(color);
	}
	
	public void setColor(float r, float g, float b, float a) {
		color.set(r, g, b, a);
	}
	
	/**
	 * Returns the color the actor will be tinted when drawn. The returned instance can be modified to change the color.
	 */
	public Color getColor() {
		return color;
	}
	
	/**
	 * @return May be null.
	 * @see #setName(String)
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Set the actor's name, which is used for identification convenience and by {@link #toString()}.
	 *
	 * @param name May be null.
	 * @see Group#findActor(String)
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Changes the z-order for this actor so it is in front of all siblings.
	 */
	public void toFront() {
		setZIndex(Integer.MAX_VALUE);
	}
	
	/**
	 * Changes the z-order for this actor so it is in back of all siblings.
	 */
	public void toBack() {
		setZIndex(0);
	}
	
	/**
	 * Sets the z-index of this actor. The z-index is the index into the parent's {@link Group#getChildren() children}, where a
	 * lower index is below a higher index. Setting a z-index higher than the number of children will move the child to the front.
	 * Setting a z-index less than zero is invalid.
	 *
	 * @return true if the z-index changed.
	 */
	public boolean setZIndex(int index) {
		if (index < 0)
			throw new IllegalArgumentException("ZIndex cannot be < 0.");
		Group parent = this.parent;
		if (parent == null)
			return false;
		Array<Actor> children = parent.children;
		if (children.size <= 1)
			return false;
		index = Math.min(index, children.size - 1);
		if (children.get(index) == this)
			return false;
		if (!children.removeValue(this, true))
			return false;
		children.insert(index, this);
		return true;
	}
	
	/**
	 * Returns the z-index of this actor, or -1 if the actor is not in a group.
	 *
	 * @see #setZIndex(int)
	 */
	public int getZIndex() {
		Group parent = this.parent;
		if (parent == null)
			return -1;
		return parent.children.indexOf(this, true);
	}
	
	/**
	 * Calls {@link #clipBegin(float, float, float, float)} to clip this actor's bounds.
	 */
	public boolean clipBegin() {
		return clipBegin(x, y, width, height);
	}
	
	/**
	 * Clips the specified screen aligned rectangle, specified relative to the transform matrix of the stage's Batch. The
	 * transform matrix and the stage's camera must not have rotational components. Calling this method must be followed by a call
	 * to {@link #clipEnd()} if true is returned.
	 *
	 * @return false if the clipping area is zero and no drawing should occur.
	 * @see Scissors
	 */
	public boolean clipBegin(float x, float y, float width, float height) {
		if (width <= 0 || height <= 0)
			return false;
		Stage stage = this.stage;
		if (stage == null)
			return false;
		Rectangle tableBounds = Rectangle.tmp;
		tableBounds.x = x;
		tableBounds.y = y;
		tableBounds.width = width;
		tableBounds.height = height;
		Rectangle scissorBounds = Pools.obtain(Rectangle.class, Rectangle::new);
		stage.calculateScissors(tableBounds, scissorBounds);
		if (Scissors.pushScissors(scissorBounds))
			return true;
		Pools.free(scissorBounds);
		return false;
	}
	
	/**
	 * Ends clipping begun by {@link #clipBegin(float, float, float, float)}.
	 */
	public void clipEnd() {
		Pools.free(Scissors.popScissors());
	}
	
	/**
	 * Transforms the specified point in screen coordinates to the actor's local coordinate system.
	 *
	 * @see Stage#screenToStageCoordinates(Vector2)
	 */
	public Vector2 screenToLocalCoordinates(Vector2 screenCoords) {
		Stage stage = this.stage;
		if (stage == null)
			return screenCoords;
		return stageToLocalCoordinates(stage.screenToStageCoordinates(screenCoords));
	}
	
	/**
	 * Transforms the specified point in the stage's coordinates to the actor's local coordinate system.
	 */
	public Vector2 stageToLocalCoordinates(Vector2 stageCoords) {
		if (parent != null)
			parent.stageToLocalCoordinates(stageCoords);
		parentToLocalCoordinates(stageCoords);
		return stageCoords;
	}
	
	public Vector2 parentToLocalCoordinates(Vector2 parentCoords) {
		final float rotation = this.rotation;
		final float scaleX = this.scaleX;
		final float scaleY = this.scaleY;
		final float childX = x;
		final float childY = y;
		if (rotation == 0) {
			if (scaleX == 1 && scaleY == 1) {
				parentCoords.x -= childX;
				parentCoords.y -= childY;
			} else {
				final float originX = this.originX;
				final float originY = this.originY;
				parentCoords.x = (parentCoords.x - childX - originX) / scaleX + originX;
				parentCoords.y = (parentCoords.y - childY - originY) / scaleY + originY;
			}
		} else {
			final float cos = (float) Math.cos(rotation * MathUtils.degreesToRadians);
			final float sin = (float) Math.sin(rotation * MathUtils.degreesToRadians);
			final float originX = this.originX;
			final float originY = this.originY;
			final float tox = parentCoords.x - childX - originX;
			final float toy = parentCoords.y - childY - originY;
			parentCoords.x = (tox * cos + toy * sin) / scaleX + originX;
			parentCoords.y = (tox * -sin + toy * cos) / scaleY + originY;
		}
		return parentCoords;
	}
	
	public Vector2 localToScreenCoordinates(Vector2 localCoords) {
		Stage stage = this.stage;
		if (stage == null)
			return localCoords;
		return stage.stageToScreenCoordinates(localToAscendantCoordinates(null, localCoords));
	}
	
	public Vector2 localToStageCoordinates(Vector2 localCoords) {
		return localToAscendantCoordinates(null, localCoords);
	}
	
	public Vector2 localToParentCoordinates(Vector2 localCoords) {
		final float rotation = -this.rotation;
		final float scaleX = this.scaleX;
		final float scaleY = this.scaleY;
		final float x = this.x;
		final float y = this.y;
		if (rotation == 0) {
			if (scaleX == 1 && scaleY == 1) {
				localCoords.x += x;
				localCoords.y += y;
			} else {
				final float originX = this.originX;
				final float originY = this.originY;
				localCoords.x = (localCoords.x - originX) * scaleX + originX + x;
				localCoords.y = (localCoords.y - originY) * scaleY + originY + y;
			}
		} else {
			final float cos = (float) Math.cos(rotation * MathUtils.degreesToRadians);
			final float sin = (float) Math.sin(rotation * MathUtils.degreesToRadians);
			final float originX = this.originX;
			final float originY = this.originY;
			final float tox = (localCoords.x - originX) * scaleX;
			final float toy = (localCoords.y - originY) * scaleY;
			localCoords.x = (tox * cos + toy * sin) + originX + x;
			localCoords.y = (tox * -sin + toy * cos) + originY + y;
		}
		return localCoords;
	}
	
	public Vector2 localToAscendantCoordinates(Actor ascendant, Vector2 localCoords) {
		Actor actor = this;
		do {
			actor.localToParentCoordinates(localCoords);
			actor = actor.parent;
			if (actor == ascendant)
				return localCoords;
		} while (actor != null);
		throw new IllegalArgumentException("Actor is not an ascendant: " + ascendant);
	}
	
	public Vector2 localToActorCoordinates(Actor actor, Vector2 localCoords) {
		localToStageCoordinates(localCoords);
		return actor.stageToLocalCoordinates(localCoords);
	}
	
	public void drawDebug(ShapeRenderer shapes) {
		drawDebugBounds(shapes);
	}
	
	protected void drawDebugBounds(ShapeRenderer shapes) {
		if (!debug)
			return;
		shapes.set(ShapeType.Line);
		if (stage != null)
			shapes.setColor(stage.getDebugColor());
		shapes.rect(x, y, originX, originY, width, height, scaleX, scaleY, rotation);
	}
	
	public void setDebug(boolean enabled) {
		debug = enabled;
		if (enabled)
			Stage.debug = true;
	}
	
	public boolean getDebug() {
		return debug;
	}
	
	public Actor debug() {
		setDebug(true);
		return this;
	}
	
	public String toString() {
		String name = this.name;
		if (name == null) {
			name = getClass().getName();
			int dotIndex = name.lastIndexOf('.');
			if (dotIndex != -1)
				name = name.substring(dotIndex + 1);
		}
		return name;
	}
	
}
