package me.nulldoubt.micro.scenes.scene2d.actions;

import me.nulldoubt.micro.scenes.scene2d.Action;
import me.nulldoubt.micro.scenes.scene2d.EventListener;

/** Removes a listener from an actor.
 * @author Nathan Sweet */
public class RemoveListenerAction extends Action {
	private EventListener listener;
	private boolean capture;

	public boolean act (float delta) {
		if (capture)
			target.removeCaptureListener(listener);
		else
			target.removeListener(listener);
		return true;
	}

	public EventListener getListener () {
		return listener;
	}

	public void setListener (EventListener listener) {
		this.listener = listener;
	}

	public boolean getCapture () {
		return capture;
	}

	public void setCapture (boolean capture) {
		this.capture = capture;
	}

	public void reset () {
		super.reset();
		listener = null;
	}
}
