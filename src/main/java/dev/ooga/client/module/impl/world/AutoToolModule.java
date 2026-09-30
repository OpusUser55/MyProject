package dev.ooga.client.module.impl.world;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Picks the fastest hotbar tool for the block you're mining, and optionally switches back when
 * you stop. Tools about to break are skipped so you don't lose them.
 */
public class AutoToolModule extends Module {
	public final BooleanSetting switchBack = add(new BooleanSetting("Switch Back", "Return to the slot you had once you stop mining.", true));
	public final BooleanSetting saveTools = add(new BooleanSetting("Save Tools", "Never pick a tool that's about to break.", true));
	public final NumberSetting minDurability = add(new NumberSetting("Min Durability", "Uses left below which a tool is skipped.", 10, 1, 100, 1)
			.visibleWhen(saveTools::get));

	/** Slot to return to, or -1 when we haven't switched. */
	private int previousSlot = -1;

	public AutoToolModule() {
		super("Auto Tool", "Switches to the best tool while mining.", Category.WORLD);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.level == null) return;
		Inventory inventory = mc.player.getInventory();
		boolean mining = mc.screen == null && mc.options.keyAttack.isDown()
				&& mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK;
		if (!mining) {
			if (switchBack.get() && previousSlot != -1) inventory.setSelectedSlot(previousSlot);
			previousSlot = -1;
			return;
		}

		BlockPos pos = ((BlockHitResult) mc.hitResult).getBlockPos();
		BlockState state = mc.level.getBlockState(pos);
		if (state.isAir() || state.getDestroySpeed(mc.level, pos) < 0) return;

		int current = inventory.getSelectedSlot();
		int best = current;
		float bestScore = score(inventory.getItem(current), state);
		for (int i = 0; i < 9; i++) {
			float s = score(inventory.getItem(i), state);
			if (s > bestScore) {
				bestScore = s;
				best = i;
			}
		}
		if (best != current) {
			if (previousSlot == -1) previousSlot = current;
			inventory.setSelectedSlot(best);
		}
	}

	/** Mining speed, with a bonus for tools that actually get the drop. -1 for tools we won't use. */
	private float score(ItemStack stack, BlockState state) {
		if (saveTools.get() && stack.isDamageableItem()
				&& stack.getMaxDamage() - stack.getDamageValue() <= minDurability.getInt()) {
			return -1;
		}
		float speed = stack.getDestroySpeed(state);
		if (state.requiresCorrectToolForDrops() && !stack.isCorrectToolForDrops(state)) speed /= 4f;
		return speed;
	}

	@Override
	protected void onDisable() {
		if (switchBack.get() && previousSlot != -1 && mc.player != null) mc.player.getInventory().setSelectedSlot(previousSlot);
		previousSlot = -1;
	}
}
