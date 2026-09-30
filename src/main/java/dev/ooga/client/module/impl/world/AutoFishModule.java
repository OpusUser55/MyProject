package dev.ooga.client.module.impl.world;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.util.Keys;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;

/**
 * Reels in when a fish bites and casts again. A bite shows up client-side as the bobber being
 * yanked under after it has settled on the water, which is what this watches for.
 */
public class AutoFishModule extends Module {
	public final NumberSetting recastDelay = add(new NumberSetting("Recast Delay", "Ticks to wait before casting again (20 = 1s).", 15, 5, 60, 1));
	public final NumberSetting sensitivity = add(new NumberSetting("Sensitivity", "How hard the bobber must dip to count as a bite.", 0.05, 0.02, 0.2, 0.01));

	private int settledTicks;
	private int waitTicks;

	public AutoFishModule() {
		super("Auto Fish", "Catches and recasts on its own. Hold a fishing rod.", Category.WORLD);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.screen != null || !mc.player.getMainHandItem().is(Items.FISHING_ROD)) {
			settledTicks = 0;
			return;
		}
		if (waitTicks > 0) {
			if (--waitTicks == 0 && mc.player.fishing == null) click();
			return;
		}
		FishingHook hook = mc.player.fishing;
		if (hook == null) {
			settledTicks = 0;
			return;
		}
		if (!hook.isInWater()) {
			settledTicks = 0;
			return;
		}
		double dy = hook.getDeltaMovement().y;
		// Give the bobber a second to stop bouncing after it lands.
		if (settledTicks < 20) {
			settledTicks++;
			return;
		}
		if (dy < -sensitivity.get()) {
			click();
			settledTicks = 0;
			waitTicks = recastDelay.getInt();
		}
	}

	/** One right-click: reels in, or casts when nothing is out. */
	private void click() {
		Keys.click(mc.options.keyUse);
	}

	@Override
	protected void onDisable() {
		waitTicks = 0;
		settledTicks = 0;
	}
}
