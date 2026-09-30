package dev.ooga.client.module.impl.render;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.render.GlowRenderer;
import dev.ooga.client.ui.render.Render2D;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.HitResult;

/** A clean, configurable crosshair in place of vanilla's. It opens up while your attack recharges. */
public class CustomCrosshairModule extends Module {
	public final ModeSetting style = add(new ModeSetting("Style", "Crosshair shape.", "Cross", "Cross", "Dot", "Circle", "Cross + Dot"));
	public final NumberSetting length = add(new NumberSetting("Length", "Arm length.", 4, 1, 12, 0.5, "px"));
	public final NumberSetting gap = add(new NumberSetting("Gap", "Space in the middle.", 2, 0, 8, 0.5, "px"));
	public final NumberSetting thickness = add(new NumberSetting("Thickness", "Arm thickness.", 1, 0.5, 3, 0.5, "px"));
	public final ModeSetting color = add(new ModeSetting("Color", "Crosshair colour.", "Accent", "Accent", "White", "Red", "Green"));
	public final BooleanSetting dynamic = add(new BooleanSetting("Dynamic", "Spread out while your attack is recharging.", true));
	public final BooleanSetting targetColor = add(new BooleanSetting("Target Color", "Turn red when aiming at an entity.", true));
	public final BooleanSetting glow = add(new BooleanSetting("Glow", "Soft glow around the crosshair.", true));

	public CustomCrosshairModule() {
		super("Custom Crosshair", "A clean, configurable crosshair.", Category.RENDER);
		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, vanilla -> (g, tracker) -> {
			if (isEnabled() && mc.options.getCameraType().isFirstPerson()) draw(g, tracker);
			else vanilla.render(g, tracker);
		});
	}

	private int color() {
		if (targetColor.get() && mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY) return 0xFFE5484D;
		return switch (color.get()) {
			case "White" -> 0xFFFFFFFF;
			case "Red" -> 0xFFE5484D;
			case "Green" -> 0xFF46C37B;
			default -> OogaTheme.GOLD;
		};
	}

	private void draw(GuiGraphics g, DeltaTracker tracker) {
		if (mc.player == null || mc.options.hideGui) return;
		float cx = mc.getWindow().getGuiScaledWidth() / 2f, cy = mc.getWindow().getGuiScaledHeight() / 2f;
		float charge = mc.player.getAttackStrengthScale(tracker.getGameTimeDeltaPartialTick(false));
		float spread = dynamic.get() ? (1f - charge) * 4f : 0f;
		float gp = gap.getFloat() + spread, len = length.getFloat(), th = thickness.getFloat();
		int c = color();
		boolean cross = style.is("Cross") || style.is("Cross + Dot");
		boolean dot = style.is("Dot") || style.is("Cross + Dot");
		if (glow.get()) GlowRenderer.glow(g, cx - gp - len, cy - th / 2f, (gp + len) * 2, th, th / 2f, c, 0.5f, 3f);
		if (cross) {
			Render2D.rect(g, cx - gp - len, cy - th / 2f, len, th, c);
			Render2D.rect(g, cx + gp, cy - th / 2f, len, th, c);
			Render2D.rect(g, cx - th / 2f, cy - gp - len, th, len, c);
			Render2D.rect(g, cx - th / 2f, cy + gp, th, len, c);
		}
		if (dot) Render2D.circle(g, cx, cy, Math.max(0.75f, th), c);
		if (style.is("Circle")) {
			float r = gp + len / 2f;
			Render2D.outline(g, cx - r, cy - r, r * 2, r * 2, r, Math.max(1, Math.round(th)), c);
		}
	}
}
