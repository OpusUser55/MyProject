package dev.ooga.client.module.impl.render;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Slow-motion hand swing. Blocks still break at exactly the normal speed; only the arm
 * animation (first and third person) is stretched out, so mining looks slow and smooth.
 *
 * <p>Purely visual and client-side: the server still sees one swing packet per swing, the
 * same as without the module.
 */
public class SlowMineModule extends Module {
	public final NumberSetting speed = add(new NumberSetting("Speed", "Swing speed. Lower is slower: 25% takes four times as long.", 0.35, 0.05, 1.0, 0.05, "x"));
	public final ModeSetting when = add(new ModeSetting("When", "Slow the swing only while mining, or for every swing.", "Mining", "Mining", "Always"));
	public final BooleanSetting fullSwing = add(new BooleanSetting("Full Swing", "Let each swing play all the way through while mining, instead of restarting halfway.", true));

	public SlowMineModule() {
		super("Slow Mine", "Mine at normal speed while your hand swings in slow motion.", Category.RENDER);
	}

	/** Whether the local player's swing should be slowed right now. */
	private boolean applies() {
		if (!isEnabled() || mc.player == null) return false;
		return when.is("Always") || mining();
	}

	private boolean mining() {
		if (mc.gameMode != null && mc.gameMode.isDestroying()) return true;
		// Between blocks (the break just finished, the next hasn't started) the swing should
		// keep its pace rather than snapping back to full speed for a tick.
		HitResult hit = mc.hitResult;
		return mc.options.keyAttack.isDown() && hit != null && hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult;
	}

	/** Stretches vanilla's swing duration (in ticks) for the local player. */
	public int swingDuration(int vanilla) {
		if (!applies()) return vanilla;
		return Math.max(vanilla, Math.round(vanilla / speed.getFloat()));
	}

	/**
	 * Vanilla restarts a swing once it's halfway done if the attack key is still held, which is
	 * what makes mining look like a rapid half-swing. Returning true keeps the current swing
	 * going until it finishes.
	 */
	public boolean holdSwing(int swingTime, boolean swinging, int duration) {
		if (!applies() || !fullSwing.get() || !swinging) return false;
		return swingTime >= 0 && swingTime < duration;
	}

	@Override
	public String getSuffix() {
		return Math.round(speed.get() * 100) + "%";
	}
}
