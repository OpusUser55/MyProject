package dev.ooga.client.ui.clickgui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ooga.client.OogaClient;
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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Ooga menu: one centred window with a category rail on the left, a search field in the
 * header and module cards that expand in place to reveal their settings.
 *
 * <p>Rendering is immediate-mode. Each frame lays the window out, draws it and records
 * clickable regions ({@link Hit}) in window-local coordinates; input handlers map the mouse
 * into the same space and dispatch to those regions. That keeps layout and hit-testing
 * from ever disagreeing, even mid-animation.
 */
public class ClickGuiScreen extends Screen {
	// Window layout (GUI units, before the user's menu scale).
	private static final float WIDTH = 460f;
	private static final float HEIGHT = 292f;
	private static final float HEADER = 34f;
	private static final float SIDEBAR = 112f;
	private static final float PAD = 10f;
	private static final float CARD_GAP = 5f;
	private static final float CATEGORY_ROW = 22f;
	private static final float SEARCH_W = 136f;
	private static final float SEARCH_H = 17f;

	/** Remembered across openings; null until the menu is first opened. */
	private static Category lastCategory;

	private final ClickGuiModule config = ModuleManager.get().get(ClickGuiModule.class);
	private final Anim open = new Anim(0f, 14f);
	private final Anim categoryIndicator = new Anim(-1f, 18f);
	private final Anim scroll = new Anim(0f, 20f);
	private final Anim searchFocus = new Anim(0f, 16f);
	private final Map<Module, CardState> cards = new IdentityHashMap<>();
	/** Keyed by settings, categories and stable string ids, hence equals-based. */
	private final Map<Object, Anim> hovers = new HashMap<>();
	private final List<Hit> hits = new ArrayList<>();

	private Category category = lastCategory != null ? lastCategory : firstPopulatedCategory();
	private final StringBuilder search = new StringBuilder();
	private boolean searchFocused;
	private boolean closing;
	private float scrollTarget;
	private float maxScroll;

	private float centerX = Float.NaN;
	private float centerY;
	private boolean draggingWindow;
	private double dragOffsetX;
	private double dragOffsetY;

	private NumberSetting draggingSlider;
	private float sliderX;
	private float sliderW;
	private Module binding;

	private Setting<?> hoveredSetting;
	private long hoverStart;

	// Mouse position in window-local space for the current frame.
	private float localMouseX;
	private float localMouseY;

	private static final class CardState {
		final Anim hover = new Anim(0f, 18f);
		final Anim enabled = new Anim(0f, 16f);
		final Anim expand = new Anim(0f, 14f);
		boolean expanded;
	}

	/** A clickable rectangle in window-local coordinates. */
	private record Hit(float x, float y, float w, float h, ClickAction action) {
		boolean contains(float mx, float my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	@FunctionalInterface
	private interface ClickAction {
		/** @return true if the click was handled. */
		boolean click(int button, float localX, float localY);
	}

	public ClickGuiScreen() {
		super(Component.literal("Ooga"));
	}

	private static Category firstPopulatedCategory() {
		for (Category c : Category.values()) {
			if (!ModuleManager.get().getModules(c).isEmpty()) return c;
		}
		return Category.values()[0];
	}

	// ================================================================== geometry

	private float uiScale() {
		return config.scale.getFloat() * (0.965f + 0.035f * Anim.ease(open.get()));
	}

	private float windowWidth() {
		return Math.min(WIDTH, (width - 16f) / config.scale.getFloat());
	}

	private float windowHeight() {
		return Math.min(HEIGHT, (height - 16f) / config.scale.getFloat());
	}

	private float toLocalX(double screenX) {
		return (float) ((screenX - centerX) / uiScale() + windowWidth() / 2f);
	}

	private float toLocalY(double screenY) {
		return (float) ((screenY - centerY) / uiScale() + windowHeight() / 2f);
	}

	private void clampWindow() {
		float halfW = windowWidth() * config.scale.getFloat() / 2f;
		float halfH = windowHeight() * config.scale.getFloat() / 2f;
		centerX = Math.max(halfW + 4, Math.min(width - halfW - 4, centerX));
		centerY = Math.max(halfH + 4, Math.min(height - halfH - 4, centerY));
	}

	// ================================================================== rendering

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		if (!config.dim.get()) return;
		float t = Anim.ease(open.get());
		Render2D.verticalGradient(g, 0, 0, width, height, ColorUtil.fade(0x70050507, t), ColorUtil.fade(0xA0050507, t));
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		if (Float.isNaN(centerX)) {
			centerX = width / 2f;
			centerY = height / 2f;
		}
		clampWindow();

		float t = open.update(closing ? 0f : 1f);
		if (closing && t <= 0.02f) {
			finishClose();
			return;
		}

		hits.clear();
		localMouseX = toLocalX(mouseX);
		localMouseY = toLocalY(mouseY);
		Setting<?> previousHovered = hoveredSetting;
		hoveredSetting = null;

		float w = windowWidth();
		float h = windowHeight();
		float s = uiScale();

		g.pose().pushMatrix();
		g.pose().translate(centerX, centerY);
		g.pose().scale(s, s);
		g.pose().translate(-w / 2f, -h / 2f);
		Render2D.pushAlpha(Anim.ease(t));

		drawWindow(g, w, h);
		drawHeader(g, w);
		drawSidebar(g, h);
		drawContent(g, w, h);

		Render2D.popAlpha();
		g.pose().popMatrix();

		if (hoveredSetting != previousHovered) hoverStart = System.currentTimeMillis();
		if (hoveredSetting != null && draggingSlider == null && System.currentTimeMillis() - hoverStart > 450) {
			Render2D.pushAlpha(Anim.ease(t));
			Widgets.tooltip(g, mouseX, mouseY, hoveredSetting.getDescription(), width, height);
			Render2D.popAlpha();
		}
	}

