package dev.ooga.client.util;

/** ARGB helpers. Every color in Ooga is a packed 0xAARRGGBB int. */
public final class ColorUtil {
	private ColorUtil() {
	}

	public static int alpha(int color) {
		return color >>> 24;
	}

	public static int withAlpha(int color, int alpha) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0xFFFFFF);
	}

	/** Multiplies the color's existing alpha by {@code factor} (0..1). */
	public static int fade(int color, float factor) {
		return withAlpha(color, Math.round(alpha(color) * Math.max(0f, Math.min(1f, factor))));
	}

	public static int lerp(int from, int to, float t) {
		t = Math.max(0f, Math.min(1f, t));
		int a = Math.round((from >>> 24) + ((to >>> 24) - (from >>> 24)) * t);
		int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
		int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
		int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}
}
