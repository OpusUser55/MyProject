package dev.ooga.client.module.impl.render;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ooga.client.camera.CameraController;
import dev.ooga.client.mixin.CameraAccessor;
import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import net.minecraft.client.CameraType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Look around without turning. The mouse orbits the camera around you while your body keeps
 * facing (and walking) the way it was, so you can check behind you mid-sprint.
 *
 * <p>Unlike Freecam this isn't a {@link dev.ooga.client.camera.CameraMode}: the player keeps
 * full control, so only the view rotation is taken over.
 */
public class FreelookModule extends Module {
	public final ModeSetting mode = add(new ModeSetting("Mode", "Hold the key to look around, or press to toggle.", "Hold", "Hold", "Toggle"));
	public final ModeSetting perspective = add(new ModeSetting("Perspective", "Orbit behind you in third person, or turn your head in first person.",
			"Third Person", "Third Person", "First Person"));
	public final NumberSetting distance = add(new NumberSetting("Distance", "How far the camera sits from you in third person.", 4.0, 1.0, 12.0, 0.5, "m")
			.visibleWhen(() -> perspective.is("Third Person")));
	public final BooleanSetting clip = add(new BooleanSetting("Stop At Blocks", "Pull the camera in instead of looking through walls.", true)
			.visibleWhen(() -> perspective.is("Third Person")));
	public final NumberSetting sensitivity = add(new NumberSetting("Sensitivity", "Mouse sensitivity while looking around.", 1.0, 0.1, 3.0, 0.05, "x"));
	public final BooleanSetting invertPitch = add(new BooleanSetting("Invert Pitch", "Flip up and down while looking around.", false));

	private float yaw;
	private float pitch;
	private CameraType savedPerspective;

	public FreelookModule() {
		super("Freelook", "Look around without turning your body.", Category.RENDER);
		setDefaultKey(GLFW.GLFW_KEY_LEFT_ALT);
	}

	@Override
	protected boolean canEnable() {
		// Freecam already owns the camera; the two would fight over it.
		return inWorld() && CameraController.get().activeMode() == null;
	}

	@Override
	public boolean persistsEnabledState() {
		return false;
	}

	@Override
	public void onKeybind() {
		if (mode.is("Hold")) setEnabled(true, false);
		else toggle();
	}

	@Override
	protected void onEnable() {
		// Start looking exactly where the player looks, so there's no jump.
		yaw = mc.player.getYRot();
		pitch = mc.player.getXRot();
		savedPerspective = mc.options.getCameraType();
		applyPerspective();
	}

	@Override
	protected void onDisable() {
		if (savedPerspective != null) mc.options.setCameraType(savedPerspective);
		savedPerspective = null;
	}

	@Override
	public void onTick() {
		if (!inWorld() || CameraController.get().activeMode() != null) {
			setEnabled(false, false);
			return;
		}
		if (mode.is("Hold") && (mc.screen != null || getKey() == -1
				|| !InputConstants.isKeyDown(mc.getWindow(), getKey()))) {
			setEnabled(false, false);
			return;
		}
		// F5 cycles perspective; keep the one Freelook needs until it ends.
		applyPerspective();
	}

	private void applyPerspective() {
		CameraType wanted = perspective.is("First Person") ? CameraType.FIRST_PERSON : CameraType.THIRD_PERSON_BACK;
		if (mc.options.getCameraType() != wanted) mc.options.setCameraType(wanted);
	}

	/** Mouse look. Returns true if it turned the free camera instead of the player. */
	public boolean onTurn(double deltaYaw, double deltaPitch) {
		if (!isEnabled()) return false;
		double scale = 0.15 * sensitivity.get();
		yaw += (float) (deltaYaw * scale);
		pitch = Mth.clamp(pitch + (float) (deltaPitch * scale * (invertPitch.get() ? -1 : 1)), -90f, 90f);
		return true;
	}

	/** Called at the end of {@code Camera.setup}, after vanilla has placed the camera. */
	public void apply(CameraAccessor camera, Entity entity, float partialTick) {
		if (!isEnabled() || entity == null) return;
		camera.ooga$setRotation(yaw, pitch);
		if (!perspective.is("Third Person")) return;

		Vec3 eye = entity.getEyePosition(partialTick);
		Vec3 back = Vec3.directionFromRotation(pitch, yaw).scale(-distance.get());
		camera.ooga$setPosition(eye.add(back.scale(clip.get() ? freeFraction(entity, eye, back) : 1.0)));
	}

	/**
	 * How far along {@code back} the camera can go before hitting a block, 0..1. Like vanilla,
	 * casts from a few points around the eye so the near plane doesn't clip into walls.
	 */
	private double freeFraction(Entity entity, Vec3 eye, Vec3 back) {
		double length = back.length();
		double fraction = 1.0;
		for (int i = 0; i < 8; i++) {
			Vec3 offset = new Vec3(((i & 1) * 2 - 1) * 0.1, ((i >> 1 & 1) * 2 - 1) * 0.1, ((i >> 2 & 1) * 2 - 1) * 0.1);
			Vec3 from = eye.add(offset);
			HitResult hit = mc.level.clip(new ClipContext(from, from.add(back), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity));
			if (hit.getType() == HitResult.Type.MISS) continue;
			double reached = hit.getLocation().distanceTo(from) / length;
			fraction = Math.min(fraction, reached);
		}
		return Math.max(0.0, fraction);
	}

	@Override
	public String getSuffix() {
		return mode.get();
	}
}
