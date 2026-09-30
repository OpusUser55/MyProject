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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Your main inventory (the 27 slots above the hotbar) always on screen. */
public class InventoryHudModule extends Module {
	public final NumberSetting scale = add(new NumberSetting("Scale", "Panel size.", 1.0, 0.5, 2.0, 0.05, "x"));

	public InventoryHudModule() {
		super("Inventory HUD", "Shows your inventory on screen.", Category.HUD);
		hideFromList();
		HudManager.get().register(new Element());
	}

	private final class Element extends HudElement {
		Element() {
			super("inventory", "Inventory", Anchor.END, 1f, Anchor.START, 0.5f);
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
			float cell = 17f, pad = 4f, header = 11f;
			float w = pad * 2 + cell * 9, h = pad * 2 + header + cell * 3;
			width = w * s;
			height = h * s;
			if (mc.player == null) return;
			g.pose().pushMatrix();
			g.pose().translate(x, y);
			g.pose().scale(s, s);
			Render2D.roundRect(g, 0, 0, w, h, OogaTheme.RADIUS_CARD, 0xD90E0F12);
			Render2D.outline(g, 0, 0, w, h, OogaTheme.RADIUS_CARD, OogaTheme.BORDER);
			Render2D.circle(g, pad + 2.5f, pad + 3.5f, 2f, OogaTheme.GOLD);
			OogaFonts.draw(g, "Inventory", pad + 8f, pad, OogaTheme.TEXT, Weight.SEMIBOLD, 0.85f);
			Inventory inventory = mc.player.getInventory();
			for (int i = 0; i < 27; i++) {
				float cx = pad + (i % 9) * cell, cy = pad + header + (i / 9) * cell;
				Render2D.roundRect(g, cx + 0.5f, cy + 0.5f, cell - 1f, cell - 1f, 2f, 0x14FFFFFF);
				ItemStack stack = inventory.getItem(9 + i);
				if (stack.isEmpty()) continue;
				int ix = Math.round(cx), iy = Math.round(cy);
				g.renderItem(stack, ix, iy);
				g.renderItemDecorations(mc.font, stack, ix, iy);
			}
			g.pose().popMatrix();
		}
	}
}
