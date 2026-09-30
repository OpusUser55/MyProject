package dev.ooga.client.module.impl.client;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.clickgui.ClickGuiScreen;
import dev.ooga.client.ui.clickgui.PanelClickGuiScreen;
import org.lwjgl.glfw.GLFW;

public class ClickGuiModule extends Module {
	public final ModeSetting layout = add(new ModeSetting("Layout", "Panels: a floating panel per category. Window: one window with a sidebar.", "Panels", "Panels", "Window"));
	public final NumberSetting scale = add(new NumberSetting("Scale", "Size of the menu.", 1.0, 0.75, 1.25, 0.05, "x"));
	public final NumberSetting panelWidth = add(new NumberSetting("Panel Width", "Width of each category panel.", 116, 96, 160, 2, "px")
			.visibleWhen(() -> layout.is("Panels")));
	public final ModeSetting density = add(new ModeSetting("Density", "Row height in panels.", "Normal", "Compact", "Normal", "Relaxed")
			.visibleWhen(() -> layout.is("Panels")));
	public final ModeSetting enabledStyle = add(new ModeSetting("Enabled Style", "How enabled modules are marked.", "Bar", "Bar", "Fill", "Text")
			.visibleWhen(() -> layout.is("Panels")));
	public final ModeSetting keybinds = add(new ModeSetting("Show Keybinds", "When to show a module's bound key.", "Hover", "Hover", "Always", "Never"));
	public final BooleanSetting cascade = add(new BooleanSetting("Cascade Open", "Panels appear one after another when the menu opens.", true)
			.visibleWhen(() -> layout.is("Panels")));
	public final BooleanSetting descriptions = add(new BooleanSetting("Descriptions", "Window layout: show a one-line description under each module.", true));
	public final BooleanSetting blur = add(new BooleanSetting("Blur", "Blur the world behind the menu.", true));
	public final BooleanSetting aurora = add(new BooleanSetting("Aurora", "Slow drifting accent-coloured light behind the panels.", true));
	public final BooleanSetting particles = add(new BooleanSetting("Particles", "Small glowing particles floating up behind the panels.", true));
	public final NumberSetting panelGlow = add(new NumberSetting("Panel Glow", "Soft light around every panel.", 0.45, 0.0, 1.0, 0.05));
	public final NumberSetting dim = add(new NumberSetting("Dim World", "How much to darken the world behind the menu.", 0.45, 0.0, 1.0, 0.05));

	public ClickGuiModule() {
		super("ClickGUI", "The Ooga menu. Bind it to any key.", Category.CLIENT);
		settingsOnly();
		setDefaultKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
	}

	@Override
	public void onKeybind() {
		if (mc.screen == null) mc.setScreen(layout.is("Window") ? new ClickGuiScreen() : new PanelClickGuiScreen());
	}
}
