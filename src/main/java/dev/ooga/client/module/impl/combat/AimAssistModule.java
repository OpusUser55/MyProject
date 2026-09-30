package dev.ooga.client.module.impl.combat;

import dev.ooga.client.OogaClient;
import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.impl.client.FriendsModule;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Gently pulls your aim toward the nearest target in front of you. Turns a little each frame,
 * eased, so it feels like a steadier hand rather than a snap.
 */
public class AimAssistModule extends Module {
	public final BooleanSetting players = add(new BooleanSetting("Players", "Aim at players.", true));
	public final BooleanSetting hostiles = add(new BooleanSetting("Hostiles", "Aim at hostile mobs.", false));
	public final NumberSetting range = add(new NumberSetting("Range", "Maximum distance.", 5.0, 2.0, 8.0, 0.1, "m"));
	public final NumberSetting fov = add(new NumberSetting("FOV", "Only targets within this angle of your crosshair.", 60, 10, 180, 5, "°"));
	public final NumberSetting strength = add(new NumberSetting("Strength", "How strongly your aim is pulled.", 0.35, 0.05, 1.0, 0.05));
	public final BooleanSetting vertical = add(new BooleanSetting("Vertical", "Also adjust pitch, not just yaw.", true));
	public final BooleanSetting onlyHoldingClick = add(new BooleanSetting("Only While Holding", "Only assist while you hold the attack button.", true));

	private long lastFrame;

	public AimAssistModule() {
		super("Aim Assist", "Smoothly nudges your aim toward the nearest target.", Category.COMBAT);
		// Runs once per rendered frame (a HUD layer is the simplest per-frame hook) so the pull is smooth.
		HudElementRegistry.addFirst(Identifier.fromNamespaceAndPath(OogaClient.MOD_ID, "aim_assist"),
				(graphics, tracker) -> onFrame(tracker.getGameTimeDeltaPartialTick(true)));
	}

	/** Called every rendered frame from the HUD layer so the pull is smooth at any frame rate. */
	public void onFrame(float partialTick) {
		long now = System.nanoTime();
		float dt = lastFrame == 0 ? 0 : Math.min(0.05f, (now - lastFrame) / 1.0E9f);
		lastFrame = now;
		if (!isEnabled() || mc.player == null || mc.level == null || mc.screen != null || dt == 0) return;
		if (onlyHoldingClick.get() && !mc.options.keyAttack.isDown()) return;
		Entity target = findTarget(partialTick);
		if (target == null) return;

		Vec3 eye = mc.player.getEyePosition(partialTick);
		Vec3 aim = target.getPosition(partialTick).add(0, target.getBbHeight() * 0.6, 0);
		float[] wanted = rotationTo(eye, aim);
		// Exponential easing: frame-rate independent, never overshoots.
		float k = 1f - (float) Math.exp(-strength.get() * 12 * dt);
		float yaw = mc.player.getYRot();
		mc.player.setYRot(yaw + Mth.wrapDegrees(wanted[0] - yaw) * k);
		if (vertical.get()) {
			float pitch = mc.player.getXRot();
			mc.player.setXRot(Mth.clamp(pitch + (wanted[1] - pitch) * k, -90f, 90f));
		}
	}

	private Entity findTarget(float partialTick) {
		Vec3 eye = mc.player.getEyePosition(partialTick);
		Entity best = null;
		double bestAngle = fov.get() / 2;
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!wanted(entity) || entity.distanceTo(mc.player) > range.get()) continue;
			float[] rot = rotationTo(eye, entity.getPosition(partialTick).add(0, entity.getBbHeight() * 0.6, 0));
			double angle = Math.hypot(Mth.wrapDegrees(rot[0] - mc.player.getYRot()), rot[1] - mc.player.getXRot());
			if (angle < bestAngle) {
				bestAngle = angle;
				best = entity;
			}
		}
		return best;
	}

	private boolean wanted(Entity entity) {
		if (!(entity instanceof LivingEntity living) || !living.isAlive() || entity == mc.player || entity.isInvisible()) return false;
		if (entity instanceof Player) return players.get() && !FriendsModule.protects(entity);
		return entity instanceof Enemy && hostiles.get();
	}

	private static float[] rotationTo(Vec3 from, Vec3 to) {
		double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
		float pitch = (float) -(Mth.atan2(dy, horizontal) * Mth.RAD_TO_DEG);
		return new float[]{yaw, pitch};
	}
}
