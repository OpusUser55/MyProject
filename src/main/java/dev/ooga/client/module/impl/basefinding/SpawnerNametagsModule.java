package dev.ooga.client.module.impl.basefinding;

import dev.ooga.client.OogaClient;
import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.render.Projector;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.render.GlowRenderer;
import dev.ooga.client.ui.render.OogaFonts;
import dev.ooga.client.ui.render.OogaFonts.Weight;
import dev.ooga.client.ui.render.Render2D;
import dev.ooga.client.world.BlockEntityTracker;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/** A floating tag over every spawner: its mob and how far away it is, visible through walls. */
public class SpawnerNametagsModule extends Module {
	private static final int COLOR = 0xFFE8594A;

	public final NumberSetting range = add(new NumberSetting("Range", "Maximum distance.", 64, 8, 256, 8, "m"));
	public final NumberSetting scale = add(new NumberSetting("Scale", "Tag size.", 1.0, 0.5, 2.0, 0.05, "x"));

	/** Mob names are looked up once per spawner; making the display entity isn't free. */
	private final Map<Long, String> names = new HashMap<>();

	public SpawnerNametagsModule() {
		super("Spawner Nametags", "Labels spawners with their mob and distance.", Category.BASEFINDING);
		HudElementRegistry.addFirst(Identifier.fromNamespaceAndPath(OogaClient.MOD_ID, "spawner_tags"), this::render);
	}

	@Override
	protected void onDisable() {
		names.clear();
	}

	private void render(GuiGraphics g, DeltaTracker tracker) {
		if (!isEnabled() || mc.player == null || mc.level == null || mc.options.hideGui) return;
		Vec3 eye = mc.player.getEyePosition(tracker.getGameTimeDeltaPartialTick(false));
		double maxSq = range.get() * range.get();
		float s = scale.getFloat();
		for (BlockEntity be : BlockEntityTracker.all()) {
			if (!(be instanceof SpawnerBlockEntity) || be.isRemoved()) continue;
			var pos = be.getBlockPos();
			double distSq = pos.distToCenterSqr(eye);
			if (distSq > maxSq) continue;
			float[] p = Projector.toScreen(pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5);
			if (p == null) continue;
			String mob = names.computeIfAbsent(pos.asLong(), k -> SpawnerFinderModule.mobOf(be));
			String dist = Math.round(Math.sqrt(distSq)) + "m";
			float mw = OogaFonts.width(mob, Weight.SEMIBOLD, 0.85f), dw = OogaFonts.width(dist, Weight.REGULAR, 0.75f);
			float w = mw + dw + 16f, h = 11f;
			g.pose().pushMatrix();
			g.pose().translate(p[0], p[1]);
			g.pose().scale(s, s);
			float x = -w / 2f, y = -h;
			GlowRenderer.glow(g, x, y, w, h, 3f, COLOR, 0.55f, 4f);
			Render2D.roundRect(g, x, y, w, h, 3f, 0xD80E0F12);
			Render2D.circle(g, x + 5f, y + h / 2f, 2f, COLOR);
			OogaFonts.draw(g, mob, x + 10f, y + 2.2f, OogaTheme.TEXT, Weight.SEMIBOLD, 0.85f);
			OogaFonts.draw(g, dist, x + 13f + mw, y + 2.8f, OogaTheme.TEXT_SECONDARY, Weight.REGULAR, 0.75f);
			g.pose().popMatrix();
		}
	}
}
