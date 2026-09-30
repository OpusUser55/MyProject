package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.hud.AdminsHud;
import dev.ooga.client.ui.hud.HudManager;
import dev.ooga.client.ui.notify.Notification;
import dev.ooga.client.ui.notify.NotificationManager;
import dev.ooga.client.util.ChatUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Spots server staff in the tab list and warns you when they join or leave. A player counts
 * as staff when their tab name or team prefix carries a staff rank, when they sit in spectator
 * mode (how most staff vanish and watch), or when their name is in
 * {@code config/ooga/staff.txt}.
 */
public class AdminDetectorModule extends Module {
	private static final Pattern RANKS = Pattern.compile(
			"\\b(owner|co-?owner|founder|manager|admin|administrator|sr\\.? ?mod|jr\\.? ?mod|mod|moderator|helper|staff|dev|developer|support|trial ?mod|sr\\.? ?admin)\\b");
	private static final Path LIST = FabricLoader.getInstance().getConfigDir().resolve("ooga").resolve("staff.txt");

	public final BooleanSetting ranks = add(new BooleanSetting("Rank Names", "Count players whose tab name or prefix has a staff rank (Admin, Mod, Helper…).", true));
	public final BooleanSetting spectators = add(new BooleanSetting("Spectators", "Count players in spectator mode: vanished staff usually are.", true));
	public final BooleanSetting customList = add(new BooleanSetting("Custom List", "Count names listed in config/ooga/staff.txt, one per line.", true));
	public final BooleanSetting alerts = add(new BooleanSetting("Alerts", "Chat message and notification when staff join or leave.", true));
	public final BooleanSetting sound = add(new BooleanSetting("Sound", "Warning sound when staff join.", true));
	public final NumberSetting scale = add(new NumberSetting("Scale", "Panel size.", 1.0, 0.5, 2.0, 0.05, "x"));

	/** A staff member currently online, and why we think so. */
	public record Staff(PlayerInfo info, String name, String reason) {
	}

	private final List<Staff> online = new ArrayList<>();
	private final Set<String> known = new HashSet<>();
	private Set<String> custom = Set.of();
	private long customLoadedAt;
	private boolean primed;

	public AdminDetectorModule() {
		super("Admin Detector", "Lists staff online and warns you when they join or leave.", Category.MISC);
		enableByDefault();
		HudManager.get().register(new AdminsHud(this));
	}

	@Override
	protected void onEnable() {
		known.clear();
		online.clear();
		primed = false;
	}

	public List<Staff> online() {
		return online;
	}

	@Override
	public void onTick() {
		if (mc.getConnection() == null || mc.player == null) {
			online.clear();
			known.clear();
			primed = false;
			return;
		}
		if (mc.player.tickCount % 10 != 0) return;
		reloadCustomList();

		Map<String, Staff> now = new LinkedHashMap<>();
		for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
			String name = info.getProfile().name();
			if (name == null || name.equals(mc.player.getGameProfile().name())) continue;
			String reason = reason(info, name);
			if (reason != null) now.put(name, new Staff(info, name, reason));
		}
		online.clear();
		online.addAll(now.values());
		online.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));

		// Everyone already online when we join is listed silently; only changes are announced.
		if (primed && alerts.get()) {
			for (String name : now.keySet()) {
				if (!known.contains(name)) announce(name, now.get(name).reason(), true);
			}
			for (String name : known) {
				if (!now.containsKey(name)) announce(name, null, false);
			}
		}
		known.clear();
		known.addAll(now.keySet());
		primed = true;
	}

	private String reason(PlayerInfo info, String name) {
		if (customList.get() && custom.contains(name.toLowerCase(Locale.ROOT))) return "listed";
		if (ranks.get()) {
			String rank = rankOf(info);
			if (rank != null) return rank;
		}
		if (spectators.get() && info.getGameMode() == GameType.SPECTATOR) return "spectator";
		return null;
	}

	private static String rankOf(PlayerInfo info) {
		StringBuilder text = new StringBuilder();
		Component display = info.getTabListDisplayName();
		if (display != null) text.append(display.getString()).append(' ');
		PlayerTeam team = info.getTeam();
		if (team != null) text.append(team.getPlayerPrefix().getString()).append(' ').append(team.getName());
		var matcher = RANKS.matcher(text.toString().toLowerCase(Locale.ROOT).replace('_', ' '));
		return matcher.find() ? matcher.group(1) : null;
	}

	private void announce(String name, String reason, boolean joined) {
		String what = joined ? "joined" : "left";
		ChatUtil.info("Staff " + what + ": " + name + (reason != null ? " (" + reason + ")" : ""));
		NotificationManager.get().push(joined ? "Staff online" : "Staff left", name, Notification.Kind.INFO);
		if (joined && sound.get()) {
			mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), 0.7f, 0.8f));
		}
	}

	/** Re-reads staff.txt every few seconds so edits apply without restarting. */
	private void reloadCustomList() {
		long time = System.currentTimeMillis();
		if (time - customLoadedAt < 5000) return;
		customLoadedAt = time;
		try {
			if (!Files.exists(LIST)) {
				Files.createDirectories(LIST.getParent());
				Files.writeString(LIST, "# One staff name per line. Lines starting with # are ignored.\n", StandardCharsets.UTF_8);
			}
			Set<String> names = new HashSet<>();
			for (String line : Files.readAllLines(LIST, StandardCharsets.UTF_8)) {
				String trimmed = line.trim();
				if (!trimmed.isEmpty() && !trimmed.startsWith("#")) names.add(trimmed.toLowerCase(Locale.ROOT));
			}
			custom = names;
		} catch (IOException e) {
			ModuleManager.LOGGER.warn("Couldn't read {}", LIST, e);
		}
	}
}
