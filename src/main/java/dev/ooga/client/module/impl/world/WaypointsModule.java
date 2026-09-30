package dev.ooga.client.module.impl.world;

import dev.ooga.client.OogaClient;
import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.render.Projector;
import dev.ooga.client.render.WorldOverlay;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.notify.Notification;
import dev.ooga.client.ui.notify.NotificationManager;
import dev.ooga.client.ui.render.GlowRenderer;
import dev.ooga.client.ui.render.OogaFonts;
import dev.ooga.client.ui.render.OogaFonts.Weight;
import dev.ooga.client.ui.render.Render2D;
import dev.ooga.client.util.ChatUtil;
import dev.ooga.client.util.ColorUtil;
import dev.ooga.client.world.Waypoints;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Waypoints: glowing beams in the world and labels with distance on screen. Press the
 * keybind to drop one where you stand; your death point is saved automatically. Manage them
 * with {@code .wp} in chat.
 */
public class WaypointsModule extends Module {
	public final BooleanSetting beams = add(new BooleanSetting("Beams", "A light beam at each waypoint, visible from far away.", true));
	public final BooleanSetting labels = add(new BooleanSetting("Labels", "Name and distance on screen.", true));
	public final BooleanSetting deaths = add(new BooleanSetting("Death Waypoints", "Save a waypoint where you die.", true));
	public final NumberSetting maxDistance = add(new NumberSetting("Max Distance", "Hide waypoints further than this. 0 shows all.", 0, 0, 10000, 100, "m"));

	private boolean wasDead;

	public WaypointsModule() {
		super("Waypoints", "Beams and labels for saved places. Keybind drops one; .wp in chat manages them.", Category.WORLD);
		enableByDefault();
		hideFromList();
		WorldOverlay.register(this::drawWorld);
		HudElementRegistry.addFirst(Identifier.fromNamespaceAndPath(OogaClient.MOD_ID, "waypoints"), this::drawLabels);
	}

	@Override
	public void onKeybind() {
		if (mc.player == null) return;
		var w = Waypoints.add(Waypoints.nextName("Waypoint"), mc.player.blockPosition());
		NotificationManager.get().push("Waypoint added", w.name + " · " + w.x + ", " + w.y + ", " + w.z, Notification.Kind.INFO);
	}

	@Override
	public void onTick() {
		if (mc.player == null) return;
		boolean dead = mc.player.isDeadOrDying() || mc.screen instanceof DeathScreen;
		if (dead && !wasDead && deaths.get()) {
			var w = Waypoints.add("Death", mc.player.blockPosition());
			ChatUtil.info("Died at " + w.x + ", " + w.y + ", " + w.z + " (saved as waypoint \"Death\")");
		}
		wasDead = dead;
	}

	private boolean inRange(Waypoints.Waypoint w, Vec3 from) {
		double max = maxDistance.get();
		return max <= 0 || Vec3.atCenterOf(w.pos()).distanceTo(from) <= max;
	}

	private void drawWorld(WorldOverlay.Drawer drawer, float partialTick) {
		if (!isEnabled() || !beams.get() || mc.level == null || mc.player == null) return;
		Vec3 eye = mc.player.getEyePosition(partialTick);
		for (Waypoints.Waypoint w : Waypoints.here()) {
			if (!inRange(w, eye)) continue;
			double cx = w.x + 0.5, cz = w.z + 0.5;
			AABB beam = new AABB(cx - 0.12, mc.level.getMinY(), cz - 0.12, cx + 0.12, mc.level.getMaxY(), cz + 0.12);
			drawer.box(beam, ColorUtil.withAlpha(w.color, 70), ColorUtil.withAlpha(w.color, 150));
			drawer.box(new AABB(w.pos()), ColorUtil.withAlpha(w.color, 45), ColorUtil.withAlpha(w.color, 220));
		}
	}

	private void drawLabels(GuiGraphics g, DeltaTracker tracker) {
		if (!isEnabled() || !labels.get() || mc.player == null || mc.options.hideGui) return;
		Vec3 eye = mc.player.getEyePosition(tracker.getGameTimeDeltaPartialTick(false));
		for (Waypoints.Waypoint w : Waypoints.here()) {
			if (!inRange(w, eye)) continue;
			float[] p = Projector.toScreen(w.x + 0.5, w.y + 1.6, w.z + 0.5);
			if (p == null) continue;
			String dist = Math.round(Vec3.atCenterOf(w.pos()).distanceTo(eye)) + "m";
			float nameW = OogaFonts.width(w.name, Weight.SEMIBOLD, 0.85f);
			float distW = OogaFonts.width(dist, Weight.REGULAR, 0.75f);
			float bw = nameW + distW + 14f, bh = 11f;
			float x = p[0] - bw / 2f, y = p[1] - bh;
			GlowRenderer.glow(g, x, y, bw, bh, 3f, w.color, 0.5f, 5f);
			Render2D.roundRect(g, x, y, bw, bh, 3f, 0xD80E0F12);
			Render2D.circle(g, x + 5f, y + bh / 2f, 2f, w.color);
			OogaFonts.draw(g, w.name, x + 9f, y + 2.2f, OogaTheme.TEXT, Weight.SEMIBOLD, 0.85f);
			OogaFonts.draw(g, dist, x + 12f + nameW, y + 2.8f, OogaTheme.TEXT_SECONDARY, Weight.REGULAR, 0.75f);
		}
	}
}
