package dev.ooga.client.module.impl.render;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.util.Anim;
import org.lwjgl.glfw.GLFW;

public class ZoomModule extends Module {
	public final ModeSetting mode = add(new ModeSetting("Mode", "Hold the key to zoom, or press to toggle.", "Hold", "Hold", "Toggle"));
	public final NumberSetting factor = add(new NumberSetting("Factor", "How far to zoom in.", 4.0, 1.5, 20.0, 0.5, "x"));
	public final BooleanSetting scroll = add(new BooleanSetting("Scroll Adjust", "Mouse wheel changes zoom while zoomed.", true));
	public final BooleanSetting smooth = add(new BooleanSetting("Smooth", "Ease in and out of zoom.", true));
	public final BooleanSetting sensitivity = add(new BooleanSetting("Scale Sensitivity", "Slow mouse look down while zoomed.", true));
	public final BooleanSetting cinematic = add(new BooleanSetting("Cinematic Camera", "Smooth, weighted mouse look while zoomed.", false));
	public final BooleanSetting hideHand = add(new BooleanSetting("Hide Hand", "Hide your hand and item while zoomed.", true));

	private boolean savedSmoothCamera;

	private final Anim progress = new Anim(1f, 12f);
	private double scrollFactor = 1.0;

	public ZoomModule() {
		super("Zoom", "Magnify your view like a spyglass.", Category.RENDER);
		setDefaultKey(GLFW.GLFW_KEY_Z);
	}

	@Override
	public void onKeybind() {
		if (mode.is("Hold")) {
			// Hold-to-zoom is momentary; toasts on every press would be noise.
			setEnabled(true, false);
		} else {
			toggle();
		}
	}

	@Override
	public void onTick() {
		if (mode.is("Hold") && (mc.screen != null || getKey() == -1
				|| !InputConstants.isKeyDown(mc.getWindow(), getKey()))) {
			setEnabled(false, false);
		}
	}

	@Override
	protected void onEnable() {
		scrollFactor = 1.0;
		savedSmoothCamera = mc.options.smoothCamera;
		if (cinematic.get()) mc.options.smoothCamera = true;
	}

	@Override
	protected void onDisable() {
		if (cinematic.get()) mc.options.smoothCamera = savedSmoothCamera;
	}

	public boolean hidesHand() {
		return isEnabled() && hideHand.get();
	}

	@Override
	public boolean persistsEnabledState() {
		return false;
	}

	/** Current magnification including easing, 1 when fully zoomed out. */
	public float currentZoom() {
		float target = isEnabled() ? (float) (factor.get() * scrollFactor) : 1f;
		if (!smooth.get()) {
			progress.snap(target);
			return target;
		}
		return progress.update(target);
	}

	public double sensitivityMultiplier() {
		if (!sensitivity.get()) return 1.0;
		return 1.0 / Math.max(1f, progress.get());
	}

	public boolean onScroll(double amount) {
		if (!isEnabled() || !scroll.get() || amount == 0 || mc.screen != null) return false;
		scrollFactor = Math.max(0.25, Math.min(4.0, scrollFactor * (amount > 0 ? 1.2 : 1 / 1.2)));
		return true;
	}

	@Override
	public String getSuffix() {
		return factor.format();
	}
}
