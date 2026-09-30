package dev.ooga.client;

import dev.ooga.client.camera.CameraController;
import dev.ooga.client.command.CommandManager;
import dev.ooga.client.config.ConfigManager;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.impl.client.MusicModule;
import dev.ooga.client.module.impl.client.NotificationsModule;
import dev.ooga.client.module.impl.misc.AutoReconnectModule;
import dev.ooga.client.module.impl.misc.FakePayModule;
import dev.ooga.client.module.impl.misc.FakeScoreboardModule;
import dev.ooga.client.module.impl.render.FreecamModule;
import dev.ooga.client.module.impl.render.FreelookModule;
import dev.ooga.client.module.impl.render.FullbrightModule;
import dev.ooga.client.render.WorldOverlay;
import dev.ooga.client.ui.hud.HudManager;
import dev.ooga.client.ui.hud.ScreenOverlay;
import dev.ooga.client.ui.notify.Notification;
import dev.ooga.client.ui.notify.NotificationManager;
import dev.ooga.client.world.BlockEntityTracker;
import dev.ooga.client.world.ChunkScanner;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.resources.Identifier;

public class OogaClient implements ClientModInitializer {
	public static final String MOD_ID = "ooga";
	public static final String VERSION = "0.1.0";

	@Override
	public void onInitializeClient() {
		ModuleManager modules = ModuleManager.get();
		modules.init();
		// Registered last so toasts always draw above other HUD elements.
		// World labels and screen flashes sit under the toasts but over the other HUD elements.
		HudManager.get().register(new ScreenOverlay());
		HudManager.get().register(NotificationManager.get());

		// One toggle, every system in sync: notification, HUD list (reads state live),
		// ClickGUI (reads state live) and config.
		modules.addToggleListener(OogaClient::announceToggle);
		modules.addDirtyListener(ConfigManager.get()::markDirty);

		WorldOverlay.init();
		BlockEntityTracker.init();
		ChunkScanner.init();

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "hud"), HudManager.get()::render);

		// Options and the window exist by the time the client has started, so restoring saved
		// module state (e.g. Fullbright touching gamma) is safe from here on.
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> ConfigManager.get().load());

		// Music controls are clickable while chat is open, the same way chat links are.
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof DisconnectedScreen) {
				AutoReconnectModule reconnect = modules.get(AutoReconnectModule.class);
				ScreenEvents.afterRender(screen).register((s, g, mouseX, mouseY, tickDelta) -> {
					String status = reconnect.status();
					if (status != null) g.drawCenteredString(client.font, status, s.width / 2, 12, 0xFFFFD36B);
				});
				return;
			}
			if (!(screen instanceof ChatScreen)) return;
			MusicModule music = modules.get(MusicModule.class);
			ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> !music.hud().handleClick(event.x(), event.y(), event.button()));
		});

		ClientTickEvents.START_CLIENT_TICK.register(client -> CameraController.get().tick());
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			modules.tick();
			ChunkScanner.tick();
			ConfigManager.get().tick();
		});

		// ".command" chat lines are handled locally; "..text" sends ".text".
		ClientSendMessageEvents.ALLOW_CHAT.register(message -> !CommandManager.handle(message));
		ClientSendMessageEvents.MODIFY_CHAT.register(CommandManager::unescape);

		// Fake Pay swallows your own /pay before it leaves the client.
		ClientSendMessageEvents.ALLOW_COMMAND.register(command -> !modules.get(FakePayModule.class).handleCommand(command));

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			modules.get(FakeScoreboardModule.class).onWorldJoin();
			modules.get(FakePayModule.class).resetSpent();
			modules.get(AutoReconnectModule.class).onJoin();
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			modules.get(FreecamModule.class).setEnabled(false, false);
			modules.get(FreelookModule.class).setEnabled(false, false);
			CameraController.get().exit();
			BlockEntityTracker.clear();
			ChunkScanner.clear();
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			modules.get(FreecamModule.class).setEnabled(false, false);
			modules.get(FullbrightModule.class).restoreGamma();
			ConfigManager.get().save();
		});
	}

	private static void announceToggle(Module module, boolean announce) {
		if (!announce || module.isSettingsOnly()) return;
		NotificationsModule settings = NotificationsModule.instance();
		if (settings == null || !settings.toggles.get()) return;
		// Don't announce the notifications module turning itself on or off via a toast.
		if (module == settings) return;
		boolean on = module.isEnabled();
		NotificationManager.get().push(module.getName(), on ? "Enabled" : "Disabled",
				on ? Notification.Kind.ENABLED : Notification.Kind.DISABLED);
	}
}
