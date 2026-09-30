package dev.ooga.client.command;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.impl.client.FriendsModule;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.module.setting.Setting;
import dev.ooga.client.util.ChatUtil;
import dev.ooga.client.world.Finds;
import dev.ooga.client.world.Waypoints;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Client commands typed in chat with a leading dot, e.g. {@code .toggle esp}. They never reach
 * the server. {@code .help} lists them.
 */
public final class Commands {
	public static final String PREFIX = ".";

	private record Command(String usage, String help, Consumer<String[]> run) {
	}

	private static final Map<String, Command> COMMANDS = new LinkedHashMap<>();

	private Commands() {
	}

	public static void init() {
		register("help", "", "List commands.", args -> {
			ChatUtil.info("Commands:");
			COMMANDS.forEach((name, c) -> ChatUtil.info(PREFIX + name + (c.usage().isEmpty() ? "" : " " + c.usage()) + "  - " + c.help()));
		});
		register("toggle", "<module>", "Turn a module on or off.", args -> {
			Module m = module(args, 0);
			if (m == null) return;
			m.toggle();
			ChatUtil.info(m.getName() + (m.isEnabled() ? " enabled" : " disabled"));
		});
		register("bind", "<module> <key>", "Bind a module to a key (e.g. .bind esp g). 'none' unbinds.", args -> {
			if (args.length < 2) {
				usage("bind");
				return;
			}
			Module m = module(Arrays.copyOf(args, args.length - 1), 0);
			if (m == null) return;
			String key = args[args.length - 1].toLowerCase(Locale.ROOT);
			if (key.equals("none")) {
				m.setKey(-1);
				ChatUtil.info(m.getName() + " unbound");
				return;
			}
			InputConstants.Key parsed = InputConstants.getKey("key.keyboard." + key);
			if (parsed == null || parsed.equals(InputConstants.UNKNOWN)) {
				ChatUtil.info("Unknown key: " + key);
				return;
			}
			m.setKey(parsed.getValue());
			ChatUtil.info(m.getName() + " bound to " + key.toUpperCase(Locale.ROOT));
		});
		register("set", "<module> <setting> <value>", "Change a setting (spaces in names: use_underscores).", args -> {
			if (args.length < 3) {
				usage("set");
				return;
			}
			Module m = module(new String[]{args[0]}, 0);
			if (m == null) return;
			String wanted = simplify(args[1]);
			for (Setting<?> setting : m.getSettings()) {
				if (!simplify(setting.getName()).equals(wanted)) continue;
				String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
				if (setting instanceof BooleanSetting b) b.set(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("on"));
				else if (setting instanceof ModeSetting mode) mode.set(value);
				else if (setting instanceof NumberSetting num) {
					try {
						num.set(Double.parseDouble(value));
					} catch (NumberFormatException e) {
						ChatUtil.info("Not a number: " + value);
						return;
					}
				}
				ChatUtil.info(m.getName() + " › " + setting.getName() + " = " + setting.get());
				return;
			}
			ChatUtil.info("No setting '" + args[1] + "' in " + m.getName());
		});
		register("friend", "add|remove|list <name>", "Manage friends.", args -> {
			FriendsModule friends = ModuleManager.get().get(FriendsModule.class);
			if (args.length >= 1 && args[0].equalsIgnoreCase("list")) {
				ChatUtil.info("Friends: " + (friends.friends().isEmpty() ? "none" : String.join(", ", friends.friends())));
				return;
			}
			if (args.length < 2) {
				usage("friend");
				return;
			}
			boolean add = args[0].equalsIgnoreCase("add");
			friends.setFriend(args[1], add);
			ChatUtil.info(args[1] + (add ? " added to" : " removed from") + " friends");
		});
		register("wp", "add [name] | del <name> | list | clear", "Manage waypoints here.", args -> {
			Minecraft mc = Minecraft.getInstance();
			String sub = args.length == 0 ? "list" : args[0].toLowerCase(Locale.ROOT);
			String name = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : null;
			switch (sub) {
				case "add" -> {
					if (mc.player == null) return;
					var w = Waypoints.add(name != null ? name : Waypoints.nextName("Waypoint"), mc.player.blockPosition());
					ChatUtil.info("Waypoint " + w.name + " at " + w.x + ", " + w.y + ", " + w.z);
				}
				case "del", "remove" -> ChatUtil.info(name != null && Waypoints.remove(name) ? "Removed " + name : "No waypoint named " + name);
				case "clear" -> ChatUtil.info("Removed " + Waypoints.clearHere() + " waypoints here");
				default -> {
					var here = Waypoints.here();
					if (here.isEmpty()) ChatUtil.info("No waypoints here. .wp add <name>");
					for (var w : here) ChatUtil.info(w.name + ": " + w.x + ", " + w.y + ", " + w.z);
				}
			}
		});
		register("config", "save|load|delete <name> | list", "Save and load named configs.", args -> {
			String sub = args.length == 0 ? "list" : args[0].toLowerCase(Locale.ROOT);
			String name = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "";
			switch (sub) {
				case "save" -> ChatUtil.info(dev.ooga.client.config.Profiles.save(name) ? "Saved config " + name : "Give the config a name");
				case "load" -> ChatUtil.info(dev.ooga.client.config.Profiles.load(name) ? "Loaded config " + name : "No config called " + name);
				case "delete" -> ChatUtil.info(dev.ooga.client.config.Profiles.delete(name) ? "Deleted config " + name : "No config called " + name);
				default -> {
					var saved = dev.ooga.client.config.Profiles.list();
					List<String> names = new ArrayList<>();
					for (var e : saved) names.add(e.name());
					ChatUtil.info(saved.isEmpty() ? "No saved configs. .config save <name>" : "Configs: " + String.join(", ", names));
				}
			}
		});
		register("finds", "[clear]", "List recent finds, or forget them.", args -> {
			if (args.length > 0 && args[0].equalsIgnoreCase("clear")) {
				Finds.clear();
				ChatUtil.info("Finds cleared");
				return;
			}
			List<Finds.Find> finds = Finds.recent();
			if (finds.isEmpty()) ChatUtil.info("Nothing found yet");
			for (Finds.Find f : finds) {
				ChatUtil.info(f.type() + (f.detail() == null ? "" : " · " + f.detail()) + " at " + f.pos().getX() + ", " + f.pos().getY() + ", " + f.pos().getZ());
			}
		});
		register("coords", "", "Copy your coordinates.", args -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player == null) return;
			String text = mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ();
			mc.keyboardHandler.setClipboard(text);
			ChatUtil.info("Copied " + text);
		});
		register("modules", "", "List modules that are on.", args -> {
			List<String> on = new ArrayList<>();
			for (Module m : ModuleManager.get().getModules()) if (m.isEnabled() && !m.isSettingsOnly()) on.add(m.getName());
			ChatUtil.info(on.size() + " on: " + String.join(", ", on));
		});

		ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
			if (!message.startsWith(PREFIX) || message.length() < 2 || message.startsWith(PREFIX + PREFIX)) return true;
			Minecraft.getInstance().gui.getChat().addRecentChat(message);
			run(message.substring(PREFIX.length()));
			return false;
		});
	}

	private static void register(String name, String usage, String help, Consumer<String[]> run) {
		COMMANDS.put(name, new Command(usage, help, run));
	}

	private static void run(String line) {
		String[] parts = line.trim().split("\\s+");
		Command command = COMMANDS.get(parts[0].toLowerCase(Locale.ROOT));
		if (command == null) {
			ChatUtil.info("Unknown command. Try " + PREFIX + "help");
			return;
		}
		try {
			command.run().accept(Arrays.copyOfRange(parts, 1, parts.length));
		} catch (RuntimeException e) {
			ChatUtil.info("That didn't work: " + e.getMessage());
		}
	}

	private static void usage(String name) {
		ChatUtil.info("Usage: " + PREFIX + name + " " + COMMANDS.get(name).usage());
	}

	/** Finds a module from the words starting at {@code from}, ignoring spaces and case. */
	private static Module module(String[] args, int from) {
		if (args.length <= from) {
			ChatUtil.info("Which module?");
			return null;
		}
		String wanted = simplify(String.join("", Arrays.copyOfRange(args, from, args.length)));
		for (Module m : ModuleManager.get().getModules()) if (simplify(m.getName()).equals(wanted)) return m;
		ChatUtil.info("No module called " + String.join(" ", Arrays.copyOfRange(args, from, args.length)));
		return null;
	}

	private static String simplify(String s) {
		return s.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "").replace("-", "");
	}
}
