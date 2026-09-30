package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;

import java.util.concurrent.ThreadLocalRandom;

/** Small, random actions every so often so the server doesn't kick you for being idle. */
public class AntiAfkModule extends Module {
	public final NumberSetting interval = add(new NumberSetting("Interval", "Seconds between actions (randomised a bit).", 30, 5, 300, 5, "s"));
	public final BooleanSetting jump = add(new BooleanSetting("Jump", "Jump now and then.", true));
	public final BooleanSetting look = add(new BooleanSetting("Look Around", "Turn your head a little.", true));
	public final BooleanSetting swing = add(new BooleanSetting("Swing", "Swing your arm.", true));

	private long next;

	public AntiAfkModule() {
		super("Anti AFK", "Keeps you from being kicked for idling.", Category.MISC);
	}

	@Override
	public void onTick() {
		if (mc.player == null) return;
		long now = System.currentTimeMillis();
		if (now < next) return;
		ThreadLocalRandom random = ThreadLocalRandom.current();
		next = now + (long) (interval.get() * 1000 * (0.75 + random.nextDouble() * 0.5));
		if (jump.get() && mc.player.onGround()) mc.player.jumpFromGround();
		if (look.get()) mc.player.setYRot(mc.player.getYRot() + (float) (random.nextDouble() * 40 - 20));
		if (swing.get()) mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
	}
}
