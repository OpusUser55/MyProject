package dev.ooga.client.module.impl.combat;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Eats a golden apple from your hotbar when your health drops, then switches back. */
public class AutoGapModule extends Module {
	public final NumberSetting health = add(new NumberSetting("Health", "Eat at or below this much health.", 10, 2, 19, 1));
	public final BooleanSetting preferEnchanted = add(new BooleanSetting("Prefer Enchanted", "Use enchanted golden apples first.", false));

	private int previousSlot = -1;
	private boolean eating;

	public AutoGapModule() {
		super("Auto Gap", "Eats golden apples when you're low.", Category.COMBAT);
	}

	@Override
	protected void onDisable() {
		stop();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.screen != null) {
			stop();
			return;
		}
		boolean low = mc.player.getHealth() <= health.getInt();
		if (eating) {
			boolean holding = mc.player.getMainHandItem().is(Items.GOLDEN_APPLE) || mc.player.getMainHandItem().is(Items.ENCHANTED_GOLDEN_APPLE);
			if (!low || !holding) stop();
			else mc.options.keyUse.setDown(true);
			return;
		}
		if (!low || mc.player.isUsingItem()) return;
		int slot = find(preferEnchanted.get() ? Items.ENCHANTED_GOLDEN_APPLE : Items.GOLDEN_APPLE);
		if (slot < 0) slot = find(preferEnchanted.get() ? Items.GOLDEN_APPLE : Items.ENCHANTED_GOLDEN_APPLE);
		if (slot < 0) return;
		previousSlot = mc.player.getInventory().getSelectedSlot();
		mc.player.getInventory().setSelectedSlot(slot);
		mc.options.keyUse.setDown(true);
		eating = true;
	}

	private int find(Item item) {
		Inventory inventory = mc.player.getInventory();
		for (int i = 0; i < Inventory.getSelectionSize(); i++) if (inventory.getItem(i).is(item)) return i;
		return -1;
	}

	private void stop() {
		if (!eating) return;
		eating = false;
		mc.options.keyUse.setDown(false);
		if (mc.player != null && previousSlot >= 0) mc.player.getInventory().setSelectedSlot(previousSlot);
		previousSlot = -1;
	}
}
