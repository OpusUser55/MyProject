package dev.ooga.client.module.impl.basefinding;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.notify.Notification;
import dev.ooga.client.ui.notify.NotificationManager;
import dev.ooga.client.util.ChatUtil;
import dev.ooga.client.world.Finds;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Hands-free base hunting: keeps using random teleport, waits for the area to load and your
 * finders to scan it, and stops the moment any of them turns something up.
 */
public class RtpBaseFinderModule extends Module {
	public final ModeSetting command = add(new ModeSetting("Command", "The random-teleport command your server uses.", "rtp", "rtp", "rtp overworld", "rtp nether", "rtp end", "wild"));
	public final NumberSetting interval = add(new NumberSetting("Interval", "Seconds between teleports (loading and scanning time). Keep above the server's cooldown.", 20, 5, 120, 1, "s"));
	public final BooleanSetting stopOnFind = add(new BooleanSetting("Stop On Find", "Stop teleporting when a finder reports something.", true));
	public final NumberSetting maxTeleports = add(new NumberSetting("Max Teleports", "Stop after this many. 0 means no limit.", 0, 0, 500, 5));

	private long next;
	private int seenFinds;
	private int teleports;

	public RtpBaseFinderModule() {
		super("RTP Base Finder", "Random-teleports until your finders spot a base.", Category.BASEFINDING);
	}

	@Override
	protected void onEnable() {
		next = 0;
		teleports = 0;
		seenFinds = Finds.total();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.getConnection() == null) return;
		if (stopOnFind.get() && Finds.total() > seenFinds) {
			var find = Finds.recent().get(0);
			ChatUtil.info("RTP Base Finder stopped: " + find.type() + " at " + find.pos().getX() + ", " + find.pos().getY() + ", " + find.pos().getZ());
			NotificationManager.get().push("Base found!", find.type() + " · " + find.pos().getX() + ", " + find.pos().getZ(), Notification.Kind.INFO);
			mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.6f, 0.9f));
			setEnabled(false, false);
			return;
		}
		long now = System.currentTimeMillis();
		if (now < next || mc.screen != null) return;
		if (maxTeleports.getInt() > 0 && teleports >= maxTeleports.getInt()) {
			ChatUtil.info("RTP Base Finder: " + teleports + " teleports, nothing found.");
			setEnabled(false, false);
			return;
		}
		mc.getConnection().sendCommand(command.get());
		teleports++;
		// A little jitter so the rhythm isn't perfectly regular.
		next = now + Math.round(interval.get() * 1000) + ThreadLocalRandom.current().nextInt(0, 1500);
		seenFinds = Finds.total();
	}

	@Override
	public String getSuffix() {
		return Integer.toString(teleports);
	}
}
