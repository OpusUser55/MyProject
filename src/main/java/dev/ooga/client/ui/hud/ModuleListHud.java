package dev.ooga.client.ui.hud;

import dev.ooga.client.module.Module;
import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.impl.client.ModuleListModule;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.render.OogaFonts;
import dev.ooga.client.ui.render.OogaFonts.Weight;
import dev.ooga.client.ui.render.Render2D;
import dev.ooga.client.util.Anim;
import dev.ooga.client.util.ColorUtil;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Enabled-module list. Rows slide in from the screen edge they're anchored to and collapse
 * smoothly when a module turns off. Names are neutral text; gold is kept for the edge accent
 * and suffixes, so the list reads calmly even with many modules on.
 */
public class ModuleListHud extends HudElement {
	private static final float ROW_HEIGHT = 11f;
	private static final float PAD_X = 4f;

	private final ModuleListModule module;
	private final Map<Module, Anim> rows = new IdentityHashMap<>();

	public ModuleListHud(ModuleListModule module) {
		super("module_list", "Module List", Anchor.END, 1f, Anchor.START, 0f);
		this.module = module;
	}

	@Override
	public NumberSetting scaleSetting() {
		return module.scale;
	}

	@Override
	public boolean isVisible() {
		return module.isEnabled();
	}

	private String name(Module m) {
		return switch (module.textCase.get()) {
			case "lowercase" -> m.getName().toLowerCase(Locale.ROOT);
			case "UPPERCASE" -> m.getName().toUpperCase(Locale.ROOT);
			default -> m.getName();
		};
	}

	private float rowHeight() {
		return ROW_HEIGHT + module.spacing.getFloat();
	}

	private float labelWidth(Module m) {
		String suffix = module.suffixes.get() ? m.getSuffix() : null;
		float w = OogaFonts.width(name(m), Weight.REGULAR);
		if (suffix != null) w += 3f + OogaFonts.width(suffix, Weight.REGULAR);
		return w;
	}

	@Override
	protected void render(GuiGraphics g, float x, float y, float delta) {
		float scale = module.scale.getFloat();
		List<Module> shown = new ArrayList<>();
		for (Module m : ModuleManager.get().getModules()) {
			if (m.isHiddenFromList() || m.isSettingsOnly()) continue;
			Anim anim = rows.computeIfAbsent(m, k -> new Anim(0f, 14f));
			anim.update(m.isEnabled() ? 1f : 0f);
			if (anim.get() > 0.005f) shown.add(m);
		}
		if (module.sort.is("Length")) shown.sort(Comparator.comparingDouble((Module m) -> -labelWidth(m)));
		else shown.sort(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER));

		float maxWidth = 0;
		for (Module m : shown) maxWidth = Math.max(maxWidth, labelWidth(m) + PAD_X * 2 + 2f);
		if (shown.isEmpty()) maxWidth = 60f;

		boolean right = anchoredRight();
		float totalHeight = 0;
		for (Module m : shown) totalHeight += rowHeight() * Anim.ease(rows.get(m).get());
		this.width = maxWidth * scale;
		this.height = Math.max(rowHeight(), totalHeight) * scale;

		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);

		float rowY = 0;
		float bg = module.opacity.getFloat();
		for (Module m : shown) {
			float t = Anim.ease(rows.get(m).get());
			float rowWidth = labelWidth(m) + PAD_X * 2 + 2f;
			float slide = (1f - t) * (rowWidth + 6f);
			float rowX = right ? maxWidth - rowWidth + slide : -slide;
			float h = rowHeight() * t;

			Render2D.pushAlpha(t);
			if (bg > 0) Render2D.rect(g, rowX, rowY, rowWidth, h, ColorUtil.withAlpha(0x0E0F12, Math.round(235 * bg)));
			if (module.accentBar.get()) {
				float barX = right ? rowX + rowWidth - 1.5f : rowX;
				Render2D.rect(g, barX, rowY, 1.5f, h, OogaTheme.GOLD);
			}
			float textX = rowX + PAD_X + (right ? 0 : 2f);
			float textY = rowY + (h - 8f) / 2f;
			OogaFonts.draw(g, name(m), textX, textY, OogaTheme.TEXT, Weight.REGULAR);
			String suffix = module.suffixes.get() ? m.getSuffix() : null;
			if (suffix != null) {
				float sx = textX + OogaFonts.width(name(m), Weight.REGULAR) + 3f;
				int suffixColor = module.suffixColor.is("Muted") ? OogaTheme.TEXT_SECONDARY : OogaTheme.GOLD_TEXT;
				OogaFonts.draw(g, suffix, sx, textY, suffixColor, Weight.REGULAR);
			}
			Render2D.popAlpha();
			rowY += h;
		}

		g.pose().popMatrix();
	}
}
