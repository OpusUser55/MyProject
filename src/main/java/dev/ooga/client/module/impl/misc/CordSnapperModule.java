package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.ui.notify.Notification;
import dev.ooga.client.ui.notify.NotificationManager;

/** One key copies your coordinates to the clipboard. Bind it by middle-clicking the row. */
public class CordSnapperModule extends Module {
	public final BooleanSetting withDimension = add(new BooleanSetting("Dimension", "Add the dimension after the coordinates.", false));

	public CordSnapperModule() {
		super("Cord Snapper", "Copy your coordinates to the clipboard with one key.", Category.MISC);
		hideFromList();
	}

	@Override
	protected boolean canEnable() {
		return false;
	}

	@Override
	public void onKeybind() {
		if (mc.player == null || mc.level == null) return;
		String text = mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ();
		if (withDimension.get()) text += " " + mc.level.dimension().identifier().getPath();
		mc.keyboardHandler.setClipboard(text);
		NotificationManager.get().push("Copied", text, Notification.Kind.INFO);
	}
}
