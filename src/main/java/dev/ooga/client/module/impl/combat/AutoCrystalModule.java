package dev.ooga.client.module.impl.combat;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.util.Delay;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Crystal macro driven by your crosshair: hold right click with end crystals and it places a
 * crystal on the obsidian you're aiming at, then breaks any crystal you're looking at, at a
 * randomised click speed. Optionally puts obsidian down first when you aim at something else.
 */
public class AutoCrystalModule extends Module {
	public final BooleanSetting holdUse = add(new BooleanSetting("Hold Right Click", "Only work while you hold the use button.", true));
	public final NumberSetting placeDelay = add(new NumberSetting("Place Delay", "Ticks between crystal placements.", 1, 0, 10, 1, "t"));
	public final NumberSetting minCps = add(new NumberSetting("Min CPS", "Slowest break speed, in clicks per second.", 10, 1, 20, 1));
	public final NumberSetting maxCps = add(new NumberSetting("Max CPS", "Fastest break speed, in clicks per second.", 14, 1, 20, 1));
	public final BooleanSetting placeObsidian = add(new BooleanSetting("Place Obsidian", "Put obsidian down first when you aim at a block that isn't obsidian or bedrock.", false));
	public final NumberSetting switchDelay = add(new NumberSetting("Switch Delay", "Ticks to wait after switching to obsidian before placing it.", 1, 0, 10, 1, "t")
			.visibleWhen(placeObsidian::get));
	public final BooleanSetting stopOnKill = add(new BooleanSetting("Stop On Kill", "Pause after a player dies nearby, so their loot isn't blown up.", true));
	public final NumberSetting stopTime = add(new NumberSetting("Stop Time", "Seconds to stay paused.", 3, 0.5, 10, 0.5, "s")
			.visibleWhen(stopOnKill::get));

	private final Delay place = new Delay();
	private final Delay attack = new Delay();
	private final Delay obsidian = new Delay();
	private final DeathWatch deaths = new DeathWatch();
	/** Slot to return to after placing obsidian, or -1. */
	private int returnSlot = -1;
	/** When obsidian was last placed; it's never placed more than once a second. */
	private long lastObsidian;

	public AutoCrystalModule() {
		super("Auto Crystal", "Places and breaks end crystals where you aim, at human-like speed.", Category.COMBAT);
	}

	@Override
	protected void onDisable() {
		returnSlot = -1;
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.level == null || mc.gameMode == null || mc.screen != null) return;
		place.tick();
		attack.tick();
		if (deaths.paused(stopOnKill.get(), 12, stopTime.get())) return;
		if (holdUse.get() && !mc.options.keyUse.isDown()) return;
		HitResult hit = mc.hitResult;

		// Break: a crystal under the crosshair.
		if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof EndCrystal crystal) {
			if (attack.done()) {
				mc.gameMode.attack(mc.player, crystal);
				mc.player.swing(InteractionHand.MAIN_HAND);
				startAttackDelay();
			}
			return;
		}
		if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return;
		BlockPos pos = blockHit.getBlockPos();
		BlockState state = mc.level.getBlockState(pos);
		boolean base = state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK);

		// Mid obsidian placement: after the switch delay, place it and go back to crystals.
		if (returnSlot >= 0) {
			if (!obsidian.tick()) return;
			if (!base && mc.player.getMainHandItem().is(Items.OBSIDIAN)) {
				mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHit);
				mc.player.swing(InteractionHand.MAIN_HAND);
				lastObsidian = System.currentTimeMillis();
			}
			mc.player.getInventory().setSelectedSlot(returnSlot);
			returnSlot = -1;
			place.start(placeDelay.getInt(), placeDelay.getInt());
			return;
		}
		if (!mc.player.getMainHandItem().is(Items.END_CRYSTAL)) return;

		if (!base) {
			if (!placeObsidian.get() || System.currentTimeMillis() - lastObsidian < 1000) return;
			int slot = hotbarSlot(Items.OBSIDIAN);
			if (slot < 0) return;
			returnSlot = mc.player.getInventory().getSelectedSlot();
			mc.player.getInventory().setSelectedSlot(slot);
			obsidian.start(switchDelay.getInt(), switchDelay.getInt());
			return;
		}

		// Place: obsidian with room for a crystal on top.
		if (!place.done()) return;
		BlockPos above = pos.above();
		if (!mc.level.isEmptyBlock(above)) return;
		AABB space = new AABB(above).expandTowards(0, 1, 0);
		if (!mc.level.getEntities((Entity) null, space, e -> !e.isSpectator()).isEmpty()) return;
		mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHit);
		mc.player.swing(InteractionHand.MAIN_HAND);
		place.start(placeDelay.getInt(), placeDelay.getInt());
	}

	private void startAttackDelay() {
		int lo = (int) Math.round(Math.min(minCps.get(), maxCps.get()));
		int hi = (int) Math.round(Math.max(minCps.get(), maxCps.get()));
		// Clicks per second to ticks between clicks (20 ticks per second).
		int slow = Math.max(1, Math.round(20f / lo));
		int fast = Math.max(1, Math.round(20f / hi));
		attack.start(fast, slow);
	}

	private int hotbarSlot(Item item) {
		Inventory inventory = mc.player.getInventory();
		for (int i = 0; i < Inventory.getSelectionSize(); i++) if (inventory.getItem(i).is(item)) return i;
		return -1;
	}
}
