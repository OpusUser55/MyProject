package dev.ooga.client.module.impl.basefinding;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.render.TracerOrigin;
import dev.ooga.client.render.WorldOverlay;
import dev.ooga.client.ui.notify.Notification;
import dev.ooga.client.ui.notify.NotificationManager;
import dev.ooga.client.util.ChatUtil;
import dev.ooga.client.util.ColorUtil;
import dev.ooga.client.world.BlockEntityTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Finds mob spawners as their chunks load. Each one is announced once per session with its
 * coordinates, and highlighted through walls while in range.
 */
public class SpawnerFinderModule extends Module {
	private static final int COLOR = 0xE8594A;

	public final BooleanSetting trial = add(new BooleanSetting("Trial Spawners", "Include trial spawners from trial chambers.", false));
	public final BooleanSetting chat = add(new BooleanSetting("Chat Alerts", "Post each new spawner's coordinates in chat.", true));
	public final BooleanSetting toast = add(new BooleanSetting("Notifications", "Show a notification for each new spawner.", true));
	public final BooleanSetting tracers = add(new BooleanSetting("Tracers", "Lines from your view to each spawner.", true));
	public final NumberSetting range = add(new NumberSetting("Range", "Maximum highlight distance.", 256, 16, 512, 8, "m"));

	private final Set<BlockPos> announced = new HashSet<>();
	private int visible;

	public SpawnerFinderModule() {
		super("Spawner Finder", "Alerts and highlights mob spawners in loaded chunks.", Category.BASEFINDING);
		WorldOverlay.register(this::draw);
	}

	@Override
	protected void onEnable() {
		announced.clear();
	}

	private boolean matches(BlockEntity be) {
		return be instanceof SpawnerBlockEntity || (trial.get() && be instanceof TrialSpawnerBlockEntity);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.level == null) return;
		for (BlockEntity be : BlockEntityTracker.all()) {
			if (!matches(be) || be.isRemoved()) continue;
			BlockPos pos = be.getBlockPos().immutable();
			if (!announced.add(pos)) continue;
			int distance = (int) Math.sqrt(pos.distToCenterSqr(mc.player.position()));
			String where = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
			if (chat.get()) ChatUtil.info("Spawner at " + where + " (" + distance + "m)");
			if (toast.get()) NotificationManager.get().push("Spawner found", where, Notification.Kind.INFO);
		}
	}

	private void draw(WorldOverlay.Drawer drawer, float partialTick) {
		if (!isEnabled() || mc.player == null) return;
		double maxSq = range.get() * range.get();
		Vec3 eye = mc.player.getEyePosition(partialTick);
		Vec3 origin = TracerOrigin.get(drawer, partialTick);
		int count = 0;
		for (BlockEntity be : BlockEntityTracker.all()) {
			if (!matches(be) || be.isRemoved()) continue;
			BlockPos pos = be.getBlockPos();
			if (pos.distToCenterSqr(eye) > maxSq) continue;
			AABB box = new AABB(pos);
			drawer.box(box, ColorUtil.withAlpha(COLOR, 50), ColorUtil.withAlpha(COLOR, 230));
			if (tracers.get()) drawer.line(origin, box.getCenter(), ColorUtil.withAlpha(COLOR, 170));
			count++;
		}
		visible = count;
	}

	@Override
	public String getSuffix() {
		return Integer.toString(visible);
	}
}
