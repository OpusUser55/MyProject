package dev.ooga.client;

import dev.ooga.client.camera.CameraController;
import dev.ooga.client.config.ConfigManager;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.impl.client.NotificationsModule;
import dev.ooga.client.module.impl.render.FreecamModule;
import dev.ooga.client.module.impl.render.FullbrightModule;
import dev.ooga.client.ui.hud.HudManager;
import dev.ooga.client.ui.notify.Notification;
import dev.ooga.client.ui.notify.NotificationManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

public class OogaClient implements ClientModInitializer {
	public static final String MOD_ID = "ooga";
	public static final String VERSION = "0.1.0";

	@Override
	public void onInitializeClient() {
		ModuleManager modules = ModuleManager.get();
		modules.init();
		// Registered last so toasts always draw above other HUD elements.
		HudManager.get().register(NotificationManager.get());

		// One toggle, every system in sync: notification, HUD list (reads state live),
		// ClickGUI (reads state live) and config.
		modules.addToggleListener(OogaClient::announceToggle);
		modules.addDirtyListener(ConfigManager.get()::markDirty);

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "hud"), HudManager.get()::render);

		// Options and the window exist by the time the client has started, so restoring saved
		// module state (e.g. Fullbright touching gamma) is safe from here on.
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> ConfigManager.get().load());

		ClientTickEvents.START_CLIENT_TICK.register(client -> CameraController.get().tick());
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			modules.tick();
			ConfigManager.get().tick();
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			modules.get(FreecamModule.class).setEnabled(false, false);
			CameraController.get().exit();
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
