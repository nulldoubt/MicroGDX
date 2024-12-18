package me.nulldoubt.micro;

import me.nulldoubt.micro.input.NativeInputConfiguration;
import me.nulldoubt.micro.utils.collections.ObjectIntMap;

public interface Input {
	
	interface TextInputListener {
		
		void input(String text);
		
		void canceled();
		
	}
	
	class Buttons {
		
		public static final int LEFT = 0;
		public static final int RIGHT = 1;
		public static final int MIDDLE = 2;
		public static final int BACK = 3;
		public static final int FORWARD = 4;
		
	}
	
	class Keys {
		
		public static final int ANY_KEY = -1;
		public static final int NUM_0 = 7;
		public static final int NUM_1 = 8;
		public static final int NUM_2 = 9;
		public static final int NUM_3 = 10;
		public static final int NUM_4 = 11;
		public static final int NUM_5 = 12;
		public static final int NUM_6 = 13;
		public static final int NUM_7 = 14;
		public static final int NUM_8 = 15;
		public static final int NUM_9 = 16;
		public static final int A = 29;
		public static final int ALT_LEFT = 57;
		public static final int ALT_RIGHT = 58;
		public static final int APOSTROPHE = 75;
		public static final int AT = 77;
		public static final int B = 30;
		public static final int BACK = 4;
		public static final int BACKSLASH = 73;
		public static final int C = 31;
		public static final int CALL = 5;
		public static final int CAMERA = 27;
		public static final int CAPS_LOCK = 115;
		public static final int CLEAR = 28;
		public static final int COMMA = 55;
		public static final int D = 32;
		public static final int DEL = 67;
		public static final int BACKSPACE = 67;
		public static final int FORWARD_DEL = 112;
		public static final int DPAD_CENTER = 23;
		public static final int DPAD_DOWN = 20;
		public static final int DPAD_LEFT = 21;
		public static final int DPAD_RIGHT = 22;
		public static final int DPAD_UP = 19;
		public static final int CENTER = 23;
		public static final int DOWN = 20;
		public static final int LEFT = 21;
		public static final int RIGHT = 22;
		public static final int UP = 19;
		public static final int E = 33;
		public static final int ENDCALL = 6;
		public static final int ENTER = 66;
		public static final int ENVELOPE = 65;
		public static final int EQUALS = 70;
		public static final int EXPLORER = 64;
		public static final int F = 34;
		public static final int FOCUS = 80;
		public static final int G = 35;
		public static final int GRAVE = 68;
		public static final int H = 36;
		public static final int HEADSETHOOK = 79;
		public static final int HOME = 3;
		public static final int I = 37;
		public static final int J = 38;
		public static final int K = 39;
		public static final int L = 40;
		public static final int LEFT_BRACKET = 71;
		public static final int M = 41;
		public static final int MEDIA_FAST_FORWARD = 90;
		public static final int MEDIA_NEXT = 87;
		public static final int MEDIA_PLAY_PAUSE = 85;
		public static final int MEDIA_PREVIOUS = 88;
		public static final int MEDIA_REWIND = 89;
		public static final int MEDIA_STOP = 86;
		public static final int MENU = 82;
		public static final int MINUS = 69;
		public static final int MUTE = 91;
		public static final int N = 42;
		public static final int NOTIFICATION = 83;
		public static final int NUM = 78;
		public static final int O = 43;
		public static final int P = 44;
		public static final int PAUSE = 121; // aka break
		public static final int PERIOD = 56;
		public static final int PLUS = 81;
		public static final int POUND = 18;
		public static final int POWER = 26;
		public static final int PRINT_SCREEN = 120; // aka SYSRQ
		public static final int Q = 45;
		public static final int R = 46;
		public static final int RIGHT_BRACKET = 72;
		public static final int S = 47;
		public static final int SCROLL_LOCK = 116;
		public static final int SEARCH = 84;
		public static final int SEMICOLON = 74;
		public static final int SHIFT_LEFT = 59;
		public static final int SHIFT_RIGHT = 60;
		public static final int SLASH = 76;
		public static final int SOFT_LEFT = 1;
		public static final int SOFT_RIGHT = 2;
		public static final int SPACE = 62;
		public static final int STAR = 17;
		public static final int SYM = 63; // on MacOS, this is Command (⌘)
		public static final int T = 48;
		public static final int TAB = 61;
		public static final int U = 49;
		public static final int UNKNOWN = 0;
		public static final int V = 50;
		public static final int VOLUME_DOWN = 25;
		public static final int VOLUME_UP = 24;
		public static final int W = 51;
		public static final int X = 52;
		public static final int Y = 53;
		public static final int Z = 54;
		public static final int META_ALT_LEFT_ON = 16;
		public static final int META_ALT_ON = 2;
		public static final int META_ALT_RIGHT_ON = 32;
		public static final int META_SHIFT_LEFT_ON = 64;
		public static final int META_SHIFT_ON = 1;
		public static final int META_SHIFT_RIGHT_ON = 128;
		public static final int META_SYM_ON = 4;
		public static final int CONTROL_LEFT = 129;
		public static final int CONTROL_RIGHT = 130;
		public static final int ESCAPE = 111;
		public static final int END = 123;
		public static final int INSERT = 124;
		public static final int PAGE_UP = 92;
		public static final int PAGE_DOWN = 93;
		public static final int PICTSYMBOLS = 94;
		public static final int SWITCH_CHARSET = 95;
		public static final int BUTTON_CIRCLE = 255;
		public static final int BUTTON_A = 96;
		public static final int BUTTON_B = 97;
		public static final int BUTTON_C = 98;
		public static final int BUTTON_X = 99;
		public static final int BUTTON_Y = 100;
		public static final int BUTTON_Z = 101;
		public static final int BUTTON_L1 = 102;
		public static final int BUTTON_R1 = 103;
		public static final int BUTTON_L2 = 104;
		public static final int BUTTON_R2 = 105;
		public static final int BUTTON_THUMBL = 106;
		public static final int BUTTON_THUMBR = 107;
		public static final int BUTTON_START = 108;
		public static final int BUTTON_SELECT = 109;
		public static final int BUTTON_MODE = 110;
		
