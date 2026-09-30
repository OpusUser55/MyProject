package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.ModeSetting;

/**
 * Hides your username everywhere text is drawn (chat, tab list, scoreboard, name tags, HUD),
 * for recording or streaming. Client-side only: other players still see your real name.
 */
public class NameProtectModule extends Module {
	private static NameProtectModule instance;

	public final ModeSetting replacement = add(new ModeSetting("Replace With", "What your name is shown as.", "Me", "Me", "You", "Player", "Hidden"));

	public NameProtectModule() {
		super("Name Protect", "Hides your username on screen for recordings and streams.", Category.MISC);
		instance = this;
	}

	/** The text to draw instead of {@code text}, or null to leave it alone. Called for every string drawn. */
	public static String protect(String text) {
		NameProtectModule self = instance;
		if (self == null || !self.isEnabled() || mc.player == null || text == null) return null;
		String name = mc.player.getGameProfile().name();
		if (name == null || name.length() < 2 || !text.contains(name)) return null;
		String with = self.replacement.is("Hidden") ? "*".repeat(name.length()) : self.replacement.get();
		// Never produce text that still contains the name, or the renderer would loop on it.
		if (with.contains(name)) return null;
		return text.replace(name, with);
	}
}
