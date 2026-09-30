package dev.ooga.client.module.impl.combat;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.util.Delay;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;

/** Puts on the best armour in your inventory, one piece at a time with small random delays. */
public class AutoArmorModule extends Module {
	/** Menu slots of head, chest, legs and feet in the player's inventory menu. */
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final int[] MENU_SLOT = {5, 6, 7, 8};
	private static final String[] TIERS = {"leather", "golden", "chainmail", "copper", "iron", "turtle", "diamond", "netherite"};

	public final BooleanSetting keepElytra = add(new BooleanSetting("Keep Elytra", "Never swap a worn elytra for a chestplate.", true));
	public final NumberSetting minDelay = add(new NumberSetting("Min Delay", "Fewest ticks between clicks.", 2, 0, 10, 1, "t"));
	public final NumberSetting maxDelay = add(new NumberSetting("Max Delay", "Most ticks between clicks.", 4, 0, 10, 1, "t"));

	private final Delay delay = new Delay();

	public AutoArmorModule() {
		super("Auto Armor", "Equips the best armour you have.", Category.COMBAT);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.gameMode == null || mc.player.containerMenu != mc.player.inventoryMenu) return;
		if (!delay.tick()) return;
		Inventory inventory = mc.player.getInventory();
		int id = mc.player.inventoryMenu.containerId;
		for (int s = 0; s < SLOTS.length; s++) {
			ItemStack worn = mc.player.getItemBySlot(SLOTS[s]);
			if (keepElytra.get() && worn.is(Items.ELYTRA)) continue;
			int bestIndex = -1;
			int bestScore = score(worn, SLOTS[s]);
			for (int i = 0; i < 36; i++) {
				int sc = score(inventory.getItem(i), SLOTS[s]);
				if (sc > bestScore) {
					bestScore = sc;
					bestIndex = i;
				}
			}
			if (bestIndex < 0) continue;
			if (!worn.isEmpty()) {
				// Take the old piece off first; the new one goes on next time round.
				mc.gameMode.handleInventoryMouseClick(id, MENU_SLOT[s], 0, ClickType.QUICK_MOVE, mc.player);
			} else {
				int menuSlot = bestIndex < 9 ? 36 + bestIndex : bestIndex;
				mc.gameMode.handleInventoryMouseClick(id, menuSlot, 0, ClickType.QUICK_MOVE, mc.player);
			}
			delay.start(minDelay.getInt(), Math.max(minDelay.getInt(), maxDelay.getInt()));
			return;
		}
	}

	/** Higher is better; -1 if it can't go in this slot. Material tier first, enchantments break ties. */
	private static int score(ItemStack stack, EquipmentSlot slot) {
		if (stack.isEmpty()) return -1;
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		if (equippable == null || equippable.slot() != slot || stack.is(Items.ELYTRA) || stack.is(Items.CARVED_PUMPKIN)) return -1;
		String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
		int tier = 0;
		for (int i = 0; i < TIERS.length; i++) if (path.startsWith(TIERS[i] + "_")) tier = i + 1;
		return tier * 100 + stack.getEnchantments().size() * 5;
	}
}
