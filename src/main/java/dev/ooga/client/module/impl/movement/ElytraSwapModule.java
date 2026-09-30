package dev.ooga.client.module.impl.movement;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.notify.Notification;
import dev.ooga.client.ui.notify.NotificationManager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Swaps a fresh elytra from your inventory onto your back before the worn one breaks, so long
 * flights don't end in a fall.
 */
public class ElytraSwapModule extends Module {
	/** Chestplate slot in the player's inventory menu. */
	private static final int CHEST_SLOT = 6;

	public final NumberSetting threshold = add(new NumberSetting("Swap At", "Swap when the worn elytra has this many uses left.", 20, 2, 100, 1));

	private int cooldown;

	public ElytraSwapModule() {
		super("Elytra Swap", "Replaces your elytra before it breaks.", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		if (cooldown > 0) {
			cooldown--;
			return;
		}
		// Only with no screen open (or our own inventory), so we never click into a chest menu.
		if (mc.player == null || mc.gameMode == null || mc.screen != null) return;
		ItemStack worn = mc.player.getItemBySlot(EquipmentSlot.CHEST);
		if (!worn.is(Items.ELYTRA) || usesLeft(worn) > threshold.getInt()) return;

		int best = -1;
		int bestUses = threshold.getInt();
		for (int i = 0; i < 36; i++) {
			ItemStack stack = mc.player.getInventory().getItem(i);
			if (stack.is(Items.ELYTRA) && usesLeft(stack) > bestUses) {
				best = i;
				bestUses = usesLeft(stack);
			}
		}
		if (best == -1) return;

		// Inventory-menu slot ids: hotbar 0-8 sits at 36-44, the main inventory keeps its index.
		int slotId = best < 9 ? best + 36 : best;
		int container = mc.player.inventoryMenu.containerId;
		mc.gameMode.handleInventoryMouseClick(container, slotId, 0, ClickType.PICKUP, mc.player);
		mc.gameMode.handleInventoryMouseClick(container, CHEST_SLOT, 0, ClickType.PICKUP, mc.player);
		mc.gameMode.handleInventoryMouseClick(container, slotId, 0, ClickType.PICKUP, mc.player);
		NotificationManager.get().push("Elytra Swap", "Put on a fresh elytra (" + bestUses + " uses)", Notification.Kind.INFO);
		cooldown = 20;
	}

	private static int usesLeft(ItemStack stack) {
		return stack.getMaxDamage() - stack.getDamageValue();
	}
}
