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
	public final BooleanSetting descriptions = add(new BooleanSetting("Descriptions", "Window layout: show a one-line description under each module.", true));
	public final BooleanSetting dim = add(new BooleanSetting("Dim World", "Darken the world behind the menu.", true));

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