	private void drawWindow(GuiGraphics g, float w, float h) {
		// A faint gold halo frames the window without competing with its contents.
		GlowRenderer.glow(g, 0, 0, w, h, OogaTheme.RADIUS_WINDOW, OogaTheme.GOLD, 0.18f, 6f);
		Render2D.roundRect(g, 0, 0, w, h, OogaTheme.RADIUS_WINDOW, OogaTheme.SURFACE_WINDOW);
		// Sidebar surface: rounded on the left, square where it meets the content.
		Render2D.roundRect(g, 0, 0, SIDEBAR + OogaTheme.RADIUS_WINDOW * 2, h, OogaTheme.RADIUS_WINDOW, OogaTheme.SURFACE_SIDEBAR);
		Render2D.rect(g, SIDEBAR, 0, OogaTheme.RADIUS_WINDOW * 2, h, OogaTheme.SURFACE_WINDOW);
		Render2D.rect(g, SIDEBAR, HEADER, 1f, h - HEADER, OogaTheme.DIVIDER);
		Render2D.rect(g, 0, HEADER, w, 1f, OogaTheme.DIVIDER);
		// A short gold rule under the brand ties the header to the accent colour.
		Render2D.rect(g, PAD, HEADER - 1f, 22f, 1f, ColorUtil.fade(OogaTheme.GOLD, 0.8f));
		Render2D.outline(g, 0, 0, w, h, OogaTheme.RADIUS_WINDOW, OogaTheme.BORDER);
	}