		public static final int NUMPAD_0 = 144;
		public static final int NUMPAD_1 = 145;
		public static final int NUMPAD_2 = 146;
		public static final int NUMPAD_3 = 147;
		public static final int NUMPAD_4 = 148;
		public static final int NUMPAD_5 = 149;
		public static final int NUMPAD_6 = 150;
		public static final int NUMPAD_7 = 151;
		public static final int NUMPAD_8 = 152;
		public static final int NUMPAD_9 = 153;
		
		public static final int NUMPAD_DIVIDE = 154;
		public static final int NUMPAD_MULTIPLY = 155;
		public static final int NUMPAD_SUBTRACT = 156;
		public static final int NUMPAD_ADD = 157;
		public static final int NUMPAD_DOT = 158;
		public static final int NUMPAD_COMMA = 159;
		public static final int NUMPAD_ENTER = 160;
		public static final int NUMPAD_EQUALS = 161;
		public static final int NUMPAD_LEFT_PAREN = 162;
		public static final int NUMPAD_RIGHT_PAREN = 163;
		public static final int NUM_LOCK = 143;
		
		public static final int COLON = 243;
		public static final int F1 = 131;
		public static final int F2 = 132;
		public static final int F3 = 133;
		public static final int F4 = 134;
		public static final int F5 = 135;
		public static final int F6 = 136;
		public static final int F7 = 137;
		public static final int F8 = 138;
		public static final int F9 = 139;
		public static final int F10 = 140;
		public static final int F11 = 141;
		public static final int F12 = 142;
		public static final int F13 = 183;
		public static final int F14 = 184;
		public static final int F15 = 185;
		public static final int F16 = 186;
		public static final int F17 = 187;
		public static final int F18 = 188;
		public static final int F19 = 189;
		public static final int F20 = 190;
		public static final int F21 = 191;
		public static final int F22 = 192;
		public static final int F23 = 193;
		public static final int F24 = 194;
		
		public static final int MAX_KEYCODE = 255;
		
