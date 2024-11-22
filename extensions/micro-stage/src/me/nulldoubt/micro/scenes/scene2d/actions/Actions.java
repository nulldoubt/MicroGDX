package me.nulldoubt.micro.scenes.scene2d.actions;

import me.nulldoubt.micro.graphics.Color;
import me.nulldoubt.micro.math.Interpolation;
import me.nulldoubt.micro.scenes.scene2d.Action;
import me.nulldoubt.micro.scenes.scene2d.Actor;
import me.nulldoubt.micro.scenes.scene2d.EventListener;
import me.nulldoubt.micro.scenes.scene2d.Touchable;
import me.nulldoubt.micro.utils.pools.Pool;
import me.nulldoubt.micro.utils.pools.Pools;

import java.util.function.Supplier;

public class Actions {
	
	public static <T extends Action> T action(Class<T> type, final Supplier<T> supplier) {
		Pool<T> pool = Pools.get(type, supplier);
		T action = pool.obtain();
		action.setPool(pool);
		return action;
	}
	
	public static AddAction addAction(Action action) {
		AddAction addAction = action(AddAction.class, AddAction::new);
		addAction.setAction(action);
		return addAction;
	}
	
	public static AddAction addAction(Action action, Actor targetActor) {
		AddAction addAction = action(AddAction.class, AddAction::new);
		addAction.setTarget(targetActor);
		addAction.setAction(action);
		return addAction;
	}
	
	public static RemoveAction removeAction(Action action) {
		RemoveAction removeAction = action(RemoveAction.class, RemoveAction::new);
		removeAction.setAction(action);
		return removeAction;
	}
	
	public static RemoveAction removeAction(Action action, Actor targetActor) {
		RemoveAction removeAction = action(RemoveAction.class, RemoveAction::new);
		removeAction.setTarget(targetActor);
		removeAction.setAction(action);
		return removeAction;
	}
	
	public static MoveToAction moveTo(float x, float y) {
		return moveTo(x, y, 0, null);
	}
	
	public static MoveToAction moveTo(float x, float y, float duration) {
		return moveTo(x, y, duration, null);
	}
	
	public static MoveToAction moveTo(float x, float y, float duration, Interpolation interpolation) {
		MoveToAction action = action(MoveToAction.class, MoveToAction::new);
		action.setPosition(x, y);
		action.setDuration(duration);
		action.setInterpolation(interpolation);
		return action;
	}
	
	public static MoveToAction moveToAligned(float x, float y, int alignment) {
		return moveToAligned(x, y, alignment, 0, null);
	}
	
	public static MoveToAction moveToAligned(float x, float y, int alignment, float duration) {
		return moveToAligned(x, y, alignment, duration, null);
	}
	
	public static MoveToAction moveToAligned(float x, float y, int alignment, float duration, Interpolation interpolation) {
		MoveToAction action = action(MoveToAction.class, MoveToAction::new);
		action.setPosition(x, y, alignment);
		action.setDuration(duration);
		action.setInterpolation(interpolation);
		return action;
	}
	
	public static MoveByAction moveBy(float amountX, float amountY) {
		return moveBy(amountX, amountY, 0, null);
	}
	
	public static MoveByAction moveBy(float amountX, float amountY, float duration) {
		return moveBy(amountX, amountY, duration, null);
	}
	
	public static MoveByAction moveBy(float amountX, float amountY, float duration, Interpolation interpolation) {
		MoveByAction action = action(MoveByAction.class, MoveByAction::new);
		action.setAmount(amountX, amountY);
		action.setDuration(duration);
		action.setInterpolation(interpolation);
		return action;
	}
	
	public static SizeToAction sizeTo(float x, float y) {
		return sizeTo(x, y, 0, null);
	}
	
	public static SizeToAction sizeTo(float x, float y, float duration) {
		return sizeTo(x, y, duration, null);
	}
	
