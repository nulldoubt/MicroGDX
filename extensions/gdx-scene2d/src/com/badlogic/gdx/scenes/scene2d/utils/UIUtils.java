
package com.badlogic.gdx.scenes.scene2d.utils;

import com.badlogic.gdx.Micro;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.utils.Os;
import com.badlogic.gdx.utils.SharedLibraryLoader;

public final class UIUtils {
	private UIUtils () {
	}

	public static boolean isAndroid = SharedLibraryLoader.os == Os.Android;
	public static boolean isMac = SharedLibraryLoader.os == Os.MacOsX;
	public static boolean isWindows = SharedLibraryLoader.os == Os.Windows;
	public static boolean isLinux = SharedLibraryLoader.os == Os.Linux;
	public static boolean isIos = SharedLibraryLoader.os == Os.IOS;

	public static boolean left () {
		return Micro.input.isButtonPressed(Buttons.LEFT);
	}

	public static boolean left (int button) {
		return button == Buttons.LEFT;
	}

	public static boolean right () {
		return Micro.input.isButtonPressed(Buttons.RIGHT);
	}

	public static boolean right (int button) {
		return button == Buttons.RIGHT;
	}

	public static boolean middle () {
		return Micro.input.isButtonPressed(Buttons.MIDDLE);
	}

	public static boolean middle (int button) {
		return button == Buttons.MIDDLE;
	}

	public static boolean shift () {
		return Micro.input.isKeyPressed(Keys.SHIFT_LEFT) || Micro.input.isKeyPressed(Keys.SHIFT_RIGHT);
	}

	public static boolean shift (int keycode) {
		return keycode == Keys.SHIFT_LEFT || keycode == Keys.SHIFT_RIGHT;
	}

	public static boolean ctrl () {
		if (isMac)
			return Micro.input.isKeyPressed(Keys.SYM);
		else
			return Micro.input.isKeyPressed(Keys.CONTROL_LEFT) || Micro.input.isKeyPressed(Keys.CONTROL_RIGHT);
	}

	public static boolean ctrl (int keycode) {
		if (isMac)
			return keycode == Keys.SYM;
		else
			return keycode == Keys.CONTROL_LEFT || keycode == Keys.CONTROL_RIGHT;
	}

	public static boolean alt () {
		return Micro.input.isKeyPressed(Keys.ALT_LEFT) || Micro.input.isKeyPressed(Keys.ALT_RIGHT);
	}

	public static boolean alt (int keycode) {
		return keycode == Keys.ALT_LEFT || keycode == Keys.ALT_RIGHT;
	}
}
