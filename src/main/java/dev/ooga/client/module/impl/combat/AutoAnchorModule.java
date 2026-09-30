package dev.ooga.client.module.impl.combat;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.impl.client.SafetyModule;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.util.Delay;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.HashSet;
import java.util.Set;

/**
 * Anchor macro driven by your crosshair: look at a respawn anchor while holding right click and
 * it charges it with glowstone, switches to your detonate slot and blows it, then (optionally)
 * picks the next anchor back up. Each step waits a random delay and can be given a chance to
 * skip, so the rhythm isn't machine-perfect.
 *
 * <p>Anchors only explode outside the Nether; in the Nether using one just sets your spawn.
 */
public class AutoAnchorModule extends Module {
	private enum Step { READY, CHARGE, DETONATE }

	public final BooleanSetting holdUse = add(new BooleanSetting("Hold Right Click", "Only work while you hold the use button.", true));
	public final NumberSetting charges = add(new NumberSetting("Charges", "Glowstone to put in before detonating. One is enough to explode.", 1, 1, 4, 1));
	public final NumberSetting detonateSlot = add(new NumberSetting("Detonate Slot", "Hotbar slot (1-9) held to detonate. Must not be glowstone; a totem is ideal.", 9, 1, 9, 1));
	public final BooleanSetting switchBack = add(new BooleanSetting("Switch Back", "Pick your anchors back up after detonating, ready for the next one.", true));
	public final BooleanSetting onlyOwn = add(new BooleanSetting("Only Own", "Only touch anchors you placed yourself this session.", false));
	public final BooleanSetting onlyCharge = add(new BooleanSetting("Only Charge", "Charge anchors but never detonate them.", false));
	public final NumberSetting minDelay = add(new NumberSetting("Min Delay", "Fewest ticks between steps (switch, charge, detonate).", 1, 0, 10, 1, "t"));
	public final NumberSetting maxDelay = add(new NumberSetting("Max Delay", "Most ticks between steps.", 2, 0, 10, 1, "t"));
	public final NumberSetting skipChance = add(new NumberSetting("Skip Chance", "Percent chance to hesitate a tick at each step.", 10, 0, 50, 5, "%"));
	public final BooleanSetting lootProtect = add(new BooleanSetting("Loot Protect", "Don't detonate while a player died nearby or items lie on the ground close by.", false));

	private final Delay delay = new Delay();
	private final DeathWatch deaths = new DeathWatch();
	private final Set<BlockPos> placed = new HashSet<>();
	private Step step = Step.READY;
	private BlockPos lastAimedAir;

	public AutoAnchorModule() {
		super("Auto Anchor", "Charges and detonates respawn anchors you look at.", Category.COMBAT);
	}

	@Override
	protected void onDisable() {
		step = Step.READY;
		placed.clear();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.level == null || mc.gameMode == null || mc.screen != null) return;
		trackOwnPlacements();
		if (!delay.tick()) return;
		if (holdUse.get() && !mc.options.keyUse.isDown()) return;
		if (!(mc.hitResult instanceof BlockHitResult hit) || mc.hitResult.getType() != HitResult.Type.BLOCK) {
			step = Step.READY;
			return;
		}
		BlockPos pos = hit.getBlockPos();
		BlockState state = mc.level.getBlockState(pos);
		if (!state.is(Blocks.RESPAWN_ANCHOR)) {
			step = Step.READY;
			return;
		}
		if (onlyOwn.get() && !placed.contains(pos)) return;
		if (!Delay.chance(100 - skipChance.get())) return;

		int charge = state.getValue(RespawnAnchorBlock.CHARGE);
		Inventory inventory = mc.player.getInventory();
		if (charge < charges.getInt()) {
			// Charge: hold glowstone and use it on the anchor.
			if (!mc.player.getMainHandItem().is(Items.GLOWSTONE)) {
				int slot = hotbarSlot(Items.GLOWSTONE);
				if (slot < 0) return;
				inventory.setSelectedSlot(slot);
				step = Step.CHARGE;
				pause();
				return;
			}
			use(hit);
			pause();
			return;
		}
		if (onlyCharge.get()) return;
		if (lootProtect.get() && (deaths.paused(true, 10, 3) || lootNearby(pos))) return;

		// Detonate: hold something that isn't glowstone and use the charged anchor.
		int slot = detonateSlot.getInt() - 1;
		if (inventory.getItem(slot).is(Items.GLOWSTONE)) return;
		if (inventory.getSelectedSlot() != slot) {
			inventory.setSelectedSlot(slot);
			step = Step.DETONATE;
			pause();
			return;
		}
		use(hit);
		placed.remove(pos);
		step = Step.READY;
		if (switchBack.get()) {
			int anchors = hotbarSlot(Items.RESPAWN_ANCHOR);
			if (anchors >= 0) inventory.setSelectedSlot(anchors);
		}
		pause();
	}

	/** Remembers anchors that appear where you were aiming with an anchor in hand. */
	private void trackOwnPlacements() {
		if (!onlyOwn.get()) return;
		if (lastAimedAir != null && mc.level.getBlockState(lastAimedAir).is(Blocks.RESPAWN_ANCHOR)) {
			placed.add(lastAimedAir);
			lastAimedAir = null;
		}
		if (mc.player.getMainHandItem().is(Items.RESPAWN_ANCHOR) && mc.hitResult instanceof BlockHitResult hit
				&& mc.hitResult.getType() == HitResult.Type.BLOCK) {
			lastAimedAir = hit.getBlockPos().relative(hit.getDirection());
		}
	}

	private void use(BlockHitResult hit) {
		mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
		mc.player.swing(InteractionHand.MAIN_HAND);
	}

	private void pause() {
		delay.start(minDelay.getInt(), Math.max(minDelay.getInt(), maxDelay.getInt()));
	}

	private boolean lootNearby(BlockPos pos) {
		AABB area = new AABB(pos).inflate(6);
		for (Entity entity : mc.level.getEntities((Entity) null, area, e -> e instanceof ItemEntity)) {
			if (!entity.isRemoved()) return true;
		}
		return false;
	}

	private int hotbarSlot(Item item) {
		Inventory inventory = mc.player.getInventory();
		for (int i = 0; i < Inventory.getSelectionSize(); i++) if (inventory.getItem(i).is(item)) return i;
		return -1;
	}

	@Override
	public String getSuffix() {
		return step == Step.READY ? null : step.name().charAt(0) + step.name().substring(1).toLowerCase();
	}

	@Override
	public boolean isBlatant() {
		return true;
	}
}
