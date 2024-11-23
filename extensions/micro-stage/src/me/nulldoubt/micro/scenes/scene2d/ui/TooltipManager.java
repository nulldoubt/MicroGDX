package me.nulldoubt.micro.scenes.scene2d.ui;

import me.nulldoubt.micro.Files;
import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.math.Interpolation;
import me.nulldoubt.micro.scenes.scene2d.Stage;
import me.nulldoubt.micro.utils.Timer;
import me.nulldoubt.micro.utils.Timer.Task;
import me.nulldoubt.micro.utils.collections.Array;

import static me.nulldoubt.micro.math.Interpolation.fade;
import static me.nulldoubt.micro.scenes.scene2d.actions.Actions.*;

public class TooltipManager {
	
	private static TooltipManager instance;
	private static Files files;
	
	/**
	 * Seconds from when an actor is hovered to when the tooltip is shown. Default is 2. Call {@link #hideAll()} after changing to
	 * reset internal state.
	 */
	public float initialTime = 2;
	/**
	 * Once a tooltip is shown, this is used instead of {@link #initialTime}. Default is 0.
	 */
	public float subsequentTime = 0;
	/**
	 * Seconds to use {@link #subsequentTime}. Default is 1.5.
	 */
	public float resetTime = 1.5f;
	/**
	 * If false, tooltips will not be shown. Default is true.
	 */
	public boolean enabled = true;
	/**
	 * If false, tooltips will be shown without animations. Default is true.
	 */
	public boolean animations = true;
	/**
	 * The maximum width of a {@link TextTooltip}. The label will wrap if needed. Default is Integer.MAX_VALUE.
	 */
	public float maxWidth = Integer.MAX_VALUE;
	/**
	 * The distance from the mouse position to offset the tooltip actor. Default is 15,19.
	 */
	public float offsetX = 15, offsetY = 19;
	/**
	 * The distance from the tooltip actor position to the edge of the screen where the actor will be shown on the other side of
	 * the mouse cursor. Default is 7.
	 */
	public float edgeDistance = 7;
	
	final Array<Tooltip> shown = new Array();
	
	float time = initialTime;
	final Task resetTask = new Task() {
		public void run() {
			time = initialTime;
		}
	};
	
	Tooltip showTooltip;
	final Task showTask = new Task() {
		public void run() {
			if (showTooltip == null || showTooltip.targetActor == null)
				return;
			
			Stage stage = showTooltip.targetActor.getStage();
			if (stage == null)
				return;
			stage.addActor(showTooltip.container);
			showTooltip.container.toFront();
			shown.add(showTooltip);
			
			showTooltip.container.clearActions();
			showAction(showTooltip);
			
			if (!showTooltip.instant) {
				time = subsequentTime;
				resetTask.cancel();
			}
		}
	};
	
	public void touchDown(Tooltip tooltip) {
		showTask.cancel();
		if (tooltip.container.remove())
			resetTask.cancel();
		resetTask.run();
		if (enabled || tooltip.always) {
			showTooltip = tooltip;
			Timer.schedule(showTask, time);
		}
	}
	
	public void enter(Tooltip tooltip) {
		showTooltip = tooltip;
		showTask.cancel();
		if (enabled || tooltip.always) {
			if (time == 0 || tooltip.instant)
				showTask.run();
			else
				Timer.schedule(showTask, time);
		}
	}
	
	public void hide(Tooltip tooltip) {
		showTooltip = null;
		showTask.cancel();
		if (tooltip.container.hasParent()) {
			shown.removeValue(tooltip, true);
			hideAction(tooltip);
			resetTask.cancel();
			Timer.schedule(resetTask, resetTime);
		}
	}
	
	/**
	 * Called when tooltip is shown. Default implementation sets actions to animate showing.
	 */
	protected void showAction(Tooltip tooltip) {
		float actionTime = animations ? (time > 0 ? 0.5f : 0.15f) : 0.1f;
		tooltip.container.setTransform(true);
		tooltip.container.getColor().a = 0.2f;
		tooltip.container.setScale(0.05f);
		tooltip.container.addAction(parallel(fadeIn(actionTime, fade), scaleTo(1, 1, actionTime, Interpolation.fade)));
	}
	
	/**
	 * Called when tooltip is hidden. Default implementation sets actions to animate hiding and to remove the actor from the stage
	 * when the actions are complete. A subclass must at least remove the actor.
	 */
	protected void hideAction(Tooltip tooltip) {
		tooltip.container
				.addAction(sequence(parallel(alpha(0.2f, 0.2f, fade), scaleTo(0.05f, 0.05f, 0.2f, Interpolation.fade)), removeActor()));
	}
	
	public void hideAll() {
		resetTask.cancel();
		showTask.cancel();
		time = initialTime;
		showTooltip = null;
		
		for (Tooltip tooltip : shown)
			tooltip.hide();
		shown.clear();
	}
	
	/**
	 * Shows all tooltips on hover without a delay for {@link #resetTime} seconds.
	 */
	public void instant() {
		time = 0;
		showTask.run();
		showTask.cancel();
	}
	
	public static TooltipManager getInstance() {
		if (files == null || files != Micro.files) {
			files = Micro.files;
			instance = new TooltipManager();
		}
		return instance;
	}
	
}