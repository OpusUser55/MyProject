package dev.ooga.client.module.impl.combat;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.ui.notify.Notification;
import dev.ooga.client.ui.notify.NotificationManager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * One key swaps between your elytra and your best chestplate. Bind it in the menu
 * (middle-click the row); it has no on/off state of its own.
 */
public class ElytraSwapModule extends Module {
	/** Menu slot of the chest armour in the player's own inventory menu. */
	private static final int CHEST_SLOT = 6;
	private static final List<Item> CHESTPLATES = List.of(Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE,
			Items.IRON_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.COPPER_CHESTPLATE, Items.LEATHER_CHESTPLATE);

	public ElytraSwapModule() {
		super("Elytra Swap", "Swap between elytra and chestplate with one key.", Category.COMBAT);
		hideFromList();
	}

	@Override
	protected boolean canEnable() {
		return false;
	}

	@Override
	public void onKeybind() {
		if (mc.player == null || mc.gameMode == null || mc.player.containerMenu != mc.player.inventoryMenu) return;
		boolean wearingElytra = mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
		int index = -1;
		if (wearingElytra) {
			for (Item chestplate : CHESTPLATES) {
				index = find(chestplate);
				if (index >= 0) break;
			}
		} else {
			index = find(Items.ELYTRA);
		}
		if (index < 0) {
			NotificationManager.get().push(getName(), wearingElytra ? "No chestplate to swap to" : "No elytra in inventory", Notification.Kind.INFO);
			return;
		}
		int containerId = mc.player.inventoryMenu.containerId;
		if (index < 9) {
			// Hotbar: one swap click between the armour slot and that hotbar key.
			mc.gameMode.handleInventoryMouseClick(containerId, CHEST_SLOT, index, ClickType.SWAP, mc.player);
		} else {
			// Main inventory: pick up, drop into the chest slot, put the old piece back.
			mc.gameMode.handleInventoryMouseClick(containerId, index, 0, ClickType.PICKUP, mc.player);
			mc.gameMode.handleInventoryMouseClick(containerId, CHEST_SLOT, 0, ClickType.PICKUP, mc.player);
			mc.gameMode.handleInventoryMouseClick(containerId, index, 0, ClickType.PICKUP, mc.player);
		}
	}

	private int find(Item item) {
		Inventory inventory = mc.player.getInventory();
		for (int i = 0; i < 36; i++) if (inventory.getItem(i).is(item)) return i;
		return -1;
	}
}
