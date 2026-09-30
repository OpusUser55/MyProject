package dev.ooga.client.module.impl.client;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.hud.HudElement;
import dev.ooga.client.ui.hud.HudManager;
import dev.ooga.client.ui.render.OogaFonts;
import dev.ooga.client.ui.render.OogaFonts.Weight;
import dev.ooga.client.ui.render.Render2D;
import dev.ooga.client.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Armour (plus held items) with durability, so you know when to repair before it breaks. */
public class ArmorHudModule extends Module {
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

	public final BooleanSetting held = add(new BooleanSetting("Held Items", "Also show main and offhand items.", true));
	public final BooleanSetting percent = add(new BooleanSetting("Percent", "Durability as a percentage instead of uses left.", true));
	public final NumberSetting scale = add(new NumberSetting("Scale", "Panel size.", 1.0, 0.5, 2.0, 0.05, "x"));

	public ArmorHudModule() {
		super("Armor HUD", "Armour and tool durability at a glance.", Category.HUD);
		hideFromList();
		HudManager.get().register(new Element());
	}

	private final class Element extends HudElement {
		Element() {
			super("armor", "Armor HUD", Anchor.CENTER, 0.5f, Anchor.END, 0.84f);
		}

		@Override
		public boolean isVisible() {
			return isEnabled() && Minecraft.getInstance().player != null;
		}

		@Override
		public NumberSetting scaleSetting() {
			return scale;
		}

		@Override
		protected void render(GuiGraphics g, float x, float y, float delta) {
			Minecraft mc = Minecraft.getInstance();
			float s = scale.getFloat();
			float cell = 20f;
			int count = held.get() ? SLOTS.length : 4;
			float w = cell * count + 4f, h = 30f;
			width = w * s;
			height = h * s;
			if (mc.player == null) return;
			g.pose().pushMatrix();
			g.pose().translate(x, y);
			g.pose().scale(s, s);
			Render2D.roundRect(g, 0, 0, w, h, OogaTheme.RADIUS_CARD, 0xC80E0F12);
			for (int i = 0; i < count; i++) {
				ItemStack stack = mc.player.getItemBySlot(SLOTS[i]);
				float cx = 2f + i * cell;
				if (stack.isEmpty()) continue;
				g.renderItem(stack, Math.round(cx + 2), 2);
				if (!stack.isDamageableItem()) {
					if (stack.getCount() > 1) OogaFonts.drawCentered(g, Integer.toString(stack.getCount()), cx + cell / 2f, 20f, OogaTheme.TEXT, Weight.SEMIBOLD, 0.7f);
					continue;
				}
				int max = stack.getMaxDamage();
				int left = max - stack.getDamageValue();
				float ratio = left / (float) max;
				int color = ColorUtil.lerp(0xFFE5484D, 0xFF46C37B, ratio);
				String text = percent.get() ? Math.round(ratio * 100) + "%" : Integer.toString(left);
				OogaFonts.drawCentered(g, text, cx + cell / 2f, 20f, color, Weight.SEMIBOLD, 0.7f);
			}
			g.pose().popMatrix();
		}
	}
}
