package dev.ooga.client.module.impl.client;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.util.ColorUtil;

/** Global look-and-feel options. Read through the static accessors from anywhere in the UI. */
public class ClientSettings extends Module {
	private static ClientSettings instance;

	public final ModeSetting accent = add(new ModeSetting("Accent", "Shade of the signature yellow.", "Gold",
			"Gold", "Amber", "Honey", "Champagne", "Lemon")
			.onChange(this::applyTheme));
	public final ModeSetting corners = add(new ModeSetting("Corners", "Roundness of panels and controls.", "Rounded", "Rounded", "Soft", "Sharp")
			.onChange(this::applyTheme));
	public final NumberSetting panelOpacity = add(new NumberSetting("Panel Opacity", "How solid menu and HUD panels are.", 0.93, 0.6, 1.0, 0.01));
	public final BooleanSetting glow = add(new BooleanSetting("Glow", "Soft golden glow on active elements. Turn off on low-end hardware.", true));
	public final NumberSetting glowIntensity = add(new NumberSetting("Glow Intensity", "How strong the glow is.", 1.0, 0.1, 2.0, 0.05)
			.visibleWhen(glow::get));
	public final NumberSetting glowRadius = add(new NumberSetting("Glow Radius", "How far the glow spreads.", 4.0, 1.0, 8.0, 0.5, "px")
			.visibleWhen(glow::get));
	public final NumberSetting glowPulse = add(new NumberSetting("Glow Pulse", "Speed of the glow's slow breathing. 0 disables it.", 0.5, 0.0, 2.0, 0.05)
			.visibleWhen(glow::get));
	public final BooleanSetting animations = add(new BooleanSetting("Animations", "Animate panels, toggles and notifications.", true));
	public final NumberSetting animationSpeed = add(new NumberSetting("Animation Speed", "Speed multiplier for all UI animations.", 1.0, 0.25, 3.0, 0.05, "x")
			.visibleWhen(animations::get));
	public final BooleanSetting customFont = add(new BooleanSetting("Custom Font", "Use Ooga's typeface instead of the Minecraft font.", true));

	public ClientSettings() {
		super("Client Settings", "Glow, animation and typography preferences.", Category.CLIENT);
		settingsOnly();
		instance = this;
		applyTheme();
	}

	private void applyTheme() {
		OogaTheme.applyAccent(OogaTheme.Accent.byLabel(accent.get()));
		OogaTheme.applyCorners(switch (corners.get()) {
			case "Soft" -> 0.6f;
			case "Sharp" -> 0.2f;
			default -> 1f;
		});
	}

	/** Applies the Panel Opacity setting to a surface colour's own alpha. */
	public static int surface(int color) {
		return instance == null ? color : ColorUtil.fade(color, instance.panelOpacity.getFloat() / 0.93f);
	}

	public static boolean glowEnabled() {
		return instance == null || instance.glow.get();
	}

	public static float glowIntensity() {
		return instance == null ? 1f : instance.glowIntensity.getFloat();
	}

	public static float glowRadius() {
		return instance == null ? 4f : instance.glowRadius.getFloat();
	}

	public static float glowPulseSpeed() {
		return instance == null ? 0.5f : instance.glowPulse.getFloat();
	}

	/** Animation speed multiplier; 10 means "instant". */
	public static float animationSpeed() {
		if (instance == null) return 1f;
		return instance.animations.get() ? instance.animationSpeed.getFloat() : 10f;
	}

	public static boolean customFont() {
		return instance == null || instance.customFont.get();
	}
}