		public static String toString(final int keycode) {
			if (keycode < 0)
				throw new IllegalArgumentException("keycode cannot be negative, keycode: " + keycode);
			if (keycode > MAX_KEYCODE)
				throw new IllegalArgumentException("keycode cannot be greater than 255, keycode: " + keycode);
			return switch (keycode) {
				case UNKNOWN -> "Unknown";
				case SOFT_LEFT -> "Soft Left";
				case SOFT_RIGHT -> "Soft Right";
				case HOME -> "Home";
				case BACK -> "Back";
				case CALL -> "Call";
				case ENDCALL -> "End Call";
				case NUM_0 -> "0";
				case NUM_1 -> "1";
				case NUM_2 -> "2";
				case NUM_3 -> "3";
				case NUM_4 -> "4";
				case NUM_5 -> "5";
				case NUM_6 -> "6";
				case NUM_7 -> "7";
				case NUM_8 -> "8";
				case NUM_9 -> "9";
				case STAR -> "*";
				case POUND -> "#";
				case UP -> "Up";
				case DOWN -> "Down";
				case LEFT -> "Left";
				case RIGHT -> "Right";
				case CENTER -> "Center";
				case VOLUME_UP -> "Volume Up";
				case VOLUME_DOWN -> "Volume Down";
				case POWER -> "Power";
				case CAMERA -> "Camera";
				case CLEAR -> "Clear";
				case A -> "A";
				case B -> "B";
				case C -> "C";
				case D -> "D";
				case E -> "E";
				case F -> "F";
				case G -> "G";
				case H -> "H";
				case I -> "I";
				case J -> "J";
				case K -> "K";
				case L -> "L";
				case M -> "M";
				case N -> "N";
				case O -> "O";
				case P -> "P";
				case Q -> "Q";
				case R -> "R";
				case S -> "S";
				case T -> "T";
				case U -> "U";
				case V -> "V";
				case W -> "W";
				case X -> "X";
				case Y -> "Y";
				case Z -> "Z";
				case COMMA -> ",";
				case PERIOD -> ".";
				case ALT_LEFT -> "L-Alt";
				case ALT_RIGHT -> "R-Alt";
				case SHIFT_LEFT -> "L-Shift";
				case SHIFT_RIGHT -> "R-Shift";
				case TAB -> "Tab";
				case SPACE -> "Space";
				case SYM -> "SYM";
				case EXPLORER -> "Explorer";
				case ENVELOPE -> "Envelope";
				case ENTER -> "Enter";
				case DEL -> "Delete";
				case GRAVE -> "`";
				case MINUS -> "-";
				case EQUALS -> "=";
				case LEFT_BRACKET -> "[";
				case RIGHT_BRACKET -> "]";
				case BACKSLASH -> "\\";
				case SEMICOLON -> ";";
				case APOSTROPHE -> "'";
				case SLASH -> "/";
				case AT -> "@";
				case NUM -> "Num";
				case HEADSETHOOK -> "Headset Hook";
				case FOCUS -> "Focus";
				case PLUS -> "Plus";
				case MENU -> "Menu";
				case NOTIFICATION -> "Notification";
				case SEARCH -> "Search";
				case MEDIA_PLAY_PAUSE -> "Play/Pause";
				case MEDIA_STOP -> "Stop Media";
				case MEDIA_NEXT -> "Next Media";
				case MEDIA_PREVIOUS -> "Prev Media";
				case MEDIA_REWIND -> "Rewind";
				case MEDIA_FAST_FORWARD -> "Fast Forward";
				case MUTE -> "Mute";
				case PAGE_UP -> "Page Up";
				case PAGE_DOWN -> "Page Down";
				case PICTSYMBOLS -> "PICTSYMBOLS";
				case SWITCH_CHARSET -> "SWITCH_CHARSET";
				case BUTTON_A -> "A Button";
				case BUTTON_B -> "B Button";
				case BUTTON_C -> "C Button";
				case BUTTON_X -> "X Button";
				case BUTTON_Y -> "Y Button";
				case BUTTON_Z -> "Z Button";
				case BUTTON_L1 -> "L1 Button";
				case BUTTON_R1 -> "R1 Button";
				case BUTTON_L2 -> "L2 Button";
				case BUTTON_R2 -> "R2 Button";
				case BUTTON_THUMBL -> "Left Thumb";
				case BUTTON_THUMBR -> "Right Thumb";
				case BUTTON_START -> "Start";
				case BUTTON_SELECT -> "Select";
				case BUTTON_MODE -> "Button Mode";
				case FORWARD_DEL -> "Forward Delete";
				case CONTROL_LEFT -> "L-Ctrl";
				case CONTROL_RIGHT -> "R-Ctrl";
				case ESCAPE -> "Escape";
				case END -> "End";
				case INSERT -> "Insert";
				case NUMPAD_0 -> "Numpad 0";
				case NUMPAD_1 -> "Numpad 1";
				case NUMPAD_2 -> "Numpad 2";
				case NUMPAD_3 -> "Numpad 3";
				case NUMPAD_4 -> "Numpad 4";
				case NUMPAD_5 -> "Numpad 5";
				case NUMPAD_6 -> "Numpad 6";
				case NUMPAD_7 -> "Numpad 7";
				case NUMPAD_8 -> "Numpad 8";
				case NUMPAD_9 -> "Numpad 9";
				case COLON -> ":";
				case F1 -> "F1";
				case F2 -> "F2";
				case F3 -> "F3";
				case F4 -> "F4";
				case F5 -> "F5";
				case F6 -> "F6";
				case F7 -> "F7";
				case F8 -> "F8";
				case F9 -> "F9";
				case F10 -> "F10";
				case F11 -> "F11";
				case F12 -> "F12";
				case F13 -> "F13";
				case F14 -> "F14";
				case F15 -> "F15";
				case F16 -> "F16";
				case F17 -> "F17";
				case F18 -> "F18";
				case F19 -> "F19";
				case F20 -> "F20";
				case F21 -> "F21";
				case F22 -> "F22";
				case F23 -> "F23";
				case F24 -> "F24";
				case NUMPAD_DIVIDE -> "Num /";
				case NUMPAD_MULTIPLY -> "Num *";
				case NUMPAD_SUBTRACT -> "Num -";
				case NUMPAD_ADD -> "Num +";
				case NUMPAD_DOT -> "Num .";
				case NUMPAD_COMMA -> "Num ,";
				case NUMPAD_ENTER -> "Num Enter";
				case NUMPAD_EQUALS -> "Num =";
				case NUMPAD_LEFT_PAREN -> "Num (";
				case NUMPAD_RIGHT_PAREN -> "Num )";
				case NUM_LOCK -> "Num Lock";
				case CAPS_LOCK -> "Caps Lock";
				case SCROLL_LOCK -> "Scroll Lock";
				case PAUSE -> "Pause";
				case PRINT_SCREEN -> "Print";
				default -> null;
			};
		}
		
