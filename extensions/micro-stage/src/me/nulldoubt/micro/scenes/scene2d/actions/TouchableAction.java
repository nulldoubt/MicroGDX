package me.nulldoubt.micro.scenes.scene2d.actions;

import me.nulldoubt.micro.scenes.scene2d.Action;
import me.nulldoubt.micro.scenes.scene2d.Touchable;

public class TouchableAction extends Action {
	
	public Touchable touchable;
	
	public boolean act(float delta) {
		target.setTouchable(touchable);
		return true;
	}
	
}
