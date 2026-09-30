package dev.ooga.client.module.impl.client;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.hud.HudManager;
import dev.ooga.client.ui.hud.InfoHud;
import net.minecraft.world.phys.Vec3;

public class InfoHudModule extends Module {
	public final BooleanSetting fps = add(new BooleanSetting("FPS", "Frames per second.", true));
	public final BooleanSetting coords = add(new BooleanSetting("Coordinates", "Your position.", true));
	public final BooleanSetting otherDimension = add(new BooleanSetting("Nether Coords", "Also show the matching Overworld/Nether position.", true)
			.visibleWhen(coords::get));
	public final BooleanSetting facing = add(new BooleanSetting("Facing", "Compass direction you're looking.", true));
	public final BooleanSetting speed = add(new BooleanSetting("Speed", "Horizontal speed in blocks per second.", false));
	public final BooleanSetting ping = add(new BooleanSetting("Ping", "Latency to the server.", true));
	public final BooleanSetting tps = add(new BooleanSetting("TPS", "Server ticks per second (20 is healthy), estimated from time updates.", true));
	public final BooleanSetting server = add(new BooleanSetting("Server", "The address of the server you're on.", false));
	public final NumberSetting decimals = add(new NumberSetting("Decimals", "Decimal places for coordinates.", 0, 0, 2, 1)
			.visibleWhen(coords::get));
	public final BooleanSetting labels = add(new BooleanSetting("Labels", "Show the small FPS / XYZ / DIR labels.", true));
	public final NumberSetting scale = add(new NumberSetting("Scale", "Panel size.", 1.0, 0.5, 2.0, 0.05, "x"));

	private Vec3 lastPosition;
	private double blocksPerSecond;

	public InfoHudModule() {
		super("Info HUD", "FPS, coordinates, facing, speed and ping.", Category.HUD);
		hideFromList();
		enableByDefault();
		HudManager.get().register(new InfoHud(this));
	}

	@Override
	public void onTick() {
		if (mc.player == null) {
			lastPosition = null;
			blocksPerSecond = 0;
			return;
		}
		Vec3 now = mc.player.position();
		if (lastPosition != null) {
			double dx = now.x - lastPosition.x;
			double dz = now.z - lastPosition.z;
			double instant = Math.sqrt(dx * dx + dz * dz) * 20.0;
			// Light smoothing so the number is readable while moving.
			blocksPerSecond += (instant - blocksPerSecond) * 0.35;
		}
		lastPosition = now;
	}

	public double blocksPerSecond() {
		return blocksPerSecond;
	}
}
