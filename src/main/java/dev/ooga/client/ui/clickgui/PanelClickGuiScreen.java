package dev.ooga.client.ui.clickgui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.impl.client.ClickGuiModule;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.module.setting.Setting;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.hud.HudEditorScreen;
import dev.ooga.client.ui.render.GlowRenderer;
import dev.ooga.client.ui.render.Icon;
import dev.ooga.client.ui.render.OogaFonts;
import dev.ooga.client.ui.render.OogaFonts.Weight;
import dev.ooga.client.ui.render.Render2D;
import dev.ooga.client.util.Anim;
import dev.ooga.client.util.ColorUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The panel layout: every category floats as its own compact panel, all visible at once,
 * with one row per module. Enabled rows light up gold with a soft glow; right-click a row to
 * unfold its settings underneath. Panels drag by their header, collapse with the chevron and
 * remember both across sessions. Typing anywhere searches: matching rows stay bright and the
 * rest fade back.
 */
public class PanelClickGuiScreen extends Screen {
	private static final float PANEL_W = 116f;
	private static final float HEADER_H = 19f;
	private static final float ROW_H = 15f;
	private static final float GAP = 8f;
	private static final float MARGIN = 8f;
	private static final float RADIUS = 5f;
	private static final String SEARCH_ID = "search";

	private final ClickGuiModule config = ModuleManager.get().get(ClickGuiModule.class);
	private final long openedAt = System.currentTimeMillis();
	private final Anim open = new Anim(0f, 12f);
	private final Anim scroll = new Anim(0f, 16f);
	private final Map<Object, Anim> anims = new HashMap<>();
	private final Map<Module, Boolean> expanded = new HashMap<>();
	private final List<Hit> hits = new ArrayList<>();

	private final StringBuilder search = new StringBuilder();
	private boolean closing;
	private float scrollTarget;

	private String draggingPanel;
	private float dragDX;
	private float dragDY;
	private NumberSetting draggingSlider;
	private float sliderX;
	private float sliderW;
	private Module binding;

	private float mouseX;
	private float mouseY;
	private Object hoveredTip;
	private String hoveredTipText;
	private long hoverStart;

