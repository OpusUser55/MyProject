package dev.ooga.client.module.impl.combat;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.impl.client.SafetyModule;
import dev.ooga.client.module.setting.NumberSetting;
import net.minecraft.world.phys.Vec3;

/** Takes less knockback from hits and explosions. 0% means none at all. */
public class VelocityModule extends Module {
	private static VelocityModule instance;

	public final NumberSetting horizontal = add(new NumberSetting("Horizontal", "Share of sideways knockback you still take. Safe Mode keeps at least 65%.", 0.7, 0.0, 1.0, 0.05));
	public final NumberSetting vertical = add(new NumberSetting("Vertical", "Share of upward knockback you still take.", 1.0, 0.0, 1.0, 0.05));
	public final NumberSetting chance = add(new NumberSetting("Chance", "Percent of hits to reduce. Below 100% looks more natural.", 80, 0, 100, 5, "%"));

	public VelocityModule() {
		super("Velocity", "Reduces the knockback you take.", Category.COMBAT);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled() && !SafetyModule.paused();
	}

	public static Vec3 scale(Vec3 motion) {
		VelocityModule self = instance;
		if (!dev.ooga.client.util.Delay.chance(self.chance.get())) return motion;
		double h = SafetyModule.atLeast(self.horizontal.get(), 0.65);
		double v = SafetyModule.atLeast(self.vertical.get(), 1.0);
		return new Vec3(motion.x * h, motion.y * v, motion.z * h);
	}

	@Override
	public String getSuffix() {
		return Math.round(horizontal.get() * 100) + "% " + Math.round(vertical.get() * 100) + "%";
	}

	@Override
	public boolean isBlatant() {
		return true;
	}
}
