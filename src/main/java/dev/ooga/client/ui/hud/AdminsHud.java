package dev.ooga.client.ui.hud;

import dev.ooga.client.module.impl.misc.AdminDetectorModule;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.render.OogaFonts;
import dev.ooga.client.ui.render.OogaFonts.Weight;
import dev.ooga.client.ui.render.Render2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;

import java.util.List;

/** "Admins" panel: every staff member online with their face and ping, or "None online". */
public class AdminsHud extends HudElement {
	private static final float LINE = 12f;
	private static final float PAD = 5f;
	private static final int ALERT = 0xFFE5484D;

	private final AdminDetectorModule module;

	public AdminsHud(AdminDetectorModule module) {
		super("admins", "Admins", Anchor.END, 1f, Anchor.START, 0.32f);
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

	@Override
	protected void render(GuiGraphics g, float x, float y, float delta) {
		List<AdminDetectorModule.Staff> staff = module.online();
		float scale = module.scale.getFloat();
		String title = "Admins";
		float w = 12f + OogaFonts.width(title, Weight.SEMIBOLD) + 18f;
		for (AdminDetectorModule.Staff s : staff) {
			float lw = 11f + OogaFonts.width(s.name(), Weight.REGULAR) + 10f + OogaFonts.width(ping(s), Weight.REGULAR, 0.8f);
			w = Math.max(w, lw);
		}
		w = Math.max(w, 80f) + PAD * 2;
		float h = PAD * 2 + 11f + Math.max(1, staff.size()) * LINE;
		width = w * scale;
		height = h * scale;

		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		Render2D.roundRect(g, 0, 0, w, h, OogaTheme.RADIUS_CARD, 0xD90E0F12);
		Render2D.outline(g, 0, 0, w, h, OogaTheme.RADIUS_CARD, staff.isEmpty() ? OogaTheme.BORDER : 0x80E5484D);
		// A little shield: red while anyone is on.
		int shield = staff.isEmpty() ? OogaTheme.TEXT_MUTED : ALERT;
		Render2D.roundRect(g, PAD, PAD + 1.5f, 7f, 6f, 1.5f, shield);
		Render2D.triangle(g, PAD, PAD + 6.5f, PAD + 7f, PAD + 6.5f, PAD + 3.5f, PAD + 9.5f, shield);
		OogaFonts.draw(g, title, PAD + 12f, PAD, OogaTheme.TEXT, Weight.SEMIBOLD);
		if (!staff.isEmpty()) {
			String count = Integer.toString(staff.size());
			OogaFonts.draw(g, count, w - PAD - OogaFonts.width(count, Weight.SEMIBOLD), PAD, ALERT, Weight.SEMIBOLD);
		}

		float ly = PAD + 13f;
		if (staff.isEmpty()) {
			OogaFonts.draw(g, "None online", PAD, ly, OogaTheme.TEXT_SECONDARY, Weight.REGULAR, 0.9f);
		}
		for (AdminDetectorModule.Staff s : staff) {
			PlayerFaceRenderer.draw(g, s.info().getSkin(), Math.round(PAD), Math.round(ly), 8);
			OogaFonts.draw(g, s.name(), PAD + 11f, ly, OogaTheme.TEXT, Weight.REGULAR);
			String ping = ping(s);
			OogaFonts.draw(g, ping, w - PAD - OogaFonts.width(ping, Weight.REGULAR, 0.8f), ly + 1f, OogaTheme.TEXT_SECONDARY, Weight.REGULAR, 0.8f);
			ly += LINE;
		}
		g.pose().popMatrix();
	}

	private static String ping(AdminDetectorModule.Staff s) {
		return s.info().getLatency() + "ms";
	}
}
