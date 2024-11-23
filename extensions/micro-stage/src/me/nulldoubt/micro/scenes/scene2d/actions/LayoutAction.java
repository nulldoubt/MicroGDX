package me.nulldoubt.micro.scenes.scene2d.actions;

import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.scenes.scene2d.Action;
import me.nulldoubt.micro.scenes.scene2d.Actor;
import me.nulldoubt.micro.scenes.scene2d.utils.Layout;

public class LayoutAction extends Action {
	
	public boolean enabled;
	
	public void setTarget(Actor actor) {
		if (actor != null && !(actor instanceof Layout))
			throw new MicroRuntimeException("Actor must implement layout: " + actor);
		super.setTarget(actor);
	}
	
	public boolean act(float delta) {
		((Layout) target).setLayoutEnabled(enabled);
		return true;
	}
	
}
