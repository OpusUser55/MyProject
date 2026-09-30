package dev.ooga.client.ui.clickgui;

import dev.ooga.client.module.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

/** Keyboard handling for a focused {@link StringSetting}, shared by both ClickGUI layouts. */
final class TextEdit {
	private TextEdit() {
	}

	/** @return false when editing should stop (Enter or Escape). */
	static boolean key(StringSetting setting, KeyEvent event) {
		int key = event.key();
		boolean ctrl = (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
		if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) return false;
		String value = setting.get();
		if (key == GLFW.GLFW_KEY_BACKSPACE && !value.isEmpty()) {
			setting.set(ctrl ? "" : value.substring(0, value.offsetByCodePoints(value.length(), -1)));
		} else if (ctrl && key == GLFW.GLFW_KEY_V) {
			setting.set(value + Minecraft.getInstance().keyboardHandler.getClipboard());
		}
		return true;
	}

	static void type(StringSetting setting, int codepoint) {
		if (Character.isISOControl(codepoint)) return;
		setting.set(setting.get() + Character.toString(codepoint));
	}
}
