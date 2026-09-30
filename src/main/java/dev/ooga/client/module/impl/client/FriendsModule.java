package dev.ooga.client.module.impl.client;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.ui.notify.Notification;
import dev.ooga.client.ui.notify.NotificationManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Your friends list. Middle-click a player to add or remove them. While on, combat modules
 * leave friends alone and ESP, nametags and the radar show them in green. Saved in
 * {@code config/ooga/friends.txt}, one name per line.
 */
public class FriendsModule extends Module {
	public static final int COLOR = 0xFF46C37B;
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("ooga").resolve("friends.txt");
	private static FriendsModule instance;

	public final BooleanSetting middleClick = add(new BooleanSetting("Middle Click", "Middle-click a player to add or remove them.", true));
	public final BooleanSetting protect = add(new BooleanSetting("Don't Attack", "Trigger Bot, Aim Assist and Auto Log ignore friends.", true));
	public final BooleanSetting highlight = add(new BooleanSetting("Highlight", "Show friends in green in ESP, nametags and the radar.", true));

	private final Set<String> names = new TreeSet<>();
	private boolean loaded;

	public FriendsModule() {
		super("Friends", "Middle-click players to befriend them; combat leaves friends alone.", Category.CLIENT);
		enableByDefault();
		hideFromList();
		instance = this;
	}

	private void load() {
		if (loaded) return;
		loaded = true;
		try {
			if (!Files.exists(FILE)) return;
			for (String line : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
				String name = line.trim();
				if (!name.isEmpty() && !name.startsWith("#")) names.add(name.toLowerCase(Locale.ROOT));
			}
		} catch (IOException e) {
			ModuleManager.LOGGER.warn("Couldn't read {}", FILE, e);
		}
	}

	private void save() {
		List<String> lines = new ArrayList<>();
		lines.add("# One friend per line.");
		lines.addAll(names);
		try {
			Files.createDirectories(FILE.getParent());
			Files.write(FILE, lines, StandardCharsets.UTF_8);
		} catch (IOException e) {
			ModuleManager.LOGGER.warn("Couldn't write {}", FILE, e);
		}
	}

	public boolean isFriend(String name) {
		load();
		return name != null && names.contains(name.toLowerCase(Locale.ROOT));
	}

	/** Called on a middle click with no screen open. */
	public void onMiddleClick() {
		if (!isEnabled() || !middleClick.get() || !(mc.crosshairPickEntity instanceof Player player)) return;
		load();
		String name = player.getGameProfile().name();
		boolean added = names.add(name.toLowerCase(Locale.ROOT));
		if (!added) names.remove(name.toLowerCase(Locale.ROOT));
		save();
		NotificationManager.get().push(added ? "Friend added" : "Friend removed", name, Notification.Kind.INFO);
	}

	/** True if {@code entity} is a friend and combat should leave it alone. */
	public static boolean protects(Entity entity) {
		FriendsModule self = instance;
		return self != null && self.isEnabled() && self.protect.get() && entity instanceof Player player
				&& self.isFriend(player.getGameProfile().name());
	}

	/** True if {@code entity} is a friend and visuals should show it in {@link #COLOR}. */
	public static boolean highlights(Entity entity) {
		FriendsModule self = instance;
		return self != null && self.isEnabled() && self.highlight.get() && entity instanceof Player player
				&& self.isFriend(player.getGameProfile().name());
	}
}
