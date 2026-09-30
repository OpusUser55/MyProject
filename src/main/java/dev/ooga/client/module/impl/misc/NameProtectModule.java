package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.StringSetting;

/**
 * Replaces your username everywhere the game draws text — chat, tab list, scoreboard, name
 * tags, menus — so it never shows up in screenshots or recordings. Purely visual: the server
 * still knows who you are.
 */
public class NameProtectModule extends Module {
	/** Cached for the hot path below; a module lookup per rendered string would add up. */
	private static NameProtectModule instance;

	public final StringSetting replacement = add(new StringSetting("Name", "What to show instead of your username.", "Me"));

	public NameProtectModule() {
		super("Name Protect", "Hides your username on screen.", Category.MISC);
		instance = this;
	}

	/** Hot path: called for every string the font renders, so keep it allocation-free when there's no match. */
	public static String filter(String text) {
		NameProtectModule module = instance;
		if (module == null || !module.isEnabled() || text == null || text.isEmpty() || mc.getUser() == null) return text;
		String name = mc.getUser().getName();
		if (name == null || name.isEmpty() || !text.contains(name)) return text;
		return text.replace(name, module.replacement.get());
	}
}
