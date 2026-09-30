package dev.ooga.client.ui.hud;

import dev.ooga.client.module.impl.client.ItemCountHudModule;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.render.OogaFonts;
import dev.ooga.client.ui.render.OogaFonts.Weight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Item icons with how many of each you carry across your whole inventory (offhand and armor included). */
public class ItemCountHud extends HudElement {
	private static final float CELL = 18f;
	private static final float TEXT_H = 7f;

	private final ItemCountHudModule module;

	public ItemCountHud(ItemCountHudModule module) {
		super("itemcount", "Item Count HUD", Anchor.END, 1f, Anchor.END, 0.84f);
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

	private record Entry(ItemStack icon, int count) {
	}

	private List<Entry> entries() {
		Minecraft mc = Minecraft.getInstance();
		List<Entry> entries = new ArrayList<>();
		if (mc.player == null) return entries;
		Inventory inventory = mc.player.getInventory();
		for (Item item : module.tracked()) {
			int count = 0;
			for (int i = 0; i < inventory.getContainerSize(); i++) {
				ItemStack stack = inventory.getItem(i);
				if (stack.is(item)) count += stack.getCount();
			}
			if (count > 0 || !module.hideEmpty.get()) entries.add(new Entry(new ItemStack(item), count));
		}
		if (module.food.get()) {
			int count = 0;
			for (int i = 0; i < inventory.getContainerSize(); i++) {
				ItemStack stack = inventory.getItem(i);
				if (stack.has(DataComponents.FOOD)) count += stack.getCount();
			}
			if (count > 0 || !module.hideEmpty.get()) entries.add(new Entry(new ItemStack(Items.COOKED_BEEF), count));
		}
		return entries;
	}

	@Override
	protected void render(GuiGraphics g, float x, float y, float delta) {
		List<Entry> entries = entries();
		float scale = module.scale.getFloat();
		boolean vertical = module.vertical.get();
		float cellH = CELL + TEXT_H;
		if (entries.isEmpty()) {
			// Keep a size so the element can still be found and dragged in the HUD editor.
			width = CELL * scale;
			height = cellH * scale;
			return;
		}
		float w = vertical ? CELL : CELL * entries.size();
		float h = vertical ? cellH * entries.size() : cellH;
		width = w * scale;
		height = h * scale;

		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		for (int i = 0; i < entries.size(); i++) {
			Entry e = entries.get(i);
			float cx = vertical ? 0 : i * CELL;
			float cy = vertical ? i * cellH : 0;
			g.renderItem(e.icon(), Math.round(cx + 1), Math.round(cy + 1));
			String label = Integer.toString(e.count());
			float tw = OogaFonts.width(label, Weight.SEMIBOLD, 0.65f);
			OogaFonts.draw(g, label, cx + (CELL - tw) / 2f, cy + CELL, e.count() == 0 ? OogaTheme.TEXT_MUTED : OogaTheme.TEXT, Weight.SEMIBOLD, 0.65f);
		}
		g.pose().popMatrix();
	}
}
