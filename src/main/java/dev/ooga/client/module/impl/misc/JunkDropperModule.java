package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.util.Delay;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Set;

/** Throws out stone, dirt, netherrack and other mining junk so your inventory never fills up. */
public class JunkDropperModule extends Module {
	private static final Set<Item> JUNK = Set.of(Items.COBBLESTONE, Items.COBBLED_DEEPSLATE, Items.DIRT, Items.GRAVEL, Items.NETHERRACK,
			Items.ANDESITE, Items.DIORITE, Items.GRANITE, Items.TUFF, Items.CALCITE, Items.BASALT, Items.BLACKSTONE, Items.SOUL_SOIL,
			Items.ROTTEN_FLESH, Items.POISONOUS_POTATO);

	public final BooleanSetting keepHotbar = add(new BooleanSetting("Keep Hotbar", "Never drop from the hotbar (keep blocks for bridging).", true));
	public final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between drops.", 3, 1, 20, 1, "t"));

	private final Delay wait = new Delay();

	public JunkDropperModule() {
		super("Junk Dropper", "Drops stone, dirt, netherrack and other junk automatically.", Category.MISC);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.gameMode == null || mc.player.containerMenu != mc.player.inventoryMenu) return;
		if (!wait.tick()) return;
		Inventory inventory = mc.player.getInventory();
		for (int i = keepHotbar.get() ? 9 : 0; i < 36; i++) {
			if (!JUNK.contains(inventory.getItem(i).getItem())) continue;
			int slot = i < 9 ? 36 + i : i;
			mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot, 1, ClickType.THROW, mc.player);
			wait.start(delay.getInt(), delay.getInt());
			return;
		}
	}
}
