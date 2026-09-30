package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.NumberSetting;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

/**
 * Adds a Reconnect button to the disconnect screen and, while enabled, counts down and
 * rejoins the last server by itself. Auto Log's own disconnects are never undone.
 */
public class AutoReconnectModule extends Module {
	private static AutoReconnectModule instance;

	public final NumberSetting delay = add(new NumberSetting("Delay", "Seconds to wait before reconnecting.", 5, 1, 60, 1, "s"));

	private ServerData lastServer;
	/** Set by Auto Log so a deliberate disconnect isn't immediately reversed. */
	private boolean suppressNext;

	public AutoReconnectModule() {
		super("Auto Reconnect", "Rejoins the server automatically after you get disconnected.", Category.MISC);
		instance = this;
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (client.getCurrentServer() != null) lastServer = client.getCurrentServer();
		});
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof DisconnectedScreen) decorate(screen, width, height);
		});
	}

	public static void suppressNextReconnect() {
		if (instance != null) instance.suppressNext = true;
	}

	private void decorate(Screen screen, int width, int height) {
		if (lastServer == null) return;
		boolean auto = isEnabled() && !suppressNext;
		suppressNext = false;
		ServerData server = lastServer;
		long deadline = System.currentTimeMillis() + delay.getInt() * 1000L;
		Button button = Button.builder(label(auto, deadline), b -> connect(server))
				.bounds(width / 2 - 100, height - 30, 200, 20).build();
		Screens.getButtons(screen).add(button);
		if (!auto) return;
		ScreenEvents.afterTick(screen).register(s -> {
			if (mc.screen != s) return;
			button.setMessage(label(true, deadline));
			if (System.currentTimeMillis() >= deadline) connect(server);
		});
	}

	private static Component label(boolean auto, long deadline) {
		if (!auto) return Component.literal("Reconnect");
		long left = Math.max(0, (deadline - System.currentTimeMillis() + 999) / 1000);
		return Component.literal("Reconnecting in " + left + "s");
	}

	private void connect(ServerData server) {
		Screen parent = new JoinMultiplayerScreen(new TitleScreen());
		ConnectScreen.startConnecting(parent, mc, ServerAddress.parseString(server.ip), server, false, null);
	}
}
