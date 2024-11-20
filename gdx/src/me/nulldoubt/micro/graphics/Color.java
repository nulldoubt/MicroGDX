package me.nulldoubt.micro.graphics;

import me.nulldoubt.micro.math.MathUtils;
import me.nulldoubt.micro.utils.Numbers;

public class Color {
	
	public static final Color WHITE = new Color(1, 1, 1, 1);
	public static final Color LIGHT_GRAY = new Color(0xbfbfbfff);
	public static final Color GRAY = new Color(0x7f7f7fff);
	public static final Color DARK_GRAY = new Color(0x3f3f3fff);
	public static final Color BLACK = new Color(0, 0, 0, 1);
	
	public static final float WHITE_FLOAT_BITS = WHITE.toFloatBits();
	
	public static final Color CLEAR = new Color(0, 0, 0, 0);
	public static final Color CLEAR_WHITE = new Color(1, 1, 1, 0);
	
	public static final Color BLUE = new Color(0, 0, 1, 1);
	public static final Color NAVY = new Color(0, 0, 0.5f, 1);
	public static final Color ROYAL = new Color(0x4169e1ff);
	public static final Color SLATE = new Color(0x708090ff);
	public static final Color SKY = new Color(0x87ceebff);
	public static final Color CYAN = new Color(0, 1, 1, 1);
	public static final Color TEAL = new Color(0, 0.5f, 0.5f, 1);
	
	public static final Color GREEN = new Color(0x00ff00ff);
	public static final Color CHARTREUSE = new Color(0x7fff00ff);
	public static final Color LIME = new Color(0x32cd32ff);
	public static final Color FOREST = new Color(0x228b22ff);
	public static final Color OLIVE = new Color(0x6b8e23ff);
	
	public static final Color YELLOW = new Color(0xffff00ff);
	public static final Color GOLD = new Color(0xffd700ff);
	public static final Color GOLDENROD = new Color(0xdaa520ff);
	public static final Color ORANGE = new Color(0xffa500ff);
	
	public static final Color BROWN = new Color(0x8b4513ff);
	public static final Color TAN = new Color(0xd2b48cff);
	public static final Color FIREBRICK = new Color(0xb22222ff);
	
	public static final Color RED = new Color(0xff0000ff);
	public static final Color SCARLET = new Color(0xff341cff);
	public static final Color CORAL = new Color(0xff7f50ff);
	public static final Color SALMON = new Color(0xfa8072ff);
	public static final Color PINK = new Color(0xff69b4ff);
	public static final Color MAGENTA = new Color(1, 0, 1, 1);
	
	public static final Color PURPLE = new Color(0xa020f0ff);
	public static final Color VIOLET = new Color(0xee82eeff);
	public static final Color MAROON = new Color(0xb03060ff);
	
	public float r, g, b, a;
	
	public Color() {}
	
	public Color(int rgba8888) {
		rgba8888ToColor(this, rgba8888);
	}
	
