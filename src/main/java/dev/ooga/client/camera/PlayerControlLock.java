package dev.ooga.client.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;

/**
 * Keeps the real player standing still while the camera flies around.
 *
 * <p>Instead of intercepting individual movement code paths, the player's input source is
 * swapped for an inert one. Movement keys keep working as {@code KeyMapping}s, so the camera
 * can still read them, but they no longer reach the player.
 */
final class PlayerControlLock {
	private boolean locked;

	void lock() {
		locked = true;
		apply();
	}

	/** Re-applies the lock if the game replaced the input (e.g. after respawning). */
	void tick() {
		if (locked) apply();
	}

	private void apply() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null && player.input instanceof KeyboardInput) {
			player.input = new ClientInput();
		}
	}

	void unlock() {
		if (!locked) return;
		locked = false;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && !(mc.player.input instanceof KeyboardInput)) {
			mc.player.input = new KeyboardInput(mc.options);
		}
	}
}