	private void drawHeader(GuiGraphics g, float w) {
		// Hits are tested newest-first, so this catch-all goes in before the header's controls:
		// anywhere in the header that isn't a control drags the window.
		hits.add(new Hit(0, 0, w, HEADER, (button, mx, my) -> {
			if (button != 0) return false;
			searchFocused = false;
			draggingWindow = true;
			return true;
		}));

		// Brand.
		float cy = HEADER / 2f;
		GlowRenderer.glow(g, PAD + 1.5f, cy - 3.5f, 7f, 7f, 3.5f, OogaTheme.GOLD, 0.7f);
		Render2D.diamond(g, PAD + 5f, cy, 5.2f, 0x59F2C14E);
		Render2D.diamond(g, PAD + 5f, cy, 2.6f, OogaTheme.GOLD_BRIGHT);
		float tx = PAD + 15f;
		for (char c : "OOGA".toCharArray()) {
			String ch = String.valueOf(c);
			OogaFonts.draw(g, ch, tx, cy - 5f, OogaTheme.TEXT, Weight.DISPLAY);
			tx += OogaFonts.width(ch, Weight.DISPLAY) + 1.2f;
		}
		String version = "v" + OogaClient.VERSION;
		OogaFonts.draw(g, version, tx + 4f, cy - 2.4f, OogaTheme.TEXT_MUTED, Weight.REGULAR, 0.72f);

		// "Edit HUD" ghost button on the right.
		String hudLabel = "Edit HUD";
		float bw = OogaFonts.width(hudLabel, Weight.SEMIBOLD, 0.8f) + 22f;
		float bh = SEARCH_H;
		float bx = w - PAD - bw;
		float by = cy - bh / 2f;
		float bHover = hover("editHud", bx, by, bw, bh);
		Render2D.roundRect(g, bx, by, bw, bh, OogaTheme.RADIUS_CONTROL + 1, ColorUtil.lerp(OogaTheme.SURFACE_CARD, OogaTheme.SURFACE_CARD_HOVER, bHover));
		Render2D.outline(g, bx, by, bw, bh, OogaTheme.RADIUS_CONTROL + 1, ColorUtil.lerp(OogaTheme.BORDER, 0x66F2C14E, bHover));
		Icon.MOVE.draw(g, bx + 6f, cy - 3.5f, 7f, ColorUtil.lerp(OogaTheme.TEXT_SECONDARY, OogaTheme.GOLD, bHover));
		OogaFonts.draw(g, hudLabel, bx + 16f, cy - 3.3f, OogaTheme.TEXT_SECONDARY, Weight.SEMIBOLD, 0.8f);
		hits.add(new Hit(bx, by, bw, bh, (button, mx, my) -> {
			if (button != 0) return false;
			minecraft.setScreen(new HudEditorScreen(this));
			return true;
		}));

		// Search field.
		float sx = bx - 8f - SEARCH_W;
		float sy = cy - SEARCH_H / 2f;
		float focus = searchFocus.update(searchFocused ? 1f : 0f);
		float sHover = hover("search", sx, sy, SEARCH_W, SEARCH_H);
		if (focus > 0.01f) GlowRenderer.glow(g, sx, sy, SEARCH_W, SEARCH_H, OogaTheme.RADIUS_CONTROL + 1, OogaTheme.GOLD, 0.35f * focus);
		Render2D.roundRect(g, sx, sy, SEARCH_W, SEARCH_H, OogaTheme.RADIUS_CONTROL + 1, OogaTheme.SURFACE_INSET);
		int border = ColorUtil.lerp(ColorUtil.lerp(OogaTheme.BORDER, OogaTheme.BORDER_STRONG, sHover), 0x99F2C14E, focus);
		Render2D.outline(g, sx, sy, SEARCH_W, SEARCH_H, OogaTheme.RADIUS_CONTROL + 1, border);
		Icon.SEARCH.draw(g, sx + 6f, cy - 3.5f, 7f, ColorUtil.lerp(OogaTheme.TEXT_MUTED, OogaTheme.GOLD, focus));
		float textX = sx + 17f;
		float maxText = SEARCH_W - 22f;
		if (search.isEmpty()) {
			OogaFonts.draw(g, searchFocused ? "" : "Search modules", textX, cy - 3.4f, OogaTheme.TEXT_MUTED, Weight.REGULAR, 0.85f);
		} else {
			String shown = search.toString();
			// Keep the end of long queries visible, where the caret is.
			while (shown.length() > 1 && OogaFonts.width(shown, Weight.REGULAR, 0.85f) > maxText) shown = shown.substring(1);
			OogaFonts.draw(g, shown, textX, cy - 3.4f, OogaTheme.TEXT, Weight.REGULAR, 0.85f);
			textX += OogaFonts.width(shown, Weight.REGULAR, 0.85f);
		}
		if (searchFocused && (System.currentTimeMillis() / 530) % 2 == 0) {
			Render2D.rect(g, textX + 0.5f, cy - 4f, 0.75f, 8f, OogaTheme.GOLD);
		}
		hits.add(new Hit(sx, sy, SEARCH_W, SEARCH_H, (button, mx, my) -> {
			if (button == 1) search.setLength(0);
			searchFocused = true;
			return true;
		}));

	}

