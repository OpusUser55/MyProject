package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.ui.notify.Notification;
import dev.ooga.client.ui.notify.NotificationManager;
import dev.ooga.client.waypoint.Waypoint;
import dev.ooga.client.waypoint.WaypointStore;
import net.minecraft.core.BlockPos;

/**
 * Bind a key to this and press it to drop a waypoint where you stand ("mark1", "mark2", …).
 * It has no on state: pressing the key (or clicking it in the menu) just marks the spot.
 */
public class MarkSpotModule extends Module {
	public MarkSpotModule() {
		super("Mark Spot", "Press its key to save a waypoint where you stand.", Category.MISC);
		hideFromList();
	}

	@Override
	public void onKeybind() {
		mark();
	}

	@Override
	protected void onEnable() {
		mark();
	}

	@Override
	public void onTick() {
		// Clicking it in the menu marks once and switches straight back off.
		setEnabled(false, false);
	}

	@Override
	public boolean persistsEnabledState() {
		return false;
	}

	private void mark() {
		if (mc.player == null) return;
		int n = 1;
		while (WaypointStore.find("mark" + n) != null) n++;
		BlockPos pos = mc.player.blockPosition();
		WaypointStore.add(new Waypoint("mark" + n, pos, WaypointStore.currentDimension()));
		NotificationManager.get().push("Marked", "mark" + n + " at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ(), Notification.Kind.INFO);
	}
}
