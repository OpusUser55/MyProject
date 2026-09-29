package dev.ooga.client.module.impl.render;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.render.WorldOverlay;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.util.ColorUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Outlines entities through walls using Minecraft's own glowing-outline pass, recoloured to
 * Ooga gold. Reusing the vanilla pass keeps it cheap and compatible with shaders.
 */
public class EspModule extends Module {
	public final BooleanSetting players = add(new BooleanSetting("Players", "Outline other players.", true));
	public final BooleanSetting hostiles = add(new BooleanSetting("Hostiles", "Outline hostile mobs.", false));
	public final BooleanSetting items = add(new BooleanSetting("Items", "Outline dropped items.", false));
	public final BooleanSetting passive = add(new BooleanSetting("Passive Mobs", "Outline animals and other passive mobs.", false));
	public final BooleanSetting invisible = add(new BooleanSetting("Invisible", "Also outline invisible entities.", true));
	public final NumberSetting range = add(new NumberSetting("Range", "Only outline entities within this distance.", 128, 8, 256, 4, "m"));
	public final ModeSetting color = add(new ModeSetting("Color", "Accent colour, each entity's team colour, or white.", "Accent", "Accent", "Team", "White"));
	public final ModeSetting style = add(new ModeSetting("Style", "Glow: vanilla-style outline. Box: a see-through box around each entity.", "Glow", "Glow", "Box", "Both"));
	public final NumberSetting boxFill = add(new NumberSetting("Box Fill", "Opacity of the box's fill.", 0.12, 0.0, 0.5, 0.01)
			.visibleWhen(() -> !style.is("Glow")));

	public EspModule() {
		super("ESP", "See players and mobs through walls.", Category.RENDER);
		WorldOverlay.register(this::drawBoxes);
	}

	public boolean shouldOutline(Entity entity) {
		return !style.is("Box") && wanted(entity);
	}

	private boolean wanted(Entity entity) {
		if (!isEnabled() || entity == mc.player || mc.player == null) return false;
		if (!invisible.get() && entity.isInvisible()) return false;
		double r = range.get();
		if (entity.distanceToSqr(mc.player) > r * r) return false;
		if (entity instanceof Player) return players.get();
		if (entity instanceof Enemy) return hostiles.get();
		if (entity instanceof ItemEntity) return items.get();
		if (entity instanceof Mob) return passive.get();
		return false;
	}

	private void drawBoxes(WorldOverlay.Drawer drawer, float partialTick) {
		if (!isEnabled() || style.is("Glow") || mc.level == null) return;
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!wanted(entity)) continue;
			int rgb = color.is("Team") ? entity.getTeamColor() : color.is("White") ? 0xFFFFFF : OogaTheme.GOLD & 0xFFFFFF;
			if (entity instanceof Player && !color.is("Team") && entity.isInvisible()) rgb = 0xB57EDC;
			// Interpolate so boxes glide with the entity instead of stepping each tick.
			Vec3 now = entity.getPosition(partialTick);
			AABB box = entity.getBoundingBox().move(now.subtract(entity.position())).inflate(0.05);
			drawer.box(box, ColorUtil.withAlpha(rgb, Math.round(255 * boxFill.getFloat())), ColorUtil.withAlpha(rgb, 230));
		}
	}

	/** @return an RGB colour override, or -1 to keep vanilla's. */
	public int outlineColor(Entity entity) {
		if (!shouldOutline(entity) || color.is("Team")) return -1;
		return color.is("White") ? 0xFFFFFF : OogaTheme.GOLD & 0xFFFFFF;
	}
}
