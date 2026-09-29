package dev.ooga.client.ui.hud;

import dev.ooga.client.module.impl.client.InfoHudModule;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.render.OogaFonts;
import dev.ooga.client.ui.render.OogaFonts.Weight;
import dev.ooga.client.ui.render.Render2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * A compact stack of labelled readouts. Labels are small muted caps and values are bright,
 * so the eye lands on numbers; nothing here is gold because nothing here is a state.
 */
public class InfoHud extends HudElement {
	private static final String[] COMPASS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
	private static final float LINE = 11f;
	private static final float PAD = 5f;
	private static final float LABEL_SCALE = 0.62f;

	private final InfoHudModule module;

	public InfoHud(InfoHudModule module) {
		super("info", "Info HUD", Anchor.START, 0f, Anchor.START, 0.1f);
		this.module = module;
	}

	@Override
	public boolean isVisible() {
		return module.isEnabled() && Minecraft.getInstance().player != null;
	}

	@Override
	public NumberSetting scaleSetting() {
		return module.scale;
	}

	private record Line(String label, String value, String extra) {
	}

	private List<Line> lines() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		List<Line> lines = new ArrayList<>();
		if (module.fps.get()) lines.add(new Line("FPS", Integer.toString(mc.getFps()), null));
		if (module.coords.get() && player != null) {
			String xyz = String.format("%.0f  %.0f  %.0f", player.getX(), player.getY(), player.getZ());
			String extra = null;
			if (module.otherDimension.get() && mc.level != null) {
				if (mc.level.dimension() == Level.NETHER) {
					extra = String.format("%.0f  %.0f", player.getX() * 8, player.getZ() * 8);
				} else if (mc.level.dimension() == Level.OVERWORLD) {
					extra = String.format("%.0f  %.0f", player.getX() / 8, player.getZ() / 8);
				}
			}
			lines.add(new Line("XYZ", xyz, extra));
		}
		if (module.facing.get() && player != null) {
			float yaw = ((player.getYRot() % 360) + 360) % 360;
			int index = Math.round(yaw / 45f) % 8;
			lines.add(new Line("DIR", COMPASS[index], String.format("%.0f°", yaw)));
		}
		if (module.speed.get()) lines.add(new Line("SPD", String.format("%.1f b/s", module.blocksPerSecond()), null));
		if (module.ping.get() && player != null && mc.getConnection() != null) {
			PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
			if (info != null) lines.add(new Line("PING", info.getLatency() + " ms", null));
		}
		return lines;
	}

	@Override
	protected void render(GuiGraphics g, float x, float y, float delta) {
		List<Line> lines = lines();
		float scale = module.scale.getFloat();
		if (lines.isEmpty()) {
			width = height = 0;
			return;
		}

		float labelW = 0;
		for (Line line : lines) labelW = Math.max(labelW, OogaFonts.width(line.label(), Weight.SEMIBOLD, LABEL_SCALE));
		float valueX = PAD + labelW + 6f;
		float w = 0;
		for (Line line : lines) {
			float lw = valueX + OogaFonts.width(line.value(), Weight.REGULAR);
			if (line.extra() != null) lw += 6f + OogaFonts.width(line.extra(), Weight.REGULAR, 0.8f);
			w = Math.max(w, lw);
		}
		w += PAD;
		float h = lines.size() * LINE + PAD * 2 - 2f;
		width = w * scale;
		height = h * scale;

		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		Render2D.roundRect(g, 0, 0, w, h, OogaTheme.RADIUS_CARD, 0xD90E0F12);
		Render2D.outline(g, 0, 0, w, h, OogaTheme.RADIUS_CARD, OogaTheme.BORDER);

		float ly = PAD;
		for (Line line : lines) {
			OogaFonts.draw(g, line.label(), PAD, ly + 1.8f, OogaTheme.TEXT_MUTED, Weight.SEMIBOLD, LABEL_SCALE);
			OogaFonts.draw(g, line.value(), valueX, ly, OogaTheme.TEXT, Weight.REGULAR);
			if (line.extra() != null) {
				float ex = valueX + OogaFonts.width(line.value(), Weight.REGULAR) + 6f;
				OogaFonts.draw(g, line.extra(), ex, ly + 1f, OogaTheme.TEXT_SECONDARY, Weight.REGULAR, 0.8f);
			}
			ly += LINE;
		}
		g.pose().popMatrix();
	}
}
