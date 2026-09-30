package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.impl.client.FriendsModule;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.util.ChatUtil;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Accepts teleport requests automatically, from friends only by default. Recognises the usual
 * wording ("X has requested to teleport to you", "X wants to teleport to you", "tpa request
 * from X"…) and answers with {@code /tpaccept X}.
 */
public class AutoTpaModule extends Module {
	private static final Pattern REQUEST = Pattern.compile(
			"(?:^|\\s)([A-Za-z0-9_]{3,16})\\s+(?:has\\s+)?(?:requested|wants|sent|would like)[^\\n]*?(?:teleport|tpa)|(?:teleport|tpa)\\s+request\\s+from\\s+([A-Za-z0-9_]{3,16})",
			Pattern.CASE_INSENSITIVE);

	public final ModeSetting from = add(new ModeSetting("From", "Whose requests to accept.", "Friends", "Friends", "Everyone"));
	public final NumberSetting delay = add(new NumberSetting("Delay", "Seconds to wait before accepting.", 1.0, 0.0, 5.0, 0.25, "s"));

	private String pending;
	private long acceptAt;

	public AutoTpaModule() {
		super("Auto TPA", "Accepts teleport requests from friends automatically.", Category.MISC);
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay) onMessage(message);
		});
		ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, time) -> onMessage(message));
	}

	private void onMessage(Component message) {
		if (!isEnabled() || mc.player == null) return;
		String text = message.getString();
		if (!text.toLowerCase(Locale.ROOT).contains("tp") && !text.toLowerCase(Locale.ROOT).contains("teleport")) return;
		Matcher matcher = REQUEST.matcher(text);
		if (!matcher.find()) return;
		String name = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
		if (name == null || name.equalsIgnoreCase(mc.player.getGameProfile().name())) return;
		if (from.is("Friends") && !ModuleManager.get().get(FriendsModule.class).isFriend(name)) return;
		pending = name;
		acceptAt = System.currentTimeMillis() + Math.round(delay.get() * 1000);
	}

	@Override
	public void onTick() {
		if (pending == null || mc.getConnection() == null || System.currentTimeMillis() < acceptAt) return;
		mc.getConnection().sendCommand("tpaccept " + pending);
		ChatUtil.info("Accepted teleport request from " + pending);
		pending = null;
	}
}
