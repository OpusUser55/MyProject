package dev.ooga.client.module.impl.render;

import dev.ooga.client.camera.CameraController;
import dev.ooga.client.camera.FreeCamera;
import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.notify.Notification;
import dev.ooga.client.ui.notify.NotificationManager;
import net.minecraft.world.entity.player.Input;

public class FreecamModule extends Module {
	public final NumberSetting speed = add(new NumberSetting("Speed", "Flight speed.", 1.0, 0.1, 10.0, 0.1, "x"));
	public final NumberSetting verticalSpeed = add(new NumberSetting("Vertical Speed", "Up/down speed relative to horizontal.", 1.0, 0.1, 3.0, 0.05, "x"));
	public final NumberSetting sprintBoost = add(new NumberSetting("Sprint Boost", "Speed multiplier while holding sprint.", 2.5, 1.0, 5.0, 0.1, "x"));
	public final NumberSetting smoothing = add(new NumberSetting("Smoothing", "How gradually the camera speeds up and slows down.", 0.55, 0.0, 0.95, 0.05));
	public final NumberSetting horizontalSensitivity = add(new NumberSetting("Horizontal Sensitivity", "Mouse sensitivity for turning.", 1.0, 0.1, 3.0, 0.05, "x"));
	public final NumberSetting verticalSensitivity = add(new NumberSetting("Vertical Sensitivity", "Mouse sensitivity for looking up and down.", 1.0, 0.1, 3.0, 0.05, "x"));
	public final ModeSetting flightMode = add(new ModeSetting("Flight Mode", "Look: fly where you face. Level: forward stays horizontal.", "Look", "Look", "Level"));
	public final ModeSetting collision = add(new ModeSetting("Collision", "Whether the camera passes through blocks.", "None", "None", "Blocks"));
	public final BooleanSetting scrollSpeed = add(new BooleanSetting("Scroll Speed", "Mouse wheel changes speed while flying.", true));
	public final BooleanSetting showHand = add(new BooleanSetting("Show Hand", "Keep your first-person hand and item on screen.", true));
	public final BooleanSetting cameraLighting = add(new BooleanSetting("Camera Lighting", "Light the hand from where the camera is.", true)
			.visibleWhen(showHand::get));
	public final BooleanSetting showPlayer = add(new BooleanSetting("Show Player", "Draw your body where you left it.", true));
	public final BooleanSetting hideHud = add(new BooleanSetting("Hide HUD", "Hide the HUD while flying, for clean shots.", false));
	public final BooleanSetting staySneaking = add(new BooleanSetting("Stay Sneaking", "If you were sneaking when you turned Freecam on, your body keeps sneaking while the camera flies.", true));
	public final BooleanSetting steadyView = add(new BooleanSetting("Steady View", "Turn off view bobbing and FOV effects while flying, so the camera never wobbles or zooms.", true));
	public final BooleanSetting allowInteraction = add(new BooleanSetting("Allow Interaction", "Let clicks mine, attack and use items from your body.", false));

	private final FreeCamera camera = new FreeCamera(this);
	/** The body we detached from; a different one means we respawned or changed dimension. */
	private Object body;
	private boolean sneakBody;
	/** The user's own view options while Steady View overrides them; null otherwise. */
	private Boolean savedBob;
	private Double savedFovEffect;

	public FreecamModule() {
		super("Freecam", "Detach the camera and fly freely while your body stays put.", Category.RENDER);
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
	protected void onEnable() {
		// canEnable() guarantees a world, so entering can't fail here; onTick() is the safety net.
		body = mc.player;
		sneakBody = mc.player != null && mc.player.isShiftKeyDown();
		CameraController.get().enter(camera);
		if (steadyView.get()) {
			savedBob = mc.options.bobView().get();
			savedFovEffect = mc.options.fovEffectScale().get();
			mc.options.bobView().set(false);
			mc.options.fovEffectScale().set(0.0);
		}
		holdSneak();
	}

	@Override
	protected void onDisable() {
		if (CameraController.get().isActive(camera)) CameraController.get().exit();
		if (savedBob != null) mc.options.bobView().set(savedBob);
		if (savedFovEffect != null) mc.options.fovEffectScale().set(savedFovEffect);
		savedBob = null;
		savedFovEffect = null;
		body = null;
		sneakBody = false;
	}

	@Override
	public void onTick() {
		// The controller exits on its own when the world goes away; keep our state honest.
		// Respawning or changing dimension gives us a new player: land the camera then too.
		if (!CameraController.get().isActive(camera) || mc.player != body) {
			setEnabled(false, false);
			return;
		}
		holdSneak();
	}

	/** Keeps the parked body crouched: stays on edges and doesn't float up in water. */
	private void holdSneak() {
		if (!staySneaking.get() || !sneakBody || mc.player == null) return;
		mc.player.input.keyPresses = new Input(false, false, false, false, false, true, false);
	}

	/** @return true if the scroll was used to change speed. */
	public boolean onScroll(double amount) {
		if (!isEnabled() || !scrollSpeed.get() || amount == 0 || mc.screen != null) return false;
		// Proportional steps feel even across the range; the floor keeps low speeds from sticking.
		double step = Math.max(0.1, speed.get() * 0.15);
		speed.set(speed.get() + (amount > 0 ? step : -step));
		NotificationManager.get().push("Freecam", "Speed " + speed.format(), Notification.Kind.INFO);
		return true;
	}

	@Override
	public String getSuffix() {
		return speed.format();
	}
}
