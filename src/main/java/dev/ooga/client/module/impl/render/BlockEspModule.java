package dev.ooga.client.module.impl.render;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.module.setting.StringSetting;
import dev.ooga.client.render.TracerOrigin;
import dev.ooga.client.render.WorldOverlay;
import dev.ooga.client.util.ColorUtil;
import dev.ooga.client.world.BlockUpdates;
import dev.ooga.client.world.ChunkScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Highlights any blocks you list by ID ("diamond_ore, minecraft:beacon, spawner"). Finds them
 * in loaded chunk data and keeps up with live block changes, like the Debris Finder.
 */
public class BlockEspModule extends Module implements ChunkScanner.Listener, BlockUpdates.Listener {
	private static final Map<String, Integer> COLORS = Map.of(
			"Gold", 0xF2C14E, "Cyan", 0x5CC8FF, "Red", 0xE8594A, "Green", 0x6BE3A4, "Purple", 0xC58CFF, "White", 0xFFFFFF);

	public final StringSetting blocks = add(new StringSetting("Blocks", "Block IDs to highlight, comma separated. \"minecraft:\" is optional.",
			"diamond_ore, deepslate_diamond_ore").onChange(this::reload));
	public final ModeSetting color = add(new ModeSetting("Color", "Highlight colour.", "Cyan", "Gold", "Cyan", "Red", "Green", "Purple", "White"));
	public final BooleanSetting tracers = add(new BooleanSetting("Tracers", "Lines to each block.", false));
	public final NumberSetting range = add(new NumberSetting("Range", "Maximum highlight distance.", 96, 16, 512, 8, "m"));
	public final NumberSetting limit = add(new NumberSetting("Max Shown", "Draw at most this many (nearest first).", 300, 10, 2000, 10));

	private Set<Block> targets = Set.of();
	private final Map<Long, Set<BlockPos>> found = new HashMap<>();
	private Set<BlockPos> building;
	private int total;

	public BlockEspModule() {
		super("Block ESP", "Highlights the blocks you choose through walls.", Category.RENDER);
		ChunkScanner.register(this);
		BlockUpdates.register(this);
		WorldOverlay.register(this::draw);
		parseTargets();
	}

	private void parseTargets() {
		Set<Block> parsed = new HashSet<>();
		for (String part : blocks.get().split(",")) {
			String id = part.trim().toLowerCase(Locale.ROOT);
			if (id.isEmpty()) continue;
			if (!id.contains(":")) id = "minecraft:" + id;
			try {
				BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id)).ifPresent(parsed::add);
			} catch (RuntimeException ignored) {
				// Not a valid ID; skip it.
			}
		}
		targets = parsed;
	}

	private void reload() {
		parseTargets();
		found.clear();
		total = 0;
		if (isEnabled()) ChunkScanner.rescanLoaded();
	}

	@Override
	protected void onEnable() {
		reload();
	}

	@Override
	public boolean active() {
		return isEnabled() && !targets.isEmpty();
	}

	@Override
	public Predicate<BlockState> filter() {
		Set<Block> t = targets;
		return state -> t.contains(state.getBlock());
	}

	@Override
	public void beginChunk(ChunkPos pos) {
		building = new HashSet<>();
	}

	@Override
	public void block(int x, int y, int z, BlockState state) {
		building.add(new BlockPos(x, y, z));
	}

	@Override
	public void endChunk(ChunkPos pos) {
		if (building.isEmpty()) found.remove(pos.toLong());
		else found.put(pos.toLong(), building);
		building = null;
		recount();
	}

	@Override
	public void forgetChunk(ChunkPos pos) {
		if (found.remove(pos.toLong()) != null) recount();
	}

	@Override
	public void clear() {
		found.clear();
		total = 0;
	}

	@Override
	public void blockChanged(BlockPos pos, BlockState state) {
		if (!isEnabled()) return;
		long chunk = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
		if (targets.contains(state.getBlock())) found.computeIfAbsent(chunk, k -> new HashSet<>()).add(pos);
		else {
			Set<BlockPos> set = found.get(chunk);
			if (set == null || !set.remove(pos)) return;
		}
		recount();
	}

	private void recount() {
		int n = 0;
		for (Set<BlockPos> set : found.values()) n += set.size();
		total = n;
	}

	private void draw(WorldOverlay.Drawer drawer, float partialTick) {
		if (!isEnabled() || mc.player == null || found.isEmpty()) return;
		int rgb = COLORS.getOrDefault(color.get(), 0x5CC8FF);
		double maxSq = range.get() * range.get();
		Vec3 eye = mc.player.getEyePosition(partialTick);
		Vec3 origin = TracerOrigin.get(drawer, partialTick);
		int left = limit.getInt();
		for (Set<BlockPos> set : found.values()) {
			for (BlockPos pos : set) {
				if (pos.distToCenterSqr(eye) > maxSq) continue;
				AABB box = new AABB(pos);
				drawer.box(box, ColorUtil.withAlpha(rgb, 45), ColorUtil.withAlpha(rgb, 220));
				if (tracers.get()) drawer.line(origin, box.getCenter(), ColorUtil.withAlpha(rgb, 140));
				if (--left <= 0) return;
			}
		}
	}

	@Override
	public String getSuffix() {
		return Integer.toString(total);
	}
}
