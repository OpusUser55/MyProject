package dev.ooga.client.module.impl.render;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.module.impl.combat.HitboxModule;
import dev.ooga.client.render.WorldOverlay;
import dev.ooga.client.util.ColorUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws entities' real hitboxes (like F3+B, but cleaner): the box, eye height and where they
 * look. With Hitbox on, the expanded pick area is drawn too.
 */
public class RealHitboxModule extends Module {
	public final BooleanSetting playersOnly = add(new BooleanSetting("Players Only", "Only draw players' hitboxes.", true));
	public final BooleanSetting look = add(new BooleanSetting("Look Direction", "Show eye height and where each entity is looking.", true));
	public final NumberSetting range = add(new NumberSetting("Range", "Maximum distance.", 32, 4, 128, 4, "m"));

	public RealHitboxModule() {
		super("Real Hitbox", "Shows the true hitboxes of players and mobs.", Category.RENDER);
		WorldOverlay.register(this::draw);
	}

	private void draw(WorldOverlay.Drawer drawer, float partialTick) {
		if (!isEnabled() || mc.level == null || mc.player == null) return;
		double maxSq = range.get() * range.get();
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (entity == mc.player || !(entity instanceof LivingEntity)) continue;
			if (playersOnly.get() && !(entity instanceof Player)) continue;
			if (entity.distanceToSqr(mc.player) > maxSq) continue;
			Vec3 now = entity.getPosition(partialTick);
			AABB box = entity.getBoundingBox().move(now.subtract(entity.position()));
			drawer.outline(box, 0xE6FFFFFF);
			float extra = HitboxModule.extra(entity);
			if (extra > 0) drawer.box(box.inflate(extra), ColorUtil.withAlpha(0xE5484D, 25), ColorUtil.withAlpha(0xE5484D, 160));
			if (!look.get()) continue;
			double eyeY = now.y + entity.getEyeHeight();
			drawer.line(box.minX, eyeY, box.minZ, box.maxX, eyeY, box.minZ, 0xE6E5484D);
			drawer.line(box.minX, eyeY, box.maxZ, box.maxX, eyeY, box.maxZ, 0xE6E5484D);
			drawer.line(box.minX, eyeY, box.minZ, box.minX, eyeY, box.maxZ, 0xE6E5484D);
			drawer.line(box.maxX, eyeY, box.minZ, box.maxX, eyeY, box.maxZ, 0xE6E5484D);
			Vec3 eye = new Vec3(now.x, eyeY, now.z);
			drawer.line(eye, eye.add(entity.getViewVector(partialTick).scale(2)), 0xE64F8FE8);
		}
	}
}
