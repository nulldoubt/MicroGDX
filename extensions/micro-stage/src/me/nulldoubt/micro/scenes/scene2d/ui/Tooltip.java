package me.nulldoubt.micro.scenes.scene2d.ui;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.math.Vector2;
import me.nulldoubt.micro.scenes.scene2d.*;

public class Tooltip<T extends Actor> extends InputListener {
	
	static Vector2 tmp = new Vector2();
	
	private final TooltipManager manager;
	final Container<T> container;
	boolean instant, always, touchIndependent;
	Actor targetActor;
	
	public Tooltip(T contents) {
		this(contents, TooltipManager.getInstance());
	}
	
	public Tooltip(T contents, TooltipManager manager) {
		this.manager = manager;
		
		container = new Container<>(contents) {
			public void act(float delta) {
				super.act(delta);
				if (targetActor != null && targetActor.getStage() == null)
					remove();
			}
		};
		container.setTouchable(Touchable.disabled);
	}
	
	public TooltipManager getManager() {
		return manager;
	}
	
	public Container<T> getContainer() {
		return container;
	}
	
	public void setActor(T contents) {
		container.setActor(contents);
	}
	
	public T getActor() {
		return container.getActor();
	}
	
	public void setInstant(boolean instant) {
		this.instant = instant;
	}
	
	public void setAlways(boolean always) {
		this.always = always;
	}
	
	public void setTouchIndependent(boolean touchIndependent) {
		this.touchIndependent = touchIndependent;
	}
	
	public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
		if (instant) {
			container.toFront();
			return false;
		}
		manager.touchDown(this);
		return false;
	}
	
	public boolean mouseMoved(InputEvent event, float x, float y) {
		if (container.hasParent())
			return false;
		setContainerPosition(event.getListenerActor(), x, y);
		return true;
	}
	
	private void setContainerPosition(Actor actor, float x, float y) {
		this.targetActor = actor;
		Stage stage = actor.getStage();
		if (stage == null)
			return;
		
		container.setSize(manager.maxWidth, Integer.MAX_VALUE);
		container.validate();
		container.width(container.getActor().getWidth());
		container.pack();
		
		float offsetX = manager.offsetX, offsetY = manager.offsetY, dist = manager.edgeDistance;
		Vector2 point = actor.localToStageCoordinates(tmp.set(x + offsetX, y - offsetY - container.getHeight()));
		if (point.y < dist)
			point = actor.localToStageCoordinates(tmp.set(x + offsetX, y + offsetY));
		if (point.x < dist)
			point.x = dist;
		if (point.x + container.getWidth() > stage.getWidth() - dist)
			point.x = stage.getWidth() - dist - container.getWidth();
		if (point.y + container.getHeight() > stage.getHeight() - dist)
			point.y = stage.getHeight() - dist - container.getHeight();
		container.setPosition(point.x, point.y);
		
		point = actor.localToStageCoordinates(tmp.set(actor.getWidth() / 2, actor.getHeight() / 2));
		point.sub(container.getX(), container.getY());
		container.setOrigin(point.x, point.y);
	}
	
	public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
		if (pointer != -1)
			return;
		if (touchIndependent && Micro.input.isTouched())
			return;
		Actor actor = event.getListenerActor();
		if (fromActor != null && fromActor.isDescendantOf(actor))
			return;
		setContainerPosition(actor, x, y);
		manager.enter(this);
	}
	
	public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
		if (toActor != null && toActor.isDescendantOf(event.getListenerActor()))
			return;
		hide();
	}
	
	public void hide() {
		manager.hide(this);
	}
	
}
