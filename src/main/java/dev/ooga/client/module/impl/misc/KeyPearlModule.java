package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.ui.notify.Notification;
import dev.ooga.client.ui.notify.NotificationManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Throws an ender pearl from your hotbar the moment you press the keybind, then switches back.
 * Bind it in the menu (middle-click the row). The module has no on/off state of its own.
 */
public class KeyPearlModule extends Module {
	public final BooleanSetting switchBack = add(new BooleanSetting("Switch Back", "Return to the slot you had after throwing.", true));
	public final BooleanSetting windCharge = add(new BooleanSetting("Wind Charge", "Throw a wind charge instead of a pearl.", false));

	public KeyPearlModule() {
		super("Key Pearl", "Throw a pearl (or wind charge) from anywhere in your hotbar with one key.", Category.MISC);
		hideFromList();
	}

	@Override
	public void onKeybind() {
		if (mc.player == null || mc.gameMode == null || mc.screen != null) return;
		Item wanted = windCharge.get() ? Items.WIND_CHARGE : Items.ENDER_PEARL;
		Inventory inventory = mc.player.getInventory();
		int slot = -1;
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			if (inventory.getItem(i).is(wanted)) {
				slot = i;
				break;
			}
		}
		if (slot < 0) {
			NotificationManager.get().push(getName(), "No " + (windCharge.get() ? "wind charge" : "ender pearl") + " in hotbar", Notification.Kind.INFO);
			return;
		}
		if (mc.player.getCooldowns().isOnCooldown(inventory.getItem(slot))) return;
		int previous = inventory.getSelectedSlot();
		inventory.setSelectedSlot(slot);
		mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
		if (switchBack.get()) inventory.setSelectedSlot(previous);
	}

	@Override
	protected boolean canEnable() {
		// A trigger, not a toggle: pressing the key throws; there's nothing to keep on.
		return false;
	}
}