		private static ObjectIntMap<String> keyNames;
		
		public static int valueOf(final String keyName) {
			if (keyNames == null)
				initializeKeyNames();
			return keyNames.get(keyName, -1);
		}
		
		private static void initializeKeyNames() {
			keyNames = new ObjectIntMap<>();
			for (int i = 0; i < 256; i++) {
				String name = toString(i);
				if (name != null)
					keyNames.put(name, i);
			}
		}
		
	}
	
	enum Peripheral {
		HardwareKeyboard, OnscreenKeyboard, MultitouchScreen, Accelerometer, Compass, Vibrator, HapticFeedback, Gyroscope, RotationVector, Pressure
	}
	
	float getAccelerometerX();
	
	float getAccelerometerY();
	
	float getAccelerometerZ();
	
	float getGyroscopeX();
	
	float getGyroscopeY();
	
	float getGyroscopeZ();
	
	int getMaxPointers();
	
	int getX();
	
	int getX(final int pointer);
	
	int getDeltaX();
	
	int getDeltaX(final int pointer);
	
	int getY();
	
	int getY(final int pointer);
	
	int getDeltaY();
	
	int getDeltaY(final int pointer);
	
	boolean isTouched();
	
	boolean justTouched();
	
	boolean isTouched(final int pointer);
	
	float getPressure();
	
	float getPressure(final int pointer);
	
	boolean isButtonPressed(final int button);
	
	boolean isButtonJustPressed(final int button);
	
	boolean isKeyPressed(final int key);
	
	boolean isKeyJustPressed(final int key);
	
	void getTextInput(final TextInputListener listener, final String title, final String text, final String hint);
	
	void getTextInput(final TextInputListener listener, final String title, final String text, final String hint, final OnscreenKeyboardType type);
	
	void setOnscreenKeyboardVisible(final boolean visible);
	
	void setOnscreenKeyboardVisible(final boolean visible, final OnscreenKeyboardType type);
	
	interface InputStringValidator {
		
		boolean validate(String toCheck);
		
	}
	
	void openTextInputField(final NativeInputConfiguration configuration);
	
	void closeTextInputField(final boolean sendReturn);
	
	interface KeyboardHeightObserver {
		
		void onKeyboardHeightChanged(final int height);
		
	}
	
	void setKeyboardHeightObserver(KeyboardHeightObserver observer);
	
	enum OnscreenKeyboardType {
		Default, NumberPad, PhonePad, Email, Password, URI
	}
	
	void vibrate(final int milliseconds);
	
	void vibrate(final int milliseconds, final boolean fallback);
	
	void vibrate(final int milliseconds, final int amplitude, final boolean fallback);
	
	void vibrate(final VibrationType vibrationType);
	
	enum VibrationType {
		LIGHT, MEDIUM, HEAVY;
	}
	
	float getAzimuth();
	
	float getPitch();
	
	float getRoll();
	
	void getRotationMatrix(final float[] matrix);
	
	long getCurrentEventTime();
	
	void setCatchKey(final int keycode, final boolean catchKey);
	
	boolean isCatchKey(final int keycode);
	
	void setInputProcessor(final InputProcessor processor);
	
	InputProcessor getInputProcessor();
	
	boolean isPeripheralAvailable(final Peripheral peripheral);
	
	int getRotation();
	
	Orientation getNativeOrientation();
	
	enum Orientation {
		Landscape, Portrait
	}
	
	void setCursorCatched(final boolean catched);
	
	boolean isCursorCatched();
	
	void setCursorPosition(final int x, final int y);
	
}
