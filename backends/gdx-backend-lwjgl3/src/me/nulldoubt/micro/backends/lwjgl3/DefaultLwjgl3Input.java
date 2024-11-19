package me.nulldoubt.micro.backends.lwjgl3;

import me.nulldoubt.micro.AbstractInput;
import me.nulldoubt.micro.InputEventQueue;
import me.nulldoubt.micro.InputProcessor;
import me.nulldoubt.micro.graphics.glutils.HdpiUtils;
import me.nulldoubt.micro.input.NativeInputConfiguration;
import org.lwjgl.glfw.*;

import java.util.Arrays;

public class DefaultLwjgl3Input extends AbstractInput implements Lwjgl3Input {
	
	final Lwjgl3Window window;
	final InputEventQueue eventQueue = new InputEventQueue();
	final boolean[] justPressedButtons = new boolean[5];
	int mouseX, mouseY;
	int mousePressed;
	int deltaX, deltaY;
	boolean justTouched;
	char lastCharacter;
	GLFWCharCallback charCallback = new GLFWCharCallback() {
		@Override
		public void invoke(long window, int codepoint) {
			if ((codepoint & 0xff00) == 0xf700)
				return;
			lastCharacter = (char) codepoint;
			DefaultLwjgl3Input.this.window.getGraphics().requestRendering();
			eventQueue.keyTyped((char) codepoint, System.nanoTime());
		}
	};
	private final GLFWKeyCallback keyCallback = new GLFWKeyCallback() {
		@Override
		public void invoke(long window, int key, int scancode, int action, int mods) {
			keyCallback(window, key, scancode, action, mods);
		}
	};
	private InputProcessor inputProcessor;
	private final GLFWScrollCallback scrollCallback = new GLFWScrollCallback() {
		@Override
		public void invoke(long window, double scrollX, double scrollY) {
			DefaultLwjgl3Input.this.window.getGraphics().requestRendering();
			eventQueue.scrolled(-(float) scrollX, -(float) scrollY, System.nanoTime());
		}
	};
	
	private final GLFWCursorPosCallback cursorPosCallback = new GLFWCursorPosCallback() {
		private int logicalMouseY;
		private int logicalMouseX;
		
		@Override
		public void invoke(long windowHandle, double x, double y) {
			deltaX = (int) x - logicalMouseX;
			deltaY = (int) y - logicalMouseY;
			mouseX = logicalMouseX = (int) x;
			mouseY = logicalMouseY = (int) y;
			
			if (window.getConfig().hdpiMode == HdpiUtils.HdpiMode.Pixels) {
				float xScale = window.getGraphics().getBackBufferWidth() / (float) window.getGraphics().getLogicalWidth();
				float yScale = window.getGraphics().getBackBufferHeight() / (float) window.getGraphics().getLogicalHeight();
				deltaX = (int) (deltaX * xScale);
				deltaY = (int) (deltaY * yScale);
				mouseX = (int) (mouseX * xScale);
				mouseY = (int) (mouseY * yScale);
			}
			
			DefaultLwjgl3Input.this.window.getGraphics().requestRendering();
			long time = System.nanoTime();
			if (mousePressed > 0) {
				eventQueue.touchDragged(mouseX, mouseY, 0, time);
			} else {
				eventQueue.mouseMoved(mouseX, mouseY, time);
			}
		}
	};
	
