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

	// Gold accent family.
	public static final int GOLD = 0xFFF2C14E;
	public static final int GOLD_BRIGHT = 0xFFFFD875;
	public static final int GOLD_DEEP = 0xFFC7962E;
	public static final int GOLD_TINT = 0x1FF2C14E;
	public static final int GOLD_TEXT = 0xFFF6CF6A;
	public static final int ON_GOLD = 0xFF1A1407;

	// Radii (GUI units). Kept to three values so corners feel consistent everywhere.
	public static final float RADIUS_WINDOW = 7f;
	public static final float RADIUS_CARD = 4.5f;
	public static final float RADIUS_CONTROL = 3f;

	// Spacing scale.
	public static final int SPACE_XS = 2;
	public static final int SPACE_S = 4;
	public static final int SPACE_M = 6;
	public static final int SPACE_L = 10;
}