	private void drawSidebar(GuiGraphics g, float h) {
		float y = HEADER + PAD;
		OogaFonts.draw(g, "CATEGORIES", PAD + 2f, y, OogaTheme.TEXT_MUTED, Weight.SEMIBOLD, 0.65f);
		y += 10f;

		Category[] categories = Category.values();
		boolean searching = !search.isEmpty();
		int selectedIndex = searching ? -1 : category.ordinal();
		float firstRowY = y;

		// Sliding selection pill behind the active category.
		if (selectedIndex >= 0) {
			float targetY = firstRowY + selectedIndex * CATEGORY_ROW;
			if (categoryIndicator.get() < 0) categoryIndicator.snap(targetY);
			float iy = categoryIndicator.update(targetY);
			float ix = PAD - 3f;
			float iw = SIDEBAR - PAD * 2 + 6f;
			float ih = CATEGORY_ROW - 3f;
			Render2D.roundRect(g, ix, iy, iw, ih, OogaTheme.RADIUS_CARD, OogaTheme.GOLD_TINT);
			Render2D.outline(g, ix, iy, iw, ih, OogaTheme.RADIUS_CARD, 0x2EF2C14E);
			GlowRenderer.glow(g, ix, iy + 4f, 2f, ih - 8f, 1f, OogaTheme.GOLD, 0.9f, 4f);
			Render2D.roundRect(g, ix, iy + 4f, 2f, ih - 8f, 1f, OogaTheme.GOLD);
		}

		for (Category c : categories) {
			float rowY = firstRowY + c.ordinal() * CATEGORY_ROW;
			float rx = PAD - 3f;
			float rw = SIDEBAR - PAD * 2 + 6f;
			float rh = CATEGORY_ROW - 3f;
			boolean selected = c.ordinal() == selectedIndex;
			float hv = hover(c, rx, rowY, rw, rh);
			if (!selected && hv > 0.01f) Render2D.roundRect(g, rx, rowY, rw, rh, OogaTheme.RADIUS_CARD, ColorUtil.fade(0x0DFFFFFF, hv));

			int textColor = selected ? OogaTheme.GOLD_TEXT : ColorUtil.lerp(OogaTheme.TEXT_SECONDARY, OogaTheme.TEXT, hv);
			float cy = rowY + rh / 2f;
			c.getIcon().draw(g, rx + 9f, cy - 4f, 8f, selected ? OogaTheme.GOLD : textColor);
			OogaFonts.draw(g, c.getDisplayName(), rx + 23f, cy - 4f, textColor, selected ? Weight.SEMIBOLD : Weight.REGULAR);

			int active = 0;
			for (Module m : ModuleManager.get().getModules(c)) if (m.isEnabled() && !m.isSettingsOnly()) active++;
			if (active > 0) {
				String count = Integer.toString(active);
				float cw = OogaFonts.width(count, Weight.SEMIBOLD, 0.7f);
				OogaFonts.draw(g, count, rx + rw - 8f - cw, cy - 2.8f, selected ? OogaTheme.GOLD : OogaTheme.TEXT_MUTED, Weight.SEMIBOLD, 0.7f);
			}

			hits.add(new Hit(rx, rowY, rw, rh, (button, mx, my) -> {
				if (button != 0) return false;
				selectCategory(c);
				return true;
			}));
		}

		// Footer: live summary.
		int enabled = 0;
		for (Module m : ModuleManager.get().getModules()) if (m.isEnabled() && !m.isSettingsOnly() && !m.isHiddenFromList()) enabled++;
		float fy = h - PAD - 8f;
		Render2D.circle(g, PAD + 2.5f, fy + 3f, 2f, enabled > 0 ? OogaTheme.GOLD : OogaTheme.TEXT_MUTED);
		OogaFonts.draw(g, enabled + " active", PAD + 8f, fy, OogaTheme.TEXT_MUTED, Weight.REGULAR, 0.75f);
	}

	private void selectCategory(Category c) {
		if (category != c || !search.isEmpty()) {
			scrollTarget = 0;
			scroll.snap(0);
		}
		category = c;
		lastCategory = c;
		search.setLength(0);
		searchFocused = false;
	}

	private List<Module> visibleModules() {
		if (search.isEmpty()) return ModuleManager.get().getModules(category);
		String query = search.toString().toLowerCase(Locale.ROOT).trim();
		List<Module> result = new ArrayList<>();
		// Name matches first, then description matches, so the best hit is on top.
		for (Module m : ModuleManager.get().getModules()) {
			if (m.getName().toLowerCase(Locale.ROOT).contains(query)) result.add(m);
		}
		for (Module m : ModuleManager.get().getModules()) {
			if (!result.contains(m) && m.getDescription().toLowerCase(Locale.ROOT).contains(query)) result.add(m);
		}
		return result;
	}

