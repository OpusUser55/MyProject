package dev.ooga.client.module.impl.render;

import dev.ooga.client.OogaClient;
import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.render.Projector;
import dev.ooga.client.render.WorldOverlay;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.render.GlowRenderer;
import dev.ooga.client.ui.render.OogaFonts;
import dev.ooga.client.ui.render.OogaFonts.Weight;
import dev.ooga.client.ui.render.Render2D;
import dev.ooga.client.util.ChatUtil;
import dev.ooga.client.util.ColorUtil;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Marks where players logged out while you could see them, with their name, health and the
 * time. Combat loggers and people hiding in bases leave a trace.
 */
public class LogoutSpotsModule extends Module {
	private static LogoutSpotsModule instance;
	private static final int COLOR = 0xE5484D;

	public final BooleanSetting chat = add(new BooleanSetting("Chat", "Post logouts in chat with coordinates.", true));

	private record Spot(String name, AABB box, float health, long time) {
	}

	private final Map<UUID, Spot> spots = new LinkedHashMap<>();

	public LogoutSpotsModule() {
		super("Logout Spots", "Marks where players logged out near you.", Category.RENDER);
		instance = this;
		WorldOverlay.register(this::draw);
		HudElementRegistry.addFirst(Identifier.fromNamespaceAndPath(OogaClient.MOD_ID, "logout_spots"), this::labels);
	}

	/** Called (on the client thread) with players about to leave the tab list. */
	public static void onRemove(List<UUID> ids) {
		LogoutSpotsModule self = instance;
		if (self == null || !self.isEnabled() || mc.level == null) return;
		for (UUID id : ids) {
			for (Entity entity : mc.level.entitiesForRendering()) {
				if (!(entity instanceof Player player) || player == mc.player || !player.getUUID().equals(id)) continue;
				String name = player.getGameProfile().name();
				self.spots.put(id, new Spot(name, player.getBoundingBox(), player.getHealth(), System.currentTimeMillis()));
				if (self.chat.get()) ChatUtil.info(name + " logged out at " + player.getBlockX() + ", " + player.getBlockY() + ", " + player.getBlockZ());
			}
		}
	}

	@Override
	public void onTick() {
		if (mc.level == null) {
			spots.clear();
			return;
		}
		// Forget a spot as soon as that player shows up again.
		for (Iterator<UUID> it = spots.keySet().iterator(); it.hasNext(); ) {
			UUID id = it.next();
			for (Entity e : mc.level.entitiesForRendering()) {
				if (e.getUUID().equals(id)) {
					it.remove();
					break;
				}
			}
		}
	}

	@Override
	protected void onDisable() {
		spots.clear();
	}

	private void draw(WorldOverlay.Drawer drawer, float partialTick) {
		if (!isEnabled()) return;
		for (Spot s : spots.values()) drawer.box(s.box(), ColorUtil.withAlpha(COLOR, 40), ColorUtil.withAlpha(COLOR, 220));
	}

	private void labels(GuiGraphics g, DeltaTracker tracker) {
		if (!isEnabled() || mc.player == null || mc.options.hideGui) return;
		for (Spot s : spots.values()) {
			var c = s.box().getCenter();
			float[] p = Projector.toScreen(c.x, s.box().maxY + 0.4, c.z);
			if (p == null) continue;
			long minutes = (System.currentTimeMillis() - s.time()) / 60000;
			String text = s.name() + "  " + Math.round(s.health()) + "♥  " + (minutes == 0 ? "now" : minutes + "m ago");
			float w = OogaFonts.width(text, Weight.SEMIBOLD, 0.85f) + 10f, h = 11f;
			float x = p[0] - w / 2f, y = p[1] - h;
			GlowRenderer.glow(g, x, y, w, h, 3f, 0xFF000000 | COLOR, 0.5f, 4f);
			Render2D.roundRect(g, x, y, w, h, 3f, 0xD80E0F12);
			OogaFonts.draw(g, text, x + 5f, y + 2.2f, OogaTheme.TEXT, Weight.SEMIBOLD, 0.85f);
		}
	}
}