	public static SizeToAction sizeTo(float x, float y, float duration, Interpolation interpolation) {
		SizeToAction action = action(SizeToAction.class, SizeToAction::new);
		action.setSize(x, y);
		action.setDuration(duration);
		action.setInterpolation(interpolation);
		return action;
	}
	
	public static SizeByAction sizeBy(float amountX, float amountY) {
		return sizeBy(amountX, amountY, 0, null);
	}
	
	public static SizeByAction sizeBy(float amountX, float amountY, float duration) {
		return sizeBy(amountX, amountY, duration, null);
	}
	
	public static SizeByAction sizeBy(float amountX, float amountY, float duration, Interpolation interpolation) {
		SizeByAction action = action(SizeByAction.class, SizeByAction::new);
		action.setAmount(amountX, amountY);
		action.setDuration(duration);
		action.setInterpolation(interpolation);
		return action;
	}
	
	public static ScaleToAction scaleTo(float x, float y) {
		return scaleTo(x, y, 0, null);
	}
	
	public static ScaleToAction scaleTo(float x, float y, float duration) {
		return scaleTo(x, y, duration, null);
	}
	
	public static ScaleToAction scaleTo(float x, float y, float duration, Interpolation interpolation) {
		ScaleToAction action = action(ScaleToAction.class, ScaleToAction::new);
		action.setScale(x, y);
		action.setDuration(duration);
		action.setInterpolation(interpolation);
		return action;
	}
	
	public static ScaleByAction scaleBy(float amountX, float amountY) {
		return scaleBy(amountX, amountY, 0, null);
	}
	
	public static ScaleByAction scaleBy(float amountX, float amountY, float duration) {
		return scaleBy(amountX, amountY, duration, null);
	}
	
	public static ScaleByAction scaleBy(float amountX, float amountY, float duration, Interpolation interpolation) {
		ScaleByAction action = action(ScaleByAction.class, ScaleByAction::new);
		action.setAmount(amountX, amountY);
		action.setDuration(duration);
		action.setInterpolation(interpolation);
		return action;
	}
	
	public static RotateToAction rotateTo(float rotation) {
		return rotateTo(rotation, 0, null);
	}
	
	public static RotateToAction rotateTo(float rotation, float duration) {
		return rotateTo(rotation, duration, null);
	}
	
	public static RotateToAction rotateTo(float rotation, float duration, Interpolation interpolation) {
		RotateToAction action = action(RotateToAction.class, RotateToAction::new);
		action.setRotation(rotation);
		action.setDuration(duration);
		action.setInterpolation(interpolation);
		return action;
	}
	
	public static RotateByAction rotateBy(float rotationAmount) {
		return rotateBy(rotationAmount, 0, null);
	}
	
	public static RotateByAction rotateBy(float rotationAmount, float duration) {
		return rotateBy(rotationAmount, duration, null);
	}
	
	public static RotateByAction rotateBy(float rotationAmount, float duration, Interpolation interpolation) {
		RotateByAction action = action(RotateByAction.class, RotateByAction::new);
		action.setAmount(rotationAmount);
		action.setDuration(duration);
		action.setInterpolation(interpolation);
		return action;
	}
	
	public static ColorAction color(Color color) {
		return color(color, 0, null);
	}
	
	public static ColorAction color(Color color, float duration) {
		return color(color, duration, null);
	}
	
	public static ColorAction color(Color color, float duration, Interpolation interpolation) {
		ColorAction action = action(ColorAction.class, ColorAction::new);
		action.setEndColor(color);
		action.setDuration(duration);
		action.setInterpolation(interpolation);
		return action;
	}
	
	public static AlphaAction alpha(float a) {
		return alpha(a, 0, null);
	}
	
	public static AlphaAction alpha(float a, float duration) {
		return alpha(a, duration, null);
	}
	
	public static AlphaAction alpha(float a, float duration, Interpolation interpolation) {
		AlphaAction action = action(AlphaAction.class, AlphaAction::new);
		action.setAlpha(a);
		action.setDuration(duration);
		action.setInterpolation(interpolation);
		return action;
	}
	