	private final GLFWMouseButtonCallback mouseButtonCallback = new GLFWMouseButtonCallback() {
		@Override
		public void invoke(long window, int button, int action, int mods) {
			int gdxButton = toGdxButton(button);
			if (button != -1 && gdxButton == -1)
				return;
			
			long time = System.nanoTime();
			if (action == GLFW.GLFW_PRESS) {
				mousePressed++;
				justTouched = true;
				justPressedButtons[gdxButton] = true;
				DefaultLwjgl3Input.this.window.getGraphics().requestRendering();
				eventQueue.touchDown(mouseX, mouseY, 0, gdxButton, time);
			} else {
				mousePressed = Math.max(0, mousePressed - 1);
				DefaultLwjgl3Input.this.window.getGraphics().requestRendering();
				eventQueue.touchUp(mouseX, mouseY, 0, gdxButton, time);
			}
		}
		
		private int toGdxButton(int button) {
			if (button == 0)
				return Buttons.LEFT;
			if (button == 1)
				return Buttons.RIGHT;
			if (button == 2)
				return Buttons.MIDDLE;
			if (button == 3)
				return Buttons.BACK;
			if (button == 4)
				return Buttons.FORWARD;
			return -1;
		}
	};
	
	public DefaultLwjgl3Input(Lwjgl3Window window) {
		this.window = window;
		windowHandleChanged(window.getWindowHandle());
	}
	
	void keyCallback(long window, int key, int scancode, int action, int mods) {
		switch (action) {
			case GLFW.GLFW_PRESS:
				key = getGdxKeyCode(key);
				eventQueue.keyDown(key, System.nanoTime());
				pressedKeyCount++;
				keyJustPressed = true;
				pressedKeys[key] = true;
				justPressedKeys[key] = true;
				DefaultLwjgl3Input.this.window.getGraphics().requestRendering();
				lastCharacter = 0;
				char character = characterForKeyCode(key);
				if (character != 0)
					charCallback.invoke(window, character);
				break;
			case GLFW.GLFW_RELEASE:
				key = getGdxKeyCode(key);
				pressedKeyCount--;
				pressedKeys[key] = false;
				DefaultLwjgl3Input.this.window.getGraphics().requestRendering();
				eventQueue.keyUp(key, System.nanoTime());
				break;
			case GLFW.GLFW_REPEAT:
				if (lastCharacter != 0) {
					DefaultLwjgl3Input.this.window.getGraphics().requestRendering();
					eventQueue.keyTyped(lastCharacter, System.nanoTime());
				}
				break;
		}
	}
	
	@Override
	public void windowHandleChanged(long windowHandle) {
		resetPollingStates();
		GLFW.glfwSetKeyCallback(window.getWindowHandle(), keyCallback);
		GLFW.glfwSetCharCallback(window.getWindowHandle(), charCallback);
		GLFW.glfwSetScrollCallback(window.getWindowHandle(), scrollCallback);
		GLFW.glfwSetCursorPosCallback(window.getWindowHandle(), cursorPosCallback);
		GLFW.glfwSetMouseButtonCallback(window.getWindowHandle(), mouseButtonCallback);
	}
	
	@Override
	public void update() {
		eventQueue.drain(inputProcessor);
	}
	
	@Override
	public void prepareNext() {
		if (justTouched) {
			justTouched = false;
			Arrays.fill(justPressedButtons, false);
		}
		
		if (keyJustPressed) {
			keyJustPressed = false;
			Arrays.fill(justPressedKeys, false);
		}
		deltaX = 0;
		deltaY = 0;
	}
	
	@Override
	public void resetPollingStates() {
		justTouched = false;
		keyJustPressed = false;
		Arrays.fill(justPressedKeys, false);
		Arrays.fill(justPressedButtons, false);
		eventQueue.drain(null);
	}
	
	protected char characterForKeyCode(int key) {
		return switch (key) {
			case Keys.BACKSPACE -> 8;
			case Keys.TAB -> '\t';
			case Keys.FORWARD_DEL -> 127;
			case Keys.NUMPAD_ENTER, Keys.ENTER -> '\n';
			default -> 0;
		};
	}
	
