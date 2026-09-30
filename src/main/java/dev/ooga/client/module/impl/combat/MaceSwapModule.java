package dev.ooga.client.module.impl.combat;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.module.impl.client.FriendsModule;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;

/** Swaps to your mace for the hit when you attack while falling, so you get the smash bonus. */
public class MaceSwapModule extends Module {
	public final NumberSetting minFall = add(new NumberSetting("Min Fall", "Only swap after falling at least this far (smash attacks need 1.5).", 1.5, 1.5, 10, 0.5, "m"));

	private final WeaponSwap swap = new WeaponSwap();

	public MaceSwapModule() {
		super("Mace Swap", "Hit with your mace whenever you attack mid-fall.", Category.COMBAT);
	}

	@Override
	protected void onDisable() {
		swap.cancel();
	}

	@Override
	public void onTick() {
		swap.tick();
	}

	/** Called right before an attack packet is sent. */
	public void beforeAttack(Entity target) {
		if (!isEnabled() || mc.player == null || FriendsModule.protects(target)) return;
		if (mc.player.onGround() || mc.player.fallDistance < minFall.get() || mc.player.getMainHandItem().is(Items.MACE)) return;
		Inventory inventory = mc.player.getInventory();
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			if (inventory.getItem(i).is(Items.MACE)) {
				swap.swapTo(i);
				return;
			}
		}
	}
}