	public static AlphaAction fadeOut(float duration) {
		return alpha(0, duration, null);
	}
	
	public static AlphaAction fadeOut(float duration, Interpolation interpolation) {
		AlphaAction action = action(AlphaAction.class, AlphaAction::new);
		action.setAlpha(0);
		action.setDuration(duration);
		action.setInterpolation(interpolation);
		return action;
	}
	
	public static AlphaAction fadeIn(float duration) {
		return alpha(1, duration, null);
	}
	
	public static AlphaAction fadeIn(float duration, Interpolation interpolation) {
		AlphaAction action = action(AlphaAction.class, AlphaAction::new);
		action.setAlpha(1);
		action.setDuration(duration);
		action.setInterpolation(interpolation);
		return action;
	}
	
	public static VisibleAction show() {
		return visible(true);
	}
	
	public static VisibleAction hide() {
		return visible(false);
	}
	
	public static VisibleAction visible(boolean visible) {
		VisibleAction action = action(VisibleAction.class, VisibleAction::new);
		action.setVisible(visible);
		return action;
	}
	
	public static TouchableAction touchable(Touchable touchable) {
		TouchableAction action = action(TouchableAction.class, TouchableAction::new);
		action.setTouchable(touchable);
		return action;
	}
	
	public static RemoveActorAction removeActor() {
		return action(RemoveActorAction.class, RemoveActorAction::new);
	}
	
	public static RemoveActorAction removeActor(Actor removeActor) {
		RemoveActorAction action = action(RemoveActorAction.class, RemoveActorAction::new);
		action.setTarget(removeActor);
		return action;
	}
	
	public static DelayAction delay(float duration) {
		DelayAction action = action(DelayAction.class, DelayAction::new);
		action.setDuration(duration);
		return action;
	}
	
	public static DelayAction delay(float duration, Action delayedAction) {
		DelayAction action = action(DelayAction.class, DelayAction::new);
		action.setDuration(duration);
		action.setAction(delayedAction);
		return action;
	}
	
	public static TimeScaleAction timeScale(float scale, Action scaledAction) {
		TimeScaleAction action = action(TimeScaleAction.class, TimeScaleAction::new);
		action.setScale(scale);
		action.setAction(scaledAction);
		return action;
	}
	
	public static SequenceAction sequence(Action action1) {
		SequenceAction action = action(SequenceAction.class, SequenceAction::new);
		action.addAction(action1);
		return action;
	}
	
	public static SequenceAction sequence(Action action1, Action action2) {
		SequenceAction action = action(SequenceAction.class, SequenceAction::new);
		action.addAction(action1);
		action.addAction(action2);
		return action;
	}
	
	public static SequenceAction sequence(Action action1, Action action2, Action action3) {
		SequenceAction action = action(SequenceAction.class, SequenceAction::new);
		action.addAction(action1);
		action.addAction(action2);
		action.addAction(action3);
		return action;
	}
	
	public static SequenceAction sequence(Action action1, Action action2, Action action3, Action action4) {
		SequenceAction action = action(SequenceAction.class, SequenceAction::new);
		action.addAction(action1);
		action.addAction(action2);
		action.addAction(action3);
		action.addAction(action4);
		return action;
	}
	
	public static SequenceAction sequence(Action action1, Action action2, Action action3, Action action4, Action action5) {
		SequenceAction action = action(SequenceAction.class, SequenceAction::new);
		action.addAction(action1);
		action.addAction(action2);
		action.addAction(action3);
		action.addAction(action4);
		action.addAction(action5);
		return action;
	}
	
	public static SequenceAction sequence(Action... actions) {
		SequenceAction action = action(SequenceAction.class, SequenceAction::new);
		for (Action value : actions)
			action.addAction(value);
		return action;
	}
	
	public static SequenceAction sequence() {
		return action(SequenceAction.class, SequenceAction::new);
	}
	
	public static ParallelAction parallel(Action action1) {
		ParallelAction action = action(ParallelAction.class, ParallelAction::new);
		action.addAction(action1);
		return action;
	}
	
