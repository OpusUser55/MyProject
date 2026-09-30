package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.impl.client.FriendsModule;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.util.Delay;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Clicks for you at a randomised rate while you hold the attack button. */
public class AutoClickerModule extends Module {
	public final NumberSetting minCps = add(new NumberSetting("Min CPS", "Slowest clicks per second.", 9, 1, 20, 1));
	public final NumberSetting maxCps = add(new NumberSetting("Max CPS", "Fastest clicks per second.", 12, 1, 20, 1));
	public final BooleanSetting onlyEntities = add(new BooleanSetting("Only Entities", "Only click when your crosshair is on an entity (mining is left alone).", true));
	public final BooleanSetting waitCooldown = add(new BooleanSetting("Respect Cooldown", "Wait for your weapon to recharge instead of spamming weak hits.", false));

	private final Delay delay = new Delay();

	public AutoClickerModule() {
		super("Auto Clicker", "Clicks at a random CPS while you hold attack.", Category.MISC);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.gameMode == null || mc.screen != null || !mc.options.keyAttack.isDown()) return;
		if (!delay.tick()) return;
		HitResult hit = mc.hitResult;
		boolean onEntity = hit instanceof EntityHitResult && hit.getType() == HitResult.Type.ENTITY;
		if (onlyEntities.get() && !onEntity) return;
		if (waitCooldown.get() && mc.player.getAttackStrengthScale(0.5f) < 1f) return;
		if (onEntity) {
			var target = ((EntityHitResult) hit).getEntity();
			if (FriendsModule.protects(target)) return;
			mc.gameMode.attack(mc.player, target);
		}
		mc.player.swing(InteractionHand.MAIN_HAND);
		int lo = (int) Math.min(minCps.get(), maxCps.get()), hi = (int) Math.max(minCps.get(), maxCps.get());
		delay.start(Math.max(1, Math.round(20f / hi)), Math.max(1, Math.round(20f / lo)));
	}
}
