package dev.ooga.client.camera;

import dev.ooga.client.mixin.CameraAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Owns which camera is in charge: the normal player camera (no active mode) or a detached
 * {@link CameraMode} such as Freecam.
 *
 * <p>This is the only class that touches Minecraft's camera and the related global state.
 * Everything it changes on {@link #enter} — perspective, chunk occlusion culling and player
 * input — is recorded and restored on {@link #exit}, so leaving a mode always returns the game
 * to exactly the state it was in, whichever way the mode ended.
 */
public final class CameraController {
	private static final CameraController INSTANCE = new CameraController();

	private final PlayerControlLock controlLock = new PlayerControlLock();
	private CameraMode mode;
	private CameraType savedPerspective;
	private boolean savedSmartCull;
	private Vec3 lastAppliedPosition;

	private CameraController() {
	}

	public static CameraController get() {
		return INSTANCE;
	}

	public CameraMode activeMode() {
		return mode;
	}

	public boolean isActive(CameraMode candidate) {
		return mode != null && mode == candidate;
	}

	/** @return false if no world is loaded, in which case nothing changes. */
	public boolean enter(CameraMode newMode) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return false;
		if (mode != null) exit();

		// Seed from what the user currently sees, so enabling never causes a jump.
		Camera camera = mc.gameRenderer.getMainCamera();
		Vec3 start = camera.isInitialized() ? camera.getPosition() : mc.player.getEyePosition();
		float yaw = camera.isInitialized() ? camera.getYRot() : mc.player.getYRot();
		float pitch = camera.isInitialized() ? camera.getXRot() : mc.player.getXRot();

		savedPerspective = mc.options.getCameraType();
		savedSmartCull = mc.smartCull;
		// First person keeps vanilla submitting the hand and the crosshair.
		mc.options.setCameraType(CameraType.FIRST_PERSON);
		// Occlusion culling assumes the eye is in open air; a free camera may be inside blocks.
		mc.smartCull = false;
		controlLock.lock();

		mode = newMode;
		newMode.begin(start, yaw, pitch);
		return true;
	}

	public void exit() {
		if (mode == null) return;
		mode = null;
		lastAppliedPosition = null;

		Minecraft mc = Minecraft.getInstance();
		controlLock.unlock();
		if (savedPerspective != null) mc.options.setCameraType(savedPerspective);
		mc.smartCull = savedSmartCull;
		savedPerspective = null;
	}

	/** Start of every client tick. */
	public void tick() {
		if (mode == null) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			exit();
			return;
		}
		controlLock.tick();
		if (!mc.options.getCameraType().isFirstPerson()) mc.options.setCameraType(CameraType.FIRST_PERSON);
		mode.tick();
	}

	/** Mouse look. Returns true if the active mode consumed it (the player won't turn). */
	public boolean onTurn(double deltaYaw, double deltaPitch) {
		if (mode == null) return false;
		mode.turn(deltaYaw, deltaPitch);
		return true;
	}

	/** Called at the end of {@code Camera.setup}, after vanilla has positioned the camera. */
	public void apply(CameraAccessor camera, float partialTick) {
		if (mode == null) return;
		Vec3 position = mode.position(partialTick);
		camera.ooga$setRotation(mode.yaw(partialTick), mode.pitch(partialTick));
		camera.ooga$setPosition(position);
		// A "detached" camera makes vanilla draw the local player's body in the world.
		camera.ooga$setDetached(mode.renderPlayerBody());
		lastAppliedPosition = position;
	}

	public Vec3 lastAppliedPosition() {
		return lastAppliedPosition;
	}

	public boolean blocksInteraction() {
		return mode != null && !mode.allowInteraction();
	}
}
