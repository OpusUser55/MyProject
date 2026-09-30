package dev.ooga.client.module.impl.combat;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.util.Delay;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;

/**
 * Keeps a Totem of Undying in your offhand.
 *
 * <ul>
 *   <li><b>Inventory</b> (default) does it the way a player would: opens your inventory,
 *   waits a moment, swaps the totem into the offhand, waits again and closes it. Every wait is
 *   a random number of ticks in the range you set.</li>
 *   <li><b>Instant</b> swaps it in without opening anything, as fast as you allow.</li>
 * </ul>
 *
 * With <b>Hover Refill</b>, it also refills whenever you open your inventory yourself.
 */
public class AutoTotemModule extends Module {
	/** The swap button number that means "the offhand" in inventory clicks. */
	private static final int OFFHAND_BUTTON = 40;

	private enum Step { IDLE, OPENING, SWAPPING, CLOSING }

	public final ModeSetting mode = add(new ModeSetting("Mode", "Inventory: open, swap, close like a player. Instant: swap without opening anything.", "Inventory", "Inventory", "Instant"));
	public final NumberSetting openMin = add(new NumberSetting("Open Delay Min", "Fewest ticks from the pop until the inventory opens.", 1, 0, 10, 1, "t")
			.visibleWhen(() -> mode.is("Inventory")));
	public final NumberSetting openMax = add(new NumberSetting("Open Delay Max", "Most ticks from the pop until the inventory opens.", 3, 0, 10, 1, "t")
			.visibleWhen(() -> mode.is("Inventory")));
	public final NumberSetting swapMin = add(new NumberSetting("Swap Delay Min", "Fewest ticks with the inventory open before the swap (and before closing).", 1, 0, 10, 1, "t"));
	public final NumberSetting swapMax = add(new NumberSetting("Swap Delay Max", "Most ticks with the inventory open before the swap (and before closing).", 3, 0, 10, 1, "t"));
	public final BooleanSetting hover = add(new BooleanSetting("Hover Refill", "Also refill while you have your own inventory open.", true));
	public final BooleanSetting fromHotbar = add(new BooleanSetting("Use Hotbar", "Allow taking totems from the hotbar too, not just the inventory.", true));

	private Step step = Step.IDLE;
	private final Delay delay = new Delay();
	/** Whether the open inventory is one we opened (and so should close). */
	private boolean openedByUs;

	public AutoTotemModule() {
		super("Auto Totem", "Puts a totem back in your offhand, the way a player would.", Category.COMBAT);
	}

	@Override
	protected void onDisable() {
		if (openedByUs && mc.player != null && mc.screen instanceof InventoryScreen) mc.player.closeContainer();
		reset();
	}

	private void reset() {
		step = Step.IDLE;
		openedByUs = false;
		delay.clear();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.gameMode == null) {
			reset();
			return;
		}
		boolean needs = !mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING) && totemSlot() >= 0;
		if (mode.is("Instant")) {
			tickInstant(needs);
			return;
		}
		switch (step) {
			case IDLE -> {
				if (!needs) return;
				// The player opened their own inventory: refill right there.
				if (mc.screen instanceof InventoryScreen) {
					if (!hover.get()) return;
					openedByUs = false;
					step = Step.SWAPPING;
					delay.start(swapMin.getInt(), swapMax.getInt());
				} else if (mc.screen == null) {
					step = Step.OPENING;
					delay.start(openMin.getInt(), openMax.getInt());
				}
			}
			case OPENING -> {
				if (!delay.tick()) return;
				if (mc.screen != null) {
					// Something else is open (chat, a chest…); try again once it's closed.
					reset();
					return;
				}
				mc.setScreen(new InventoryScreen(mc.player));
				openedByUs = true;
				step = Step.SWAPPING;
				delay.start(swapMin.getInt(), swapMax.getInt());
			}
			case SWAPPING -> {
				if (!(mc.screen instanceof InventoryScreen)) {
					reset();
					return;
				}
				if (!delay.tick()) return;
				if (needs && mc.player.inventoryMenu.getCarried().isEmpty()) swap(totemSlot());
				if (openedByUs) {
					step = Step.CLOSING;
					delay.start(swapMin.getInt(), swapMax.getInt());
				} else {
					reset();
				}
			}
			case CLOSING -> {
				if (!delay.tick()) return;
				if (mc.screen instanceof InventoryScreen) mc.player.closeContainer();
				reset();
			}
		}
	}

	private void tickInstant(boolean needs) {
		if (!needs || mc.player.containerMenu != mc.player.inventoryMenu) {
			delay.start(swapMin.getInt(), swapMax.getInt());
			return;
		}
		if (!delay.tick()) return;
		swap(totemSlot());
		delay.start(swapMin.getInt(), swapMax.getInt());
	}

	/** Inventory index of a totem to use, or -1. Main inventory first, so the hotbar stays intact. */
	private int totemSlot() {
		Inventory inventory = mc.player.getInventory();
		for (int i = 9; i < 36; i++) if (inventory.getItem(i).is(Items.TOTEM_OF_UNDYING)) return i;
		if (fromHotbar.get()) {
			for (int i = 0; i < 9; i++) if (inventory.getItem(i).is(Items.TOTEM_OF_UNDYING)) return i;
		}
		return -1;
	}

	private void swap(int index) {
		if (index < 0) return;
		// Inventory index to menu slot: hotbar 0-8 sits at 36-44, the rest keep their index.
		int slot = index < 9 ? 36 + index : index;
		mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot, OFFHAND_BUTTON, ClickType.SWAP, mc.player);
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
