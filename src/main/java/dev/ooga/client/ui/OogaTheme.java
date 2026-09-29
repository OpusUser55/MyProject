package dev.ooga.client.ui;

/**
 * Ooga's visual tokens. Charcoal surfaces carry the UI; gold is reserved for state and
 * emphasis (enabled modules, selection, sliders, focus) so it keeps its meaning.
 */
public final class OogaTheme {
	private OogaTheme() {
	}

	// Surfaces, darkest to lightest.
	public static final int SURFACE_WINDOW = 0xEB0D0E11;
	public static final int SURFACE_SIDEBAR = 0xF0101115;
	public static final int SURFACE_CARD = 0xFF16181D;
	public static final int SURFACE_CARD_HOVER = 0xFF1C1F25;
	public static final int SURFACE_INSET = 0xFF0F1014;
	public static final int SURFACE_CONTROL = 0xFF262930;

	// Lines.
	public static final int BORDER = 0x1FFFFFFF;
	public static final int BORDER_STRONG = 0x33FFFFFF;
	public static final int DIVIDER = 0x14FFFFFF;

	// Text.
	public static final int TEXT = 0xFFEDEDF0;
	public static final int TEXT_SECONDARY = 0xFFA3A6AF;
	public static final int TEXT_MUTED = 0xFF63666F;

	/**
	 * Accent presets. All stay in the yellow family, the client's signature, and differ in
	 * warmth and saturation. Each defines the base, a brighter highlight, a deeper shade and a
	 * text tint tuned to stay readable on the charcoal surfaces.
	 */
	public enum Accent {
		GOLD("Gold", 0xF2C14E, 0xFFD875, 0xC7962E, 0xF6CF6A),
		AMBER("Amber", 0xF4A83A, 0xFFC56B, 0xC27C1C, 0xF7B95C),
		HONEY("Honey", 0xE3AE4F, 0xF2C878, 0xB0802E, 0xEBBE6D),
		CHAMPAGNE("Champagne", 0xE2C98E, 0xF1DDB0, 0xB39A5E, 0xEAD6A6),
		LEMON("Lemon", 0xF1D550, 0xFBE588, 0xC2A62A, 0xF4DD72);

		public final String label;
		final int base, bright, deep, text;

		Accent(String label, int base, int bright, int deep, int text) {
			this.label = label;
			this.base = base;
			this.bright = bright;
			this.deep = deep;
			this.text = text;
		}

		public static Accent byLabel(String label) {
			for (Accent a : values()) if (a.label.equalsIgnoreCase(label)) return a;
			return GOLD;
		}
	}

	// Accent family. Not final: they follow the Accent setting in Client Settings.
	public static int GOLD;
	public static int GOLD_BRIGHT;
	public static int GOLD_DEEP;
	public static int GOLD_TINT;
	public static int GOLD_TEXT;
	public static final int ON_GOLD = 0xFF1A1407;

	// Radii (GUI units). Three sizes only, all scaled together by the Corners setting.
	private static final float BASE_WINDOW = 7f;
	private static final float BASE_CARD = 4.5f;
	private static final float BASE_CONTROL = 3f;
	public static float RADIUS_WINDOW = BASE_WINDOW;
	public static float RADIUS_CARD = BASE_CARD;
	public static float RADIUS_CONTROL = BASE_CONTROL;
	private static float cornerFactor = 1f;

	static {
		applyAccent(Accent.GOLD);
	}

	public static void applyAccent(Accent accent) {
		GOLD = 0xFF000000 | accent.base;
		GOLD_BRIGHT = 0xFF000000 | accent.bright;
		GOLD_DEEP = 0xFF000000 | accent.deep;
		GOLD_TEXT = 0xFF000000 | accent.text;
		GOLD_TINT = 0x1F000000 | accent.base;
	}

	/** Accent colour at the given alpha (0..255). */
	public static int accent(int alpha) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (GOLD & 0xFFFFFF);
	}

	public static void applyCorners(float factor) {
		cornerFactor = factor;
		RADIUS_WINDOW = BASE_WINDOW * factor;
		RADIUS_CARD = BASE_CARD * factor;
		RADIUS_CONTROL = BASE_CONTROL * factor;
	}

	/** Scales a component-specific radius by the Corners setting. */
	public static float corner(float radius) {
		return radius * cornerFactor;
	}

	// Spacing scale.
	public static final int SPACE_XS = 2;
	public static final int SPACE_S = 4;
	public static final int SPACE_M = 6;
	public static final int SPACE_L = 10;
}
