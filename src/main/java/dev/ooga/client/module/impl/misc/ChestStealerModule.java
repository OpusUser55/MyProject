package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.impl.client.SafetyModule;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.util.Delay;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ShulkerBoxMenu;

/** Empties chests, barrels and shulkers into your inventory when you open them. */
public class ChestStealerModule extends Module {
	public final NumberSetting minDelay = add(new NumberSetting("Min Delay", "Fewest ticks between items.", 1, 0, 10, 1, "t"));
	public final NumberSetting maxDelay = add(new NumberSetting("Max Delay", "Most ticks between items.", 2, 0, 10, 1, "t"));
	public final BooleanSetting autoClose = add(new BooleanSetting("Auto Close", "Close the container once it's empty or you're full.", true));

	private final Delay delay = new Delay();

	public ChestStealerModule() {
		super("Chest Stealer", "Takes everything out of containers you open.", Category.MISC);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.gameMode == null || !(mc.screen instanceof AbstractContainerScreen<?> screen)) return;
		AbstractContainerMenu menu = screen.getMenu();
		int containerSlots;
		if (menu instanceof ChestMenu chest) containerSlots = chest.getRowCount() * 9;
		else if (menu instanceof ShulkerBoxMenu) containerSlots = 27;
		else return;
		if (!delay.tick()) return;
		for (int i = 0; i < containerSlots; i++) {
			if (!menu.slots.get(i).hasItem()) continue;
			mc.gameMode.handleInventoryMouseClick(menu.containerId, i, 0, ClickType.QUICK_MOVE, mc.player);
			int lo = (int) SafetyModule.atLeast(minDelay.getInt(), 2);
			delay.start(lo, Math.max(lo, maxDelay.getInt()));
			// Still there: our inventory is full.
			if (menu.slots.get(i).hasItem() && autoClose.get()) mc.player.closeContainer();
			return;
		}
		if (autoClose.get()) mc.player.closeContainer();
	}

	@Override
	public boolean isBlatant() {
		return true;
	}
}
