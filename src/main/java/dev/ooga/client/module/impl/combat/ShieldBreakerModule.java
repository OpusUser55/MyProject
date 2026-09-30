package dev.ooga.client.module.impl.combat;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.impl.client.FriendsModule;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;

/** When you hit someone who's blocking with a shield, swings with an axe to disable it. */
public class ShieldBreakerModule extends Module {
	public final BooleanSetting switchBack = add(new BooleanSetting("Switch Back", "Go back to your weapon after the axe hit.", true));

	private final WeaponSwap swap = new WeaponSwap();

	public ShieldBreakerModule() {
		super("Shield Breaker", "Switches to an axe to break the shield of a blocking target.", Category.COMBAT);
	}

	@Override
	protected void onDisable() {
		swap.cancel();
	}

	@Override
	public void onTick() {
		if (switchBack.get()) swap.tick();
	}

	/** Called right before an attack packet is sent. */
	public void beforeAttack(Entity target) {
		if (!isEnabled() || mc.player == null || !(target instanceof LivingEntity living) || !living.isBlocking()) return;
		if (FriendsModule.protects(target) || mc.player.getMainHandItem().is(ItemTags.AXES)) return;
		Inventory inventory = mc.player.getInventory();
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			if (inventory.getItem(i).is(ItemTags.AXES)) {
				swap.swapTo(i);
				return;
			}
		}
	}
}
