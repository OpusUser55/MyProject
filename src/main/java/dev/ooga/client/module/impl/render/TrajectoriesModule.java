package dev.ooga.client.module.impl.render;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.render.WorldOverlay;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.util.ColorUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Shows where the pearl, arrow, trident or potion in your hand will land. */
public class TrajectoriesModule extends Module {
	private record Throw(double speed, double gravity, float pitchOffset) {
	}

	public TrajectoriesModule() {
		super("Trajectories", "The flight path of pearls, arrows, tridents and potions before you throw.", Category.RENDER);
		WorldOverlay.register(this::draw);
	}

	private Throw physics(ItemStack stack) {
		if (stack.is(Items.ENDER_PEARL) || stack.is(Items.SNOWBALL) || stack.is(Items.EGG)) return new Throw(1.5, 0.03, 0f);
		if (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) return new Throw(0.5, 0.05, -20f);
		if (stack.is(Items.EXPERIENCE_BOTTLE)) return new Throw(0.7, 0.07, -20f);
		if (stack.is(Items.TRIDENT)) return new Throw(2.5, 0.05, 0f);
		if (stack.is(Items.CROSSBOW) && CrossbowItem.isCharged(stack)) return new Throw(3.15, 0.05, 0f);
		if (stack.is(Items.BOW)) {
			float power = mc.player.getUseItem() == stack ? BowItem.getPowerForTime(mc.player.getTicksUsingItem()) : 1f;
			return power < 0.1f ? null : new Throw(power * 3.0, 0.05, 0f);
		}
		return null;
	}

	private void draw(WorldOverlay.Drawer drawer, float partialTick) {
		if (!isEnabled() || mc.player == null || mc.level == null) return;
		ItemStack stack = mc.player.getMainHandItem();
		Throw t = physics(stack);
		if (t == null) {
			stack = mc.player.getOffhandItem();
			t = physics(stack);
		}
		if (t == null) return;
		Vec3 pos = mc.player.getEyePosition(partialTick).add(0, -0.1, 0);
		Vec3 velocity = Vec3.directionFromRotation(mc.player.getXRot() + t.pitchOffset(), mc.player.getYRot()).scale(t.speed());
		int color = OogaTheme.GOLD;
		for (int step = 0; step < 300; step++) {
			Vec3 next = pos.add(velocity);
			BlockHitResult hit = mc.level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
			if (hit.getType() != HitResult.Type.MISS) {
				drawer.line(pos, hit.getLocation(), ColorUtil.withAlpha(color, 230));
				Vec3 at = hit.getLocation();
				drawer.box(new AABB(at.x - 0.25, at.y - 0.25, at.z - 0.25, at.x + 0.25, at.y + 0.25, at.z + 0.25),
						ColorUtil.withAlpha(color, 60), ColorUtil.withAlpha(color, 240));
				return;
			}
			if (step > 0) drawer.line(pos, next, ColorUtil.withAlpha(color, 230));
			pos = next;
			velocity = velocity.scale(0.99).add(0, -t.gravity(), 0);
			if (pos.y < mc.level.getMinY() - 16) return;
		}
	}
}
