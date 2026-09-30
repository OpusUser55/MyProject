package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.ModeSetting;

/** Shows default skins instead of real ones, on your screen only (for recordings). */
public class SkinProtectModule extends Module {
	private static SkinProtectModule instance;

	public final ModeSetting who = add(new ModeSetting("Who", "Whose skins to hide.", "Me", "Me", "Everyone"));

	public SkinProtectModule() {
		super("Skin Protect", "Hides your (or everyone's) skin on your screen.", Category.MISC);
		instance = this;
	}

	/** Whether the skin of the player with this name should be replaced. */
	public static boolean hides(String name) {
		SkinProtectModule self = instance;
		if (self == null || !self.isEnabled()) return false;
		if (self.who.is("Everyone")) return true;
		return mc.player != null && name != null && name.equals(mc.player.getGameProfile().name());
	}
}
