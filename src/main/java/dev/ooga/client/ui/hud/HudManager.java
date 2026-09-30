package dev.ooga.client.ui.hud;

import com.google.gson.JsonObject;
import dev.ooga.client.config.ConfigManager;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.render.Render2D;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Owns every HUD element and draws them as one Fabric HUD layer. */
public final class HudManager {
	private static final HudManager INSTANCE = new HudManager();
	private final List<HudElement> elements = new ArrayList<>();

	private HudManager() {
	}

	public static HudManager get() {
		return INSTANCE;
	}

	public void register(HudElement element) {
		elements.add(element);
	}

	public List<HudElement> getElements() {
		return Collections.unmodifiableList(elements);
	}

	public void render(GuiGraphics g, DeltaTracker tracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui) return;
		OogaTheme.frame();
		// The HUD editor draws the elements itself, with handles.
		if (mc.screen instanceof HudEditorScreen) return;
		renderElements(g, tracker.getGameTimeDeltaPartialTick(false));
	}

	void renderElements(GuiGraphics g, float delta) {
		for (HudElement element : elements) {
			if (!element.isVisible()) continue;
			Render2D.pushAlpha(1f);
			element.renderAt(g, delta);
			Render2D.popAlpha();
		}
	}

	public void onMoved() {
		ConfigManager.get().markDirty();
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		for (HudElement element : elements) json.add(element.getId(), element.toJson());
		return json;
	}

	public void fromJson(JsonObject json) {
		for (HudElement element : elements) {
			if (json.has(element.getId())) element.fromJson(json.getAsJsonObject(element.getId()));
		}
	}
}
