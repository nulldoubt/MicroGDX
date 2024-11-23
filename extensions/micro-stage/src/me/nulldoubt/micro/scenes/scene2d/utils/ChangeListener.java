package me.nulldoubt.micro.scenes.scene2d.utils;

import me.nulldoubt.micro.scenes.scene2d.Actor;
import me.nulldoubt.micro.scenes.scene2d.Event;
import me.nulldoubt.micro.scenes.scene2d.EventListener;

public abstract class ChangeListener implements EventListener {
	
	public boolean handle(Event event) {
		if (!(event instanceof ChangeEvent))
			return false;
		changed((ChangeEvent) event, event.getTarget());
		return false;
	}
	
	public abstract void changed(ChangeEvent event, Actor actor);
	
	public static class ChangeEvent extends Event {}
	
}