	public int getGdxKeyCode(int lwjglKeyCode) {
		return switch (lwjglKeyCode) {
			case GLFW.GLFW_KEY_SPACE -> Keys.SPACE;
			case GLFW.GLFW_KEY_APOSTROPHE -> Keys.APOSTROPHE;
			case GLFW.GLFW_KEY_COMMA -> Keys.COMMA;
			case GLFW.GLFW_KEY_MINUS -> Keys.MINUS;
			case GLFW.GLFW_KEY_PERIOD -> Keys.PERIOD;
			case GLFW.GLFW_KEY_SLASH -> Keys.SLASH;
			case GLFW.GLFW_KEY_0 -> Keys.NUM_0;
			case GLFW.GLFW_KEY_1 -> Keys.NUM_1;
			case GLFW.GLFW_KEY_2 -> Keys.NUM_2;
			case GLFW.GLFW_KEY_3 -> Keys.NUM_3;
			case GLFW.GLFW_KEY_4 -> Keys.NUM_4;
			case GLFW.GLFW_KEY_5 -> Keys.NUM_5;
			case GLFW.GLFW_KEY_6 -> Keys.NUM_6;
			case GLFW.GLFW_KEY_7 -> Keys.NUM_7;
			case GLFW.GLFW_KEY_8 -> Keys.NUM_8;
			case GLFW.GLFW_KEY_9 -> Keys.NUM_9;
			case GLFW.GLFW_KEY_SEMICOLON -> Keys.SEMICOLON;
			case GLFW.GLFW_KEY_EQUAL -> Keys.EQUALS;
			case GLFW.GLFW_KEY_A -> Keys.A;
			case GLFW.GLFW_KEY_B -> Keys.B;
			case GLFW.GLFW_KEY_C -> Keys.C;
			case GLFW.GLFW_KEY_D -> Keys.D;
			case GLFW.GLFW_KEY_E -> Keys.E;
			case GLFW.GLFW_KEY_F -> Keys.F;
			case GLFW.GLFW_KEY_G -> Keys.G;
			case GLFW.GLFW_KEY_H -> Keys.H;
			case GLFW.GLFW_KEY_I -> Keys.I;
			case GLFW.GLFW_KEY_J -> Keys.J;
			case GLFW.GLFW_KEY_K -> Keys.K;
			case GLFW.GLFW_KEY_L -> Keys.L;
			case GLFW.GLFW_KEY_M -> Keys.M;
			case GLFW.GLFW_KEY_N -> Keys.N;
			case GLFW.GLFW_KEY_O -> Keys.O;
			case GLFW.GLFW_KEY_P -> Keys.P;
			case GLFW.GLFW_KEY_Q -> Keys.Q;
			case GLFW.GLFW_KEY_R -> Keys.R;
			case GLFW.GLFW_KEY_S -> Keys.S;
			case GLFW.GLFW_KEY_T -> Keys.T;
			case GLFW.GLFW_KEY_U -> Keys.U;
			case GLFW.GLFW_KEY_V -> Keys.V;
			case GLFW.GLFW_KEY_W -> Keys.W;
			case GLFW.GLFW_KEY_X -> Keys.X;
			case GLFW.GLFW_KEY_Y -> Keys.Y;
			case GLFW.GLFW_KEY_Z -> Keys.Z;
			case GLFW.GLFW_KEY_LEFT_BRACKET -> Keys.LEFT_BRACKET;
			case GLFW.GLFW_KEY_BACKSLASH -> Keys.BACKSLASH;
			case GLFW.GLFW_KEY_RIGHT_BRACKET -> Keys.RIGHT_BRACKET;
			case GLFW.GLFW_KEY_GRAVE_ACCENT -> Keys.GRAVE;
			case GLFW.GLFW_KEY_ESCAPE -> Keys.ESCAPE;
			case GLFW.GLFW_KEY_ENTER -> Keys.ENTER;
			case GLFW.GLFW_KEY_TAB -> Keys.TAB;
			case GLFW.GLFW_KEY_BACKSPACE -> Keys.BACKSPACE;
			case GLFW.GLFW_KEY_INSERT -> Keys.INSERT;
			case GLFW.GLFW_KEY_DELETE -> Keys.FORWARD_DEL;
			case GLFW.GLFW_KEY_RIGHT -> Keys.RIGHT;
			case GLFW.GLFW_KEY_LEFT -> Keys.LEFT;
			case GLFW.GLFW_KEY_DOWN -> Keys.DOWN;
			case GLFW.GLFW_KEY_UP -> Keys.UP;
			case GLFW.GLFW_KEY_PAGE_UP -> Keys.PAGE_UP;
			case GLFW.GLFW_KEY_PAGE_DOWN -> Keys.PAGE_DOWN;
			case GLFW.GLFW_KEY_HOME -> Keys.HOME;
			case GLFW.GLFW_KEY_END -> Keys.END;
			case GLFW.GLFW_KEY_CAPS_LOCK -> Keys.CAPS_LOCK;
			case GLFW.GLFW_KEY_SCROLL_LOCK -> Keys.SCROLL_LOCK;
			case GLFW.GLFW_KEY_PRINT_SCREEN -> Keys.PRINT_SCREEN;
			case GLFW.GLFW_KEY_PAUSE -> Keys.PAUSE;
			case GLFW.GLFW_KEY_F1 -> Keys.F1;
			case GLFW.GLFW_KEY_F2 -> Keys.F2;
			case GLFW.GLFW_KEY_F3 -> Keys.F3;
			case GLFW.GLFW_KEY_F4 -> Keys.F4;
			case GLFW.GLFW_KEY_F5 -> Keys.F5;
			case GLFW.GLFW_KEY_F6 -> Keys.F6;
			case GLFW.GLFW_KEY_F7 -> Keys.F7;
			case GLFW.GLFW_KEY_F8 -> Keys.F8;
			case GLFW.GLFW_KEY_F9 -> Keys.F9;
			case GLFW.GLFW_KEY_F10 -> Keys.F10;
			case GLFW.GLFW_KEY_F11 -> Keys.F11;
			case GLFW.GLFW_KEY_F12 -> Keys.F12;
			case GLFW.GLFW_KEY_F13 -> Keys.F13;
			case GLFW.GLFW_KEY_F14 -> Keys.F14;
			case GLFW.GLFW_KEY_F15 -> Keys.F15;
			case GLFW.GLFW_KEY_F16 -> Keys.F16;
			case GLFW.GLFW_KEY_F17 -> Keys.F17;
			case GLFW.GLFW_KEY_F18 -> Keys.F18;
			case GLFW.GLFW_KEY_F19 -> Keys.F19;
			case GLFW.GLFW_KEY_F20 -> Keys.F20;
			case GLFW.GLFW_KEY_F21 -> Keys.F21;
			case GLFW.GLFW_KEY_F22 -> Keys.F22;
			case GLFW.GLFW_KEY_F23 -> Keys.F23;
			case GLFW.GLFW_KEY_F24 -> Keys.F24;
			case GLFW.GLFW_KEY_NUM_LOCK -> Keys.NUM_LOCK;
			case GLFW.GLFW_KEY_KP_0 -> Keys.NUMPAD_0;
			case GLFW.GLFW_KEY_KP_1 -> Keys.NUMPAD_1;
			case GLFW.GLFW_KEY_KP_2 -> Keys.NUMPAD_2;
			case GLFW.GLFW_KEY_KP_3 -> Keys.NUMPAD_3;
			case GLFW.GLFW_KEY_KP_4 -> Keys.NUMPAD_4;
			case GLFW.GLFW_KEY_KP_5 -> Keys.NUMPAD_5;
			case GLFW.GLFW_KEY_KP_6 -> Keys.NUMPAD_6;
			case GLFW.GLFW_KEY_KP_7 -> Keys.NUMPAD_7;
			case GLFW.GLFW_KEY_KP_8 -> Keys.NUMPAD_8;
			case GLFW.GLFW_KEY_KP_9 -> Keys.NUMPAD_9;
			case GLFW.GLFW_KEY_KP_DECIMAL -> Keys.NUMPAD_DOT;
			case GLFW.GLFW_KEY_KP_DIVIDE -> Keys.NUMPAD_DIVIDE;
			case GLFW.GLFW_KEY_KP_MULTIPLY -> Keys.NUMPAD_MULTIPLY;
			case GLFW.GLFW_KEY_KP_SUBTRACT -> Keys.NUMPAD_SUBTRACT;
			case GLFW.GLFW_KEY_KP_ADD -> Keys.NUMPAD_ADD;
			case GLFW.GLFW_KEY_KP_ENTER -> Keys.NUMPAD_ENTER;
			case GLFW.GLFW_KEY_KP_EQUAL -> Keys.NUMPAD_EQUALS;
			case GLFW.GLFW_KEY_LEFT_SHIFT -> Keys.SHIFT_LEFT;
			case GLFW.GLFW_KEY_LEFT_CONTROL -> Keys.CONTROL_LEFT;
			case GLFW.GLFW_KEY_LEFT_ALT -> Keys.ALT_LEFT;
			case GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER -> Keys.SYM;
			case GLFW.GLFW_KEY_RIGHT_SHIFT -> Keys.SHIFT_RIGHT;
			case GLFW.GLFW_KEY_RIGHT_CONTROL -> Keys.CONTROL_RIGHT;
			case GLFW.GLFW_KEY_RIGHT_ALT -> Keys.ALT_RIGHT;
			case GLFW.GLFW_KEY_MENU -> Keys.MENU;
			default -> Keys.UNKNOWN;
		};
	}
	
