package dev.ooga.client.module.impl.render;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.render.WorldOverlay;
import dev.ooga.client.util.ColorUtil;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Shows where a thrown or shot item will land: ender pearls, snowballs, eggs, potions, XP
 * bottles, tridents, and bows or crossbows while drawn/charged. Simulates the same speed,
 * drag and gravity the game uses, tick by tick, until the path hits a block.
 */
public class TrajectoriesModule extends Module {
	private static final int COLOR = 0xF2C14E;
	private static final int MAX_STEPS = 300;

	public final BooleanSetting offhand = add(new BooleanSetting("Offhand", "Also predict for the item in your offhand.", true));
	public final BooleanSetting landingBox = add(new BooleanSetting("Landing Box", "Mark where the path ends.", true));

	/** Launch speed, gravity per tick and pitch offset for one kind of projectile. */
	private record Ballistics(double speed, double gravity, float pitchOffset) {
	}

	public TrajectoriesModule() {
		super("Trajectories", "Predicts where pearls, arrows and potions land.", Category.RENDER);
		WorldOverlay.register(this::draw);
	}

	private Ballistics ballistics(LocalPlayer player, ItemStack stack) {
		if (stack.is(Items.ENDER_PEARL) || stack.is(Items.SNOWBALL) || stack.is(Items.EGG)) return new Ballistics(1.5, 0.03, 0);
		if (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) return new Ballistics(0.5, 0.05, -20);
		if (stack.is(Items.EXPERIENCE_BOTTLE)) return new Ballistics(0.7, 0.07, -20);
		if (stack.is(Items.TRIDENT) && player.isUsingItem()) return new Ballistics(2.5, 0.05, 0);
		if (stack.is(Items.BOW) && player.isUsingItem()) {
			float power = bowPower(player.getTicksUsingItem());
			return power < 0.1f ? null : new Ballistics(power * 3.0, 0.05, 0);
		}
		if (stack.is(Items.CROSSBOW) && isCharged(stack)) return new Ballistics(3.15, 0.05, 0);
		return null;
	}

	/** Same curve as the bow: a quick ramp that tops out after one second of drawing. */
	private static float bowPower(int ticks) {
		float f = ticks / 20f;
		f = (f * f + f * 2f) / 3f;
		return Math.min(1f, f);
	}

	private static boolean isCharged(ItemStack crossbow) {
		var projectiles = crossbow.get(DataComponents.CHARGED_PROJECTILES);
		return projectiles != null && !projectiles.isEmpty();
	}

	private void draw(WorldOverlay.Drawer drawer, float partialTick) {
		LocalPlayer player = mc.player;
		if (!isEnabled() || player == null || mc.level == null) return;
		Ballistics b = ballistics(player, player.getMainHandItem());
		if (b == null && offhand.get()) b = ballistics(player, player.getOffhandItem());
		if (b == null) return;

		float yaw = player.getViewYRot(partialTick);
		float pitch = player.getViewXRot(partialTick);
		double yawRad = Math.toRadians(yaw);
		double pitchRad = Math.toRadians(pitch);
		double pitchLaunch = Math.toRadians(pitch + b.pitchOffset());
		Vec3 dir = new Vec3(-Math.sin(yawRad) * Math.cos(pitchRad), -Math.sin(pitchLaunch), Math.cos(yawRad) * Math.cos(pitchRad)).normalize();

		// Projectiles inherit the shooter's motion (vertical only while airborne).
		Vec3 motion = player.getDeltaMovement();
		Vec3 velocity = dir.scale(b.speed()).add(motion.x, player.onGround() ? 0 : motion.y, motion.z);
		Vec3 pos = player.getEyePosition(partialTick).subtract(0, 0.1, 0);

		int color = ColorUtil.withAlpha(COLOR, 220);
		for (int i = 0; i < MAX_STEPS; i++) {
			Vec3 next = pos.add(velocity);
			HitResult hit = mc.level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
			if (hit.getType() != HitResult.Type.MISS) {
				Vec3 end = hit.getLocation();
				// Skip the first bit of the path; it starts inside your view and just adds clutter.
				if (i > 0) drawer.line(pos, end, color);
				if (landingBox.get()) {
					drawer.box(new AABB(end.x - 0.25, end.y - 0.25, end.z - 0.25, end.x + 0.25, end.y + 0.25, end.z + 0.25),
							ColorUtil.withAlpha(COLOR, 60), ColorUtil.withAlpha(COLOR, 240));
				}
				return;
			}
			if (i > 0) drawer.line(pos, next, color);
			pos = next;
			velocity = velocity.scale(0.99).subtract(0, b.gravity(), 0);
			if (pos.y < mc.level.getMinY() - 64) return;
		}
	}
}
