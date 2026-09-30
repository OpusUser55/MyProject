package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.ui.notify.Notification;
import dev.ooga.client.ui.notify.NotificationManager;
import dev.ooga.client.util.ChatUtil;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.sounds.SoundEvents;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Tells you when another player comes into (or leaves) render distance. */
public class VisualRangeModule extends Module {
	public final BooleanSetting leave = add(new BooleanSetting("Leaving", "Also report players leaving range.", true));
	public final BooleanSetting chat = add(new BooleanSetting("Chat", "Post in chat as well as a notification.", false));
	public final BooleanSetting sound = add(new BooleanSetting("Sound", "Ping when someone enters.", true));

	/** Players currently in range, by UUID, with the name last seen (they're gone from the world when they leave). */
	private final Map<UUID, String> inRange = new HashMap<>();
	private boolean primed;

	public VisualRangeModule() {
		super("Visual Range", "Alerts when players enter or leave render distance.", Category.MISC);
	}

	@Override
	protected void onEnable() {
		inRange.clear();
		primed = false;
	}

	@Override
	public void onTick() {
		if (!inWorld()) {
			inRange.clear();
			primed = false;
			return;
		}
		Map<UUID, String> now = new HashMap<>();
		for (AbstractClientPlayer player : mc.level.players()) {
			if (player == mc.player) continue;
			now.put(player.getUUID(), player.getName().getString());
		}
		// The first tick after enabling or joining just records who's already here.
		if (primed) {
			for (Map.Entry<UUID, String> e : now.entrySet()) {
				if (!inRange.containsKey(e.getKey())) report(e.getValue() + " entered visual range", true);
			}
			if (leave.get()) {
				for (Map.Entry<UUID, String> e : inRange.entrySet()) {
					if (!now.containsKey(e.getKey())) report(e.getValue() + " left visual range", false);
				}
			}
		}
		inRange.clear();
		inRange.putAll(now);
		primed = true;
	}

	private void report(String message, boolean entered) {
		NotificationManager.get().push("Visual Range", message, Notification.Kind.INFO);
		if (chat.get()) ChatUtil.info(message);
		if (entered && sound.get()) mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6f, 1.6f);
	}

	@Override
	public String getSuffix() {
		return Integer.toString(inRange.size());
	}
}
