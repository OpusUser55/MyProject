package dev.ooga.client.module.impl.world;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.NumberSetting;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

/**
 * Keeps hotbar stacks topped up from your inventory while you build: when a stack drops below
 * a threshold (or runs out), the same item is moved in from the main inventory.
 */
public class ReplenishModule extends Module {
	public final NumberSetting threshold = add(new NumberSetting("Threshold", "Refill a stack when it drops below this many items.", 8, 1, 63, 1));
	public final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between refills (20 = 1s).", 3, 1, 20, 1));

	/** What each hotbar slot last held, so a stack that ran out completely can still be refilled. */
	private final ItemStack[] remembered = new ItemStack[9];
	private int cooldown;

	public ReplenishModule() {
		super("Replenish", "Refills hotbar stacks from your inventory.", Category.WORLD);
		Arrays.fill(remembered, ItemStack.EMPTY);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.gameMode == null || mc.screen != null) return;
		if (cooldown > 0) {
			cooldown--;
			return;
		}
		Inventory inventory = mc.player.getInventory();
		// Never refill while something is on the cursor; a click would drop or swap it.
		if (!mc.player.inventoryMenu.getCarried().isEmpty()) return;

		for (int hotbar = 0; hotbar < 9; hotbar++) {
			ItemStack stack = inventory.getItem(hotbar);
			ItemStack wanted = stack.isEmpty() ? remembered[hotbar] : stack;
			if (!stack.isEmpty()) remembered[hotbar] = stack.copy();
			if (wanted.isEmpty() || !wanted.isStackable()) continue;
			if (!stack.isEmpty() && stack.getCount() >= Math.min(threshold.getInt(), stack.getMaxStackSize())) continue;

			int source = findSource(inventory, wanted);
			if (source == -1) {
				if (stack.isEmpty()) remembered[hotbar] = ItemStack.EMPTY;
				continue;
			}
			refill(source, hotbar);
			cooldown = delay.getInt();
			return;
		}
	}

	private static int findSource(Inventory inventory, ItemStack wanted) {
		for (int i = 9; i < 36; i++) {
			if (ItemStack.isSameItemSameComponents(inventory.getItem(i), wanted)) return i;
		}
		return -1;
	}

	/** Pick up the inventory stack, drop it onto the hotbar slot (merging), and put any rest back. */
	private void refill(int source, int hotbar) {
		int container = mc.player.inventoryMenu.containerId;
		int hotbarSlot = 36 + hotbar;
		mc.gameMode.handleInventoryMouseClick(container, source, 0, ClickType.PICKUP, mc.player);
		mc.gameMode.handleInventoryMouseClick(container, hotbarSlot, 0, ClickType.PICKUP, mc.player);
		if (!mc.player.inventoryMenu.getCarried().isEmpty()) {
			mc.gameMode.handleInventoryMouseClick(container, source, 0, ClickType.PICKUP, mc.player);
		}
	}

	@Override
	protected void onDisable() {
		Arrays.fill(remembered, ItemStack.EMPTY);
	}
}
