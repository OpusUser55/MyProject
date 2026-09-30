package dev.ooga.client.module.impl.combat;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.NumberSetting;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;

/** Keeps a Totem of Undying in your offhand, refilling it from your inventory after it pops. */
public class AutoTotemModule extends Module {
	/** The swap button number that means "the offhand" in inventory clicks. */
	private static final int OFFHAND_BUTTON = 40;

	public final NumberSetting delay = add(new NumberSetting("Delay", "Ticks to wait before refilling, to look less instant.", 1, 0, 10, 1, "t"));

	private int wait;

	public AutoTotemModule() {
		super("Auto Totem", "Puts a totem back in your offhand whenever it's empty.", Category.COMBAT);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.gameMode == null) return;
		// Only with the player's own inventory open (or no screen): other containers use other slot ids.
		if (mc.player.containerMenu != mc.player.inventoryMenu) return;
		if (mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
			wait = delay.getInt();
			return;
		}
		if (wait > 0) {
			wait--;
			return;
		}
		Inventory inventory = mc.player.getInventory();
		for (int i = 0; i < 36; i++) {
			if (!inventory.getItem(i).is(Items.TOTEM_OF_UNDYING)) continue;
			// Inventory index to menu slot: hotbar 0-8 sits at 36-44, the rest keep their index.
			int slot = i < 9 ? 36 + i : i;
			mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot, OFFHAND_BUTTON, ClickType.SWAP, mc.player);
			wait = delay.getInt();
			return;
		}
	}

	@Override
	public String getSuffix() {
		if (mc.player == null) return null;
		int count = 0;
		Inventory inventory = mc.player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i).is(Items.TOTEM_OF_UNDYING)) count += inventory.getItem(i).getCount();
		}
		return Integer.toString(count);
	}
}
