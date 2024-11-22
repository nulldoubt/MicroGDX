package me.nulldoubt.micro.scenes.scene2d.utils;

import me.nulldoubt.micro.scenes.scene2d.Actor;
import me.nulldoubt.micro.scenes.scene2d.Event;
import me.nulldoubt.micro.scenes.scene2d.EventListener;

public abstract class FocusListener implements EventListener {
	
	public boolean handle(Event event) {
		if (!(event instanceof FocusEvent focusEvent))
			return false;
		switch (focusEvent.getType()) {
			case keyboard:
				keyboardFocusChanged(focusEvent, event.getTarget(), focusEvent.isFocused());
				break;
			case scroll:
				scrollFocusChanged(focusEvent, event.getTarget(), focusEvent.isFocused());
				break;
		}
		return false;
	}
	
	public void keyboardFocusChanged(FocusEvent event, Actor actor, boolean focused) {}
	
	public void scrollFocusChanged(FocusEvent event, Actor actor, boolean focused) {}
	
	public static class FocusEvent extends Event {
		
		private boolean focused;
		private Type type;
		private Actor relatedActor;
		
		public void reset() {
			super.reset();
			relatedActor = null;
		}
		
		public boolean isFocused() {
			return focused;
		}
		
		public void setFocused(boolean focused) {
			this.focused = focused;
		}
		
		public Type getType() {
			return type;
		}
		
		public void setType(Type focusType) {
			this.type = focusType;
		}
		
		/**
		 * The actor related to the event. When focus is lost, this is the new actor being focused, or null. When focus is gained,
		 * this is the previous actor that was focused, or null.
		 */
		public Actor getRelatedActor() {
			return relatedActor;
		}
		
		/**
		 * @param relatedActor May be null.
		 */
		public void setRelatedActor(Actor relatedActor) {
			this.relatedActor = relatedActor;
		}
		
		/**
		 * @author Nathan Sweet
		 */
		public enum Type {
			keyboard, scroll
		}
		
	}
	
}
