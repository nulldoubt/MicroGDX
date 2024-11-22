package me.nulldoubt.micro.backends.android.keyboardheight;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

public class StandardKeyboardHeightProvider extends PopupWindow implements KeyboardHeightProvider {
	
	private KeyboardHeightObserver observer;
	
	private static int keyboardLandscapeHeight;
	
	private static int keyboardPortraitHeight;
	
	private final View popupView;
	private final View parentView;
	private final Activity activity;
	
	public StandardKeyboardHeightProvider(Activity activity) {
		super(activity);
		this.activity = activity;
		
		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout linearLayout = new LinearLayout(inflater.getContext());
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		linearLayout.setLayoutParams(layoutParams);
		this.popupView = linearLayout;
		setContentView(popupView);
		
		setSoftInputMode(LayoutParams.SOFT_INPUT_ADJUST_RESIZE | LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
		
		parentView = activity.findViewById(android.R.id.content);
		
		setWidth(0);
		setHeight(android.view.ViewGroup.LayoutParams.MATCH_PARENT);
		
		popupView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
			if (popupView != null)
				handleOnGlobalLayout();
		});
	}
	
	@Override
	public void start() {
		if (!isShowing() && parentView.getWindowToken() != null) {
			setBackgroundDrawable(new ColorDrawable(0));
			showAtLocation(parentView, Gravity.NO_GRAVITY, 0, 0);
		}
	}
	
	@Override
	public void close() {
		this.observer = null;
		dismiss();
	}
	
	@Override
	public void setKeyboardHeightObserver(KeyboardHeightObserver observer) {
		this.observer = observer;
	}
	
	private int getScreenOrientation() {
		return activity.getResources().getConfiguration().orientation;
	}
	
	private void handleOnGlobalLayout() {
		
		Point screenSize = new Point();
		activity.getWindowManager().getDefaultDisplay().getSize(screenSize);
		
		Rect rect = new Rect();
		popupView.getWindowVisibleDisplayFrame(rect);
		
		int orientation = getScreenOrientation();
		int keyboardHeight = screenSize.y - rect.bottom;
		int leftInset = rect.left;
		int rightInset = Math.abs(screenSize.x - rect.right + rect.left);
		
		if (keyboardHeight == 0) {
			notifyKeyboardHeightChanged(0, leftInset, rightInset, orientation);
		} else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			keyboardPortraitHeight = keyboardHeight;
			notifyKeyboardHeightChanged(keyboardPortraitHeight, leftInset, rightInset, orientation);
		} else {
			keyboardLandscapeHeight = keyboardHeight;
			notifyKeyboardHeightChanged(keyboardLandscapeHeight, leftInset, rightInset, orientation);
		}
	}
	
	private void notifyKeyboardHeightChanged(int height, int leftInset, int rightInset, int orientation) {
		if (observer != null)
			observer.onKeyboardHeightChanged(height, leftInset, rightInset, orientation);
	}
	
	@Override
	public int getKeyboardLandscapeHeight() {
		return keyboardLandscapeHeight;
	}
	
	@Override
	public int getKeyboardPortraitHeight() {
		return keyboardPortraitHeight;
	}
	
}
