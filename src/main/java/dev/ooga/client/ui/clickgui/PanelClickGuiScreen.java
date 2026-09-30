package dev.ooga.client.ui.clickgui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.impl.client.ClickGuiModule;
import dev.ooga.client.module.impl.client.ClientSettings;
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
	private static final float HEADER_H = 19f;
	private static final float GAP = 8f;
	private static final float MARGIN = 8f;
	/** Panels start below the tab bar. */
	private static final float TOP = 32f;
	private static final String SEARCH_ID = "search";

	private final ClickGuiModule config = ModuleManager.get().get(ClickGuiModule.class);
	private final long openedAt = System.currentTimeMillis();
	private final Anim open = new Anim(0f, 12f);
	private final Anim scroll = new Anim(0f, 16f);
	private final Map<Object, Anim> anims = new HashMap<>();
	private final Map<Module, Boolean> expanded = new HashMap<>();
	private final List<Hit> hits = new ArrayList<>();
	private final MenuBackdrop backdrop = new MenuBackdrop();

	private enum Tab {
		MAIN("Main"), CONFIGS("Configs"), FINDS("Finds"), THEME("Theme");

		final String label;

		Tab(String label) {
			this.label = label;
		}
	}

	/** The last tab used, remembered while the game runs. */
	private static Tab lastTab = Tab.MAIN;
	private Tab tab = lastTab;
	private final Anim tabX = new Anim(-1f, 16f);
	private final Anim tabW = new Anim(0f, 16f);
	private final Anim pageScroll = new Anim(0f, 16f);
	private float pageScrollTarget;
	private final StringBuilder configName = new StringBuilder();
	private String confirmDelete;
	private long confirmUntil;
	private String status;
	private long statusUntil;

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

	private float panelW() {
		return config.panelWidth.getFloat();
	}

	private float rowH() {
		return switch (config.density.get()) {
			case "Compact" -> 13f;
			case "Relaxed" -> 17f;
			default -> 15f;
		};
	}

	/** Real font sizes, expressed as scales of the 9px base, so text is never resampled. */
	private static final float S8 = 8f / 9f;
	private static final float S7 = 7f / 9f;

	/** Top of text of the given pixel size, optically centred in a box of height {@code h}. */
	private static float textY(float top, float h, int size) {
		return top + (h - size * 0.73f) / 2f - 1.2f;
	}

	private static float radius() {
		return OogaTheme.corner(5f);
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

	/**
	 * Places panels that have no saved position: left to right across the top, then any that
	 * don't fit go under whichever column is currently shortest.
	 */
	private void placeDefaults() {
		List<String> ids = new ArrayList<>();
		List<Float> heights = new ArrayList<>();
		for (Category c : categories()) {
			ids.add(c.name());
			heights.add(HEADER_H + ModuleManager.get().getModules(c).size() * rowH() + 3f);
		}
		ids.add(SEARCH_ID);
		heights.add(HEADER_H + 46f);

		List<float[]> columns = new ArrayList<>(); // {x, bottom}
		for (float x = MARGIN; x + panelW() <= screenW() - MARGIN; x += panelW() + GAP) columns.add(new float[]{x, TOP - GAP});
		if (columns.isEmpty()) columns.add(new float[]{MARGIN, TOP - GAP});

		// Account for panels the user has already placed, so new ones don't land on top of them.
		for (int i = 0; i < ids.size(); i++) {
			PanelLayout.Entry e = PanelLayout.get(ids.get(i));
			if (!e.placed()) continue;
			for (float[] col : columns) {
				if (Math.abs(col[0] - e.x) < panelW() / 2f) col[1] = Math.max(col[1], e.y + heights.get(i));
			}
		}
		for (int i = 0; i < ids.size(); i++) {
			PanelLayout.Entry e = PanelLayout.get(ids.get(i));
			if (e.placed() && e.y < TOP) e.y = TOP;
			if (e.placed()) continue;
			float[] target = columns.get(0);
			for (float[] col : columns) if (col[1] < target[1]) target = col;
			e.x = target[0];
			e.y = target[1] + GAP;
			target[1] = e.y + heights.get(i);
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
		OogaTheme.frame();
		backdrop.draw(g, config, width, height, Anim.ease(open.get()));
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
		if (tab == Tab.MAIN) {
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
		} else {
			drawPage(g, Anim.ease(t));
		}

		drawTabs(g, Anim.ease(t));
		drawBrand(g, Anim.ease(t));
		g.pose().popMatrix();

		if (hoveredTip != previousTip) hoverStart = System.currentTimeMillis();
		if (hoveredTip != null && hoveredTipText != null && draggingPanel == null && draggingSlider == null
				&& System.currentTimeMillis() - hoverStart > 500) {
			Render2D.pushAlpha(Anim.ease(t));
			Widgets.tooltip(g, rawX, rawY, hoveredTipText, width, height);
			Render2D.popAlpha();
		}
	}

	/** A glowing wordmark at the bottom centre with live stats. */
	private void drawBrand(GuiGraphics g, float appear) {
		if (appear <= 0.01f) return;
		Render2D.pushAlpha(appear);
		int total = 0, on = 0;
		for (Module m : ModuleManager.get().getModules()) {
			if (m.isSettingsOnly()) continue;
			total++;
			if (m.isEnabled()) on++;
		}
		String word = "OOGA";
		String stats = "v" + dev.ooga.client.OogaClient.VERSION + "  ·  " + on + " / " + total + " modules on";
		float wordScale = 1.5f;
		float letter = 1.6f;
		float wordW = 0;
		for (char ch : word.toCharArray()) wordW += OogaFonts.width(String.valueOf(ch), Weight.DISPLAY, wordScale) + letter;
		float statsW = OogaFonts.width(stats, Weight.REGULAR, S7);
		float cx = screenW() / 2f;
		float y = screenH() - 34f;
		float x = cx - wordW / 2f;
		// Neon wordmark: a tight glow hugging the letters.
		GlowRenderer.glow(g, x, y + 2f, wordW, 11f, 3f, OogaTheme.GOLD, 0.8f, 5f);
		int i = 0;
		for (char ch : word.toCharArray()) {
			String c = String.valueOf(ch);
			// Each letter picks its colour further along the accent gradient, shimmering over time.
			float wave = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 600.0 - i * 0.9));
			OogaFonts.draw(g, c, x, y, OogaTheme.gradient(wave), Weight.DISPLAY, wordScale);
			x += OogaFonts.width(c, Weight.DISPLAY, wordScale) + letter;
			i++;
		}
		OogaFonts.draw(g, stats, cx - statsW / 2f, y + 19f, OogaTheme.TEXT_SECONDARY, Weight.REGULAR, S7);
		Render2D.popAlpha();
	}

	/** Panels cascade in one after another when the menu opens, and leave together. */
	private float stagger(int index, long elapsed, float open) {
		if (closing || !config.cascade.get()) return Anim.ease(open);
		Anim a = anim("panel#" + index, 0f, 13f);
		return Anim.ease(a.update(elapsed > index * 35L ? 1f : 0f));
	}

	private float panelBodyHeight(Category c) {
		float h = 0;
		for (Module m : ModuleManager.get().getModules(c)) h += rowH() + settingsHeight(m) * expandAmount(m);
		return h + 3f;
	}

	private float expandAmount(Module m) {
		return Anim.ease(anim(m, 0f, 14f).get());
	}

	private void drawPanelFrame(GuiGraphics g, String id, float x, float y, float h, float appear, Icon icon, String title, String badge) {
		PanelLayout.Entry e = PanelLayout.get(id);
		boolean dragging = id.equals(draggingPanel);
		float lift = anim("lift#" + id, 0f, 16f).update(dragging ? 1f : 0f);

		// Neon edge: a tight, bright glow hugging the border (wider while dragged).
		float ambient = config.panelGlow.getFloat() * appear;
		if (ambient + lift > 0.01f) GlowRenderer.glow(g, x, y, panelW(), h, radius(), OogaTheme.GOLD, 0.7f * ambient + 0.4f * lift, 2.5f + 3f * lift);
		Render2D.roundRect(g, x, y, panelW(), h, radius(), ClientSettings.surface(0xEE0E0F12));

		// Header: slightly lifted surface, gold icon, tracked caps title.
		float headerHover = hover("header#" + id, x, y, panelW(), HEADER_H);
		Render2D.roundRect(g, x, y, panelW(), Math.min(h, HEADER_H + radius()), radius(),
				ClientSettings.surface(ColorUtil.lerp(0xF015171C, 0xF01A1C22, headerHover)));
		if (h > HEADER_H + radius()) Render2D.rect(g, x, y + HEADER_H, panelW(), radius(), ClientSettings.surface(0xEE0E0F12));
		GlowRenderer.glow(g, x + 7f, y + HEADER_H / 2f - 4f, 8f, 8f, 2f, OogaTheme.GOLD, 0.9f * appear, 3f);
		icon.draw(g, x + 7f, y + HEADER_H / 2f - 4f, 8f, OogaTheme.GOLD);
		float tx = x + 20f;
		String upper = title.toUpperCase(Locale.ROOT);
		float titleW = 0;
		for (char ch : upper.toCharArray()) titleW += OogaFonts.width(String.valueOf(ch), Weight.SEMIBOLD, S8) + 0.8f;
		// The title glows faintly in the accent colour.
		GlowRenderer.glow(g, tx, y + 6f, titleW, HEADER_H - 12f, 2f, OogaTheme.GOLD, 0.35f * appear, 4f);
		int letter = 0;
		for (char ch : upper.toCharArray()) {
			String c = String.valueOf(ch);
			// Letters run along the accent gradient, tinted towards white.
			int color = ColorUtil.lerp(OogaTheme.gradient(letter / (float) Math.max(1, upper.length() - 1)), 0xFFFFFFFF, 0.55f);
			OogaFonts.draw(g, c, tx, textY(y, HEADER_H, 8), color, Weight.SEMIBOLD, S8);
			tx += OogaFonts.width(c, Weight.SEMIBOLD, S8) + 0.8f;
			letter++;
		}
		if (badge != null) {
			OogaFonts.draw(g, badge, tx + 4f, textY(y, HEADER_H, 7), OogaTheme.TEXT_MUTED, Weight.SEMIBOLD, S7);
		}

		// Collapse chevron.
		float cx = x + panelW() - 15f;
		float chevHover = hover("chev#" + id, cx - 2, y + 3, 13, HEADER_H - 6);
		float collapsed = Anim.ease(anim("collapse#" + id, e.collapsed ? 1f : 0f, 14f).get());
		(collapsed > 0.5f ? Icon.CHEVRON_RIGHT : Icon.CHEVRON).draw(g, cx, y + HEADER_H / 2f - 4f, 8f,
				ColorUtil.lerp(OogaTheme.TEXT_MUTED, OogaTheme.GOLD, chevHover));

		// Header divider: a plain hairline, with a short accent segment under the icon.
		if (h > HEADER_H + 1) {
			// Accent line under the header: bright under the icon, sweeping into the second
			// accent colour and fading out towards the right.
			Render2D.rect(g, x, y + HEADER_H, panelW(), 1f, OogaTheme.DIVIDER);
			Render2D.horizontalGradient(g, x + 1f, y + HEADER_H, panelW() * 0.75f, 1f, OogaTheme.GOLD, 0);
			Render2D.horizontalGradient(g, x + 1f, y + HEADER_H, panelW() - 2f, 1f, 0, ColorUtil.withAlpha(OogaTheme.ACCENT_2, 0x70));
			GlowRenderer.glow(g, x + 6f, y + HEADER_H, panelW() * 0.4f, 1f, 0.5f, OogaTheme.GOLD, 0.45f * appear, 3f);
			// A faint accent wash at the top of the body, like light spilling from the header.
			Render2D.verticalGradient(g, x + 1f, y + HEADER_H + 1f, panelW() - 2f, 10f, OogaTheme.accent(0x14), 0x00000000);
		}
		Render2D.outline(g, x, y, panelW(), h, radius(), OogaTheme.accent(Math.round(0x40 + 0x60 * Math.min(1f, ambient + lift))));

		// Header hits: chevron toggles collapse, anything else drags. Chevron added last = wins.
		hits.add(new Hit(x, y, panelW(), HEADER_H, (button, mx, my) -> {
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
			g.enableScissor(Math.round(x), Math.round(y + HEADER_H + 1), Math.round(x + panelW()), Math.round(y + h));
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
		float hv = visible ? hover("row#" + m.getName(), x, y, panelW(), rowH()) : 0f;
		float expand = anim(m, 0f, 14f).update(expanded.getOrDefault(m, false) ? 1f : 0f);

		Render2D.pushAlpha(match);
		float inset = 3f;
		float rowR = OogaTheme.corner(3f);
		String style = config.enabledStyle.get();
		boolean fillStyle = style.equals("Fill");
		if (hv > 0.01f) {
			Render2D.roundRect(g, x + inset, y + 0.5f, panelW() - inset * 2, rowH() - 1f, rowR, ColorUtil.fade(0x0DFFFFFF, hv));
		}
		if (on > 0.01f && !style.equals("Text")) {
			if (fillStyle) {
				Render2D.roundRect(g, x + inset, y + 0.5f, panelW() - inset * 2, rowH() - 1f, rowR, ColorUtil.fade(OogaTheme.accent(0x24), on));
			} else {
				// Enabled: a wash of accent light fading out across the row, and a glowing bar.
				Render2D.horizontalGradient(g, x + inset, y + 0.5f, (panelW() - inset * 2) * 0.85f, rowH() - 1f,
						ColorUtil.fade(OogaTheme.accent(0x2E), on), 0);
				float barH = rowH() - 7f;
				GlowRenderer.glow(g, x + inset, y + 3.5f, 1.5f, barH, 0.75f, OogaTheme.GOLD, on, 4f);
				Render2D.roundRect(g, x + inset, y + 3.5f, 1.5f, barH, 0.75f, ColorUtil.fade(OogaTheme.GOLD, on));
			}
		}
		if (!search.isEmpty() && matched) {
			Render2D.outline(g, x + inset, y + 0.5f, panelW() - inset * 2, rowH() - 1f, rowR, OogaTheme.accent(0x70));
		}

		int nameColor = ColorUtil.lerp(ColorUtil.lerp(OogaTheme.TEXT_SECONDARY, OogaTheme.TEXT, hv), OogaTheme.GOLD_TEXT, on);
		if (m.isSettingsOnly()) nameColor = ColorUtil.lerp(OogaTheme.TEXT_SECONDARY, OogaTheme.TEXT, hv);
		float controlsW = 24f;
		String name = OogaFonts.trim(m.getName(), Weight.REGULAR, S8, panelW() - 14f - controlsW);
		// Rows lean in slightly toward the cursor.
		float slide = 1.5f * hv + 1f * on;
		// Enabled names glow.
		if (on > 0.01f && !m.isSettingsOnly()) {
			GlowRenderer.glow(g, x + 9f + slide, y + 4f, OogaFonts.width(name, Weight.REGULAR, S8), rowH() - 8f, 2f, OogaTheme.GOLD, 0.45f * on, 3.5f);
		}
		OogaFonts.draw(g, name, x + 9f + slide, textY(y, rowH(), 8), nameColor, Weight.REGULAR, S8);

		float cy = y + rowH() / 2f;
		if (binding == m) {
			Widgets.chip(g, x + panelW() - 6f, cy, "...", OogaTheme.ON_GOLD, OogaTheme.GOLD, 0, 0.66f);
		} else if (m.isSettingsOnly()) {
			(expand > 0.5f ? Icon.CHEVRON : Icon.CHEVRON_RIGHT).draw(g, x + panelW() - 15f, cy - 3.5f, 7f,
					ColorUtil.lerp(OogaTheme.TEXT_MUTED, OogaTheme.GOLD, Math.max(hv * 0.5f, expand)));
		} else {
			float keyAlpha = switch (config.keybinds.get()) {
				case "Always" -> 1f;
				case "Never" -> 0f;
				default -> hv;
			};
			if (m.getKey() != -1 && keyAlpha > 0.02f) {
				Render2D.pushAlpha(keyAlpha);
				Widgets.chip(g, x + panelW() - 28f, cy, keyName(m.getKey()), OogaTheme.TEXT_SECONDARY, OogaTheme.SURFACE_INSET, OogaTheme.BORDER, S7);
				Render2D.popAlpha();
			}
			Widgets.toggle(g, x + panelW() - 22f, cy - 4f, 15f, 8f, on, hv);
		}
		Render2D.popAlpha();

		if (visible) {
			hoveredTipIf(m, m.getDescription(), x, y, panelW(), rowH());
			hits.add(new Hit(x, y, panelW(), rowH(), (button, mx, my) -> {
				if (button == 0 && !m.isSettingsOnly()) m.toggle();
				else if (button == 1 || button == 0) {
					if (!m.getSettings().isEmpty()) expanded.put(m, !expanded.getOrDefault(m, false));
				} else if (button == 2 && !m.isSettingsOnly()) binding = m;
				return true;
			}));
		}

		float next = y + rowH();
		if (expand > 0.01f && !m.getSettings().isEmpty()) {
			float full = settingsHeight(m);
			float shown = full * Anim.ease(expand);
			Render2D.pushAlpha(Anim.ease(Math.min(1f, expand * 1.3f)) * match);
			Render2D.roundRect(g, x + 3f, next, panelW() - 6f, shown, OogaTheme.corner(3f), 0x59000000);
			float sy = next + 2f;
			for (Setting<?> setting : m.getSettings()) {
				if (!setting.isVisible()) continue;
				if (sy > next + shown || sy > panelBottom) break;
				sy = drawSetting(g, setting, x + 9f, sy, panelW() - 18f);
			}
			Render2D.popAlpha();
			next += shown;
		}
		return next;
	}

	private float settingsHeight(Module m) {
		float h = 6f; // 2px above the first setting, 4px below the last
		for (Setting<?> s : m.getSettings()) {
			if (s.isVisible()) h += s instanceof NumberSetting ? 20f : 13f;
		}
		return h;
	}

	private float drawSetting(GuiGraphics g, Setting<?> setting, float x, float y, float w) {
		float scale = S7;
		boolean number = setting instanceof NumberSetting;
		float h = number ? 20f : 13f;
		float hv = hover(setting, x - 4, y, w + 8, h);
		int label = ColorUtil.lerp(OogaTheme.TEXT_MUTED, OogaTheme.TEXT_SECONDARY, 0.5f + 0.5f * hv);
		hoveredTipIf(setting, setting.getDescription(), x - 4, y, w + 8, h);

		if (setting instanceof BooleanSetting bool) {
			OogaFonts.draw(g, OogaFonts.trim(setting.getName(), Weight.REGULAR, scale, w - 20f), x, textY(y, h, 7), label, Weight.REGULAR, scale);
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
			OogaFonts.draw(g, OogaFonts.trim(setting.getName(), Weight.REGULAR, scale, w - vw - 6f), x, y + 2f, label, Weight.REGULAR, scale);
			OogaFonts.draw(g, value, x + w - vw, y + 2f, OogaTheme.GOLD_TEXT, Weight.SEMIBOLD, scale);
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
			float chipW = OogaFonts.width(mode.get(), Weight.SEMIBOLD, S7) + 9f;
			OogaFonts.draw(g, OogaFonts.trim(setting.getName(), Weight.REGULAR, scale, w - chipW - 6f), x, textY(y, h, 7), label, Weight.REGULAR, scale);
			Widgets.chip(g, x + w, y + h / 2f, mode.get(), OogaTheme.GOLD_TEXT, ColorUtil.lerp(OogaTheme.SURFACE_INSET, OogaTheme.SURFACE_CONTROL, hv),
					ColorUtil.lerp(OogaTheme.BORDER, OogaTheme.accent(0x55), hv), S7);
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
			g.enableScissor(Math.round(x), Math.round(y + HEADER_H + 1), Math.round(x + panelW()), Math.round(y + h));
			// Field.
			float fx = x + 6f, fy = y + HEADER_H + 6f, fw = panelW() - 12f, fh = 14f;
			boolean typing = !search.isEmpty();
			float focus = anim("searchFocus", 0f, 16f).update(typing ? 1f : 0f);
			if (focus > 0.01f) GlowRenderer.glow(g, fx, fy, fw, fh, OogaTheme.corner(3f), OogaTheme.GOLD, 0.3f * focus);
			Render2D.roundRect(g, fx, fy, fw, fh, OogaTheme.corner(3f), OogaTheme.SURFACE_INSET);
			Render2D.outline(g, fx, fy, fw, fh, OogaTheme.corner(3f), ColorUtil.lerp(OogaTheme.BORDER, OogaTheme.accent(0x99), focus));
			if (typing) {
				String shown = search.toString();
				while (shown.length() > 1 && OogaFonts.width(shown, Weight.REGULAR, S8) > fw - 12f) shown = shown.substring(1);
				OogaFonts.draw(g, shown, fx + 5f, fy + 3.6f, OogaTheme.TEXT, Weight.REGULAR, S8);
				if ((System.currentTimeMillis() / 530) % 2 == 0) {
					Render2D.rect(g, fx + 5.5f + OogaFonts.width(shown, Weight.REGULAR, S8), fy + 3f, 0.75f, 8f, OogaTheme.GOLD);
				}
			} else {
				OogaFonts.draw(g, "Type to search", fx + 5f, fy + 3.6f, OogaTheme.TEXT_MUTED, Weight.REGULAR, S8);
			}

			// Edit HUD button.
			float bx = x + 6f, by = fy + fh + 5f, bw = panelW() - 12f, bh = 14f;
			float bh2 = hover("editHud", bx, by, bw, bh);
			Render2D.roundRect(g, bx, by, bw, bh, OogaTheme.corner(3f), ColorUtil.lerp(0xFF15171C, OogaTheme.accent(0x26), bh2));
			Render2D.outline(g, bx, by, bw, bh, OogaTheme.corner(3f), ColorUtil.lerp(OogaTheme.BORDER, OogaTheme.accent(0x80), bh2));
			Icon.MOVE.draw(g, bx + 6f, by + 3.5f, 7f, ColorUtil.lerp(OogaTheme.TEXT_SECONDARY, OogaTheme.GOLD, bh2));
			OogaFonts.draw(g, "Edit HUD", bx + 17f, by + 3.8f, ColorUtil.lerp(OogaTheme.TEXT_SECONDARY, OogaTheme.TEXT, bh2), Weight.SEMIBOLD, S7);
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

	// ================================================================== tabs

	private float pageMaxScroll;

	/** A glowing pill of tabs across the top; the active tab's highlight slides between them. */
	private void drawTabs(GuiGraphics g, float appear) {
		if (appear <= 0.01f) return;
		Render2D.pushAlpha(appear);
		float pad = 12f, h = 17f, gap = 2f;
		float total = 4f;
		float[] widths = new float[Tab.values().length];
		for (Tab t : Tab.values()) {
			widths[t.ordinal()] = OogaFonts.width(t.label, Weight.SEMIBOLD, S8) + pad * 2;
			total += widths[t.ordinal()] + gap;
		}
		total -= gap;
		float x = (screenW() - total) / 2f;
		float y = 7f - (1f - appear) * 8f;
		GlowRenderer.glow(g, x, y, total, h + 4f, (h + 4f) / 2f, OogaTheme.GOLD, 0.5f, 3f);
		Render2D.roundRect(g, x, y, total, h + 4f, (h + 4f) / 2f, ClientSettings.surface(0xF00E0F12));
		Render2D.outline(g, x, y, total, h + 4f, (h + 4f) / 2f, OogaTheme.accent(0x70));

		float tx = x + 2f;
		for (Tab t : Tab.values()) {
			float w = widths[t.ordinal()];
			if (t == tab) {
				float targetX = tx;
				if (tabX.get() < 0) {
					tabX.snap(targetX);
					tabW.snap(w);
				}
				float ix = tabX.update(targetX), iw = tabW.update(w);
				GlowRenderer.glow(g, ix, y + 2f, iw, h, h / 2f, OogaTheme.GOLD, 0.9f, 4f);
				Render2D.roundRect(g, ix, y + 2f, iw, h, h / 2f, OogaTheme.GOLD);
				Render2D.horizontalGradient(g, ix + h / 2f, y + 2f, iw - h, h, 0, OogaTheme.ACCENT_2);
			}
			tx += w + gap;
		}
		tx = x + 2f;
		for (Tab t : Tab.values()) {
			float w = widths[t.ordinal()];
			float hv = hover("tab#" + t, tx, y + 2f, w, h);
			boolean active = t == tab;
			int color = active ? OogaTheme.ON_GOLD : ColorUtil.lerp(OogaTheme.TEXT_SECONDARY, OogaTheme.TEXT, hv);
			OogaFonts.drawCentered(g, t.label, tx + w / 2f, textY(y + 2f, h, 8), color, Weight.SEMIBOLD, S8);
			final Tab target = t;
			hits.add(new Hit(tx, y + 2f, w, h, (button, mx, my) -> {
				if (button != 0) return false;
				switchTab(target);
				return true;
			}));
			tx += w + gap;
		}
		Render2D.popAlpha();
	}

	private void switchTab(Tab target) {
		if (tab == target) return;
		tab = target;
		lastTab = target;
		pageScrollTarget = 0;
		pageScroll.snap(0);
		draggingPanel = null;
		draggingSlider = null;
	}

	/** Shared frame for the non-panel tabs: a centred glowing card that scrolls. */
	private void drawPage(GuiGraphics g, float appear) {
		if (appear <= 0.01f) return;
		float w = Math.min(300f, screenW() - 24f);
		float x = (screenW() - w) / 2f;
		float top = TOP + 2f, bottom = screenH() - 44f;
		float h = bottom - top;
		Render2D.pushAlpha(appear);
		GlowRenderer.glow(g, x, top, w, h, radius() + 1, OogaTheme.GOLD, 0.6f * config.panelGlow.getFloat() + 0.1f, 3f);
		Render2D.roundRect(g, x, top, w, h, radius() + 1, ClientSettings.surface(0xF00E0F12));
		Render2D.outline(g, x, top, w, h, radius() + 1, OogaTheme.accent(0x80));

		float scrollY = pageScroll.update(pageScrollTarget);
		g.enableScissor(Math.round(x), Math.round(top + 1), Math.round(x + w), Math.round(bottom - 1));
		float cx = x + 10f, cw = w - 20f;
		float y = top + 10f - scrollY;
		int firstHit = hits.size();
		float end = switch (tab) {
			case CONFIGS -> drawConfigsPage(g, cx, y, cw);
			case FINDS -> drawFindsPage(g, cx, y, cw);
			case THEME -> drawThemePage(g, cx, y, cw);
			default -> y;
		};
		g.disableScissor();
		// Anything scrolled out of the card can't be clicked; partly visible things only where shown.
		for (int i = hits.size() - 1; i >= firstHit; i--) {
			Hit hit = hits.get(i);
			float y0 = Math.max(hit.y(), top), y1 = Math.min(hit.y() + hit.h(), bottom);
			if (y1 <= y0) hits.remove(i);
			else hits.set(i, new Hit(hit.x(), y0, hit.w(), y1 - y0, hit.action()));
		}
		pageMaxScroll = Math.max(0f, end + scrollY - bottom + 10f);
		if (pageScrollTarget > pageMaxScroll) pageScrollTarget = pageMaxScroll;

		if (status != null && System.currentTimeMillis() < statusUntil) {
			float sw = OogaFonts.width(status, Weight.SEMIBOLD, S7) + 12f;
			float sy = bottom - 16f;
			Render2D.roundRect(g, x + (w - sw) / 2f, sy, sw, 12f, 6f, OogaTheme.accent(0xD0));
			OogaFonts.drawCentered(g, status, x + w / 2f, sy + 2.5f, OogaTheme.ON_GOLD, Weight.SEMIBOLD, S7);
		}
		Render2D.popAlpha();
	}

	private void flash(String message) {
		status = message;
		statusUntil = System.currentTimeMillis() + 2200;
	}

	private float heading(GuiGraphics g, String title, String subtitle, float x, float y, float w) {
		GlowRenderer.glow(g, x, y + 1f, OogaFonts.width(title, Weight.SEMIBOLD, 1f), 8f, 2f, OogaTheme.GOLD, 0.35f, 4f);
		OogaFonts.draw(g, title, x, y, OogaTheme.gradient(0.3f), Weight.SEMIBOLD, 1f);
		if (subtitle != null) OogaFonts.draw(g, subtitle, x, y + 11f, OogaTheme.TEXT_MUTED, Weight.REGULAR, S7);
		return y + (subtitle != null ? 22f : 13f);
	}

	/** A pill button; {@code primary} ones are filled with the accent and glow. Returns its width. */
	private float button(GuiGraphics g, String label, float x, float y, float w, boolean primary, Runnable action) {
		float h = 13f;
		if (w <= 0) w = OogaFonts.width(label, Weight.SEMIBOLD, S7) + 12f;
		float hv = hover("btn#" + label + "#" + Math.round(x) + "#" + Math.round(y), x, y, w, h);
		if (primary) {
			GlowRenderer.glow(g, x, y, w, h, h / 2f, OogaTheme.GOLD, 0.5f + 0.4f * hv, 3f);
			Render2D.roundRect(g, x, y, w, h, h / 2f, ColorUtil.lerp(OogaTheme.GOLD, OogaTheme.GOLD_BRIGHT, hv));
		} else {
			Render2D.roundRect(g, x, y, w, h, h / 2f, ColorUtil.lerp(OogaTheme.SURFACE_CONTROL, OogaTheme.accent(0x40), hv));
			Render2D.outline(g, x, y, w, h, h / 2f, ColorUtil.lerp(OogaTheme.BORDER, OogaTheme.accent(0x90), hv));
		}
		OogaFonts.drawCentered(g, label, x + w / 2f, textY(y, h, 7), primary ? OogaTheme.ON_GOLD : OogaTheme.TEXT, Weight.SEMIBOLD, S7);
		hits.add(new Hit(x, y, w, h, (b, mx, my) -> {
			if (b != 0) return false;
			action.run();
			return true;
		}));
		return w;
	}

	private float card(GuiGraphics g, float x, float y, float w, float h, float hover) {
		Render2D.roundRect(g, x, y, w, h, OogaTheme.corner(4f), ColorUtil.lerp(0x0DFFFFFF, OogaTheme.accent(0x18), hover));
		Render2D.outline(g, x, y, w, h, OogaTheme.corner(4f), ColorUtil.lerp(OogaTheme.BORDER, OogaTheme.accent(0x60), hover));
		return h;
	}

	private static String ago(long millis) {
		if (millis <= 0) return "";
		long s = (System.currentTimeMillis() - millis) / 1000;
		if (s < 60) return "just now";
		if (s < 3600) return s / 60 + "m ago";
		if (s < 86400) return s / 3600 + "h ago";
		return s / 86400 + "d ago";
	}

	// ------------------------------------------------------------------ configs

	private void saveConfig(String name) {
		String clean = dev.ooga.client.config.Profiles.clean(name);
		if (clean.isEmpty()) {
			flash("Type a name first");
			return;
		}
		if (dev.ooga.client.config.Profiles.save(clean)) {
			flash("Saved " + clean);
			configName.setLength(0);
		} else {
			flash("Couldn't save " + clean);
		}
	}

	private float drawConfigsPage(GuiGraphics g, float x, float y, float w) {
		y = heading(g, "Configs", "Save and load your modules, keybinds and settings.", x, y, w);

		// Name field and save button.
		float fh = 14f, bw = 44f;
		float fw = w - bw - 5f;
		boolean typing = !configName.isEmpty();
		GlowRenderer.glow(g, x, y, fw, fh, OogaTheme.corner(3f), OogaTheme.GOLD, 0.35f, 3f);
		Render2D.roundRect(g, x, y, fw, fh, OogaTheme.corner(3f), OogaTheme.SURFACE_INSET);
		Render2D.outline(g, x, y, fw, fh, OogaTheme.corner(3f), OogaTheme.accent(0x90));
		String shown = typing ? configName.toString() : "Type a name, then Save";
		OogaFonts.draw(g, shown, x + 5f, y + 3.6f, typing ? OogaTheme.TEXT : OogaTheme.TEXT_MUTED, Weight.REGULAR, S8);
		if ((System.currentTimeMillis() / 530) % 2 == 0) {
			float cx = x + 5.5f + (typing ? OogaFonts.width(shown, Weight.REGULAR, S8) : 0f);
			Render2D.rect(g, cx, y + 3f, 0.75f, 8f, OogaTheme.GOLD);
		}
		button(g, "Save", x + fw + 5f, y + 0.5f, bw, true, () -> saveConfig(configName.toString()));
		y += fh + 12f;

		// Presets.
		OogaFonts.draw(g, "PRESETS", x, y, OogaTheme.TEXT_MUTED, Weight.SEMIBOLD, S7);
		y += 10f;
		float px = x;
		for (dev.ooga.client.config.Presets.Preset preset : dev.ooga.client.config.Presets.ALL) {
			float pw = OogaFonts.width(preset.name(), Weight.SEMIBOLD, S7) + 12f;
			if (px + pw > x + w) {
				px = x;
				y += 16f;
			}
			float bx = px;
			hoveredTipIf("preset#" + preset.name(), preset.description(), bx, y, pw, 13f);
			button(g, preset.name(), bx, y, pw, false, () -> {
				dev.ooga.client.config.Presets.apply(preset);
				flash(preset.name() + " applied");
			});
			px += pw + 5f;
		}
		y += 22f;

		// Saved configs.
		List<dev.ooga.client.config.Profiles.Entry> saved = dev.ooga.client.config.Profiles.list();
		OogaFonts.draw(g, "SAVED  " + saved.size(), x, y, OogaTheme.TEXT_MUTED, Weight.SEMIBOLD, S7);
		float folderW = OogaFonts.width("Open Folder", Weight.SEMIBOLD, S7) + 12f;
		button(g, "Open Folder", x + w - folderW, y - 3f, folderW, false, () -> {
			try {
				java.nio.file.Files.createDirectories(dev.ooga.client.config.Profiles.DIR);
			} catch (java.io.IOException ignored) {
				// Opening will just fail quietly.
			}
			net.minecraft.util.Util.getPlatform().openPath(dev.ooga.client.config.Profiles.DIR);
		});
		y += 14f;
		if (saved.isEmpty()) {
			OogaFonts.draw(g, "No saved configs yet.", x, y + 2f, OogaTheme.TEXT_SECONDARY, Weight.REGULAR, S8);
			y += 14f;
		}
		for (dev.ooga.client.config.Profiles.Entry entry : saved) {
			float rh = 22f;
			float hv = hover("cfg#" + entry.name(), x, y, w, rh);
			card(g, x, y, w, rh, hv);
			OogaFonts.draw(g, entry.name(), x + 7f, y + 3.5f, OogaTheme.TEXT, Weight.SEMIBOLD, S8);
			OogaFonts.draw(g, ago(entry.modified()), x + 7f, y + 12.5f, OogaTheme.TEXT_MUTED, Weight.REGULAR, S7);
			boolean confirming = entry.name().equals(confirmDelete) && System.currentTimeMillis() < confirmUntil;
			String del = confirming ? "Sure?" : "Delete";
			float dw = OogaFonts.width(del, Weight.SEMIBOLD, S7) + 12f;
			float lw = OogaFonts.width("Load", Weight.SEMIBOLD, S7) + 14f;
			float sw = OogaFonts.width("Overwrite", Weight.SEMIBOLD, S7) + 12f;
			float bx = x + w - 5f - dw;
			button(g, del, bx, y + 4.5f, dw, false, () -> {
				if (entry.name().equals(confirmDelete) && System.currentTimeMillis() < confirmUntil) {
					dev.ooga.client.config.Profiles.delete(entry.name());
					confirmDelete = null;
					flash("Deleted " + entry.name());
				} else {
					confirmDelete = entry.name();
					confirmUntil = System.currentTimeMillis() + 3000;
				}
			});
			bx -= sw + 4f;
			button(g, "Overwrite", bx, y + 4.5f, sw, false, () -> saveConfig(entry.name()));
			bx -= lw + 4f;
			button(g, "Load", bx, y + 4.5f, lw, true, () -> flash(dev.ooga.client.config.Profiles.load(entry.name()) ? "Loaded " + entry.name() : "Couldn't load " + entry.name()));
			y += rh + 4f;
		}
		return y;
	}

	// ------------------------------------------------------------------ finds & waypoints

	private float drawFindsPage(GuiGraphics g, float x, float y, float w) {
		y = heading(g, "Finds", "Everything the base finders turned up this session.", x, y, w);
		List<dev.ooga.client.world.Finds.Find> finds = dev.ooga.client.world.Finds.recent();
		var player = minecraft == null ? null : minecraft.player;
		if (finds.isEmpty()) {
			OogaFonts.draw(g, "Nothing yet. Turn on Spawner Finder, Storage ESP or the other finders.", x, y + 2f, OogaTheme.TEXT_SECONDARY, Weight.REGULAR, S7);
			y += 14f;
		}
		for (dev.ooga.client.world.Finds.Find find : finds) {
			float rh = 22f;
			float hv = hover("find#" + find.type() + find.pos().asLong(), x, y, w, rh);
			card(g, x, y, w, rh, hv);
			int dot = switch (find.type()) {
				case "Spawner" -> 0xFFE8594A;
				case "Tunnel" -> 0xFF5FB3F0;
				case "Stash" -> 0xFFD9A441;
				default -> OogaTheme.GOLD;
			};
			GlowRenderer.glowCircle(g, x + 8f, y + 11f, 2.2f, dot, 0.8f);
			Render2D.circle(g, x + 8f, y + 11f, 2.2f, dot);
			String what = find.detail() == null || find.detail().isEmpty() ? find.type() : find.type() + " · " + find.detail();
			OogaFonts.draw(g, OogaFonts.trim(what, Weight.SEMIBOLD, S8, w - 110f), x + 15f, y + 3.5f, OogaTheme.TEXT, Weight.SEMIBOLD, S8);
			String where = find.pos().getX() + ", " + find.pos().getY() + ", " + find.pos().getZ();
			if (player != null) where += "   " + Math.round(Math.sqrt(find.pos().distToCenterSqr(player.position()))) + "m";
			OogaFonts.draw(g, where, x + 15f, y + 12.5f, OogaTheme.TEXT_MUTED, Weight.REGULAR, S7);
			float cw = OogaFonts.width("Copy", Weight.SEMIBOLD, S7) + 12f;
			float ww = OogaFonts.width("Waypoint", Weight.SEMIBOLD, S7) + 12f;
			String coords = find.pos().getX() + " " + find.pos().getY() + " " + find.pos().getZ();
			button(g, "Copy", x + w - 5f - cw, y + 4.5f, cw, false, () -> {
				minecraft.keyboardHandler.setClipboard(coords);
				flash("Copied " + coords);
			});
			button(g, "Waypoint", x + w - 9f - cw - ww, y + 4.5f, ww, true, () -> {
				dev.ooga.client.world.Waypoints.add(find.type() + " " + find.pos().getX() + "," + find.pos().getZ(), find.pos());
				flash("Waypoint added");
			});
			y += rh + 4f;
		}

		y += 8f;
		y = heading(g, "Waypoints", "Here, on this server and dimension. Beams show in the world.", x, y, w);
		float aw = OogaFonts.width("Add Here", Weight.SEMIBOLD, S7) + 12f;
		button(g, "Add Here", x + w - aw, y - 20f, aw, true, () -> {
			if (minecraft.player == null) return;
			var wp = dev.ooga.client.world.Waypoints.add(dev.ooga.client.world.Waypoints.nextName("Waypoint"), minecraft.player.blockPosition());
			flash("Added " + wp.name);
		});
		var waypoints = dev.ooga.client.world.Waypoints.here();
		if (waypoints.isEmpty()) {
			OogaFonts.draw(g, "No waypoints here yet.", x, y + 2f, OogaTheme.TEXT_SECONDARY, Weight.REGULAR, S7);
			y += 14f;
		}
		for (var wp : waypoints) {
			float rh = 20f;
			float hv = hover("wp#" + wp.name, x, y, w, rh);
			card(g, x, y, w, rh, hv);
			GlowRenderer.glowCircle(g, x + 8f, y + 10f, 2.2f, wp.color, 0.8f);
			Render2D.circle(g, x + 8f, y + 10f, 2.2f, wp.color);
			OogaFonts.draw(g, wp.name, x + 15f, y + 3f, OogaTheme.TEXT, Weight.SEMIBOLD, S8);
			String where = wp.x + ", " + wp.y + ", " + wp.z;
			if (player != null) where += "   " + Math.round(Math.sqrt(wp.pos().distToCenterSqr(player.position()))) + "m";
			OogaFonts.draw(g, where, x + 15f, y + 11.5f, OogaTheme.TEXT_MUTED, Weight.REGULAR, S7);
			float dw = OogaFonts.width("Delete", Weight.SEMIBOLD, S7) + 12f;
			String name = wp.name;
			button(g, "Delete", x + w - 5f - dw, y + 3.5f, dw, false, () -> {
				dev.ooga.client.world.Waypoints.remove(name);
				flash("Removed " + name);
			});
			y += rh + 4f;
		}
		return y;
	}

	// ------------------------------------------------------------------ theme

	private float drawThemePage(GuiGraphics g, float x, float y, float w) {
		y = heading(g, "Theme", "Accent colour, glow and menu effects.", x, y, w);
		ClientSettings settings = ModuleManager.get().get(ClientSettings.class);
		OogaTheme.Accent[] accents = OogaTheme.Accent.values();
		float cell = w / 6f;
		for (int i = 0; i < accents.length; i++) {
			OogaTheme.Accent accent = accents[i];
			float cx = x + (i % 6) * cell + cell / 2f;
			float cy = y + (i / 6) * 30f + 9f;
			boolean active = settings.accent.is(accent.label);
			float hv = hover("accent#" + accent.label, cx - cell / 2f, cy - 9f, cell, 28f);
			int color = accent == OogaTheme.Accent.CHROMA ? OogaTheme.hsv((System.currentTimeMillis() % 4000) / 4000f, 0.55f, 0.97f) : 0xFF000000 | accent.base();
			GlowRenderer.glowCircle(g, cx, cy, 6f, color, active ? 1f : 0.3f + 0.5f * hv);
			if (active) Render2D.circle(g, cx, cy, 8f, 0xFFFFFFFF);
			Render2D.circle(g, cx, cy, active ? 6.5f : 6f + hv, color);
			OogaFonts.drawCentered(g, accent.label, cx, cy + 9.5f, active ? OogaTheme.TEXT : OogaTheme.TEXT_MUTED, Weight.REGULAR, 0.62f);
			hits.add(new Hit(cx - cell / 2f, cy - 9f, cell, 28f, (b, mx, my) -> {
				if (b != 0) return false;
				settings.accent.set(accent.label);
				return true;
			}));
		}
		y += ((accents.length + 5) / 6) * 30f + 6f;

		y = settingsBlock(g, "GLOW & COLOUR", settings, x, y, w);
		y = settingsBlock(g, "MENU", config, x, y + 6f, w);
		return y;
	}

	private float settingsBlock(GuiGraphics g, String title, Module module, float x, float y, float w) {
		OogaFonts.draw(g, title, x, y, OogaTheme.TEXT_MUTED, Weight.SEMIBOLD, S7);
		y += 10f;
		float h = settingsHeight(module);
		card(g, x, y, w, h, 0f);
		float sy = y + 2f;
		for (Setting<?> setting : module.getSettings()) {
			if (!setting.isVisible()) continue;
			sy = drawSetting(g, setting, x + 7f, sy, w - 14f);
		}
		return y + h + 4f;
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
			e.x = Math.max(0, Math.min(screenW() - panelW(), mx - dragDX));
			e.y = Math.max(TOP, Math.min(screenH() - HEADER_H, my - dragDY));
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
		if (tab != Tab.MAIN) {
			pageScrollTarget = Math.max(0f, Math.min(pageMaxScroll, pageScrollTarget - (float) scrollY * 20f));
			return true;
		}
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
		if (tab == Tab.CONFIGS && key != GLFW.GLFW_KEY_ESCAPE) {
			if (key == GLFW.GLFW_KEY_BACKSPACE && !configName.isEmpty()) {
				if ((event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) configName.setLength(0);
				else configName.setLength(configName.length() - 1);
				return true;
			}
			if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
				saveConfig(configName.toString());
				return true;
			}
		}
		if (tab != Tab.MAIN && key == config.getKey()) {
			onClose();
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
		if (tab == Tab.CONFIGS) {
			if (configName.length() < 24 && (Character.isLetterOrDigit(codepoint) || codepoint == ' ' || codepoint == '_' || codepoint == '-')) {
				configName.appendCodePoint(codepoint);
			}
			return true;
		}
		if (tab != Tab.MAIN) return false;
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