	@Override
	public void dispose() {
		keyCallback.free();
		charCallback.free();
		scrollCallback.free();
		cursorPosCallback.free();
		mouseButtonCallback.free();
	}
	
	@Override
	public float getAccelerometerX() {
		return 0;
	}
	
	@Override
	public float getAccelerometerY() {
		return 0;
	}
	
	@Override
	public float getAccelerometerZ() {
		return 0;
	}
	
	@Override
	public float getGyroscopeX() {
		return 0;
	}
	
	@Override
	public float getGyroscopeY() {
		return 0;
	}
	
	@Override
	public float getGyroscopeZ() {
		return 0;
	}
	
	@Override
	public int getMaxPointers() {
		return 1;
	}
	
	@Override
	public int getX() {
		return mouseX;
	}
	
	@Override
	public int getX(int pointer) {
		return pointer == 0 ? mouseX : 0;
	}
	
	@Override
	public int getDeltaX() {
		return deltaX;
	}
	
	@Override
	public int getDeltaX(int pointer) {
		return pointer == 0 ? deltaX : 0;
	}
	
	@Override
	public int getY() {
		return mouseY;
	}
	
	@Override
	public int getY(int pointer) {
		return pointer == 0 ? mouseY : 0;
	}
	
	@Override
	public int getDeltaY() {
		return deltaY;
	}
	