	private void drawContent(GuiGraphics g, float w, float h) {
		float cx = SIDEBAR + 1f;
		float cw = w - cx;
		List<Module> modules = visibleModules();

		// Section heading.
		boolean searching = !search.isEmpty();
		String title = searching ? "Search" : category.getDisplayName();
		int enabledCount = 0;
		for (Module m : modules) if (m.isEnabled() && !m.isSettingsOnly()) enabledCount++;
		String subtitle = searching
				? modules.size() + (modules.size() == 1 ? " result" : " results")
				: modules.size() + " modules  ·  " + enabledCount + " enabled";
		float hy = HEADER + PAD;
		OogaFonts.draw(g, title, cx + PAD, hy, OogaTheme.TEXT, Weight.DISPLAY);
		float titleW = OogaFonts.width(title, Weight.DISPLAY);
		OogaFonts.draw(g, subtitle, cx + PAD + titleW + 7f, hy + 2.4f, OogaTheme.TEXT_MUTED, Weight.REGULAR, 0.75f);

		float listTop = hy + 16f;
		float listBottom = h - 6f;
		float listX = cx + PAD;
		float listW = cw - PAD * 2;

		// Measure total height to clamp scrolling.
		float total = 0;
		for (Module m : modules) total += cardHeight(m) + CARD_GAP;
		maxScroll = Math.max(0, total - (listBottom - listTop));
		scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
		float offset = scroll.update(scrollTarget);

		g.enableScissor(Math.round(cx), Math.round(listTop - 2), Math.round(w - 1), Math.round(listBottom));
		float y = listTop - offset;
		boolean mouseInList = localMouseX >= cx && localMouseX < w && localMouseY >= listTop && localMouseY < listBottom;
		for (Module m : modules) {
			float ch = cardHeight(m);
			if (y + ch >= listTop - 2 && y <= listBottom) {
				drawCard(g, m, listX, y, listW, ch, mouseInList, searching);
			}
			y += ch + CARD_GAP;
		}
		if (modules.isEmpty()) {
			float ey = listTop + (listBottom - listTop) / 2f - 16f;
			Icon icon = searching ? Icon.SEARCH : category.getIcon();
			icon.draw(g, cx + cw / 2f - 6f, ey, 12f, OogaTheme.TEXT_MUTED);
			String headline = searching ? "No matches" : "Nothing here yet";
			String detail = searching ? "Try a different word, or press Esc to clear." : "Modules in this category will appear here.";
			OogaFonts.drawCentered(g, headline, cx + cw / 2f, ey + 19f, OogaTheme.TEXT_SECONDARY, Weight.SEMIBOLD, 1f);
			OogaFonts.drawCentered(g, detail, cx + cw / 2f, ey + 31f, OogaTheme.TEXT_MUTED, Weight.REGULAR, 0.78f);
		}
		g.disableScissor();

		// Soft fades at the list edges hint that there's more to scroll.
		if (offset > 0.5f) Render2D.verticalGradient(g, cx, listTop - 2, cw - 1, 8f, 0xCC0D0E11, 0x000D0E11);
		if (offset < maxScroll - 0.5f) Render2D.verticalGradient(g, cx, listBottom - 8f, cw - 1, 8f, 0x000D0E11, 0xCC0D0E11);
		if (maxScroll > 0) {
			float trackH = listBottom - listTop;
			float thumbH = Math.max(16f, trackH * trackH / (trackH + maxScroll));
			float thumbY = listTop + (trackH - thumbH) * (offset / maxScroll);
			Render2D.roundRect(g, w - 4.5f, thumbY, 2f, thumbH, 1f, 0x33FFFFFF);
		}

		final float top = listTop;
		final float bottom = listBottom;
		hits.add(new Hit(cx, top, cw, bottom - top, (button, mx, my) -> {
			searchFocused = false;
			return false;
		}));
	}

	// ------------------------------------------------------------------ module cards

	private float collapsedHeight() {
		return config.descriptions.get() ? 31f : 22f;
	}

	private float settingHeight(Setting<?> setting) {
		return setting instanceof NumberSetting ? 25f : 18f;
	}

	private float settingsHeight(Module m) {
		float total = 6f;
		for (Setting<?> s : m.getSettings()) if (s.isVisible()) total += settingHeight(s);
		return total + 4f;
	}

	private float cardHeight(Module m) {
		CardState state = cards.computeIfAbsent(m, k -> newCardState(m));
		float expand = Anim.ease(state.expand.get());
		return collapsedHeight() + (m.getSettings().isEmpty() ? 0 : settingsHeight(m) * expand);
	}

	private CardState newCardState(Module m) {
		CardState state = new CardState();
		state.enabled.snap(m.isEnabled() ? 1f : 0f);
		return state;
	}

