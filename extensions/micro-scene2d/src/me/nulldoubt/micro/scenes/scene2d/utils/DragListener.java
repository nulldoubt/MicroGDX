package me.nulldoubt.micro.scenes.scene2d.utils;

import me.nulldoubt.micro.Input.Buttons;
import me.nulldoubt.micro.math.Vector2;
import me.nulldoubt.micro.scenes.scene2d.InputEvent;
import me.nulldoubt.micro.scenes.scene2d.InputListener;

public class DragListener extends InputListener {
	
	private float tapSquareSize = 14, touchDownX = -1, touchDownY = -1, stageTouchDownX = -1, stageTouchDownY = -1;
	private float dragStartX, dragStartY, dragLastX, dragLastY, dragX, dragY;
	private int pressedPointer = -1;
	private int button;
	private boolean dragging;
	
	public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
		if (pressedPointer != -1)
			return false;
		if (pointer == 0 && this.button != -1 && button != this.button)
			return false;
		pressedPointer = pointer;
		touchDownX = x;
		touchDownY = y;
		stageTouchDownX = event.getStageX();
		stageTouchDownY = event.getStageY();
		return true;
	}
	
	public void touchDragged(InputEvent event, float x, float y, int pointer) {
		if (pointer != pressedPointer)
			return;
		if (!dragging && (Math.abs(touchDownX - x) > tapSquareSize || Math.abs(touchDownY - y) > tapSquareSize)) {
			dragging = true;
			dragStartX = x;
			dragStartY = y;
			dragStart(event, x, y, pointer);
			dragX = x;
			dragY = y;
		}
		if (dragging) {
			dragLastX = dragX;
			dragLastY = dragY;
			dragX = x;
			dragY = y;
			drag(event, x, y, pointer);
		}
	}
	
	public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
		if (pointer == pressedPointer && (this.button == -1 || button == this.button)) {
			if (dragging)
				dragStop(event, x, y, pointer);
			cancel();
		}
	}
	
	public void dragStart(InputEvent event, float x, float y, int pointer) {
	}
	
	public void drag(InputEvent event, float x, float y, int pointer) {
	}
	
	public void dragStop(InputEvent event, float x, float y, int pointer) {
	}
	
	/* If a drag is in progress, no further drag methods will be called until a new drag is started. */
	public void cancel() {
		dragging = false;
		pressedPointer = -1;
	}
	
	/**
	 * Returns true if a touch has been dragged outside the tap square.
	 */
	public boolean isDragging() {
		return dragging;
	}
	
	public void setTapSquareSize(float halfTapSquareSize) {
		tapSquareSize = halfTapSquareSize;
	}
	
	public float getTapSquareSize() {
		return tapSquareSize;
	}
	
	public float getTouchDownX() {
		return touchDownX;
	}
	
	public float getTouchDownY() {
		return touchDownY;
	}
	
	public float getStageTouchDownX() {
		return stageTouchDownX;
	}
	
	public float getStageTouchDownY() {
		return stageTouchDownY;
	}
	
	public float getDragStartX() {
		return dragStartX;
	}
	
	public void setDragStartX(float dragStartX) {
		this.dragStartX = dragStartX;
	}
	
	public float getDragStartY() {
		return dragStartY;
	}
	
	public void setDragStartY(float dragStartY) {
		this.dragStartY = dragStartY;
	}
	
	public float getDragX() {
		return dragX;
	}
	
	public float getDragY() {
		return dragY;
	}
	
	/**
	 * The distance from drag start to the current drag position.
	 */
	public float getDragDistance() {
		return Vector2.len(dragX - dragStartX, dragY - dragStartY);
	}
	
	/**
	 * Returns the amount on the x axis that the touch has been dragged since the last drag event.
	 */
	public float getDeltaX() {
		return dragX - dragLastX;
	}
	
	/**
	 * Returns the amount on the y axis that the touch has been dragged since the last drag event.
	 */
	public float getDeltaY() {
		return dragY - dragLastY;
	}
	
	public int getButton() {
		return button;
	}
	
	/**
	 */
	public void setButton(int button) {
		this.button = button;
	}
	
}
