package dev.ooga.client.module.impl.render;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.render.WorldOverlay;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.util.ColorUtil;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;

/** A trail behind you, so you can see where you've already searched (or find your way back). */
public class BreadcrumbsModule extends Module {
	public final NumberSetting length = add(new NumberSetting("Length", "How many points the trail keeps.", 1000, 100, 5000, 100));
	public final NumberSetting spacing = add(new NumberSetting("Spacing", "Distance between points.", 1.0, 0.25, 5.0, 0.25, "m"));

	private final Deque<Vec3> trail = new ArrayDeque<>();
	private Object level;

	public BreadcrumbsModule() {
		super("Breadcrumbs", "Draws a trail where you've been.", Category.RENDER);
		WorldOverlay.register(this::draw);
	}

	@Override
	protected void onDisable() {
		trail.clear();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.level == null) return;
		if (mc.level != level) {
			level = mc.level;
			trail.clear();
		}
		Vec3 feet = mc.player.position().add(0, 0.1, 0);
		if (trail.isEmpty() || trail.peekLast().distanceTo(feet) >= spacing.get()) trail.addLast(feet);
		while (trail.size() > length.getInt()) trail.removeFirst();
	}

	private void draw(WorldOverlay.Drawer drawer, float partialTick) {
		if (!isEnabled() || trail.size() < 2) return;
		int i = 0, n = trail.size();
		Vec3 prev = null;
		for (Vec3 p : trail) {
			if (prev != null && prev.distanceToSqr(p) < 100) {
				float t = i / (float) n;
				drawer.line(prev, p, ColorUtil.withAlpha(OogaTheme.gradient(t), Math.round(60 + 180 * t)));
			}
			prev = p;
			i++;
		}
	}
}
