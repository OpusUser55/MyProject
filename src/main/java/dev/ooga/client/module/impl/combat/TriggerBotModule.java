package dev.ooga.client.module.impl.combat;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.impl.client.FriendsModule;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.concurrent.ThreadLocalRandom;

/** Attacks whatever your crosshair is on as soon as your weapon has recharged. */
public class TriggerBotModule extends Module {
	public final BooleanSetting players = add(new BooleanSetting("Players", "Attack players.", true));
	public final BooleanSetting hostiles = add(new BooleanSetting("Hostiles", "Attack hostile mobs.", true));
	public final BooleanSetting passive = add(new BooleanSetting("Passive", "Attack animals and other mobs.", false));
	public final NumberSetting charge = add(new NumberSetting("Charge", "How recharged the attack must be (1.0 = full damage).", 1.0, 0.5, 1.0, 0.05));
	public final NumberSetting randomDelay = add(new NumberSetting("Random Delay", "Up to this many extra ticks before each hit, so timing isn't robotic.", 1, 0, 5, 1, "t"));
	public final BooleanSetting onlyHoldingClick = add(new BooleanSetting("Only While Holding", "Only attack while you hold the attack button.", false));

	private int wait;

	public TriggerBotModule() {
		super("Trigger Bot", "Hits the entity under your crosshair the moment your attack recharges.", Category.COMBAT);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.gameMode == null || mc.screen != null || mc.player.isUsingItem()) return;
		if (onlyHoldingClick.get() && !mc.options.keyAttack.isDown()) return;
		HitResult hit = mc.hitResult;
		if (!(hit instanceof EntityHitResult entityHit) || hit.getType() != HitResult.Type.ENTITY) return;
		Entity target = entityHit.getEntity();
		if (!wanted(target)) return;
		if (mc.player.getAttackStrengthScale(0.5f) < charge.getFloat()) return;
		if (wait > 0) {
			wait--;
			return;
		}
		mc.gameMode.attack(mc.player, target);
		mc.player.swing(InteractionHand.MAIN_HAND);
		wait = randomDelay.getInt() == 0 ? 0 : ThreadLocalRandom.current().nextInt(randomDelay.getInt() + 1);
	}

	private boolean wanted(Entity entity) {
		if (!(entity instanceof LivingEntity living) || !living.isAlive() || entity == mc.player) return false;
		if (entity instanceof Player) return players.get() && !FriendsModule.protects(entity);
		if (entity instanceof Enemy) return hostiles.get();
		return passive.get();
	}
}
