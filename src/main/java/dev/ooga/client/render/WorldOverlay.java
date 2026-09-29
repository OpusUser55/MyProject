package dev.ooga.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ooga.client.module.ModuleManager;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws world-space overlays for modules. Modules register a {@link Layer}; each frame they
 * describe boxes and lines through a {@link Drawer} in world coordinates, and this class turns
 * them into camera-relative geometry in a single see-through batch.
 *
 * <p>Lines are thin quads that always face the camera and widen with distance, so they read
 * as a constant on-screen thickness without needing a separate line pipeline.
 */
public final class WorldOverlay {
	@FunctionalInterface
	public interface Layer {
		void draw(Drawer drawer, float partialTick);
	}

	private static final List<Layer> LAYERS = new ArrayList<>();

	private WorldOverlay() {
	}

	public static void register(Layer layer) {
		LAYERS.add(layer);
	}

	public static void init() {
		WorldRenderEvents.END_MAIN.register(WorldOverlay::render);
	}

	private static void render(WorldRenderContext context) {
		if (LAYERS.isEmpty()) return;
		Vec3 camera = context.worldState().cameraRenderState.pos;
		MultiBufferSource consumers = context.consumers();
		PoseStack matrices = context.matrices();
		matrices.pushPose();
		Drawer drawer = new Drawer(consumers.getBuffer(OogaRenderTypes.OVERLAY), matrices.last().pose(), camera);
		float partialTick = net.minecraft.client.Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
		for (Layer layer : LAYERS) {
			try {
				layer.draw(drawer, partialTick);
			} catch (RuntimeException e) {
				ModuleManager.LOGGER.error("World overlay layer failed", e);
			}
		}
		matrices.popPose();
		if (consumers instanceof MultiBufferSource.BufferSource source) source.endBatch(OogaRenderTypes.OVERLAY);
	}

	public static final class Drawer {
		/** Half-width of lines per block of distance; about 1.5 px at 1080p and 70° FOV. */
		private static final float LINE_SCALE = 0.0014f;

		private final VertexConsumer buffer;
		private final Matrix4f pose;
		private final Vec3 camera;

		Drawer(VertexConsumer buffer, Matrix4f pose, Vec3 camera) {
			this.buffer = buffer;
			this.pose = pose;
			this.camera = camera;
		}

		public Vec3 camera() {
			return camera;
		}

		private void vertex(double x, double y, double z, int argb) {
			buffer.addVertex(pose, (float) (x - camera.x), (float) (y - camera.y), (float) (z - camera.z)).setColor(argb);
		}

		/** Filled box, all six faces. */
		public void fill(AABB box, int argb) {
			if ((argb >>> 24) == 0) return;
			double x0 = box.minX, y0 = box.minY, z0 = box.minZ, x1 = box.maxX, y1 = box.maxY, z1 = box.maxZ;
			quad(x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, argb);
			quad(x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, argb);
			quad(x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, argb);
			quad(x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, argb);
			quad(x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, argb);
			quad(x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, argb);
		}

		public void quad(double ax, double ay, double az, double bx, double by, double bz,
				double cx, double cy, double cz, double dx, double dy, double dz, int argb) {
			vertex(ax, ay, az, argb);
			vertex(bx, by, bz, argb);
			vertex(cx, cy, cz, argb);
			vertex(dx, dy, dz, argb);
		}

		/** The 12 edges of a box. */
		public void outline(AABB box, int argb) {
			if ((argb >>> 24) == 0) return;
			double x0 = box.minX, y0 = box.minY, z0 = box.minZ, x1 = box.maxX, y1 = box.maxY, z1 = box.maxZ;
			line(x0, y0, z0, x1, y0, z0, argb);
			line(x1, y0, z0, x1, y0, z1, argb);
			line(x1, y0, z1, x0, y0, z1, argb);
			line(x0, y0, z1, x0, y0, z0, argb);
			line(x0, y1, z0, x1, y1, z0, argb);
			line(x1, y1, z0, x1, y1, z1, argb);
			line(x1, y1, z1, x0, y1, z1, argb);
			line(x0, y1, z1, x0, y1, z0, argb);
			line(x0, y0, z0, x0, y1, z0, argb);
			line(x1, y0, z0, x1, y1, z0, argb);
			line(x1, y0, z1, x1, y1, z1, argb);
			line(x0, y0, z1, x0, y1, z1, argb);
		}

		public void box(AABB box, int fillArgb, int outlineArgb) {
			fill(box, fillArgb);
			outline(box, outlineArgb);
		}

		public void line(Vec3 a, Vec3 b, int argb) {
			line(a.x, a.y, a.z, b.x, b.y, b.z, argb);
		}

		/** A camera-facing strip from a to b. */
		public void line(double ax, double ay, double az, double bx, double by, double bz, int argb) {
			// Work relative to the camera, where the eye is the origin.
			double rax = ax - camera.x, ray = ay - camera.y, raz = az - camera.z;
			double rbx = bx - camera.x, rby = by - camera.y, rbz = bz - camera.z;
			double dx = rbx - rax, dy = rby - ray, dz = rbz - raz;
			double mx = (rax + rbx) / 2, my = (ray + rby) / 2, mz = (raz + rbz) / 2;
			// Side vector perpendicular to both the line and the view ray to its midpoint.
			double sx = dy * mz - dz * my;
			double sy = dz * mx - dx * mz;
			double sz = dx * my - dy * mx;
			double len = Math.sqrt(sx * sx + sy * sy + sz * sz);
			if (len < 1.0E-9) return;
			double wa = Math.max(0.004, Math.sqrt(rax * rax + ray * ray + raz * raz) * LINE_SCALE);
			double wb = Math.max(0.004, Math.sqrt(rbx * rbx + rby * rby + rbz * rbz) * LINE_SCALE);
			sx /= len;
			sy /= len;
			sz /= len;
			quad(ax + sx * wa, ay + sy * wa, az + sz * wa,
					bx + sx * wb, by + sy * wb, bz + sz * wb,
					bx - sx * wb, by - sy * wb, bz - sz * wb,
					ax - sx * wa, ay - sy * wa, az - sz * wa, argb);
		}
	}
}
