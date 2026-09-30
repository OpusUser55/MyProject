package dev.ooga.client.module.impl.render;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;

/**
 * Bigger, readable tags over other players with health and distance, drawn on the HUD layer so
 * they stay legible at range. Friends are tagged in blue. Drawn by {@code ScreenOverlay}.
 */
public class NametagsModule extends Module {
	public final BooleanSetting health = add(new BooleanSetting("Health", "Show health (including absorption).", true));
	public final BooleanSetting distance = add(new BooleanSetting("Distance", "Show how far away they are.", true));
	public final NumberSetting range = add(new NumberSetting("Range", "Only tag players within this distance.", 64, 8, 256, 4, "m"));
	public final NumberSetting scale = add(new NumberSetting("Scale", "Tag size.", 1.0, 0.5, 2.0, 0.05, "x"));

	public NametagsModule() {
		super("Nametags", "Clear tags with health and distance over players.", Category.RENDER);
	}
}
