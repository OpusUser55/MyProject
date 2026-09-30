package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.render.TracerOrigin;
import dev.ooga.client.render.WorldOverlay;
import dev.ooga.client.ui.notify.Notification;
import dev.ooga.client.ui.notify.NotificationManager;
import dev.ooga.client.util.ChatUtil;
import dev.ooga.client.util.ColorUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Remembers where you last died, tells you, and marks the spot so you can get your stuff back. */
public class DeathCoordsModule extends Module {
	private static final int COLOR = 0xE8594A;

	public final BooleanSetting chat = add(new BooleanSetting("Chat", "Post your death coordinates in chat (only you see it).", true));
	public final BooleanSetting waypoint = add(new BooleanSetting("Waypoint", "Mark the spot through walls until you get back to it.", true));
	public final BooleanSetting tracer = add(new BooleanSetting("Tracer", "Line from your view to the death spot.", true)
			.visibleWhen(waypoint::get));

	private boolean wasDead;
	private BlockPos deathPos;
	private ResourceKey<Level> deathDimension;

	public DeathCoordsModule() {
		super("Death Coords", "Remembers where you died and points you back.", Category.MISC);
		enableByDefault();
		WorldOverlay.register(this::draw);
	}

	@Override
	public void onTick() {
		if (!inWorld()) {
			wasDead = false;
			return;
		}
		boolean dead = mc.player.isDeadOrDying();
		if (dead && !wasDead) {
			deathPos = mc.player.blockPosition();
			deathDimension = mc.level.dimension();
			String where = deathPos.getX() + ", " + deathPos.getY() + ", " + deathPos.getZ() + " (" + dimensionName(deathDimension) + ")";
			if (chat.get()) ChatUtil.info("You died at " + where);
			NotificationManager.get().push("Death", where, Notification.Kind.INFO);
		}
		wasDead = dead;

		// Reached it: the waypoint has done its job.
		if (deathPos != null && !dead && mc.level.dimension() == deathDimension
				&& deathPos.distToCenterSqr(mc.player.position()) < 4 * 4) {
			deathPos = null;
		}
	}

	private static String dimensionName(ResourceKey<Level> dimension) {
		if (dimension == Level.NETHER) return "Nether";
		if (dimension == Level.END) return "End";
		if (dimension == Level.OVERWORLD) return "Overworld";
		return "other dimension";
	}

	private void draw(WorldOverlay.Drawer drawer, float partialTick) {
		if (!isEnabled() || !waypoint.get() || deathPos == null || mc.player == null || mc.level == null) return;
		if (mc.level.dimension() != deathDimension) return;
		AABB box = new AABB(deathPos).expandTowards(0, 1, 0);
		drawer.box(box, ColorUtil.withAlpha(COLOR, 45), ColorUtil.withAlpha(COLOR, 230));
		if (tracer.get()) {
			Vec3 origin = TracerOrigin.get(drawer, partialTick);
			drawer.line(origin, box.getCenter(), ColorUtil.withAlpha(COLOR, 180));
		}
	}
}
