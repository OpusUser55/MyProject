package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.social.FriendStore;
import dev.ooga.client.ui.notify.Notification;
import dev.ooga.client.ui.notify.NotificationManager;
import dev.ooga.client.util.ChatUtil;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.sounds.SoundEvents;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tells you when another player comes close. "Close" is render distance by default, or a
 * radius you choose; friends ({@code .friend add}) can be left out.
 */
public class VisualRangeModule extends Module {
	public final NumberSetting distance = add(new NumberSetting("Alert Distance", "Alert when a player comes this close (0 = anywhere in render distance).", 0, 0, 256, 4, "m"));
	public final BooleanSetting ignoreFriends = add(new BooleanSetting("Ignore Friends", "No alerts for players on your .friend list.", true));
	public final BooleanSetting leave = add(new BooleanSetting("Leaving", "Also report players leaving range.", true));
	public final BooleanSetting chat = add(new BooleanSetting("Chat", "Post in chat as well as a notification.", false));
	public final ModeSetting sound = add(new ModeSetting("Sound", "What to play when someone arrives.", "Ping", "Off", "Ping", "Loud"));
	public final BooleanSetting flash = add(new BooleanSetting("Screen Flash", "Briefly tint the screen edges when someone arrives.", false));

	/** Players currently in range, by UUID, with the name last seen (they're gone from the world when they leave). */
	private final Map<UUID, String> inRange = new HashMap<>();
	private boolean primed;
	private long flashUntil;

	public VisualRangeModule() {
		super("Visual Range", "Alerts when players come near or leave.", Category.MISC);
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
		double maxSq = distance.get() <= 0 ? Double.MAX_VALUE : distance.get() * distance.get();
		Map<UUID, String> now = new HashMap<>();
		for (AbstractClientPlayer player : mc.level.players()) {
			if (player == mc.player || player.distanceToSqr(mc.player) > maxSq) continue;
			String name = player.getName().getString();
			if (ignoreFriends.get() && FriendStore.isFriend(name)) continue;
			now.put(player.getUUID(), name);
		}
		// The first tick after enabling or joining just records who's already here.
		if (primed) {
			for (Map.Entry<UUID, String> e : now.entrySet()) {
				if (!inRange.containsKey(e.getKey())) arrived(e.getValue());
			}
			if (leave.get()) {
				for (Map.Entry<UUID, String> e : inRange.entrySet()) {
					if (!now.containsKey(e.getKey())) report(e.getValue() + " left range");
				}
			}
		}
		inRange.clear();
		inRange.putAll(now);
		primed = true;
	}

	private void arrived(String name) {
		String where = distance.get() > 0 ? " (within " + distance.getInt() + "m)" : "";
		report(name + " is nearby" + where);
		if (sound.is("Ping")) mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6f, 1.6f);
		else if (sound.is("Loud")) mc.player.playSound(SoundEvents.ANVIL_LAND, 1.0f, 1.2f);
		if (flash.get()) flashUntil = System.currentTimeMillis() + 600;
	}

	private void report(String message) {
		NotificationManager.get().push("Visual Range", message, Notification.Kind.INFO);
		if (chat.get()) ChatUtil.info(message);
	}

	/** 0..1 strength of the arrival flash right now; drawn by the HUD. */
	public float flashAlpha() {
		long left = flashUntil - System.currentTimeMillis();
		return isEnabled() && left > 0 ? left / 600f : 0f;
	}

	@Override
	public String getSuffix() {
		return Integer.toString(inRange.size());
	}
}
