package dev.ooga.client.module.impl.combat;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.NumberSetting;
import net.minecraft.world.phys.Vec3;

/** Takes less knockback from hits and explosions. 0% means none at all. */
public class VelocityModule extends Module {
	private static VelocityModule instance;

	public final NumberSetting horizontal = add(new NumberSetting("Horizontal", "Share of sideways knockback you still take.", 0.0, 0.0, 1.0, 0.05));
	public final NumberSetting vertical = add(new NumberSetting("Vertical", "Share of upward knockback you still take.", 0.0, 0.0, 1.0, 0.05));

	public VelocityModule() {
		super("Velocity", "Reduces the knockback you take.", Category.COMBAT);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	public static Vec3 scale(Vec3 motion) {
		VelocityModule self = instance;
		return new Vec3(motion.x * self.horizontal.get(), motion.y * self.vertical.get(), motion.z * self.horizontal.get());
	}

	@Override
	public String getSuffix() {
		return Math.round(horizontal.get() * 100) + "% " + Math.round(vertical.get() * 100) + "%";
	}
}
