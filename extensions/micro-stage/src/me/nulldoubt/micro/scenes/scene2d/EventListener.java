package me.nulldoubt.micro.scenes.scene2d;

/** Low level interface for receiving events. Typically there is a listener class for each specific event class.
 * @see InputListener
 * @see InputEvent
 * @author Nathan Sweet */
public interface EventListener {
	/** Try to handle the given event, if it is applicable.
	 * @return true if the event should be considered {@link Event#handle() handled} by scene2d. */
	public boolean handle (Event event);
}
