package dev.ooga.client.ui.hud;

import com.google.gson.JsonObject;
import dev.ooga.client.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A movable HUD widget. Positions are stored relative to the screen (0..1) so layouts survive
 * resolution and GUI-scale changes. Subclasses report their size from the last render so the
 * HUD editor can hit-test and drag them.
 */
public abstract class HudElement {
	/** Gap kept between elements and the screen edge. */
	protected static final float EDGE_MARGIN = 3f;

	/**
	 * Which point of the element is pinned. Elements in the outer thirds of the screen pin
	 * their near edge, so they hug that edge at any resolution; centred elements stay centred.
	 */
	public enum Anchor {
		START, CENTER, END;

		static Anchor of(float center, float size) {
			if (center < size / 3f) return START;
			if (center > size * 2f / 3f) return END;
			return CENTER;
		}
	}

	private final String id;
	private final String displayName;
	private Anchor anchorX;
	private Anchor anchorY;
	/** Relative (0..1) position of the anchored point. */
	private float relX;
	private float relY;
	protected float width;
	protected float height;

	protected HudElement(String id, String displayName, Anchor anchorX, float relX, Anchor anchorY, float relY) {
		this.id = id;
		this.displayName = displayName;
		this.anchorX = anchorX;
		this.relX = relX;
		this.anchorY = anchorY;
		this.relY = relY;
	}

	public abstract boolean isVisible();

	/** Draw at (x, y), which is already clamped on screen. Must update width/height. */
	protected abstract void render(GuiGraphics g, float x, float y, float delta);

	public final void renderAt(GuiGraphics g, float delta) {
		render(g, getX(), getY(), delta);
	}

	private static float resolve(Anchor anchor, float rel, float screen, float size) {
		float point = rel * screen;
		float start = switch (anchor) {
			case START -> point;
			case CENTER -> point - size / 2f;
			case END -> point - size;
		};
		return Math.max(EDGE_MARGIN, Math.min(screen - size - EDGE_MARGIN, start));
	}

	public float getX() {
		return resolve(anchorX, relX, Minecraft.getInstance().getWindow().getGuiScaledWidth(), width);
	}

	public float getY() {
		return resolve(anchorY, relY, Minecraft.getInstance().getWindow().getGuiScaledHeight(), height);
	}

	/** Moves the element so its top-left corner is at (x, y), re-deriving its anchors. */
	public void setPosition(float x, float y) {
		var window = Minecraft.getInstance().getWindow();
		float sw = window.getGuiScaledWidth();
		float sh = window.getGuiScaledHeight();
		anchorX = Anchor.of(x + width / 2f, sw);
		anchorY = Anchor.of(y + height / 2f, sh);
		relX = clamp01(pointFor(anchorX, x, width) / sw);
		relY = clamp01(pointFor(anchorY, y, height) / sh);
	}

	private static float pointFor(Anchor anchor, float start, float size) {
		return switch (anchor) {
			case START -> start;
			case CENTER -> start + size / 2f;
			case END -> start + size;
		};
	}

	private static float clamp01(float v) {
		return Math.max(0, Math.min(1, v));
	}

	/** True when the element sits on the right half, so it should grow leftward. */
	protected boolean anchoredRight() {
		return anchorX == Anchor.END;
	}

	public boolean contains(double mouseX, double mouseY) {
		float x = getX(), y = getY();
		return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
	}

	/** Elements that position themselves (like the notification stack) can't be dragged. */
	public boolean isMovable() {
		return true;
	}

	/** The setting the HUD editor adjusts when scrolling over this element, if any. */
	public NumberSetting scaleSetting() {
		return null;
	}

	public float getWidth() {
		return width;
	}

	public float getHeight() {
		return height;
	}

	public String getId() {
		return id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("x", relX);
		json.addProperty("y", relY);
		json.addProperty("anchorX", anchorX.name());
		json.addProperty("anchorY", anchorY.name());
		return json;
	}

	public void fromJson(JsonObject json) {
		if (json.has("x")) relX = json.get("x").getAsFloat();
		if (json.has("y")) relY = json.get("y").getAsFloat();
		try {
			if (json.has("anchorX")) anchorX = Anchor.valueOf(json.get("anchorX").getAsString());
			if (json.has("anchorY")) anchorY = Anchor.valueOf(json.get("anchorY").getAsString());
		} catch (IllegalArgumentException ignored) {
			// Keep defaults for unknown values.
		}
	}
}