	public static ParallelAction parallel(Action action1, Action action2) {
		ParallelAction action = action(ParallelAction.class, ParallelAction::new);
		action.addAction(action1);
		action.addAction(action2);
		return action;
	}
	
	public static ParallelAction parallel(Action action1, Action action2, Action action3) {
		ParallelAction action = action(ParallelAction.class, ParallelAction::new);
		action.addAction(action1);
		action.addAction(action2);
		action.addAction(action3);
		return action;
	}
	
	public static ParallelAction parallel(Action action1, Action action2, Action action3, Action action4) {
		ParallelAction action = action(ParallelAction.class, ParallelAction::new);
		action.addAction(action1);
		action.addAction(action2);
		action.addAction(action3);
		action.addAction(action4);
		return action;
	}
	
	public static ParallelAction parallel(Action action1, Action action2, Action action3, Action action4, Action action5) {
		ParallelAction action = action(ParallelAction.class, ParallelAction::new);
		action.addAction(action1);
		action.addAction(action2);
		action.addAction(action3);
		action.addAction(action4);
		action.addAction(action5);
		return action;
	}
	
	public static ParallelAction parallel(Action... actions) {
		ParallelAction action = action(ParallelAction.class, ParallelAction::new);
		for (Action value : actions)
			action.addAction(value);
		return action;
	}
	
	public static ParallelAction parallel() {
		return action(ParallelAction.class, ParallelAction::new);
	}
	
	public static RepeatAction repeat(int count, Action repeatedAction) {
		RepeatAction action = action(RepeatAction.class, RepeatAction::new);
		action.setCount(count);
		action.setAction(repeatedAction);
		return action;
	}
	
	public static RepeatAction forever(Action repeatedAction) {
		RepeatAction action = action(RepeatAction.class, RepeatAction::new);
		action.setCount(RepeatAction.FOREVER);
		action.setAction(repeatedAction);
		return action;
	}
	
	public static RunnableAction run(Runnable runnable) {
		RunnableAction action = action(RunnableAction.class, RunnableAction::new);
		action.setRunnable(runnable);
		return action;
	}
	
	public static LayoutAction layout(boolean enabled) {
		LayoutAction action = action(LayoutAction.class, LayoutAction::new);
		action.setLayoutEnabled(enabled);
		return action;
	}
	
	public static AfterAction after(Action action) {
		AfterAction afterAction = action(AfterAction.class, AfterAction::new);
		afterAction.setAction(action);
		return afterAction;
	}
	
	public static AddListenerAction addListener(EventListener listener, boolean capture) {
		AddListenerAction addAction = action(AddListenerAction.class, AddListenerAction::new);
		addAction.setListener(listener);
		addAction.setCapture(capture);
		return addAction;
	}
	
	public static AddListenerAction addListener(EventListener listener, boolean capture, Actor targetActor) {
		AddListenerAction addAction = action(AddListenerAction.class, AddListenerAction::new);
		addAction.setTarget(targetActor);
		addAction.setListener(listener);
		addAction.setCapture(capture);
		return addAction;
	}
	
	public static RemoveListenerAction removeListener(EventListener listener, boolean capture) {
		RemoveListenerAction addAction = action(RemoveListenerAction.class, RemoveListenerAction::new);
		addAction.setListener(listener);
		addAction.setCapture(capture);
		return addAction;
	}
	
	public static RemoveListenerAction removeListener(EventListener listener, boolean capture, Actor targetActor) {
		RemoveListenerAction addAction = action(RemoveListenerAction.class, RemoveListenerAction::new);
		addAction.setTarget(targetActor);
		addAction.setListener(listener);
		addAction.setCapture(capture);
		return addAction;
	}
	
	/**
	 * Sets the target of an action and returns the action.
	 *
	 * @param target the desired target of the action
	 * @param action the action on which to set the target
	 * @return the action with its target set
	 */
	public static Action targeting(Actor target, Action action) {
		action.setTarget(target);
		return action;
	}
	
}
