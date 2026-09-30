package dev.ooga.client.ui.hud;

import dev.ooga.client.module.impl.client.InventoryHudModule;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.render.OogaFonts;
import dev.ooga.client.ui.render.OogaFonts.Weight;
import dev.ooga.client.ui.render.Render2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** The 27 main inventory slots (plus optionally the hotbar) in a panel. */
public class InventoryHud extends HudElement {
	private static final float SLOT = 18f;
	private static final float PAD = 5f;
	private static final float TITLE_H = 13f;

	private final InventoryHudModule module;

	public InventoryHud(InventoryHudModule module) {
		super("inventory", "Inventory HUD", Anchor.END, 1f, Anchor.START, 0.3f);
		this.module = module;
	}

	@Override
	public boolean isVisible() {
		return module.isEnabled() && Minecraft.getInstance().player != null;
	}

	@Override
	public NumberSetting scaleSetting() {
		return module.scale;
	}

	@Override
	protected void render(GuiGraphics g, float x, float y, float delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		Inventory inventory = mc.player.getInventory();
		float scale = module.scale.getFloat();
		int rows = module.hotbar.get() ? 4 : 3;
		float top = module.title.get() ? TITLE_H : 0;
		float w = PAD * 2 + SLOT * 9;
		float h = PAD * 2 + top + SLOT * rows + (rows == 4 ? 3f : 0);
		width = w * scale;
		height = h * scale;

		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		Render2D.roundRect(g, 0, 0, w, h, OogaTheme.RADIUS_CARD, 0xD90E0F12);
		Render2D.outline(g, 0, 0, w, h, OogaTheme.RADIUS_CARD, OogaTheme.BORDER);
		if (module.title.get()) OogaFonts.draw(g, "Inventory", PAD + 1f, PAD, OogaTheme.TEXT, Weight.SEMIBOLD, 0.85f);

		for (int row = 0; row < rows; row++) {
			// Rows 0-2 are the main inventory (slots 9-35); row 3 is the hotbar (0-8), set slightly apart.
			int first = row < 3 ? 9 + row * 9 : 0;
			float sy = PAD + top + row * SLOT + (row == 3 ? 3f : 0);
			for (int col = 0; col < 9; col++) {
				float sx = PAD + col * SLOT;
				Render2D.roundRect(g, sx + 0.5f, sy + 0.5f, SLOT - 1f, SLOT - 1f, 2f, 0x22FFFFFF);
				ItemStack stack = inventory.getItem(first + col);
				if (stack.isEmpty()) continue;
				g.renderItem(stack, Math.round(sx + 1), Math.round(sy + 1));
				g.renderItemDecorations(mc.font, stack, Math.round(sx + 1), Math.round(sy + 1));
			}
		}
		g.pose().popMatrix();
	}
}
