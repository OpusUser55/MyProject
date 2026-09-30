package dev.ooga.client.module.impl.client;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.impl.misc.AdminDetectorModule;
import dev.ooga.client.module.setting.BooleanSetting;

/**
 * Anticheat safety for every module at once.
 *
 * <ul>
 *   <li><b>Safe Mode</b> keeps risky modules inside limits that look like a skilled player
 *   rather than a program: capped click speeds, a minimum random delay, partial (not zero)
 *   knockback reduction, small hitbox growth, and so on.</li>
 *   <li><b>Pause On Staff</b> suspends every blatant module while Admin Detector sees staff
 *   online, and resumes them when staff leave.</li>
 * </ul>
 *
 * Nothing can promise to be undetectable on every server; these just make detection a lot
 * less likely.
 */
public class SafetyModule extends Module {
	private static SafetyModule instance;

	public final BooleanSetting safeMode = add(new BooleanSetting("Safe Mode", "Keep risky modules within human-looking limits.", true));
	public final BooleanSetting pauseOnStaff = add(new BooleanSetting("Pause On Staff", "Suspend blatant modules while staff are online.", true));

	public SafetyModule() {
		super("Anticheat", "Safe limits and pausing blatant modules around staff.", Category.CLIENT);
		settingsOnly();
		instance = this;
	}

	public static boolean safe() {
		return instance == null || instance.safeMode.get();
	}

	/** True while blatant modules should sit still. */
	public static boolean paused() {
		if (instance == null || !instance.pauseOnStaff.get()) return false;
		AdminDetectorModule detector = ModuleManager.get().get(AdminDetectorModule.class);
		return detector.isEnabled() && !detector.online().isEmpty();
	}

	/** {@code value} capped at {@code safeMax} in Safe Mode. */
	public static double atMost(double value, double safeMax) {
		return safe() ? Math.min(value, safeMax) : value;
	}

	/** {@code value} raised to at least {@code safeMin} in Safe Mode. */
	public static double atLeast(double value, double safeMin) {
		return safe() ? Math.max(value, safeMin) : value;
	}
}
