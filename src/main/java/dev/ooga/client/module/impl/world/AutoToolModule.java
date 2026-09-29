package dev.ooga.client.module.impl.world;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Switches to the fastest hotbar tool for the block you're mining, and back when you stop. */
public class AutoToolModule extends Module {
	public final BooleanSetting switchBack = add(new BooleanSetting("Switch Back", "Return to the slot you had once you stop mining.", true));
	public final BooleanSetting saveTools = add(new BooleanSetting("Save Tools", "Skip tools that are about to break.", true));
	public final NumberSetting minDurability = add(new NumberSetting("Min Durability", "Uses left below which a tool is skipped.", 10, 1, 100, 1)
			.visibleWhen(saveTools::get));

	/** The slot to go back to, or -1 when we haven't switched. */
	private int previousSlot = -1;

	public AutoToolModule() {
		super("Auto Tool", "Picks the best hotbar tool for whatever you mine.", Category.WORLD);
	}

	@Override
	protected void onDisable() {
		previousSlot = -1;
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.level == null || mc.gameMode == null || mc.screen != null) return;
		Inventory inventory = mc.player.getInventory();
		HitResult hit = mc.hitResult;
		boolean mining = mc.options.keyAttack.isDown() && hit instanceof BlockHitResult && hit.getType() == HitResult.Type.BLOCK;
		if (!mining) {
			if (switchBack.get() && previousSlot >= 0) inventory.setSelectedSlot(previousSlot);
			previousSlot = -1;
			return;
		}
		BlockState state = mc.level.getBlockState(((BlockHitResult) hit).getBlockPos());
		if (state.isAir()) return;
		int current = inventory.getSelectedSlot();
		int best = current;
		float bestSpeed = usable(inventory.getItem(current)) ? inventory.getItem(current).getDestroySpeed(state) : 0f;
		for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!usable(stack)) continue;
			float speed = stack.getDestroySpeed(state);
			if (speed > bestSpeed + 0.01f) {
				bestSpeed = speed;
				best = slot;
			}
		}
		if (best == current) return;
		if (previousSlot < 0) previousSlot = current;
		inventory.setSelectedSlot(best);
	}

	private boolean usable(ItemStack stack) {
		if (!saveTools.get() || !stack.isDamageableItem()) return true;
		return stack.getMaxDamage() - stack.getDamageValue() > minDurability.getInt();
	}
}
