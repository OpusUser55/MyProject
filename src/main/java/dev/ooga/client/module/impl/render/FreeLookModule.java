package dev.ooga.client.module.impl.render;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ooga.client.camera.CameraController;
import dev.ooga.client.camera.OrbitCamera;
import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;

public class FreeLookModule extends Module {
	public final NumberSetting distance = add(new NumberSetting("Distance", "How far the camera sits from you.", 4.0, 1.0, 12.0, 0.5, "m"));
	public final NumberSetting sensitivity = add(new NumberSetting("Sensitivity", "Mouse sensitivity while looking around.", 1.0, 0.1, 3.0, 0.05, "x"));
	public final BooleanSetting hold = add(new BooleanSetting("Hold Key", "Only look around while the keybind is held.", true));

	private final OrbitCamera camera = new OrbitCamera(this);
	/** Set when the keybind (not the menu) turned us on; only then does releasing it turn us off. */
	private boolean fromKey;

	public FreeLookModule() {
		super("Free Look", "Orbit the camera around yourself while you keep moving straight.", Category.RENDER);
	}

	@Override
	protected boolean canEnable() {
		return inWorld();
	}

	@Override
	public boolean persistsEnabledState() {
		return false;
	}

	@Override
	public void onKeybind() {
		fromKey = !isEnabled();
		toggle();
	}

	@Override
	protected void onEnable() {
		CameraController.get().enter(camera);
	}

	@Override
	protected void onDisable() {
		fromKey = false;
		if (CameraController.get().isActive(camera)) CameraController.get().exit();
	}

	@Override
	public void onTick() {
		if (!CameraController.get().isActive(camera)) {
			setEnabled(false, false);
			return;
		}
		// Hold mode: let go of the key and the view snaps back.
		if (hold.get() && fromKey && mc.screen == null && !InputConstants.isKeyDown(mc.getWindow(), getKey())) {
			setEnabled(false, false);
		}
	}
}
