package dev.ooga.client.module.impl.render;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.render.TracerOrigin;
import dev.ooga.client.render.WorldOverlay;
import dev.ooga.client.ui.hud.HudManager;
import dev.ooga.client.ui.hud.WaypointHud;
import dev.ooga.client.util.ColorUtil;
import dev.ooga.client.waypoint.Waypoint;
import dev.ooga.client.waypoint.WaypointStore;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Draws saved waypoints (see {@code .wp}) as a beam with an optional tracer, and lists the
 * nearest ones on the HUD. Overworld waypoints can be shown in the Nether at /8 and vice
 * versa, which makes portal linking trivial.
 */
public class WaypointsModule extends Module {
	private static final int COLOR = 0x5CC8FF;

	public final BooleanSetting tracers = add(new BooleanSetting("Tracers", "Lines from your view to each waypoint.", false));
	public final BooleanSetting labels = add(new BooleanSetting("Labels", "Name and distance floating above each waypoint.", true));
	public final NumberSetting labelScale = add(new NumberSetting("Label Scale", "Size of the floating labels.", 1.0, 0.5, 2.0, 0.05, "x")
			.visibleWhen(labels::get));
	public final BooleanSetting beams = add(new BooleanSetting("Beams", "Tall line above each waypoint so it's visible from afar.", true));
	public final BooleanSetting crossDimension = add(new BooleanSetting("Cross Dimension", "Show Overworld waypoints in the Nether at /8, and Nether ones in the Overworld at x8.", true));
	public final BooleanSetting hud = add(new BooleanSetting("HUD List", "List the nearest waypoints with their distance.", true));
	public final NumberSetting hudCount = add(new NumberSetting("HUD Count", "How many waypoints the HUD lists.", 5, 1, 15, 1)
			.visibleWhen(hud::get));
	public final NumberSetting scale = add(new NumberSetting("Scale", "HUD list size.", 1.0, 0.5, 2.0, 0.05, "x")
			.visibleWhen(hud::get));

	/** A waypoint as placed in the current dimension. */
	public record Shown(Waypoint waypoint, Vec3 pos, boolean converted) {
	}

	public WaypointsModule() {
		super("Waypoints", "Saved places, drawn in the world. Add them with .wp add <name>.", Category.RENDER);
		enableByDefault();
		HudManager.get().register(new WaypointHud(this));
		WorldOverlay.register(this::draw);
	}

	/** Waypoints visible in the current dimension, nearest first. */
	public List<Shown> visible() {
		List<Shown> result = new ArrayList<>();
		if (mc.player == null) return result;
		String here = WaypointStore.currentDimension();
		for (Waypoint w : WaypointStore.current()) {
			Vec3 center = Vec3.atBottomCenterOf(w.pos());
			if (w.dimension().equals(here)) {
				result.add(new Shown(w, center, false));
			} else if (crossDimension.get() && w.dimension().equals("overworld") && here.equals("nether")) {
				result.add(new Shown(w, new Vec3(center.x / 8, center.y, center.z / 8), true));
			} else if (crossDimension.get() && w.dimension().equals("nether") && here.equals("overworld")) {
				result.add(new Shown(w, new Vec3(center.x * 8, center.y, center.z * 8), true));
			}
		}
		Vec3 me = mc.player.position();
		result.sort(Comparator.comparingDouble(s -> s.pos().distanceToSqr(me)));
		return result;
	}

	private void draw(WorldOverlay.Drawer drawer, float partialTick) {
		if (!isEnabled() || mc.player == null) return;
		Vec3 origin = TracerOrigin.get(drawer, partialTick);
		for (Shown s : visible()) {
			int color = s.converted() ? 0xC58CFF : COLOR;
			Vec3 p = s.pos();
			AABB box = new AABB(p.x - 0.5, p.y, p.z - 0.5, p.x + 0.5, p.y + 1, p.z + 0.5);
			drawer.box(box, ColorUtil.withAlpha(color, 50), ColorUtil.withAlpha(color, 230));
			if (beams.get()) drawer.line(p.add(0, 1, 0), p.add(0, 96, 0), ColorUtil.withAlpha(color, 200));
			if (tracers.get()) drawer.line(origin, box.getCenter(), ColorUtil.withAlpha(color, 160));
		}
	}

	@Override
	public String getSuffix() {
		return Integer.toString(WaypointStore.current().size());
	}
}
