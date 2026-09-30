package dev.ooga.client.module.impl.render;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.impl.client.FriendsModule;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.render.TracerOrigin;
import dev.ooga.client.render.WorldOverlay;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.util.ColorUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Lines from your view to players (and optionally mobs), fading with distance. */
public class TracersModule extends Module {
	public final BooleanSetting players = add(new BooleanSetting("Players", "Trace other players.", true));
	public final BooleanSetting hostiles = add(new BooleanSetting("Hostiles", "Trace hostile mobs.", false));
	public final ModeSetting color = add(new ModeSetting("Color", "Accent, or colour by distance (near = red, far = green).", "Accent", "Accent", "Distance"));
	public final NumberSetting range = add(new NumberSetting("Range", "Maximum distance.", 128, 16, 512, 8, "m"));
	public final NumberSetting opacity = add(new NumberSetting("Opacity", "Line opacity.", 0.75, 0.1, 1.0, 0.05));

	public TracersModule() {
		super("Tracers", "Lines pointing to players around you.", Category.RENDER);
		WorldOverlay.register(this::draw);
	}

	private void draw(WorldOverlay.Drawer drawer, float partialTick) {
		if (!isEnabled() || mc.player == null || mc.level == null) return;
		Vec3 origin = TracerOrigin.get(drawer, partialTick);
		double max = range.get();
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (entity == mc.player) continue;
			boolean wanted = (entity instanceof Player && players.get()) || (entity instanceof Enemy && hostiles.get());
			if (!wanted) continue;
			Vec3 pos = entity.getPosition(partialTick).add(0, entity.getBbHeight() / 2.0, 0);
			double distance = pos.distanceTo(drawer.camera());
			if (distance > max) continue;
			int rgb = color.is("Distance")
					? ColorUtil.lerp(0xFFE5484D, 0xFF46C37B, (float) Math.min(1, distance / 64.0)) & 0xFFFFFF
					: OogaTheme.GOLD & 0xFFFFFF;
			if (FriendsModule.highlights(entity)) rgb = FriendsModule.COLOR & 0xFFFFFF;
			float fade = (float) (1 - 0.6 * distance / max);
			drawer.line(origin, pos, ColorUtil.withAlpha(rgb, Math.round(255 * opacity.getFloat() * fade)));
		}
	}
}
