package dev.ooga.client.ui.hud;

import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.render.GlowRenderer;
import dev.ooga.client.ui.render.OogaFonts;
import dev.ooga.client.ui.render.OogaFonts.Weight;
import dev.ooga.client.ui.render.Render2D;
import dev.ooga.client.util.Anim;
import dev.ooga.client.util.ColorUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/**
 * Drag HUD elements into place. Elements snap to the screen centre lines and edges, scroll
 * over an element to resize it, and Esc returns to wherever the editor was opened from.
 */
public class HudEditorScreen extends Screen {
	private static final float SNAP = 4f;
	private static final float EDGE = 4f;

	private final Screen parent;
	private final Map<HudElement, Anim> hoverAnims = new HashMap<>();
	private final Anim fade = new Anim(0f, 14f);
	private HudElement dragging;
	private float grabX;
	private float grabY;
	private boolean snappedX;
	private boolean snappedY;

	public HudEditorScreen(Screen parent) {
		super(Component.literal("Ooga HUD Editor"));
		this.parent = parent;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		float t = fade.get();
		Render2D.rect(g, 0, 0, width, height, ColorUtil.fade(0x66050507, t));
		// Faint grid so alignment is easy to judge.
		int grid = 0x0AFFFFFF;
		for (int x = 0; x < width; x += 20) Render2D.rect(g, x, 0, 0.5f, height, ColorUtil.fade(grid, t));
		for (int y = 0; y < height; y += 20) Render2D.rect(g, 0, y, width, 0.5f, ColorUtil.fade(grid, t));
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		float t = Anim.ease(fade.update(1f));
		HudManager.get().renderElements(g, delta);

		if (dragging != null) {
			if (snappedX) Render2D.rect(g, width / 2f - 0.5f, 0, 1f, height, 0x88F2C14E);
			if (snappedY) Render2D.rect(g, 0, height / 2f - 0.5f, width, 1f, 0x88F2C14E);
		}

		Render2D.pushAlpha(t);
		for (HudElement element : HudManager.get().getElements()) {
			if (!element.isVisible() || !element.isMovable()) continue;
			float x = element.getX() - 2f;
			float y = element.getY() - 2f;
			float w = element.getWidth() + 4f;
			float h = element.getHeight() + 4f;
			boolean over = element == dragging || (dragging == null && element.contains(mouseX, mouseY));
			float hv = hoverAnims.computeIfAbsent(element, k -> new Anim(0f, 18f)).update(over ? 1f : 0f);

			if (hv > 0.01f) GlowRenderer.glow(g, x, y, w, h, OogaTheme.RADIUS_CONTROL, OogaTheme.GOLD, 0.5f * hv);
			Render2D.outline(g, x, y, w, h, OogaTheme.RADIUS_CONTROL, ColorUtil.lerp(0x40FFFFFF, OogaTheme.GOLD, hv));

			String label = element.getDisplayName();
			NumberSetting scale = element.scaleSetting();
			if (scale != null && hv > 0.5f) label += "  " + scale.format();
			float lw = OogaFonts.width(label, Weight.SEMIBOLD, 0.7f) + 8f;
			float ly = y > 12f ? y - 11f : y + h + 2f;
			Render2D.roundRect(g, x, ly, lw, 9f, 2f, ColorUtil.lerp(0xE0141519, OogaTheme.GOLD, hv));
			OogaFonts.draw(g, label, x + 4f, ly + 1.6f, ColorUtil.lerp(OogaTheme.TEXT_SECONDARY, OogaTheme.ON_GOLD, hv), Weight.SEMIBOLD, 0.7f);
		}

		String hint = "Drag to move  ·  Scroll to resize  ·  Esc to finish";
		float hw = OogaFonts.width(hint, Weight.REGULAR, 0.8f) + 16f;
		float hx = width / 2f - hw / 2f;
		float hy = height - 44f;
		Render2D.roundRect(g, hx, hy, hw, 14f, 7f, 0xE00E0F12);
		Render2D.outline(g, hx, hy, hw, 14f, 7f, OogaTheme.BORDER);
		OogaFonts.draw(g, hint, hx + 8f, hy + 3.6f, OogaTheme.TEXT_SECONDARY, Weight.REGULAR, 0.8f);
		Render2D.popAlpha();
	}

	private HudElement elementAt(double mouseX, double mouseY) {
		var elements = HudManager.get().getElements();
		for (int i = elements.size() - 1; i >= 0; i--) {
			HudElement element = elements.get(i);
			if (element.isVisible() && element.isMovable() && element.contains(mouseX, mouseY)) return element;
		}
		return null;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != 0) return true;
		dragging = elementAt(event.x(), event.y());
		if (dragging != null) {
			grabX = (float) event.x() - dragging.getX();
			grabY = (float) event.y() - dragging.getY();
		}
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		if (dragging == null) return true;
		float x = (float) event.x() - grabX;
		float y = (float) event.y() - grabY;
		float w = dragging.getWidth();
		float h = dragging.getHeight();

		// Snap to screen centre lines.
		snappedX = Math.abs(x + w / 2f - width / 2f) < SNAP;
		snappedY = Math.abs(y + h / 2f - height / 2f) < SNAP;
		if (snappedX) x = width / 2f - w / 2f;
		if (snappedY) y = height / 2f - h / 2f;
		// Snap to a small margin from each edge.
		if (Math.abs(x - EDGE) < SNAP) x = EDGE;
		if (Math.abs(y - EDGE) < SNAP) y = EDGE;
		if (Math.abs(x + w - (width - EDGE)) < SNAP) x = width - EDGE - w;
		if (Math.abs(y + h - (height - EDGE)) < SNAP) y = height - EDGE - h;

		dragging.setPosition(x, y);
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging != null) HudManager.get().onMoved();
		dragging = null;
		snappedX = snappedY = false;
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		HudElement element = elementAt(mouseX, mouseY);
		if (element == null || element.scaleSetting() == null) return true;
		NumberSetting scale = element.scaleSetting();
		scale.set(scale.get() + (scrollY > 0 ? 0.05 : -0.05));
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		HudManager.get().onMoved();
		if (minecraft != null) minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