	private void drawCard(GuiGraphics g, Module m, float x, float y, float w, float h, boolean mouseInList, boolean searching) {
		CardState state = cards.get(m);
		float collapsed = collapsedHeight();
		boolean overCard = mouseInList && inside(localMouseX, localMouseY, x, y, w, collapsed);
		float hv = state.hover.update(overCard ? 1f : 0f);
		float on = state.enabled.update(m.isEnabled() ? 1f : 0f);
		float expand = state.expand.update(state.expanded ? 1f : 0f);

		int fill = ColorUtil.lerp(OogaTheme.SURFACE_CARD, OogaTheme.SURFACE_CARD_HOVER, hv);
		Render2D.roundRect(g, x, y, w, h, OogaTheme.RADIUS_CARD, fill);
		int border = ColorUtil.lerp(OogaTheme.BORDER, 0x40F2C14E, on * 0.8f);
		Render2D.outline(g, x, y, w, h, OogaTheme.RADIUS_CARD, border);
		if (on > 0.01f) {
			// Gold spine marks enabled modules at a glance.
			Render2D.roundRect(g, x + 0.5f, y + 6f, 2f, collapsed - 12f, 1f, ColorUtil.fade(OogaTheme.GOLD, on));
		}

		// Text block.
		float textX = x + 10f;
		float controlsLeft = x + w - 10f;
		boolean hasToggle = !m.isSettingsOnly();
		boolean hasSettings = !m.getSettings().isEmpty();
		float toggleW = 20f;
		float toggleH = 11f;
		float cy = y + collapsed / 2f;

		// Card body: left click toggles (or expands settings-only modules), right click expands,
		// middle click binds. Added before the controls so the controls take priority.
		hits.add(new Hit(x, y, w, collapsed, (button, mx, my) -> {
			if (button == 1 || (button == 0 && !hasToggle)) {
				if (hasSettings) state.expanded = !state.expanded;
				return true;
			}
			if (button == 0) {
				m.toggle();
				return true;
			}
			if (button == 2 && hasToggle) {
				binding = m;
				return true;
			}
			return false;
		}));

		// Right-side controls, laid out right to left.
		if (hasToggle) {
			float tx = controlsLeft - toggleW;
			float tHover = hover(m.getName() + "#toggle", tx, cy - toggleH / 2f, toggleW, toggleH);
			Widgets.toggle(g, tx, cy - toggleH / 2f, toggleW, toggleH, on, tHover);
			hits.add(new Hit(tx - 2, cy - toggleH / 2f - 2, toggleW + 4, toggleH + 4, (button, mx, my) -> {
				if (button != 0) return false;
				m.toggle();
				return true;
			}));
			controlsLeft = tx - 8f;
		}
		if (hasSettings) {
			float iconSize = 8f;
			float ix = controlsLeft - iconSize;
			float iHover = hover(m.getName() + "#expand", ix - 2, cy - 6, iconSize + 4, 12);
			int iconColor = ColorUtil.lerp(ColorUtil.lerp(OogaTheme.TEXT_MUTED, OogaTheme.TEXT_SECONDARY, Math.max(iHover, hv * 0.6f)), OogaTheme.GOLD, expand);
			(expand > 0.5f ? Icon.CHEVRON : Icon.CHEVRON_RIGHT).draw(g, ix, cy - iconSize / 2f, iconSize, iconColor);
			hits.add(new Hit(ix - 3, cy - 7, iconSize + 6, 14, (button, mx, my) -> {
				state.expanded = !state.expanded;
				return true;
			}));
			controlsLeft = ix - 7f;
		}
		if (hasToggle) {
			boolean listening = binding == m;
			boolean bound = m.getKey() != -1;
			if (listening || bound || hv > 0.05f) {
				String label = listening ? "Press a key" : bound ? keyName(m.getKey()) : "Bind";
				int textColor = listening ? OogaTheme.ON_GOLD : bound ? OogaTheme.TEXT_SECONDARY : OogaTheme.TEXT_MUTED;
				int chipFill = listening ? OogaTheme.GOLD : OogaTheme.SURFACE_INSET;
				int chipBorder = listening ? 0 : OogaTheme.BORDER;
				Render2D.pushAlpha(listening || bound ? 1f : hv);
				float chipW = Widgets.chip(g, controlsLeft, cy, label, textColor, chipFill, chipBorder, 0.72f);
				Render2D.popAlpha();
				float chipX = controlsLeft - chipW;
				hits.add(new Hit(chipX, cy - 6, chipW, 12, (button, mx, my) -> {
					if (button == 1) {
						m.setKey(-1);
						binding = null;
					} else {
						binding = binding == m ? null : m;
					}
					return true;
				}));
				controlsLeft = chipX - 6f;
			}
		}

		float maxTextW = controlsLeft - textX - 4f;
		int nameColor = ColorUtil.lerp(OogaTheme.TEXT_SECONDARY, OogaTheme.TEXT, Math.max(on, hv));
		if (m.isSettingsOnly()) nameColor = OogaTheme.TEXT;
		float nameY = config.descriptions.get() ? y + 7f : cy - 4f;
		String name = OogaFonts.trim(m.getName(), Weight.SEMIBOLD, 1f, maxTextW);
		OogaFonts.draw(g, name, textX, nameY, nameColor, Weight.SEMIBOLD);
		if (searching) {
			float nameW = OogaFonts.width(name, Weight.SEMIBOLD);
			String tag = m.getCategory().getDisplayName().toUpperCase(Locale.ROOT);
			if (nameW + 40 < maxTextW) OogaFonts.draw(g, tag, textX + nameW + 6f, nameY + 2f, OogaTheme.TEXT_MUTED, Weight.SEMIBOLD, 0.62f);
		}
		if (config.descriptions.get()) {
			String description = OogaFonts.trim(m.getDescription(), Weight.REGULAR, 0.78f, maxTextW);
			OogaFonts.draw(g, description, textX, y + 18.5f, OogaTheme.TEXT_MUTED, Weight.REGULAR, 0.78f);
		}

		if (expand > 0.01f && hasSettings) drawSettings(g, m, x, y + collapsed, w, h - collapsed, expand);
	}