	public Color(float r, float g, float b, float a) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
		clamp();
	}
	
	public Color(Color color) {
		set(color);
	}
	
	public Color set(Color color) {
		this.r = color.r;
		this.g = color.g;
		this.b = color.b;
		this.a = color.a;
		return this;
	}
	
	public Color set(Color rgb, float alpha) {
		this.r = rgb.r;
		this.g = rgb.g;
		this.b = rgb.b;
		this.a = MathUtils.clamp(alpha, 0f, 1f);
		return this;
	}
	
	public Color mul(Color color) {
		this.r *= color.r;
		this.g *= color.g;
		this.b *= color.b;
		this.a *= color.a;
		return clamp();
	}
	
	public Color mul(float value) {
		this.r *= value;
		this.g *= value;
		this.b *= value;
		this.a *= value;
		return clamp();
	}
	
	public Color add(Color color) {
		this.r += color.r;
		this.g += color.g;
		this.b += color.b;
		this.a += color.a;
		return clamp();
	}
	
	public Color sub(Color color) {
		this.r -= color.r;
		this.g -= color.g;
		this.b -= color.b;
		this.a -= color.a;
		return clamp();
	}
	
	public Color clamp() {
		if (r < 0)
			r = 0;
		else if (r > 1)
			r = 1;
		
		if (g < 0)
			g = 0;
		else if (g > 1)
			g = 1;
		
		if (b < 0)
			b = 0;
		else if (b > 1)
			b = 1;
		
		if (a < 0)
			a = 0;
		else if (a > 1)
			a = 1;
		return this;
	}
	
	public Color set(float r, float g, float b, float a) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
		return clamp();
	}
	
	public Color set(int rgba) {
		rgba8888ToColor(this, rgba);
		return this;
	}
	
	public Color add(float r, float g, float b, float a) {
		this.r += r;
		this.g += g;
		this.b += b;
		this.a += a;
		return clamp();
	}
	
	public Color sub(float r, float g, float b, float a) {
		this.r -= r;
		this.g -= g;
		this.b -= b;
		this.a -= a;
		return clamp();
	}
	
	public Color mul(float r, float g, float b, float a) {
		this.r *= r;
		this.g *= g;
		this.b *= b;
		this.a *= a;
		return clamp();
	}
	
	public Color lerp(final Color target, final float t) {
		this.r += t * (target.r - this.r);
		this.g += t * (target.g - this.g);
		this.b += t * (target.b - this.b);
		this.a += t * (target.a - this.a);
		return clamp();
	}
	
	public Color lerp(final float r, final float g, final float b, final float a, final float t) {
		this.r += t * (r - this.r);
		this.g += t * (g - this.g);
		this.b += t * (b - this.b);
		this.a += t * (a - this.a);
		return clamp();
	}
	
	public Color premultiplyAlpha() {
		r *= a;
		g *= a;
		b *= a;
		return this;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Color color = (Color) o;
		return toIntBits() == color.toIntBits();
	}
	
	@Override
	public int hashCode() {
		int result = (r != +0.0f ? Numbers.floatToIntBits(r) : 0);
		result = 31 * result + (g != +0.0f ? Numbers.floatToIntBits(g) : 0);
		result = 31 * result + (b != +0.0f ? Numbers.floatToIntBits(b) : 0);
		result = 31 * result + (a != +0.0f ? Numbers.floatToIntBits(a) : 0);
		return result;
	}
	
	public float toFloatBits() {
		int color = ((int) (255 * a) << 24) | ((int) (255 * b) << 16) | ((int) (255 * g) << 8) | ((int) (255 * r));
		return Numbers.intToFloatColor(color);
	}
	
	public int toIntBits() {
		return ((int) (255 * a) << 24) | ((int) (255 * b) << 16) | ((int) (255 * g) << 8) | ((int) (255 * r));
	}
	
	public String toString() {
		StringBuilder value = new StringBuilder(Integer.toHexString(((int) (255 * r) << 24) | ((int) (255 * g) << 16) | ((int) (255 * b) << 8) | ((int) (255 * a))));
		while (value.length() < 8)
			value.insert(0, "0");
		return value.toString();
	}
	
	public static Color valueOf(String hex) {
		return valueOf(hex, new Color());
	}
	
	public static Color valueOf(String hex, Color color) {
		hex = hex.charAt(0) == '#' ? hex.substring(1) : hex;
		color.r = Integer.parseInt(hex.substring(0, 2), 16) / 255f;
		color.g = Integer.parseInt(hex.substring(2, 4), 16) / 255f;
		color.b = Integer.parseInt(hex.substring(4, 6), 16) / 255f;
		color.a = hex.length() != 8 ? 1 : Integer.parseInt(hex.substring(6, 8), 16) / 255f;
		return color;
	}
	
	public static float toFloatBits(int r, int g, int b, int a) {
		return Numbers.intToFloatColor((a << 24) | (b << 16) | (g << 8) | r);
	}
	
	public static float toFloatBits(float r, float g, float b, float a) {
		return Numbers.intToFloatColor(((int) (255 * a) << 24) | ((int) (255 * b) << 16) | ((int) (255 * g) << 8) | ((int) (255 * r)));
	}
	
	public static int toIntBits(int r, int g, int b, int a) {
		return (a << 24) | (b << 16) | (g << 8) | r;
	}
	
	public static int alpha(float alpha) {
		return (int) (alpha * 255.0f);
	}
	
	public static int luminanceAlpha(float luminance, float alpha) {
		return ((int) (luminance * 255.0f) << 8) | (int) (alpha * 255);
	}
	
	public static int rgb565(float r, float g, float b) {
		return ((int) (r * 31) << 11) | ((int) (g * 63) << 5) | (int) (b * 31);
	}
	
	public static int rgba4444(float r, float g, float b, float a) {
		return ((int) (r * 15) << 12) | ((int) (g * 15) << 8) | ((int) (b * 15) << 4) | (int) (a * 15);
	}
	
	public static int rgb888(float r, float g, float b) {
		return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
	}
	
	public static int rgba8888(float r, float g, float b, float a) {
		return ((int) (r * 255) << 24) | ((int) (g * 255) << 16) | ((int) (b * 255) << 8) | (int) (a * 255);
	}
	
	public static int argb8888(float a, float r, float g, float b) {
		return ((int) (a * 255) << 24) | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
	}
	
	public static int rgb565(Color color) {
		return ((int) (color.r * 31) << 11) | ((int) (color.g * 63) << 5) | (int) (color.b * 31);
	}
	
	public static int rgba4444(Color color) {
		return ((int) (color.r * 15) << 12) | ((int) (color.g * 15) << 8) | ((int) (color.b * 15) << 4) | (int) (color.a * 15);
	}
	
	public static int rgb888(Color color) {
		return ((int) (color.r * 255) << 16) | ((int) (color.g * 255) << 8) | (int) (color.b * 255);
	}
	
	public static int rgba8888(Color color) {
		return ((int) (color.r * 255) << 24) | ((int) (color.g * 255) << 16) | ((int) (color.b * 255) << 8) | (int) (color.a * 255);
	}
	
	public static int argb8888(Color color) {
		return ((int) (color.a * 255) << 24) | ((int) (color.r * 255) << 16) | ((int) (color.g * 255) << 8) | (int) (color.b * 255);
	}
	
	public static void rgb565ToColor(Color color, int value) {
		color.r = ((value & 0x0000F800) >>> 11) / 31f;
		color.g = ((value & 0x000007E0) >>> 5) / 63f;
		color.b = ((value & 0x0000001F)) / 31f;
	}
	
	public static void rgba4444ToColor(Color color, int value) {
		color.r = ((value & 0x0000f000) >>> 12) / 15f;
		color.g = ((value & 0x00000f00) >>> 8) / 15f;
		color.b = ((value & 0x000000f0) >>> 4) / 15f;
		color.a = ((value & 0x0000000f)) / 15f;
	}
	
	public static void rgb888ToColor(Color color, int value) {
		color.r = ((value & 0x00ff0000) >>> 16) / 255f;
		color.g = ((value & 0x0000ff00) >>> 8) / 255f;
		color.b = ((value & 0x000000ff)) / 255f;
	}
	
	public static void rgba8888ToColor(Color color, int value) {
		color.r = ((value & 0xff000000) >>> 24) / 255f;
		color.g = ((value & 0x00ff0000) >>> 16) / 255f;
		color.b = ((value & 0x0000ff00) >>> 8) / 255f;
		color.a = ((value & 0x000000ff)) / 255f;
	}
	
	public static void argb8888ToColor(Color color, int value) {
		color.a = ((value & 0xff000000) >>> 24) / 255f;
		color.r = ((value & 0x00ff0000) >>> 16) / 255f;
		color.g = ((value & 0x0000ff00) >>> 8) / 255f;
		color.b = ((value & 0x000000ff)) / 255f;
	}
	
	public static void abgr8888ToColor(Color color, int value) {
		color.a = ((value & 0xff000000) >>> 24) / 255f;
		color.b = ((value & 0x00ff0000) >>> 16) / 255f;
		color.g = ((value & 0x0000ff00) >>> 8) / 255f;
		color.r = ((value & 0x000000ff)) / 255f;
	}
	
	public static void abgr8888ToColor(Color color, float value) {
		int c = Numbers.floatToIntColor(value);
		color.a = ((c & 0xff000000) >>> 24) / 255f;
		color.b = ((c & 0x00ff0000) >>> 16) / 255f;
		color.g = ((c & 0x0000ff00) >>> 8) / 255f;
		color.r = ((c & 0x000000ff)) / 255f;
	}
	
	public Color fromHsv(float h, float s, float v) {
		float x = (h / 60f + 6) % 6;
		int i = (int) x;
		float f = x - i;
		float p = v * (1 - s);
		float q = v * (1 - s * f);
		float t = v * (1 - s * (1 - f));
		switch (i) {
			case 0:
				r = v;
				g = t;
				b = p;
				break;
			case 1:
				r = q;
				g = v;
				b = p;
				break;
			case 2:
				r = p;
				g = v;
				b = t;
				break;
			case 3:
				r = p;
				g = q;
				b = v;
				break;
			case 4:
				r = t;
				g = p;
				b = v;
				break;
			default:
				r = v;
				g = p;
				b = q;
		}
		
		return clamp();
	}
	
	public Color fromHsv(float[] hsv) {
		return fromHsv(hsv[0], hsv[1], hsv[2]);
	}
	
	public float[] toHsv(float[] hsv) {
		float max = Math.max(Math.max(r, g), b);
		float min = Math.min(Math.min(r, g), b);
		float range = max - min;
		if (range == 0) {
			hsv[0] = 0;
		} else if (max == r) {
			hsv[0] = (60 * (g - b) / range + 360) % 360;
		} else if (max == g) {
			hsv[0] = 60 * (b - r) / range + 120;
		} else {
			hsv[0] = 60 * (r - g) / range + 240;
		}
		
		if (max > 0) {
			hsv[1] = 1 - min / max;
		} else {
			hsv[1] = 0;
		}
		
		hsv[2] = max;
		
		return hsv;
	}
	
	public Color cpy() {
		return new Color(this);
	}
	
}
