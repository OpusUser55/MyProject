package dev.ooga.client.module.impl.basefinding;

import dev.ooga.client.OogaClient;
import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.render.Projector;
import dev.ooga.client.render.WorldOverlay;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.render.GlowRenderer;
import dev.ooga.client.ui.render.OogaFonts;
import dev.ooga.client.ui.render.OogaFonts.Weight;
import dev.ooga.client.ui.render.Render2D;
import dev.ooga.client.util.ColorUtil;
import dev.ooga.client.world.BlockEntityTracker;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Groups storage and spawners that sit close together and draws one big box around each group
 * with a count. A lone chest is scenery; twenty containers and six spawners in one room is a base.
 */
public class ClusterEspModule extends Module {
	private static final int COLOR = 0xB57EDC;

	public final NumberSetting minSize = add(new NumberSetting("Min Size", "Blocks a group needs to be shown.", 6, 2, 50, 1));
	public final NumberSetting linkDistance = add(new NumberSetting("Link Distance", "Blocks closer than this join the same group.", 6, 2, 16, 1, "m"));
	public final BooleanSetting spawners = add(new BooleanSetting("Spawners", "Count spawners as part of groups.", true));
	public final BooleanSetting labels = add(new BooleanSetting("Labels", "Show each group's size on screen.", true));

	private record Cluster(AABB box, int containers, int spawners) {
	}

	private final List<Cluster> clusters = new ArrayList<>();

	public ClusterEspModule() {
		super("Cluster ESP", "Boxes around groups of containers and spawners: bases and stashes.", Category.BASEFINDING);
		WorldOverlay.register(this::draw);
		HudElementRegistry.addFirst(Identifier.fromNamespaceAndPath(OogaClient.MOD_ID, "cluster_labels"), this::drawLabels);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.player.tickCount % 20 != 0) return;
		List<BlockPos> points = new ArrayList<>();
		List<Boolean> isSpawner = new ArrayList<>();
		for (BlockEntity be : BlockEntityTracker.all()) {
			if (be.isRemoved()) continue;
			boolean spawner = be instanceof SpawnerBlockEntity;
			boolean storage = be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity || be instanceof ShulkerBoxBlockEntity || be instanceof HopperBlockEntity;
			if (!storage && !(spawner && spawners.get())) continue;
			points.add(be.getBlockPos());
			isSpawner.add(spawner);
		}
		clusters.clear();
		double link = linkDistance.get() * linkDistance.get();
		boolean[] used = new boolean[points.size()];
		for (int i = 0; i < points.size(); i++) {
			if (used[i]) continue;
			// Flood fill over nearby points.
			Deque<Integer> queue = new ArrayDeque<>();
			queue.add(i);
			used[i] = true;
			int containers = 0, spawnerCount = 0;
			AABB box = null;
			while (!queue.isEmpty()) {
				int k = queue.poll();
				BlockPos p = points.get(k);
				box = box == null ? new AABB(p) : box.minmax(new AABB(p));
				if (isSpawner.get(k)) spawnerCount++;
				else containers++;
				for (int j = 0; j < points.size(); j++) {
					if (!used[j] && points.get(j).distSqr(p) <= link) {
						used[j] = true;
						queue.add(j);
					}
				}
			}
			if (containers + spawnerCount >= minSize.getInt()) clusters.add(new Cluster(box.inflate(0.5), containers, spawnerCount));
		}
	}

	@Override
	protected void onDisable() {
		clusters.clear();
	}

	private void draw(WorldOverlay.Drawer drawer, float partialTick) {
		if (!isEnabled()) return;
		for (Cluster c : clusters) drawer.box(c.box(), ColorUtil.withAlpha(COLOR, 22), ColorUtil.withAlpha(COLOR, 200));
	}

	private void drawLabels(GuiGraphics g, DeltaTracker tracker) {
		if (!isEnabled() || !labels.get() || mc.player == null || mc.options.hideGui) return;
		for (Cluster c : clusters) {
			Vec3 top = new Vec3(c.box().getCenter().x, c.box().maxY + 0.5, c.box().getCenter().z);
			float[] p = Projector.toScreen(top.x, top.y, top.z);
			if (p == null) continue;
			String text = c.containers() + " storage" + (c.spawners() > 0 ? " · " + c.spawners() + " spawners" : "");
			float w = OogaFonts.width(text, Weight.SEMIBOLD, 0.85f) + 10f, h = 11f;
			float x = p[0] - w / 2f, y = p[1] - h;
			GlowRenderer.glow(g, x, y, w, h, 3f, 0xFF000000 | COLOR, 0.55f, 4f);
			Render2D.roundRect(g, x, y, w, h, 3f, 0xD80E0F12);
			OogaFonts.draw(g, text, x + 5f, y + 2.2f, OogaTheme.TEXT, Weight.SEMIBOLD, 0.85f);
		}
	}

	@Override
	public String getSuffix() {
		return Integer.toString(clusters.size());
	}
}
