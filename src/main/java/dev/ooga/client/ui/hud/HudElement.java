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
	private final String id;
	private final String displayName;
	private float relX;
	private float relY;
	protected float width;
	protected float height;

	protected HudElement(String id, String displayName, float defaultRelX, float defaultRelY) {
		this.id = id;
		this.displayName = displayName;
		this.relX = defaultRelX;
		this.relY = defaultRelY;
	}

	public abstract boolean isVisible();

	/** Draw at (x, y), which is already clamped on screen. Must update width/height. */
	protected abstract void render(GuiGraphics g, float x, float y, float delta);

	public final void renderAt(GuiGraphics g, float delta) {
		render(g, getX(), getY(), delta);
	}

	public float getX() {
		int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
		return Math.max(0, Math.min(sw - width, relX * sw));
	}

	public float getY() {
		int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
		return Math.max(0, Math.min(sh - height, relY * sh));
	}

	public void setPosition(float x, float y) {
		var window = Minecraft.getInstance().getWindow();
		relX = Math.max(0, Math.min(1, x / window.getGuiScaledWidth()));
		relY = Math.max(0, Math.min(1, y / window.getGuiScaledHeight()));
	}

	/** True when the element sits on the right half, so it should grow leftward. */
	protected boolean anchoredRight() {
		int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
		return getX() + width / 2f > sw / 2f;
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
		return json;
	}

	public void fromJson(JsonObject json) {
		if (json.has("x")) relX = json.get("x").getAsFloat();
		if (json.has("y")) relY = json.get("y").getAsFloat();
	}
}
