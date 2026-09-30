package dev.ooga.client.module.impl.combat;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.ui.notify.Notification;
import dev.ooga.client.ui.notify.NotificationManager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * One key flips your offhand between a totem and your chosen item (crystals, golden apples or a
 * shield). Bind it by middle-clicking the row; it has no on/off state.
 */
public class OffhandSwapModule extends Module {
	public final ModeSetting other = add(new ModeSetting("Other Item", "What to swap to when holding a totem.", "Crystal", "Crystal", "Golden Apple", "Shield"));

	public OffhandSwapModule() {
		super("Offhand Swap", "One key swaps your offhand between totem and crystals, gaps or shield.", Category.COMBAT);
		hideFromList();
	}

	@Override
	protected boolean canEnable() {
		return false;
	}

	private Item otherItem() {
		return switch (other.get()) {
			case "Golden Apple" -> Items.GOLDEN_APPLE;
			case "Shield" -> Items.SHIELD;
			default -> Items.END_CRYSTAL;
		};
	}

	@Override
	public void onKeybind() {
		if (mc.player == null || mc.gameMode == null || mc.player.containerMenu != mc.player.inventoryMenu) return;
		Item want = mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING) ? otherItem() : Items.TOTEM_OF_UNDYING;
		Inventory inventory = mc.player.getInventory();
		for (int i = 0; i < 36; i++) {
			if (!inventory.getItem(i).is(want)) continue;
			int slot = i < 9 ? 36 + i : i;
			mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot, 40, ClickType.SWAP, mc.player);
			return;
		}
		NotificationManager.get().push(getName(), "No " + want.getName().getString() + " in inventory", Notification.Kind.INFO);
	}
}
