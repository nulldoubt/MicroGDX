package me.nulldoubt.micro.scenes.scene2d.actions;

import me.nulldoubt.micro.scenes.scene2d.Action;
import me.nulldoubt.micro.scenes.scene2d.Actor;
import me.nulldoubt.micro.scenes.scene2d.Event;
import me.nulldoubt.micro.scenes.scene2d.EventListener;

public abstract class EventAction<T extends Event> extends Action {
	
	final Class<? extends T> eventClass;
	boolean result, active;
	
	private final EventListener listener = new EventListener() {
		public boolean handle(Event event) {
			if (!active || !eventClass.isInstance(event))
				return false;
			result = EventAction.this.handle((T) event);
			return result;
		}
	};
	
	public EventAction(Class<? extends T> eventClass) {
		this.eventClass = eventClass;
	}
	
	public void restart() {
		result = false;
		active = false;
	}
	
	public void setTarget(Actor newTarget) {
		if (target != null)
			target.removeListener(listener);
		super.setTarget(newTarget);
		if (newTarget != null)
			newTarget.addListener(listener);
	}
	
	public abstract boolean handle(T event);
	
	public boolean act(float delta) {
		active = true;
		return result;
	}
	
	public boolean isActive() {
		return active;
	}
	
	public void setActive(boolean active) {
		this.active = active;
	}
	
}
