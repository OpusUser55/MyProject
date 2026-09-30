package dev.ooga.client.module.impl.client;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.hud.HudManager;
import dev.ooga.client.ui.hud.TargetHud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

public class TargetHudModule extends Module {
	public final NumberSetting memory = add(new NumberSetting("Memory", "How long the last entity you hit stays shown.", 5, 1, 20, 1, "s"));
	public final NumberSetting scale = add(new NumberSetting("Scale", "Panel size.", 1.0, 0.5, 2.0, 0.05, "x"));

	public TargetHudModule() {
		super("Target HUD", "Health, distance and gear of whoever you're fighting.", Category.HUD);
		hideFromList();
		HudManager.get().register(new TargetHud(this));
	}

	/** The entity you last hit (for a few seconds), else whatever living thing you're looking at. */
	public LivingEntity target() {
		if (mc.player == null) return null;
		LivingEntity hit = mc.player.getLastHurtMob();
		if (hit != null && hit.isAlive() && !hit.isRemoved()
				&& mc.player.tickCount - mc.player.getLastHurtMobTimestamp() <= memory.getInt() * 20) return hit;
		if (mc.hitResult instanceof EntityHitResult entityHit) {
			Entity entity = entityHit.getEntity();
			if (entity instanceof LivingEntity living && living.isAlive()) return living;
		}
		return null;
	}
}
