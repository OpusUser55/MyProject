package dev.ooga.client.ui.notify;

import dev.ooga.client.module.impl.client.NotificationsModule;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.hud.HudElement;
import dev.ooga.client.ui.render.GlowRenderer;
import dev.ooga.client.ui.render.OogaFonts;
import dev.ooga.client.ui.render.OogaFonts.Weight;
import dev.ooga.client.ui.render.Render2D;
import dev.ooga.client.util.Anim;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Toast-style notifications that slide in from the screen edge, stack, and slide out.
 * Toggling a module with the same name replaces its previous toast instead of stacking a
 * second one, so rapid toggling never floods the screen.
 */
public final class NotificationManager extends HudElement {
	private static final NotificationManager INSTANCE = new NotificationManager();
	private static final float WIDTH = 138f;
	private static final float HEIGHT = 30f;
	private static final float GAP = 5f;
	private static final float MARGIN = 8f;

	private final List<Notification> active = new ArrayList<>();

	private NotificationManager() {
		super("notifications", "Notifications", Anchor.END, 1f, Anchor.END, 1f);
	}

	public static NotificationManager get() {
		return INSTANCE;
	}

	public void push(String title, String message, Notification.Kind kind) {
		NotificationsModule settings = NotificationsModule.instance();
		if (settings == null || !settings.isEnabled()) return;

		for (Notification existing : active) {
			if (!existing.leaving && existing.title.equals(title)) existing.leaving = true;
		}
		active.add(new Notification(title, message, kind, Math.round(settings.duration.get() * 1000)));

		int max = settings.maxVisible.getInt();
		int live = 0;
		for (int i = active.size() - 1; i >= 0; i--) {
			Notification n = active.get(i);
			if (n.leaving) continue;
			if (++live > max) n.leaving = true;
		}
	}

	@Override
	public boolean isVisible() {
		return !active.isEmpty();
	}

	@Override
	public boolean isMovable() {
		return false;
	}

	@Override
	protected void render(GuiGraphics g, float ignoredX, float ignoredY, float delta) {
		NotificationsModule settings = NotificationsModule.instance();
		if (settings == null) return;
		var window = Minecraft.getInstance().getWindow();
		int sw = window.getGuiScaledWidth();
		int sh = window.getGuiScaledHeight();
		boolean right = settings.position.get().endsWith("Right");
		boolean bottom = settings.position.get().startsWith("Bottom");

		// Bottom stacks grow upward and sit above the vanilla hotbar-adjacent area.
		float baseY = bottom ? sh - MARGIN - HEIGHT - (right ? 0 : 22) : MARGIN;
		int slot = 0;

		// Iterate newest-first so the newest toast sits closest to the edge.
		List<Notification> ordered = new ArrayList<>(active);
		Collections.reverse(ordered);
		for (Notification n : ordered) {
			if (!n.leaving && n.lifeProgress() >= 1f) n.leaving = true;
			float slide = Anim.ease(n.slide.update(n.leaving ? 0f : 1f));
			float targetSlot = slot;
			if (n.stackY.get() < 0) n.stackY.snap(targetSlot);
			float stack = n.stackY.update(targetSlot);
			if (!n.leaving) slot++;

			float y = bottom ? baseY - stack * (HEIGHT + GAP) : baseY + stack * (HEIGHT + GAP);
			float offscreen = (WIDTH + MARGIN + 6) * (1f - slide);
			float x = right ? sw - MARGIN - WIDTH + offscreen : MARGIN - offscreen;
			drawCard(g, n, x, y, slide);
		}
		Iterator<Notification> it = active.iterator();
		while (it.hasNext()) {
			Notification n = it.next();
			if (n.leaving && n.slide.get() <= 0.01f) it.remove();
		}
		width = WIDTH;
		height = HEIGHT;
	}

	private void drawCard(GuiGraphics g, Notification n, float x, float y, float visibility) {
		Render2D.pushAlpha(visibility);
		boolean on = n.kind == Notification.Kind.ENABLED;
		boolean info = n.kind == Notification.Kind.INFO;
		int accent = on || info ? OogaTheme.GOLD : OogaTheme.TEXT_MUTED;

		if (on) GlowRenderer.glow(g, x, y, WIDTH, HEIGHT, OogaTheme.RADIUS_CARD, OogaTheme.GOLD, 0.45f * visibility);
		Render2D.roundRect(g, x, y, WIDTH, HEIGHT, OogaTheme.RADIUS_CARD, 0xF2121317);
		Render2D.outline(g, x, y, WIDTH, HEIGHT, OogaTheme.RADIUS_CARD, on ? 0x40F2C14E : OogaTheme.BORDER);

		// Status indicator: filled gold dot when on, hollow ring when off.
		float cx = x + 13f;
		float cy = y + HEIGHT / 2f - 1f;
		if (on) {
			GlowRenderer.glowCircle(g, cx, cy, 3.2f, OogaTheme.GOLD, 0.8f);
			Render2D.circle(g, cx, cy, 3.2f, OogaTheme.GOLD);
		} else {
			Render2D.outline(g, cx - 3.2f, cy - 3.2f, 6.4f, 6.4f, 3.2f, 2, accent);
		}

		float textX = x + 24f;
		float maxText = WIDTH - 30f;
		OogaFonts.draw(g, OogaFonts.trim(n.title, Weight.SEMIBOLD, 1f, maxText), textX, y + 6f, OogaTheme.TEXT, Weight.SEMIBOLD);
		String status = OogaFonts.trim(n.message, Weight.REGULAR, 0.85f, maxText);
		OogaFonts.draw(g, status, textX, y + 17f, on ? OogaTheme.GOLD_TEXT : OogaTheme.TEXT_SECONDARY, Weight.REGULAR, 0.85f);

		// Remaining-time hairline along the bottom edge.
		float remaining = 1f - n.lifeProgress();
		float barInset = OogaTheme.RADIUS_CARD;
		float barWidth = (WIDTH - barInset * 2) * remaining;
		Render2D.rect(g, x + barInset, y + HEIGHT - 1.5f, barWidth, 1f, on || info ? 0x99F2C14E : 0x55FFFFFF);
		Render2D.popAlpha();
	}
}