	private record Hit(float x, float y, float w, float h, Action action) {
		boolean contains(float mx, float my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	@FunctionalInterface
	private interface Action {
		boolean click(int button, float mx, float my);
	}

	public PanelClickGuiScreen() {
		super(Component.literal("Ooga"));
	}

	// ================================================================== geometry

	private float scale() {
		return config.scale.getFloat();
	}

	private float screenW() {
		return width / scale();
	}

	private float screenH() {
		return height / scale();
	}

	private List<Category> categories() {
		List<Category> result = new ArrayList<>();
		for (Category c : Category.values()) {
			if (!ModuleManager.get().getModules(c).isEmpty()) result.add(c);
		}
		return result;
	}

	/** Lays out any panel without a saved position left to right, wrapping when full. */
	private void placeDefaults() {
		float x = MARGIN;
		float y = MARGIN;
		float rowBottom = y;
		List<String> ids = new ArrayList<>();
		for (Category c : categories()) ids.add(c.name());
		ids.add(SEARCH_ID);
		for (String id : ids) {
			PanelLayout.Entry e = PanelLayout.get(id);
			if (e.placed()) continue;
			if (x + PANEL_W > screenW() - MARGIN) {
				x = MARGIN;
				y = rowBottom + GAP;
			}
			e.x = x;
			e.y = y;
			x += PANEL_W + GAP;
			rowBottom = Math.max(rowBottom, y + 140f);
		}
	}

	private Anim anim(Object key, float initial, float speed) {
		return anims.computeIfAbsent(key, k -> new Anim(initial, speed));
	}

	private float hover(Object key, float x, float y, float w, float h) {
		boolean over = draggingPanel == null && draggingSlider == null && inside(mouseX, mouseY, x, y, w, h);
		return anim(key, 0f, 18f).update(over ? 1f : 0f);
	}

	private static boolean inside(float mx, float my, float x, float y, float w, float h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private boolean matches(Module m) {
		if (search.isEmpty()) return true;
		String q = search.toString().toLowerCase(Locale.ROOT).trim();
		return m.getName().toLowerCase(Locale.ROOT).contains(q) || m.getDescription().toLowerCase(Locale.ROOT).contains(q);
	}

	// ================================================================== rendering

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
		float t = Anim.ease(open.get());
		if (config.dim.get()) {
			Render2D.verticalGradient(g, 0, 0, width, height, ColorUtil.fade(0x66050507, t), ColorUtil.fade(0x99050507, t));
		}
		// A low golden haze along the bottom edge, felt more than seen.
		Render2D.verticalGradient(g, 0, height * 0.62f, width, height * 0.38f, 0x00F2C14E, ColorUtil.fade(0x1CF2C14E, t));
	}

	@Override
	public void render(GuiGraphics g, int rawX, int rawY, float delta) {
		placeDefaults();
		float t = open.update(closing ? 0f : 1f);
		if (closing && t <= 0.02f) {
			if (minecraft != null) minecraft.setScreen(null);
			return;
		}

		float s = scale();
		mouseX = rawX / s;
		mouseY = rawY / s;
		hits.clear();
		Object previousTip = hoveredTip;
		hoveredTip = null;
		float offset = scroll.update(scrollTarget);

		g.pose().pushMatrix();
		g.pose().scale(s, s);

		List<Category> categories = categories();
		long elapsed = System.currentTimeMillis() - openedAt;
		// The panel being dragged draws last so it floats above the others (and wins clicks).
		for (int pass = 0; pass < 2; pass++) {
			for (int i = 0; i <= categories.size(); i++) {
				String id = i < categories.size() ? categories.get(i).name() : SEARCH_ID;
				if (id.equals(draggingPanel) != (pass == 1)) continue;
				float appear = stagger(i, elapsed, t);
				if (i < categories.size()) drawCategoryPanel(g, categories.get(i), appear, offset);
				else drawSearchPanel(g, appear, offset);
			}
		}

		g.pose().popMatrix();

		if (hoveredTip != previousTip) hoverStart = System.currentTimeMillis();
		if (hoveredTip != null && hoveredTipText != null && draggingPanel == null && draggingSlider == null
				&& System.currentTimeMillis() - hoverStart > 500) {
			Render2D.pushAlpha(Anim.ease(t));
			Widgets.tooltip(g, rawX, rawY, hoveredTipText, width, height);
			Render2D.popAlpha();
		}
	}

	/** Panels cascade in one after another when the menu opens, and leave together. */
	private float stagger(int index, long elapsed, float open) {
		if (closing) return Anim.ease(open);
		Anim a = anim("panel#" + index, 0f, 13f);
		return Anim.ease(a.update(elapsed > index * 35L ? 1f : 0f));
	}

	private float panelBodyHeight(Category c) {
		float h = 0;
		for (Module m : ModuleManager.get().getModules(c)) h += ROW_H + settingsHeight(m) * expandAmount(m);
		return h + 3f;
	}

	private float expandAmount(Module m) {
		return Anim.ease(anim(m, 0f, 14f).get());
	}

	private void drawPanelFrame(GuiGraphics g, String id, float x, float y, float h, float appear, Icon icon, String title, String badge) {
		PanelLayout.Entry e = PanelLayout.get(id);
		boolean dragging = id.equals(draggingPanel);
		float lift = anim("lift#" + id, 0f, 16f).update(dragging ? 1f : 0f);

		GlowRenderer.glow(g, x, y, PANEL_W, h, RADIUS, OogaTheme.GOLD, 0.26f + 0.35f * lift, 6f);
		Render2D.roundRect(g, x, y, PANEL_W, h, RADIUS, 0xEE0E0F12);

		// Header: slightly lifted surface, gold icon, tracked caps title.
		float headerHover = hover("header#" + id, x, y, PANEL_W, HEADER_H);
		Render2D.roundRect(g, x, y, PANEL_W, Math.min(h, HEADER_H + RADIUS), RADIUS, ColorUtil.lerp(0xFF15171C, 0xFF1A1C22, headerHover));
		if (h > HEADER_H + RADIUS) Render2D.rect(g, x, y + HEADER_H, PANEL_W, RADIUS, 0xEE0E0F12);
		icon.draw(g, x + 7f, y + HEADER_H / 2f - 4f, 8f, OogaTheme.GOLD);
		float tx = x + 20f;
		for (char ch : title.toUpperCase(Locale.ROOT).toCharArray()) {
			String c = String.valueOf(ch);
			OogaFonts.draw(g, c, tx, y + HEADER_H / 2f - 3.6f, OogaTheme.TEXT, Weight.SEMIBOLD, 0.82f);
			tx += OogaFonts.width(c, Weight.SEMIBOLD, 0.82f) + 0.8f;
		}
		if (badge != null) {
			OogaFonts.draw(g, badge, tx + 4f, y + HEADER_H / 2f - 2.8f, OogaTheme.GOLD, Weight.SEMIBOLD, 0.66f);
		}

		// Collapse chevron.
		float cx = x + PANEL_W - 15f;
		float chevHover = hover("chev#" + id, cx - 2, y + 3, 13, HEADER_H - 6);
		float collapsed = Anim.ease(anim("collapse#" + id, e.collapsed ? 1f : 0f, 14f).get());
		(collapsed > 0.5f ? Icon.CHEVRON_RIGHT : Icon.CHEVRON).draw(g, cx, y + HEADER_H / 2f - 4f, 8f,
				ColorUtil.lerp(OogaTheme.TEXT_MUTED, OogaTheme.GOLD, chevHover));

		// Gold hairline under the header, strongest at the left and fading out.
		if (h > HEADER_H + 1) {
			float lineW = PANEL_W - 12f;
			Render2D.rect(g, x + 6f, y + HEADER_H, lineW, 1f, ColorUtil.fade(OogaTheme.GOLD, 0.22f));
			GlowRenderer.glow(g, x + 6f, y + HEADER_H, lineW * 0.45f, 1f, 0.5f, OogaTheme.GOLD, 0.5f, 3f);
			Render2D.rect(g, x + 6f, y + HEADER_H, lineW * 0.45f, 1f, ColorUtil.fade(OogaTheme.GOLD, 0.75f));
		}
		Render2D.outline(g, x, y, PANEL_W, h, RADIUS, dragging ? 0x66F2C14E : OogaTheme.BORDER);

		// Header hits: chevron toggles collapse, anything else drags. Chevron added last = wins.
		hits.add(new Hit(x, y, PANEL_W, HEADER_H, (button, mx, my) -> {
			if (button == 1) {
				toggleCollapse(e);
				return true;
			}
			if (button != 0) return false;
			draggingPanel = id;
			dragDX = mx - e.x;
			dragDY = my - e.y;
			return true;
		}));
		hits.add(new Hit(cx - 3, y + 2, 14, HEADER_H - 4, (button, mx, my) -> {
			if (button != 0) return false;
			toggleCollapse(e);
			return true;
		}));
	}

	private void toggleCollapse(PanelLayout.Entry e) {
		e.collapsed = !e.collapsed;
		PanelLayout.changed();
	}

	private void drawCategoryPanel(GuiGraphics g, Category c, float appear, float offset) {
		if (appear <= 0.01f) return;
		String id = c.name();
		PanelLayout.Entry e = PanelLayout.get(id);
		float collapse = Anim.ease(anim("collapse#" + id, e.collapsed ? 1f : 0f, 14f).update(e.collapsed ? 1f : 0f));
		float body = panelBodyHeight(c) * (1f - collapse);
		float h = HEADER_H + body;

		float x = e.x;
		float y = e.y - offset + (1f - appear) * -10f;

		int active = 0;
		for (Module m : ModuleManager.get().getModules(c)) if (m.isEnabled() && !m.isSettingsOnly()) active++;

		Render2D.pushAlpha(appear);
		drawPanelFrame(g, id, x, y, h, appear, c.getIcon(), c.getDisplayName(), active > 0 ? Integer.toString(active) : null);

		if (body > 0.5f) {
			g.enableScissor(Math.round(x), Math.round(y + HEADER_H + 1), Math.round(x + PANEL_W), Math.round(y + h));
			float rowY = y + HEADER_H + 1.5f;
			for (Module m : ModuleManager.get().getModules(c)) {
				rowY = drawModuleRow(g, m, x, rowY, y + h);
			}
			g.disableScissor();
		}
		Render2D.popAlpha();
	}

	private float drawModuleRow(GuiGraphics g, Module m, float x, float y, float panelBottom) {
		float on = anim("on#" + m.getName(), m.isEnabled() ? 1f : 0f, 15f).update(m.isEnabled() ? 1f : 0f);
		boolean matched = matches(m);
		float match = anim("match#" + m.getName(), 1f, 14f).update(matched ? 1f : 0.28f);
		boolean visible = y < panelBottom;
		float hv = visible ? hover("row#" + m.getName(), x, y, PANEL_W, ROW_H) : 0f;
		float expand = anim(m, 0f, 14f).update(expanded.getOrDefault(m, false) ? 1f : 0f);

		Render2D.pushAlpha(match);
		float inset = 3f;
		if (on > 0.01f || hv > 0.01f) {
			int fill = ColorUtil.lerp(ColorUtil.fade(0x0FFFFFFF, hv), 0x1CF2C14E, on);
			Render2D.roundRect(g, x + inset, y + 0.5f, PANEL_W - inset * 2, ROW_H - 1f, 3f, fill);
		}
		if (on > 0.01f) {
			GlowRenderer.glow(g, x + inset, y + 0.5f, PANEL_W - inset * 2, ROW_H - 1f, 3f, OogaTheme.GOLD, on * 0.22f, 3f);
			GlowRenderer.glow(g, x + inset, y + 3.5f, 1.6f, ROW_H - 7f, 0.8f, OogaTheme.GOLD, on, 3.5f);
			Render2D.roundRect(g, x + inset, y + 3.5f, 1.6f, ROW_H - 7f, 0.8f, ColorUtil.fade(OogaTheme.GOLD, on));
		}
		if (!search.isEmpty() && matched) {
			Render2D.outline(g, x + inset, y + 0.5f, PANEL_W - inset * 2, ROW_H - 1f, 3f, 0x80F2C14E);
		}

		int nameColor = ColorUtil.lerp(ColorUtil.lerp(OogaTheme.TEXT_SECONDARY, OogaTheme.TEXT, hv), OogaTheme.GOLD_TEXT, on);
		if (m.isSettingsOnly()) nameColor = ColorUtil.lerp(OogaTheme.TEXT_SECONDARY, OogaTheme.TEXT, hv);
		float controlsW = 24f;
		String name = OogaFonts.trim(m.getName(), Weight.REGULAR, 0.85f, PANEL_W - 14f - controlsW);
		OogaFonts.draw(g, name, x + 9f, y + ROW_H / 2f - 3.7f, nameColor, Weight.REGULAR, 0.85f);

		float cy = y + ROW_H / 2f;
		if (binding == m) {
			Widgets.chip(g, x + PANEL_W - 6f, cy, "...", OogaTheme.ON_GOLD, OogaTheme.GOLD, 0, 0.66f);
		} else if (m.isSettingsOnly()) {
			(expand > 0.5f ? Icon.CHEVRON : Icon.CHEVRON_RIGHT).draw(g, x + PANEL_W - 15f, cy - 3.5f, 7f,
					ColorUtil.lerp(OogaTheme.TEXT_MUTED, OogaTheme.GOLD, Math.max(hv * 0.5f, expand)));
		} else {
			if (m.getKey() != -1 && hv > 0.02f) {
				Render2D.pushAlpha(hv);
				Widgets.chip(g, x + PANEL_W - 28f, cy, keyName(m.getKey()), OogaTheme.TEXT_SECONDARY, OogaTheme.SURFACE_INSET, OogaTheme.BORDER, 0.6f);
				Render2D.popAlpha();
			}
			Widgets.toggle(g, x + PANEL_W - 22f, cy - 4f, 15f, 8f, on, hv);
		}
		Render2D.popAlpha();

		if (visible) {
			hoveredTipIf(m, m.getDescription(), x, y, PANEL_W, ROW_H);
			hits.add(new Hit(x, y, PANEL_W, ROW_H, (button, mx, my) -> {
				if (button == 0 && !m.isSettingsOnly()) m.toggle();
				else if (button == 1 || button == 0) {
					if (!m.getSettings().isEmpty()) expanded.put(m, !expanded.getOrDefault(m, false));
				} else if (button == 2 && !m.isSettingsOnly()) binding = m;
				return true;
			}));
		}

		float next = y + ROW_H;
		if (expand > 0.01f && !m.getSettings().isEmpty()) {
			float full = settingsHeight(m);
			float shown = full * Anim.ease(expand);
			Render2D.pushAlpha(Anim.ease(Math.min(1f, expand * 1.3f)) * match);
			Render2D.roundRect(g, x + 3f, next, PANEL_W - 6f, shown, 3f, 0x66000000);
			float sy = next + 2f;
			for (Setting<?> setting : m.getSettings()) {
				if (!setting.isVisible()) continue;
				if (sy > next + shown || sy > panelBottom) break;
				sy = drawSetting(g, setting, x + 9f, sy, PANEL_W - 18f);
			}
			Render2D.popAlpha();
			next += shown;
		}
		return next;
	}

	private float settingsHeight(Module m) {
		float h = 4f;
		for (Setting<?> s : m.getSettings()) {
			if (s.isVisible()) h += s instanceof NumberSetting ? 20f : 13f;
		}
		return h;
	}

	private float drawSetting(GuiGraphics g, Setting<?> setting, float x, float y, float w) {
		float scale = 0.72f;
		boolean number = setting instanceof NumberSetting;
		float h = number ? 20f : 13f;
		float hv = hover(setting, x - 4, y, w + 8, h);
		int label = ColorUtil.lerp(OogaTheme.TEXT_MUTED, OogaTheme.TEXT_SECONDARY, 0.5f + 0.5f * hv);
		hoveredTipIf(setting, setting.getDescription(), x - 4, y, w + 8, h);

		if (setting instanceof BooleanSetting bool) {
			OogaFonts.draw(g, OogaFonts.trim(setting.getName(), Weight.REGULAR, scale, w - 20f), x, y + 3.5f, label, Weight.REGULAR, scale);
			float on = anim("bool#" + System.identityHashCode(setting), bool.get() ? 1f : 0f, 16f).update(bool.get() ? 1f : 0f);
			Widgets.toggle(g, x + w - 12f, y + 3f, 12f, 7f, on, hv);
			hits.add(new Hit(x - 4, y, w + 8, h, (button, mx, my) -> {
				if (button != 0) return false;
				bool.toggle();
				return true;
			}));
		} else if (setting instanceof NumberSetting num) {
			String value = num.format();
			float vw = OogaFonts.width(value, Weight.SEMIBOLD, scale);
			OogaFonts.draw(g, OogaFonts.trim(setting.getName(), Weight.REGULAR, scale, w - vw - 6f), x, y + 2.5f, label, Weight.REGULAR, scale);
			OogaFonts.draw(g, value, x + w - vw, y + 2.5f, OogaTheme.GOLD_TEXT, Weight.SEMIBOLD, scale);
			boolean active = draggingSlider == num;
			Anim shown = anim("slider#" + System.identityHashCode(setting), (float) num.getProgress(), 28f);
			float progress = active ? (float) num.getProgress() : shown.update((float) num.getProgress());
			if (active) shown.snap(progress);
			float trackX = x + 2f;
			float trackW = w - 4f;
			Widgets.slider(g, trackX, y + 14f, trackW, progress, active ? 1f : hv * 0.4f);
			hits.add(new Hit(x - 4, y + 8f, w + 8, h - 8f, (button, mx, my) -> {
				if (button == 1) {
					num.reset();
					return true;
				}
				if (button != 0) return false;
				draggingSlider = num;
				sliderX = trackX;
				sliderW = trackW;
				num.setProgress((mx - trackX) / trackW);
				return true;
			}));
		} else if (setting instanceof ModeSetting mode) {
			float chipW = OogaFonts.width(mode.get(), Weight.SEMIBOLD, 0.62f) + 9f;
			OogaFonts.draw(g, OogaFonts.trim(setting.getName(), Weight.REGULAR, scale, w - chipW - 6f), x, y + 3.5f, label, Weight.REGULAR, scale);
			Widgets.chip(g, x + w, y + h / 2f, mode.get(), OogaTheme.GOLD_TEXT, ColorUtil.lerp(OogaTheme.SURFACE_INSET, OogaTheme.SURFACE_CONTROL, hv),
					ColorUtil.lerp(OogaTheme.BORDER, 0x55F2C14E, hv), 0.62f);
			hits.add(new Hit(x - 4, y, w + 8, h, (button, mx, my) -> {
				if (button == 0) mode.cycle(true);
				else if (button == 1) mode.cycle(false);
				else return false;
				return true;
			}));
		}
		return y + h;
	}

	private void hoveredTipIf(Object key, String text, float x, float y, float w, float h) {
		if (inside(mouseX, mouseY, x, y, w, h)) {
			hoveredTip = key;
			hoveredTipText = text;
		}
	}

	private void drawSearchPanel(GuiGraphics g, float appear, float offset) {
		if (appear <= 0.01f) return;
		PanelLayout.Entry e = PanelLayout.get(SEARCH_ID);
		float collapse = Anim.ease(anim("collapse#" + SEARCH_ID, e.collapsed ? 1f : 0f, 14f).update(e.collapsed ? 1f : 0f));
		float body = 46f * (1f - collapse);
		float h = HEADER_H + body;
		float x = e.x;
		float y = e.y - offset + (1f - appear) * -10f;

		Render2D.pushAlpha(appear);
		drawPanelFrame(g, SEARCH_ID, x, y, h, appear, Icon.SEARCH, "Search", null);
		if (body > 0.5f) {
			g.enableScissor(Math.round(x), Math.round(y + HEADER_H + 1), Math.round(x + PANEL_W), Math.round(y + h));
			// Field.
			float fx = x + 6f, fy = y + HEADER_H + 6f, fw = PANEL_W - 12f, fh = 14f;
			boolean typing = !search.isEmpty();
			float focus = anim("searchFocus", 0f, 16f).update(typing ? 1f : 0f);
			if (focus > 0.01f) GlowRenderer.glow(g, fx, fy, fw, fh, 3f, OogaTheme.GOLD, 0.4f * focus);
			Render2D.roundRect(g, fx, fy, fw, fh, 3f, OogaTheme.SURFACE_INSET);
			Render2D.outline(g, fx, fy, fw, fh, 3f, ColorUtil.lerp(OogaTheme.BORDER, 0x99F2C14E, focus));
			if (typing) {
				String shown = search.toString();
				while (shown.length() > 1 && OogaFonts.width(shown, Weight.REGULAR, 0.8f) > fw - 12f) shown = shown.substring(1);
				OogaFonts.draw(g, shown, fx + 5f, fy + 3.6f, OogaTheme.TEXT, Weight.REGULAR, 0.8f);
				if ((System.currentTimeMillis() / 530) % 2 == 0) {
					Render2D.rect(g, fx + 5.5f + OogaFonts.width(shown, Weight.REGULAR, 0.8f), fy + 3f, 0.75f, 8f, OogaTheme.GOLD);
				}
			} else {
				OogaFonts.draw(g, "Type to search", fx + 5f, fy + 3.6f, OogaTheme.TEXT_MUTED, Weight.REGULAR, 0.8f);
			}

			// Edit HUD button.
			float bx = x + 6f, by = fy + fh + 5f, bw = PANEL_W - 12f, bh = 14f;
			float bh2 = hover("editHud", bx, by, bw, bh);
			Render2D.roundRect(g, bx, by, bw, bh, 3f, ColorUtil.lerp(0xFF15171C, 0x26F2C14E, bh2));
			Render2D.outline(g, bx, by, bw, bh, 3f, ColorUtil.lerp(OogaTheme.BORDER, 0x80F2C14E, bh2));
			Icon.MOVE.draw(g, bx + 6f, by + 3.5f, 7f, ColorUtil.lerp(OogaTheme.TEXT_SECONDARY, OogaTheme.GOLD, bh2));
			OogaFonts.draw(g, "Edit HUD", bx + 17f, by + 3.8f, ColorUtil.lerp(OogaTheme.TEXT_SECONDARY, OogaTheme.TEXT, bh2), Weight.SEMIBOLD, 0.75f);
			hits.add(new Hit(bx, by, bw, bh, (button, mx, my) -> {
				if (button != 0) return false;
				minecraft.setScreen(new HudEditorScreen(this));
				return true;
			}));
			hits.add(new Hit(fx, fy, fw, fh, (button, mx, my) -> {
				if (button == 1) search.setLength(0);
				return true;
			}));
			g.disableScissor();
		}
		Render2D.popAlpha();
	}

	private static String keyName(int key) {
		String name = InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
		return name.length() > 6 ? name.substring(0, 6) : name;
	}

	// ================================================================== input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (closing) return true;
		if (binding != null) {
			binding = null;
			return true;
		}
		float mx = (float) event.x() / scale();
		float my = (float) event.y() / scale();
		for (int i = hits.size() - 1; i >= 0; i--) {
			Hit hit = hits.get(i);
			if (hit.contains(mx, my) && hit.action().click(event.button(), mx, my)) return true;
		}
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		float mx = (float) event.x() / scale();
		float my = (float) event.y() / scale();
		if (draggingPanel != null) {
			PanelLayout.Entry e = PanelLayout.get(draggingPanel);
			e.x = Math.max(0, Math.min(screenW() - PANEL_W, mx - dragDX));
			e.y = Math.max(0, Math.min(screenH() - HEADER_H, my - dragDY));
			return true;
		}
		if (draggingSlider != null) {
			draggingSlider.setProgress((mx - sliderX) / sliderW);
			return true;
		}
		return super.mouseDragged(event, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (draggingPanel != null) PanelLayout.changed();
		draggingPanel = null;
		draggingSlider = null;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
		scrollTarget = Math.max(-40f, Math.min(600f, scrollTarget - (float) scrollY * 20f));
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (closing) {
			if (key == config.getKey()) closing = false;
			return true;
		}
		if (binding != null) {
			if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) binding.setKey(-1);
			else binding.setKey(key);
			binding = null;
			return true;
		}
		if (key == GLFW.GLFW_KEY_ESCAPE) {
			if (!search.isEmpty()) search.setLength(0);
			else onClose();
			return true;
		}
		if (!search.isEmpty()) {
			if (key == GLFW.GLFW_KEY_BACKSPACE) {
				if ((event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) search.setLength(0);
				else search.setLength(search.length() - 1);
				return true;
			}
			if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
				for (Module m : ModuleManager.get().getModules()) {
					if (matches(m) && !m.isSettingsOnly()) {
						m.toggle();
						break;
					}
				}
				return true;
			}
		} else if (key == config.getKey()) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		int codepoint = event.codepoint();
		if (binding != null || Character.isISOControl(codepoint)) return false;
		if (search.isEmpty() && Character.isWhitespace(codepoint)) return false;
		if (search.length() < 32) search.appendCodePoint(codepoint);
		return true;
	}

	@Override
	public void onClose() {
		closing = true;
		binding = null;
		draggingPanel = null;
		draggingSlider = null;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