	@Override
	public int getDeltaY(int pointer) {
		return pointer == 0 ? deltaY : 0;
	}
	
	@Override
	public boolean isTouched() {
		return GLFW.glfwGetMouseButton(window.getWindowHandle(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS
				|| GLFW.glfwGetMouseButton(window.getWindowHandle(), GLFW.GLFW_MOUSE_BUTTON_2) == GLFW.GLFW_PRESS
				|| GLFW.glfwGetMouseButton(window.getWindowHandle(), GLFW.GLFW_MOUSE_BUTTON_3) == GLFW.GLFW_PRESS
				|| GLFW.glfwGetMouseButton(window.getWindowHandle(), GLFW.GLFW_MOUSE_BUTTON_4) == GLFW.GLFW_PRESS
				|| GLFW.glfwGetMouseButton(window.getWindowHandle(), GLFW.GLFW_MOUSE_BUTTON_5) == GLFW.GLFW_PRESS;
	}
	
	@Override
	public boolean justTouched() {
		return justTouched;
	}
	
	@Override
	public void setInputProcessor(InputProcessor processor) {
		this.inputProcessor = processor;
	}
	
	@Override
	public boolean isTouched(int pointer) {
		return pointer == 0 && isTouched();
	}
	
	@Override
	public InputProcessor getInputProcessor() {
		return inputProcessor;
	}
	
	@Override
	public float getPressure() {
		return getPressure(0);
	}
	
	@Override
	public void setCursorCatched(boolean catched) {
		GLFW.glfwSetInputMode(window.getWindowHandle(), GLFW.GLFW_CURSOR,
				catched ? GLFW.GLFW_CURSOR_DISABLED : GLFW.GLFW_CURSOR_NORMAL);
	}
	
	@Override
	public float getPressure(int pointer) {
		return isTouched(pointer) ? 1 : 0;
	}
	
	@Override
	public boolean isCursorCatched() {
		return GLFW.glfwGetInputMode(window.getWindowHandle(), GLFW.GLFW_CURSOR) == GLFW.GLFW_CURSOR_DISABLED;
	}
	
	@Override
	public boolean isButtonPressed(int button) {
		return GLFW.glfwGetMouseButton(window.getWindowHandle(), button) == GLFW.GLFW_PRESS;
	}
	
	@Override
	public void setCursorPosition(int x, int y) {
		if (window.getConfig().hdpiMode == HdpiUtils.HdpiMode.Pixels) {
			float xScale = window.getGraphics().getLogicalWidth() / (float) window.getGraphics().getBackBufferWidth();
			float yScale = window.getGraphics().getLogicalHeight() / (float) window.getGraphics().getBackBufferHeight();
			x = (int) (x * xScale);
			y = (int) (y * yScale);
		}
		GLFW.glfwSetCursorPos(window.getWindowHandle(), x, y);
		cursorPosCallback.invoke(window.getWindowHandle(), x, y);
	}
	
	@Override
	public boolean isButtonJustPressed(int button) {
		if (button < 0 || button >= justPressedButtons.length)
			return false;
		return justPressedButtons[button];
	}
	
	@Override
	public void getTextInput(TextInputListener listener, String title, String text, String hint) {
		getTextInput(listener, title, text, hint, OnscreenKeyboardType.Default);
	}
	
	@Override
	public void getTextInput(TextInputListener listener, String title, String text, String hint, OnscreenKeyboardType type) {
		// FIXME getTextInput does nothing
		listener.canceled();
	}
	
	@Override
	public void setOnscreenKeyboardVisible(boolean visible) {}
	
	@Override
	public void setOnscreenKeyboardVisible(boolean visible, OnscreenKeyboardType type) {}
	
	@Override
	public void openTextInputField(NativeInputConfiguration configuration) {}
	
	@Override
	public void closeTextInputField(boolean sendReturn) {}
	
	@Override
	public boolean isPeripheralAvailable(Peripheral peripheral) {
		return peripheral == Peripheral.HardwareKeyboard;
	}
	
	@Override
	public void setKeyboardHeightObserver(KeyboardHeightObserver observer) {}
	
	@Override
	public int getRotation() {
		return 0;
	}
	
	@Override
	public void vibrate(int milliseconds) {}
	
	@Override
	public Orientation getNativeOrientation() {
		return Orientation.Landscape;
	}
	
	@Override
	public void vibrate(int milliseconds, boolean fallback) {}
	
	@Override
	public void vibrate(int milliseconds, int amplitude, boolean fallback) {}
	
	@Override
	public void vibrate(VibrationType vibrationType) {}
	
	@Override
	public float getAzimuth() {
		return 0;
	}
	
	@Override
	public float getPitch() {
		return 0;
	}
	
	@Override
	public float getRoll() {
		return 0;
	}
	
	@Override
	public void getRotationMatrix(float[] matrix) {}
	
	@Override
	public long getCurrentEventTime() {
		return eventQueue.getCurrentEventTime();
	}
	
}
