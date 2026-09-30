package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.NumberSetting;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

/**
 * Rejoins the last server after a disconnect, following a countdown shown on the disconnect
 * screen. Opening any other screen (e.g. pressing "Back to server list") cancels it.
 */
public class AutoReconnectModule extends Module {
	public final NumberSetting delay = add(new NumberSetting("Delay", "Seconds to wait before rejoining.", 5, 1, 60, 1, "s"));
	public final NumberSetting attempts = add(new NumberSetting("Max Attempts", "Give up after this many tries in a row (0 = never).", 0, 0, 20, 1));

	private ServerData lastServer;
	private DisconnectedScreen waitingOn;
	private long reconnectAt;
	private int tries;

	public AutoReconnectModule() {
		super("Auto Reconnect", "Rejoins the server after you get disconnected.", Category.MISC);
	}

	/** Remember where we are, so there's something to reconnect to. Called on join. */
	public void onJoin() {
		if (mc.getCurrentServer() != null) lastServer = mc.getCurrentServer();
		// A successful join resets the retry count.
		tries = 0;
	}

	@Override
	public void onTick() {
		if (!(mc.screen instanceof DisconnectedScreen screen) || lastServer == null) {
			waitingOn = null;
			return;
		}
		if (attempts.getInt() > 0 && tries >= attempts.getInt()) return;
		if (waitingOn != screen) {
			waitingOn = screen;
			reconnectAt = System.currentTimeMillis() + delay.getInt() * 1000L;
			return;
		}
		if (System.currentTimeMillis() < reconnectAt) return;
		waitingOn = null;
		tries++;
		ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()), mc,
				ServerAddress.parseString(lastServer.ip), lastServer, false, null);
	}

	/** Text for the disconnect screen, or null when no reconnect is pending. */
	public String status() {
		if (!isEnabled() || waitingOn == null || waitingOn != mc.screen) return null;
		long left = Math.max(0, reconnectAt - System.currentTimeMillis());
		String tryText = attempts.getInt() > 0 ? "  (try " + (tries + 1) + "/" + attempts.getInt() + ")" : "";
		return "Reconnecting to " + lastServer.ip + " in " + (left + 999) / 1000 + "s" + tryText;
	}
}
