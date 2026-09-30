package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.module.setting.StringSetting;
import dev.ooga.client.ui.OogaTheme;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Chat quality of life: timestamps, duplicate-spam suppression, a keyword filter and a
 * highlight + ping when someone mentions you.
 *
 * <p>Following Fabric's guidance for changing received messages, each chat and system line is
 * cancelled and then re-added to the chat box with our changes. Action-bar messages are left
 * alone.
 */
public class BetterChatModule extends Module {
	private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter HH_MM_SS = DateTimeFormatter.ofPattern("HH:mm:ss");

	public final BooleanSetting timestamps = add(new BooleanSetting("Timestamps", "Show the time in front of each message.", true));
	public final BooleanSetting seconds = add(new BooleanSetting("Seconds", "Include seconds in timestamps.", false)
			.visibleWhen(timestamps::get));
	public final BooleanSetting antiSpam = add(new BooleanSetting("Anti Spam", "Hide a message identical to one shown in the last few seconds.", true));
	public final NumberSetting spamWindow = add(new NumberSetting("Spam Window", "How long a repeated message stays hidden.", 10, 1, 60, 1, "s")
			.visibleWhen(antiSpam::get));
	public final BooleanSetting mentions = add(new BooleanSetting("Mentions", "Highlight messages that contain your name.", true));
	public final BooleanSetting mentionSound = add(new BooleanSetting("Mention Sound", "Ping when you're mentioned.", true)
			.visibleWhen(mentions::get));
	public final StringSetting extraNames = add(new StringSetting("Also Highlight", "Extra words to treat as mentions, comma separated (e.g. a nickname).", "")
			.visibleWhen(mentions::get));
	public final StringSetting filter = add(new StringSetting("Hide Words", "Hide messages containing any of these, comma separated (case-insensitive).", ""));

	private record Recent(String text, long at) {
	}

	private final List<Recent> recent = new ArrayList<>();
	private int hiddenSpam;

	public BetterChatModule() {
		super("Better Chat", "Timestamps, anti-spam, filters and mention highlights.", Category.MISC);
		ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signed, sender, params, time) -> !intercept(message));
		ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> overlay || !intercept(message));
	}

	/** @return true if we took the message over (dropped it, or re-added it ourselves). */
	private boolean intercept(Component message) {
		if (!isEnabled() || mc.player == null) return false;
		String text = message.getString();
		String lower = text.toLowerCase(Locale.ROOT);
		// The client's own lines (command output, finder alerts) are never filtered.
		boolean ours = text.startsWith("Ooga »");

		if (!ours) {
			for (String word : words(filter.get())) {
				if (lower.contains(word)) return true;
			}
		}
		if (!ours && antiSpam.get() && isSpam(text)) {
			hiddenSpam++;
			return true;
		}

		boolean mentioned = mentions.get() && mentionsMe(lower);
		MutableComponent line = Component.empty();
		if (timestamps.get()) {
			String time = LocalTime.now().format(seconds.get() ? HH_MM_SS : HH_MM);
			line.append(Component.literal("[" + time + "] ").withStyle(s -> s.withColor(0x7A7D86)));
		}
		if (mentioned) {
			line.append(Component.literal("▍").withStyle(s -> s.withColor(OogaTheme.GOLD & 0xFFFFFF)));
			if (mentionSound.get()) mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.7f, 1.3f);
		}
		// Nothing to change: let vanilla handle it so signatures, narration and reporting stay intact.
		if (line.getSiblings().isEmpty()) return false;
		line.append(message);
		mc.gui.getChat().addMessage(line);
		return true;
	}

	private boolean isSpam(String text) {
		long now = System.currentTimeMillis();
		long window = spamWindow.getInt() * 1000L;
		recent.removeIf(r -> now - r.at() > window);
		for (Recent r : recent) {
			if (r.text().equals(text)) return true;
		}
		recent.add(new Recent(text, now));
		if (recent.size() > 50) recent.remove(0);
		return false;
	}

	private boolean mentionsMe(String lower) {
		if (mc.getUser() != null) {
			String me = mc.getUser().getName().toLowerCase(Locale.ROOT);
			// Our own chat lines start with "<name>" and shouldn't ping us.
			if (!me.isEmpty() && lower.contains(me) && !lower.startsWith("<" + me + ">")) return true;
		}
		for (String word : words(extraNames.get())) {
			if (lower.contains(word)) return true;
		}
		return false;
	}

	private static List<String> words(String csv) {
		List<String> out = new ArrayList<>();
		for (String part : csv.split(",")) {
			String w = part.trim().toLowerCase(Locale.ROOT);
			if (!w.isEmpty()) out.add(w);
		}
		return out;
	}

	@Override
	protected void onDisable() {
		recent.clear();
		hiddenSpam = 0;
	}

	@Override
	public String getSuffix() {
		return hiddenSpam > 0 ? hiddenSpam + " hidden" : null;
	}
}