	private void drawSettings(GuiGraphics g, Module m, float x, float y, float w, float h, float expand) {
		Render2D.pushAlpha(Anim.ease(Math.min(1f, expand * 1.4f)));
		Render2D.rect(g, x + 10f, y, w - 20f, 1f, OogaTheme.DIVIDER);
		float rowY = y + 6f;
		float left = x + 14f;
		float right = x + w - 12f;
		float bottomLimit = y + h;
		for (Setting<?> setting : m.getSettings()) {
			if (!setting.isVisible()) continue;
			float rh = settingHeight(setting);
			if (rowY > bottomLimit) break;
			boolean rowHover = inside(localMouseX, localMouseY, x, rowY, w, rh);
			if (rowHover) hoveredSetting = setting;
			drawSetting(g, setting, left, rowY, right - left, rh, rowHover);
			rowY += rh;
		}
		Render2D.popAlpha();
	}

	private void drawSetting(GuiGraphics g, Setting<?> setting, float x, float y, float w, float h, boolean rowHover) {
		int labelColor = rowHover ? OogaTheme.TEXT : OogaTheme.TEXT_SECONDARY;
		float labelScale = 0.85f;
		float labelY = y + 5f;
		OogaFonts.draw(g, setting.getName(), x, labelY, labelColor, Weight.REGULAR, labelScale);

		if (setting instanceof BooleanSetting bool) {
			Anim a = hovers.computeIfAbsent(setting, k -> new Anim(bool.get() ? 1f : 0f, 16f));
			float on = a.update(bool.get() ? 1f : 0f);
			float tw = 16f, th = 9f;
			float tx = x + w - tw;
			float ty = y + (h - th) / 2f;
			Widgets.toggle(g, tx, ty, tw, th, on, rowHover ? 1f : 0f);
			hits.add(new Hit(x, y, w, h, (button, mx, my) -> {
				if (button != 0) return false;
				bool.toggle();
				return true;
			}));
		} else if (setting instanceof NumberSetting number) {
			String value = number.format();
			float vw = OogaFonts.width(value, Weight.SEMIBOLD, 0.8f);
			OogaFonts.draw(g, value, x + w - vw, labelY + 0.6f, OogaTheme.GOLD_TEXT, Weight.SEMIBOLD, 0.8f);
			float trackY = y + 17.5f;
			boolean active = draggingSlider == number;
			Anim a = hovers.computeIfAbsent(setting, k -> new Anim(0f, 18f));
			float activeT = a.update(active ? 1f : rowHover ? 0.4f : 0f);
			Widgets.slider(g, x, trackY, w, (float) number.getProgress(), activeT);
			hits.add(new Hit(x - 3, y + 10f, w + 6, h - 10f, (button, mx, my) -> {
				if (button != 0) return false;
				draggingSlider = number;
				sliderX = x;
				sliderW = w;
				number.setProgress((mx - x) / w);
				return true;
			}));
			hits.add(new Hit(x, y, w, 10f, (button, mx, my) -> {
				if (button == 1) {
					number.reset();
					return true;
				}
				return false;
			}));
		} else if (setting instanceof ModeSetting mode) {
			float hv = hover(setting, x + w - 90f, y, 90f, h);
			int fill = ColorUtil.lerp(OogaTheme.SURFACE_INSET, OogaTheme.SURFACE_CONTROL, hv);
			float chipW = Widgets.chip(g, x + w, y + h / 2f, mode.get() + "  ›", OogaTheme.GOLD_TEXT, fill, ColorUtil.lerp(OogaTheme.BORDER, 0x55F2C14E, hv), 0.75f);
			hits.add(new Hit(x + w - chipW - 2, y, chipW + 2, h, (button, mx, my) -> {
				if (button == 0) mode.cycle(true);
				else if (button == 1) mode.cycle(false);
				else return false;
				return true;
			}));
		}
	}

