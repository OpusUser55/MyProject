package dev.ooga.client.module.impl.client;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.hud.HudElement;
import dev.ooga.client.ui.hud.HudManager;
import dev.ooga.client.ui.render.OogaFonts;
import dev.ooga.client.ui.render.OogaFonts.Weight;
import dev.ooga.client.ui.render.Render2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;

/** Active potion effects with level and time left; effects about to run out turn red. */
public class PotionHudModule extends Module {
	public final NumberSetting scale = add(new NumberSetting("Scale", "Panel size.", 1.0, 0.5, 2.0, 0.05, "x"));

	public PotionHudModule() {
		super("Potion HUD", "Your active effects and how long they last.", Category.HUD);
		hideFromList();
		HudManager.get().register(new Element());
	}

	private static String roman(int level) {
		return switch (level) {
			case 1 -> "";
			case 2 -> " II";
			case 3 -> " III";
			case 4 -> " IV";
			case 5 -> " V";
			default -> " " + level;
		};
	}

	private final class Element extends HudElement {
		Element() {
			super("potions", "Potion HUD", Anchor.END, 1f, Anchor.END, 0.7f);
		}

		@Override
		public boolean isVisible() {
			Minecraft mc = Minecraft.getInstance();
			return isEnabled() && mc.player != null && !mc.player.getActiveEffects().isEmpty();
		}

		@Override
		public NumberSetting scaleSetting() {
			return scale;
		}

		@Override
		protected void render(GuiGraphics g, float x, float y, float delta) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player == null) return;
			float s = scale.getFloat();
			List<String[]> lines = new ArrayList<>();
			List<Integer> colors = new ArrayList<>();
			for (MobEffectInstance effect : mc.player.getActiveEffects()) {
				String name = effect.getEffect().value().getDisplayName().getString() + roman(effect.getAmplifier() + 1);
				String time;
				if (effect.isInfiniteDuration()) time = "∞";
				else {
					int seconds = effect.getDuration() / 20;
					time = seconds / 60 + ":" + String.format("%02d", seconds % 60);
				}
				lines.add(new String[]{name, time});
				colors.add(!effect.isInfiniteDuration() && effect.getDuration() < 200 ? 0xFFE5484D : 0xFF000000 | effect.getEffect().value().getColor());
			}
			float w = 60f;
			for (String[] l : lines) w = Math.max(w, OogaFonts.width(l[0], Weight.REGULAR, 0.85f) + OogaFonts.width(l[1], Weight.SEMIBOLD, 0.85f) + 22f);
			float h = lines.size() * 11f + 6f;
			width = w * s;
			height = h * s;
			g.pose().pushMatrix();
			g.pose().translate(x, y);
			g.pose().scale(s, s);
			Render2D.roundRect(g, 0, 0, w, h, OogaTheme.RADIUS_CARD, 0xD90E0F12);
			float ly = 3f;
			for (int i = 0; i < lines.size(); i++) {
				Render2D.circle(g, 6f, ly + 4f, 2.2f, colors.get(i));
				OogaFonts.draw(g, lines.get(i)[0], 12f, ly, OogaTheme.TEXT, Weight.REGULAR, 0.85f);
				float tw = OogaFonts.width(lines.get(i)[1], Weight.SEMIBOLD, 0.85f);
				OogaFonts.draw(g, lines.get(i)[1], w - 5f - tw, ly, colors.get(i) == 0xFFE5484D ? 0xFFE5484D : OogaTheme.TEXT_SECONDARY, Weight.SEMIBOLD, 0.85f);
				ly += 11f;
			}
			g.pose().popMatrix();
		}
	}
}
