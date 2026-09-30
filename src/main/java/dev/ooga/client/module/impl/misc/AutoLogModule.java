package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Disconnects you when things go wrong: low health, staff coming online, or a player getting
 * close. Turns itself off afterwards so you don't get logged out again as soon as you rejoin.
 */
public class AutoLogModule extends Module {
	public final NumberSetting health = add(new NumberSetting("Health", "Log out at or below this much health (hearts x2). 0 turns it off.", 6, 0, 19, 1));
	public final BooleanSetting onStaff = add(new BooleanSetting("On Staff", "Log out when Admin Detector sees staff come online.", false));
	public final NumberSetting playerRange = add(new NumberSetting("Player Range", "Log out when another player comes this close. 0 turns it off.", 0, 0, 128, 4, "m"));

	private int lastStaff = -1;

	public AutoLogModule() {
		super("Auto Log", "Disconnects on low health, staff or nearby players.", Category.MISC);
	}

	@Override
	protected void onEnable() {
		lastStaff = -1;
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.level == null || mc.getConnection() == null) return;
		if (health.getInt() > 0 && mc.player.getHealth() <= health.getInt() && mc.player.isAlive()) {
			logOut("health " + Math.round(mc.player.getHealth()));
			return;
		}
		if (onStaff.get()) {
			AdminDetectorModule detector = ModuleManager.get().get(AdminDetectorModule.class);
			int staff = detector.isEnabled() ? detector.online().size() : 0;
			if (lastStaff >= 0 && staff > lastStaff) {
				logOut("staff came online");
				return;
			}
			lastStaff = staff;
		}
		double range = playerRange.get();
		if (range > 0) {
			for (Entity entity : mc.level.entitiesForRendering()) {
				if (entity instanceof Player other && other != mc.player && other.distanceTo(mc.player) <= range) {
					logOut(other.getName().getString() + " came within " + Math.round(other.distanceTo(mc.player)) + "m");
					return;
				}
			}
		}
	}

	private void logOut(String reason) {
		setEnabled(false, false);
		AutoReconnectModule.suppressNextReconnect();
		mc.getConnection().getConnection().disconnect(Component.literal("[Ooga] Auto Log: " + reason));
	}
}