	// ------------------------------------------------------------------ helpers

	private static boolean inside(float mx, float my, float x, float y, float w, float h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	/** Smoothed 0..1 hover amount for any keyed region. */
	private float hover(Object key, float x, float y, float w, float h) {
		Anim a = hovers.computeIfAbsent(key, k -> new Anim(0f, 18f));
		return a.update(inside(localMouseX, localMouseY, x, y, w, h) && draggingSlider == null && !draggingWindow ? 1f : 0f);
	}

	private static String keyName(int key) {
		String name = InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
		return name.length() > 12 ? name.substring(0, 12) : name;
	}

	// ================================================================== input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (closing) return true;
		float mx = toLocalX(event.x());
		float my = toLocalY(event.y());
		int button = event.button();

		if (binding != null) {
			// Clicking anywhere while listening cancels the bind.
			binding = null;
			return true;
		}
		if (!inside(mx, my, 0, 0, windowWidth(), windowHeight())) {
			searchFocused = false;
			return true;
		}
		for (int i = hits.size() - 1; i >= 0; i--) {
			Hit hit = hits.get(i);
			if (hit.contains(mx, my) && hit.action().click(button, mx, my)) {
				if (draggingWindow) {
					dragOffsetX = event.x() - centerX;
					dragOffsetY = event.y() - centerY;
				}
				return true;
			}
		}
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		if (draggingWindow) {
			centerX = (float) (event.x() - dragOffsetX);
			centerY = (float) (event.y() - dragOffsetY);
			clampWindow();
			return true;
		}
		if (draggingSlider != null) {
			float mx = toLocalX(event.x());
			draggingSlider.setProgress((mx - sliderX) / sliderW);
			return true;
		}
		return super.mouseDragged(event, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		draggingWindow = false;
		draggingSlider = null;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget - (float) scrollY * 24f));
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (binding != null) {
			if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) binding.setKey(-1);
			else binding.setKey(key);
			binding = null;
			return true;
		}
		if (key == GLFW.GLFW_KEY_ESCAPE) {
			if (!search.isEmpty() || searchFocused) {
				search.setLength(0);
				searchFocused = false;
			} else {
				onClose();
			}
			return true;
		}
		if (searchFocused || !search.isEmpty()) {
			if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
				if ((event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) search.setLength(0);
				else search.setLength(search.length() - 1);
				searchFocused = true;
				return true;
			}
			if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
				List<Module> results = visibleModules();
				if (!results.isEmpty() && !results.get(0).isSettingsOnly()) results.get(0).toggle();
				return true;
			}
		}
		if (!searchFocused && key == config.getKey()) {
			onClose();
			return true;
		}
		if ((event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0 && key == GLFW.GLFW_KEY_F) {
			searchFocused = true;
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		int codepoint = event.codepoint();
		if (binding != null || Character.isISOControl(codepoint)) return false;
		if (!searchFocused && Character.isWhitespace(codepoint)) return false;
		// Typing anywhere starts a search — the fastest way to reach a module.
		if (search.length() < 32) search.appendCodePoint(codepoint);
		searchFocused = true;
		scrollTarget = 0;
		return true;
	}

	@Override
	public void onClose() {
		// Play the exit animation first; finishClose() runs once it's done.
		closing = true;
		binding = null;
		draggingSlider = null;
		draggingWindow = false;
	}

	private void finishClose() {
		if (minecraft != null) minecraft.setScreen(null);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
