package dev.ooga.client.command;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ooga.client.config.ConfigManager;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.module.setting.Setting;
import dev.ooga.client.module.setting.StringSetting;
import dev.ooga.client.social.FriendStore;
import dev.ooga.client.util.ChatUtil;
import dev.ooga.client.waypoint.Waypoint;
import dev.ooga.client.waypoint.WaypointStore;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Client-side chat commands, typed with a {@code .} prefix (e.g. {@code .t freecam}). They're
 * handled locally and never reach the server. Start a message with {@code ..} to send a
 * literal dot.
 */
public final class CommandManager {
	public static final String PREFIX = ".";

	@FunctionalInterface
	private interface Handler {
		void run(String[] args);
	}

	private record Command(String usage, String description, Handler handler) {
	}

	private static final Map<String, Command> COMMANDS = new LinkedHashMap<>();
	private static final Map<String, String> ALIASES = Map.of("t", "toggle", "b", "bind", "wp", "waypoint", "h", "help", "f", "friend");

	private CommandManager() {
	}

	static {
		COMMANDS.put("help", new Command("help", "List commands.", args -> {
			for (Map.Entry<String, Command> e : COMMANDS.entrySet()) {
				ChatUtil.info(PREFIX + e.getValue().usage() + " — " + e.getValue().description());
			}
		}));
		COMMANDS.put("toggle", new Command("toggle <module>", "Turn a module on or off (alias .t).", args -> {
			Module module = module(args, 0);
			if (module == null) return;
			if (module.isSettingsOnly()) {
				ChatUtil.info(module.getName() + " has no on/off state.");
				return;
			}
			module.toggle();
			ChatUtil.info(module.getName() + (module.isEnabled() ? " enabled" : " disabled"));
		}));
		COMMANDS.put("bind", new Command("bind <module> <key|none>", "Set a module's keybind (alias .b).", args -> {
			if (args.length < 2) {
				ChatUtil.info("Usage: " + PREFIX + "bind <module> <key|none>");
				return;
			}
			Module module = moduleByName(String.join(" ", Arrays.copyOf(args, args.length - 1)));
			if (module == null) return;
			String keyName = args[args.length - 1].toLowerCase(Locale.ROOT);
			if (keyName.equals("none") || keyName.equals("unbind")) {
				module.setKey(-1);
				ChatUtil.info(module.getName() + " unbound");
				return;
			}
			try {
				int key = InputConstants.getKey("key.keyboard." + keyName).getValue();
				module.setKey(key);
				ChatUtil.info(module.getName() + " bound to " + keyName.toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				ChatUtil.info("Unknown key '" + keyName + "'. Try names like r, f6, left.alt, right.shift.");
			}
		}));
		COMMANDS.put("waypoint", new Command("wp <add|remove|list|clear> [name] [x y z]", "Manage waypoints for this server.", CommandManager::waypoint));
		COMMANDS.put("friend", new Command("friend <add|remove|list> [name]", "Manage friends (alias .f). Visual Range ignores them.", args -> {
			String sub = args.length == 0 ? "list" : args[0].toLowerCase(Locale.ROOT);
			switch (sub) {
				case "add" -> {
					if (args.length < 2) ChatUtil.info("Usage: " + PREFIX + "friend add <name>");
					else ChatUtil.info(FriendStore.add(args[1]) ? "Added " + args[1] + " as a friend" : args[1] + " is already a friend");
				}
				case "remove", "del" -> {
					if (args.length < 2) ChatUtil.info("Usage: " + PREFIX + "friend remove <name>");
					else ChatUtil.info(FriendStore.remove(args[1]) ? "Removed " + args[1] : args[1] + " isn't a friend");
				}
				case "list" -> {
					List<String> friends = FriendStore.all();
					ChatUtil.info(friends.isEmpty() ? "No friends yet. " + PREFIX + "friend add <name>" : "Friends: " + String.join(", ", friends));
				}
				default -> ChatUtil.info("Usage: " + PREFIX + "friend <add|remove|list> [name]");
			}
		}));
		COMMANDS.put("settings", new Command("settings <module>", "List a module's settings and their values.", args -> {
			Module module = module(args, 0);
			if (module == null) return;
			if (module.getSettings().isEmpty()) {
				ChatUtil.info(module.getName() + " has no settings.");
				return;
			}
			for (Setting<?> setting : module.getSettings()) ChatUtil.info(setting.getName() + ": " + show(setting));
		}));
		COMMANDS.put("set", new Command("set <module> <setting> <value>", "Change a setting from chat, e.g. .set debris finder range 200.", args -> {
			Match match = match(args);
			if (match == null) return;
			if (match.setting() == null || match.rest().isEmpty()) {
				ChatUtil.info("Usage: " + PREFIX + "set <module> <setting> <value>. See " + PREFIX + "settings " + match.module().getName());
				return;
			}
			String error = assign(match.setting(), match.rest());
			if (error != null) ChatUtil.info(error);
			else ChatUtil.info(match.module().getName() + " › " + match.setting().getName() + " = " + show(match.setting()));
		}));
		COMMANDS.put("reset", new Command("reset <module> [setting]", "Reset one setting, or all of a module's settings, to default.", args -> {
			Match match = match(args);
			if (match == null) return;
			if (match.setting() != null) {
				match.setting().reset();
				ChatUtil.info(match.module().getName() + " › " + match.setting().getName() + " reset to " + show(match.setting()));
			} else {
				for (Setting<?> setting : match.module().getSettings()) setting.reset();
				ChatUtil.info("Reset all of " + match.module().getName() + "'s settings");
			}
		}));
		COMMANDS.put("profile", new Command("profile <save|load|list|delete> [name]", "Save and switch between whole setups.", args -> {
			ConfigManager config = ConfigManager.get();
			String sub = args.length == 0 ? "list" : args[0].toLowerCase(Locale.ROOT);
			String name = args.length > 1 ? ConfigManager.cleanProfileName(args[1]) : "";
			switch (sub) {
				case "save" -> {
					if (name.isEmpty()) ChatUtil.info("Usage: " + PREFIX + "profile save <name> (letters, digits, - and _)");
					else ChatUtil.info(config.saveProfile(name) ? "Saved profile " + name : "Couldn't save " + name);
				}
				case "load" -> {
					if (name.isEmpty()) ChatUtil.info("Usage: " + PREFIX + "profile load <name>");
					else ChatUtil.info(config.loadProfile(name) ? "Loaded profile " + name : "No profile called " + name);
				}
				case "delete", "del" -> ChatUtil.info(config.deleteProfile(name) ? "Deleted profile " + name : "No profile called " + name);
				case "list" -> {
					List<String> names = config.profiles();
					ChatUtil.info(names.isEmpty() ? "No profiles yet. " + PREFIX + "profile save <name>" : "Profiles: " + String.join(", ", names));
				}
				default -> ChatUtil.info("Usage: " + PREFIX + "profile <save|load|list|delete> [name]");
			}
		}));
		COMMANDS.put("nether", new Command("nether [x z]", "Convert coordinates between the Overworld and the Nether (yours if none given).", args -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player == null) return;
			double x = mc.player.getX(), z = mc.player.getZ();
			if (args.length >= 2) {
				x = Double.parseDouble(args[0]);
				z = Double.parseDouble(args[1]);
			}
			boolean inNether = args.length < 2 && WaypointStore.currentDimension().equals("nether");
			if (inNether) {
				ChatUtil.info(String.format("Nether %.0f, %.0f  →  Overworld %.0f, %.0f", x, z, x * 8, z * 8));
			} else {
				ChatUtil.info(String.format("Overworld %.0f, %.0f  →  Nether %.0f, %.0f", x, z, x / 8, z / 8));
				if (args.length >= 2) ChatUtil.info(String.format("Nether %.0f, %.0f  →  Overworld %.0f, %.0f", x, z, x * 8, z * 8));
			}
		}));
		COMMANDS.put("coords", new Command("coords", "Copy your coordinates to the clipboard.", args -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player == null) return;
			BlockPos p = mc.player.blockPosition();
			String text = p.getX() + " " + p.getY() + " " + p.getZ();
			mc.keyboardHandler.setClipboard(text);
			ChatUtil.info("Copied " + text);
		}));
		COMMANDS.put("modules", new Command("modules", "List every module and whether it's on.", args -> {
			StringJoiner on = new StringJoiner(", ");
			StringJoiner off = new StringJoiner(", ");
			for (Module m : ModuleManager.get().getModules()) {
				if (m.isSettingsOnly()) continue;
				(m.isEnabled() ? on : off).add(m.getName());
			}
			ChatUtil.info("On: " + (on.length() == 0 ? "none" : on));
			ChatUtil.info("Off: " + (off.length() == 0 ? "none" : off));
		}));
	}

	/**
	 * @param message the raw chat line
	 * @return true if it was a client command (handled here, must not be sent)
	 */
	public static boolean handle(String message) {
		if (!message.startsWith(PREFIX) || message.startsWith(PREFIX + PREFIX)) return false;
		String[] parts = message.substring(PREFIX.length()).trim().split("\\s+");
		if (parts.length == 0 || parts[0].isEmpty()) return false;
		String name = parts[0].toLowerCase(Locale.ROOT);
		name = ALIASES.getOrDefault(name, name);
		Command command = COMMANDS.get(name);
		// Messages like "..." or ".5 blocks" aren't commands; let them through as chat.
		if (command == null) {
			if (!Character.isLetter(parts[0].charAt(0))) return false;
			ChatUtil.info("Unknown command. Type " + PREFIX + "help");
			return true;
		}
		try {
			command.handler().run(Arrays.copyOfRange(parts, 1, parts.length));
		} catch (RuntimeException e) {
			ChatUtil.info("That didn't work: " + e.getMessage());
		}
		return true;
	}

	/** A ".." message: strip one dot and send the rest as normal chat. */
	public static String unescape(String message) {
		return message.startsWith(PREFIX + PREFIX) ? message.substring(PREFIX.length()) : message;
	}

	private record Match(Module module, Setting<?> setting, String rest) {
	}

	/**
	 * Splits "debris finder max veins 20" into module, setting and value by matching the
	 * longest leading words against module names, then setting names.
	 */
	private static Match match(String[] args) {
		for (int end = args.length; end >= 1; end--) {
			String wanted = normalize(String.join(" ", Arrays.copyOfRange(args, 0, end)));
			for (Module m : ModuleManager.get().getModules()) {
				if (!normalize(m.getName()).equals(wanted)) continue;
				String[] rest = Arrays.copyOfRange(args, end, args.length);
				for (int send = rest.length; send >= 1; send--) {
					String settingName = normalize(String.join(" ", Arrays.copyOfRange(rest, 0, send)));
					for (Setting<?> setting : m.getSettings()) {
						if (normalize(setting.getName()).equals(settingName)) {
							return new Match(m, setting, String.join(" ", Arrays.copyOfRange(rest, send, rest.length)));
						}
					}
				}
				return new Match(m, null, String.join(" ", rest));
			}
		}
		ChatUtil.info(args.length == 0 ? "Which module?" : "No module matches that. Type " + PREFIX + "modules");
		return null;
	}

	/** @return an error message, or null when the value was applied. */
	private static String assign(Setting<?> setting, String value) {
		String v = value.trim();
		if (setting instanceof BooleanSetting bool) {
			switch (v.toLowerCase(Locale.ROOT)) {
				case "on", "true", "yes", "1" -> bool.set(true);
				case "off", "false", "no", "0" -> bool.set(false);
				case "toggle" -> bool.toggle();
				default -> {
					return "Use on, off or toggle.";
				}
			}
		} else if (setting instanceof NumberSetting number) {
			try {
				number.set(Double.parseDouble(v.replace("%", "").replace("m", "").replace("x", "").replace("s", "")));
			} catch (NumberFormatException e) {
				return "Not a number: " + v + " (range " + number.getMin() + " to " + number.getMax() + ")";
			}
		} else if (setting instanceof ModeSetting mode) {
			for (String option : mode.getModes()) {
				if (normalize(option).equals(normalize(v))) {
					mode.set(option);
					return null;
				}
			}
			return "Options: " + String.join(", ", mode.getModes());
		} else if (setting instanceof StringSetting text) {
			text.set(v);
		} else {
			return "That setting can't be changed from chat.";
		}
		return null;
	}

	private static String show(Setting<?> setting) {
		if (setting instanceof BooleanSetting bool) return bool.get() ? "on" : "off";
		if (setting instanceof NumberSetting number) return number.format();
		if (setting instanceof StringSetting text) return "\"" + text.get() + "\"";
		return String.valueOf(setting.get());
	}

	private static Module module(String[] args, int from) {
		if (args.length <= from) {
			ChatUtil.info("Which module?");
			return null;
		}
		return moduleByName(String.join(" ", Arrays.copyOfRange(args, from, args.length)));
	}

	/** Matches ignoring case, spaces and dashes, so "freecam", "Free-Cam" and "free cam" all work. */
	private static Module moduleByName(String name) {
		String wanted = normalize(name);
		for (Module m : ModuleManager.get().getModules()) {
			if (normalize(m.getName()).equals(wanted)) return m;
		}
		ChatUtil.info("No module called '" + name + "'. Type " + PREFIX + "modules");
		return null;
	}

	private static String normalize(String s) {
		return s.toLowerCase(Locale.ROOT).replace(" ", "").replace("-", "").replace("_", "");
	}

	private static void waypoint(String[] args) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		String sub = args.length == 0 ? "list" : args[0].toLowerCase(Locale.ROOT);
		switch (sub) {
			case "add", "set" -> {
				if (args.length < 2) {
					ChatUtil.info("Usage: " + PREFIX + "wp add <name> [x y z]");
					return;
				}
				BlockPos pos = mc.player.blockPosition();
				String name = args[1];
				if (args.length >= 5) {
					pos = new BlockPos(Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
				}
				WaypointStore.add(new Waypoint(name, pos, WaypointStore.currentDimension()));
				ChatUtil.info("Waypoint " + name + " set at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
			}
			case "remove", "del", "delete" -> {
				if (args.length < 2) {
					ChatUtil.info("Usage: " + PREFIX + "wp remove <name>");
					return;
				}
				ChatUtil.info(WaypointStore.remove(args[1]) ? "Removed " + args[1] : "No waypoint called " + args[1]);
			}
			case "clear" -> ChatUtil.info("Removed " + WaypointStore.clear() + " waypoints on this server.");
			case "list" -> {
				List<Waypoint> all = WaypointStore.current();
				if (all.isEmpty()) {
					ChatUtil.info("No waypoints here yet. " + PREFIX + "wp add <name>");
					return;
				}
				for (Waypoint w : all) {
					BlockPos p = w.pos();
					ChatUtil.info(w.name() + ": " + p.getX() + ", " + p.getY() + ", " + p.getZ() + " (" + w.dimension() + ")");
				}
			}
			default -> ChatUtil.info("Usage: " + PREFIX + "wp <add|remove|list|clear>");
		}
	}
}
