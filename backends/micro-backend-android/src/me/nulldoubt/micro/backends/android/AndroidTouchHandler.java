package me.nulldoubt.micro.backends.android;

import android.content.Context;
import android.view.MotionEvent;
import me.nulldoubt.micro.Input.Buttons;
import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.backends.android.DefaultAndroidInput.TouchEvent;

import java.util.Arrays;

public class AndroidTouchHandler {
	
	public void onTouch(MotionEvent event, DefaultAndroidInput input) {
		final int action = event.getAction() & MotionEvent.ACTION_MASK;
		int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
		int pointerId = event.getPointerId(pointerIndex);
		
		int x = 0, y = 0;
		int realPointerIndex = 0;
		int button = Buttons.LEFT;
		
		long timeStamp = System.nanoTime();
		synchronized (input) {
			switch (action) {
				case MotionEvent.ACTION_DOWN:
				case MotionEvent.ACTION_POINTER_DOWN:
					realPointerIndex = input.getFreePointerIndex(); // get a free pointer index as reported by Input.getX() etc.
					if (realPointerIndex >= DefaultAndroidInput.NUM_TOUCHES)
						break;
					input.realId[realPointerIndex] = pointerId;
					x = (int) event.getX(pointerIndex);
					y = (int) event.getY(pointerIndex);
					button = toGdxButton(event.getButtonState());
					if (button != -1)
						postTouchEvent(input, TouchEvent.TOUCH_DOWN, x, y, realPointerIndex, button, timeStamp);
					input.touchX[realPointerIndex] = x;
					input.touchY[realPointerIndex] = y;
					input.deltaX[realPointerIndex] = 0;
					input.deltaY[realPointerIndex] = 0;
					input.touched[realPointerIndex] = (button != -1);
					input.button[realPointerIndex] = button;
					input.pressure[realPointerIndex] = event.getPressure(pointerIndex);
					break;
				
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_POINTER_UP:
				case MotionEvent.ACTION_OUTSIDE:
				case MotionEvent.ACTION_CANCEL:
					realPointerIndex = input.lookUpPointerIndex(pointerId);
					if (realPointerIndex == -1)
						break;
					if (realPointerIndex >= DefaultAndroidInput.NUM_TOUCHES)
						break;
					input.realId[realPointerIndex] = -1;
					x = (int) event.getX(pointerIndex);
					y = (int) event.getY(pointerIndex);
					button = input.button[realPointerIndex];
					if (button != -1) {
						if (action == MotionEvent.ACTION_CANCEL)
							postTouchEvent(input, TouchEvent.TOUCH_CANCELLED, x, y, realPointerIndex, button, timeStamp);
						else
							postTouchEvent(input, TouchEvent.TOUCH_UP, x, y, realPointerIndex, button, timeStamp);
					}
					input.touchX[realPointerIndex] = x;
					input.touchY[realPointerIndex] = y;
					input.deltaX[realPointerIndex] = 0;
					input.deltaY[realPointerIndex] = 0;
					input.touched[realPointerIndex] = false;
					input.button[realPointerIndex] = 0;
					input.pressure[realPointerIndex] = 0;
					if (action == MotionEvent.ACTION_CANCEL) {
						Arrays.fill(input.realId, -1);
						Arrays.fill(input.touched, false);
					}
					break;
				
				case MotionEvent.ACTION_MOVE:
					int pointerCount = event.getPointerCount();
					for (int i = 0; i < pointerCount; i++) {
						pointerIndex = i;
						pointerId = event.getPointerId(pointerIndex);
						x = (int) event.getX(pointerIndex);
						y = (int) event.getY(pointerIndex);
						realPointerIndex = input.lookUpPointerIndex(pointerId);
						if (realPointerIndex == -1)
							continue;
						if (realPointerIndex >= DefaultAndroidInput.NUM_TOUCHES)
							break;
						button = input.button[realPointerIndex];
						if (button != -1)
							postTouchEvent(input, TouchEvent.TOUCH_DRAGGED, x, y, realPointerIndex, button, timeStamp);
						else
							postTouchEvent(input, TouchEvent.TOUCH_MOVED, x, y, realPointerIndex, 0, timeStamp);
						input.deltaX[realPointerIndex] = x - input.touchX[realPointerIndex];
						input.deltaY[realPointerIndex] = y - input.touchY[realPointerIndex];
						input.touchX[realPointerIndex] = x;
						input.touchY[realPointerIndex] = y;
						input.pressure[realPointerIndex] = event.getPressure(pointerIndex);
					}
					break;
			}
		}
		Micro.app.getGraphics().requestRendering();
	}
	
	private void logAction(int action, int pointer) {
		String actionStr = "";
		if (action == MotionEvent.ACTION_DOWN)
			actionStr = "DOWN";
		else if (action == MotionEvent.ACTION_POINTER_DOWN)
			actionStr = "POINTER DOWN";
		else if (action == MotionEvent.ACTION_UP)
			actionStr = "UP";
		else if (action == MotionEvent.ACTION_POINTER_UP)
			actionStr = "POINTER UP";
		else if (action == MotionEvent.ACTION_OUTSIDE)
			actionStr = "OUTSIDE";
		else if (action == MotionEvent.ACTION_CANCEL)
			actionStr = "CANCEL";
		else if (action == MotionEvent.ACTION_MOVE)
			actionStr = "MOVE";
		else
			actionStr = "UNKNOWN (" + action + ")";
		Micro.app.log("AndroidMultiTouchHandler", "action " + actionStr + ", Android pointer id: " + pointer);
	}
	
	private int toGdxButton(int button) {
		if (button == 0 || button == 1)
			return Buttons.LEFT;
		if (button == 2)
			return Buttons.RIGHT;
		if (button == 4)
			return Buttons.MIDDLE;
		if (button == 8)
			return Buttons.BACK;
		if (button == 16)
			return Buttons.FORWARD;
		return -1;
	}
	
	private void postTouchEvent(DefaultAndroidInput input, int type, int x, int y, int pointer, int button, long timeStamp) {
		TouchEvent event = input.usedTouchEvents.obtain();
		event.timeStamp = timeStamp;
		event.pointer = pointer;
		event.x = x;
		event.y = y;
		event.type = type;
		event.button = button;
		input.touchEvents.add(event);
	}
	
	public boolean supportsMultitouch(Context activity) {
		return activity.getPackageManager().hasSystemFeature("android.hardware.touchscreen.multitouch");
	}
	
}
