package dev.ooga.client.module.impl.render;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;

/** Turns off distracting effects: fire on screen, totem pop animation, rain, pumpkin blur, hurt shake. */
public class NoRenderModule extends Module {
	private static NoRenderModule instance;

	public final BooleanSetting fire = add(new BooleanSetting("Fire Overlay", "Hide the flames on screen while burning.", true));
	public final BooleanSetting totem = add(new BooleanSetting("Totem Animation", "Skip the big totem pop animation.", true));
	public final BooleanSetting weather = add(new BooleanSetting("Weather", "Hide rain and snow.", false));
	public final BooleanSetting pumpkin = add(new BooleanSetting("Pumpkin Overlay", "See clearly while wearing a pumpkin.", true));
	public final BooleanSetting hurtCam = add(new BooleanSetting("Hurt Camera", "No screen shake when you take damage.", true));

	public NoRenderModule() {
		super("No Render", "Hides fire, totem animation, weather, pumpkin blur and hurt shake.", Category.RENDER);
		instance = this;
	}

	private static boolean on(BooleanSetting setting) {
		return instance != null && instance.isEnabled() && setting.get();
	}

	public static boolean fire() {
		return instance != null && on(instance.fire);
	}

	public static boolean totem() {
		return instance != null && on(instance.totem);
	}

	public static boolean weather() {
		return instance != null && on(instance.weather);
	}

	public static boolean pumpkin() {
		return instance != null && on(instance.pumpkin);
	}

	public static boolean hurtCam() {
		return instance != null && on(instance.hurtCam);
	}
}
