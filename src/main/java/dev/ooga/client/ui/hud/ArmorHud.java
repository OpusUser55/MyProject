package dev.ooga.client.ui.hud;

import dev.ooga.client.module.impl.client.ArmorHudModule;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.render.OogaFonts;
import dev.ooga.client.ui.render.OogaFonts.Weight;
import dev.ooga.client.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Worn armor (helmet first) and optionally held items, each with its durability. */
public class ArmorHud extends HudElement {
	private static final float CELL = 18f;
	private static final float TEXT_H = 7f;

	private final ArmorHudModule module;

	public ArmorHud(ArmorHudModule module) {
		super("armor", "Armor HUD", Anchor.CENTER, 0.5f, Anchor.END, 0.84f);
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

	private List<ItemStack> items() {
		Minecraft mc = Minecraft.getInstance();
		List<ItemStack> items = new ArrayList<>();
		if (mc.player == null) return items;
		for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			ItemStack stack = mc.player.getItemBySlot(slot);
			if (!stack.isEmpty()) items.add(stack);
		}
		if (module.hands.get()) {
			if (!mc.player.getMainHandItem().isEmpty()) items.add(mc.player.getMainHandItem());
			if (!mc.player.getOffhandItem().isEmpty()) items.add(mc.player.getOffhandItem());
		}
		return items;
	}

	@Override
	protected void render(GuiGraphics g, float x, float y, float delta) {
		Minecraft mc = Minecraft.getInstance();
		List<ItemStack> items = items();
		float scale = module.scale.getFloat();
		boolean vertical = module.vertical.get();
		boolean text = module.percent.get();
		float cellH = CELL + (text ? TEXT_H : 0);
		if (items.isEmpty()) {
			// Keep a size so the element can still be found and dragged in the HUD editor.
			width = CELL * scale;
			height = cellH * scale;
			return;
		}
		float w = vertical ? CELL : CELL * items.size();
		float h = vertical ? cellH * items.size() : cellH;
		width = w * scale;
		height = h * scale;

		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		for (int i = 0; i < items.size(); i++) {
			ItemStack stack = items.get(i);
			float cx = vertical ? 0 : i * CELL;
			float cy = vertical ? i * cellH : 0;
			int ix = Math.round(cx + 1), iy = Math.round(cy + 1);
			g.renderItem(stack, ix, iy);
			g.renderItemDecorations(mc.font, stack, ix, iy);
			if (text && stack.isDamageableItem()) {
				float left = 1f - (float) stack.getDamageValue() / stack.getMaxDamage();
				String label = Math.round(left * 100) + "%";
				// Red through yellow to green.
				int color = left > 0.5f ? ColorUtil.lerp(0xFFFFD34A, 0xFF5BE37A, (left - 0.5f) * 2f)
						: ColorUtil.lerp(0xFFE8594A, 0xFFFFD34A, left * 2f);
				float tw = OogaFonts.width(label, Weight.SEMIBOLD, 0.6f);
				OogaFonts.draw(g, label, cx + (CELL - tw) / 2f, cy + CELL, color, Weight.SEMIBOLD, 0.6f);
			}
		}
		g.pose().popMatrix();
	}
}
