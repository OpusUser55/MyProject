package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.impl.client.FriendsModule;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.notify.Notification;
import dev.ooga.client.ui.notify.NotificationManager;
import dev.ooga.client.util.ChatUtil;
import dev.ooga.client.world.BlockEntityTracker;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Guards your spawner base: when a player who isn't your friend comes near spawners you're
 * standing by, you get a loud alert, and optionally get logged out before they reach you.
 */
public class SpawnerProtectModule extends Module {
	public final NumberSetting range = add(new NumberSetting("Range", "How close a player must get to your spawners.", 48, 8, 128, 4, "m"));
	public final BooleanSetting logOut = add(new BooleanSetting("Log Out", "Disconnect when someone shows up.", false));

	private final Set<String> alerted = new HashSet<>();

	public SpawnerProtectModule() {
		super("Spawner Protect", "Warns you (or logs you out) when players approach your spawners.", Category.MISC);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.level == null || mc.player.tickCount % 10 != 0) return;
		boolean spawnersHere = false;
		for (BlockEntity be : BlockEntityTracker.all()) {
			if (be instanceof SpawnerBlockEntity && !be.isRemoved() && be.getBlockPos().distToCenterSqr(mc.player.position()) < 32 * 32) {
				spawnersHere = true;
				break;
			}
		}
		if (!spawnersHere) {
			alerted.clear();
			return;
		}
		double max = range.get();
		Set<String> near = new HashSet<>();
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!(entity instanceof Player other) || other == mc.player || FriendsModule.protects(other)) continue;
			if (other.distanceTo(mc.player) > max) continue;
			String name = other.getGameProfile().name();
			near.add(name);
			if (!alerted.add(name)) continue;
			int distance = Math.round(other.distanceTo(mc.player));
			ChatUtil.info(name + " is near your spawners (" + distance + "m)");
			NotificationManager.get().push("Intruder!", name + " · " + distance + "m", Notification.Kind.INFO);
			mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 0.5f, 1f));
			if (logOut.get() && mc.getConnection() != null) {
				setEnabled(false, false);
				AutoReconnectModule.suppressNextReconnect();
				mc.getConnection().getConnection().disconnect(Component.literal("[Ooga] Spawner Protect: " + name + " came near"));
				return;
			}
		}
		alerted.retainAll(near);
	}
}
