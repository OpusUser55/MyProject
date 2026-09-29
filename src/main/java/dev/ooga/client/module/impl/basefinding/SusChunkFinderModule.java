package dev.ooga.client.module.impl.basefinding;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.render.WorldOverlay;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.notify.Notification;
import dev.ooga.client.ui.notify.NotificationManager;
import dev.ooga.client.util.ChatUtil;
import dev.ooga.client.util.ColorUtil;
import dev.ooga.client.world.ChunkScanner;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Predicate;

/**
 * Flags chunks that show signs of players, by scoring two kinds of evidence:
 *
 * <ul>
 *   <li><b>Placed blocks</b> that world generation never (or almost never) produces:
 *   hoppers, observers, pistons, ender chests, shulkers, beacons and so on.</li>
 *   <li><b>Growth</b>: generated kelp stops short of its maximum age and only grows while a
 *   chunk is loaded, so lots of fully grown kelp means the chunk has been kept loaded for a
 *   long time — typically by someone living nearby.</li>
 * </ul>
 *
 * A chunk whose score reaches the threshold is highlighted and announced once.
 */
public class SusChunkFinderModule extends Module implements ChunkScanner.Listener {
	/** Weight per block of each player-made kind, and a readable name for alerts. */
	private record Evidence(double weight, String label) {
	}

	private static final Map<Block, Evidence> PLACED = new LinkedHashMap<>();

	static {
		PLACED.put(Blocks.BEACON, new Evidence(6, "beacon"));
		PLACED.put(Blocks.ENDER_CHEST, new Evidence(4, "ender chest"));
		PLACED.put(Blocks.RESPAWN_ANCHOR, new Evidence(4, "respawn anchor"));
		PLACED.put(Blocks.ENCHANTING_TABLE, new Evidence(4, "enchanting table"));
		PLACED.put(Blocks.CRAFTER, new Evidence(3, "crafter"));
		PLACED.put(Blocks.OBSERVER, new Evidence(3, "observer"));
		PLACED.put(Blocks.HOPPER, new Evidence(2.5, "hopper"));
		PLACED.put(Blocks.PISTON, new Evidence(2, "piston"));
		PLACED.put(Blocks.STICKY_PISTON, new Evidence(2, "sticky piston"));
		PLACED.put(Blocks.COMPARATOR, new Evidence(1.5, "comparator"));
		PLACED.put(Blocks.REPEATER, new Evidence(1, "repeater"));
		PLACED.put(Blocks.DROPPER, new Evidence(1.5, "dropper"));
		PLACED.put(Blocks.CRAFTING_TABLE, new Evidence(1, "crafting table"));
		PLACED.put(Blocks.FURNACE, new Evidence(0.75, "furnace"));
		PLACED.put(Blocks.BLAST_FURNACE, new Evidence(0.5, "blast furnace"));
		PLACED.put(Blocks.SMOKER, new Evidence(0.5, "smoker"));
	}

	private static final double SHULKER = 4;
	private static final double ANVIL = 2;
	private static final double GROWN_KELP = 0.3;
	/** Generated kelp tops out at age 23; 24 and 25 can only come from growth. */
	private static final int KELP_GROWN_AGE = 24;

	public final NumberSetting threshold = add(new NumberSetting("Threshold", "Score a chunk needs to be flagged.", 5, 1, 30, 0.5));
	public final BooleanSetting placedBlocks = add(new BooleanSetting("Placed Blocks", "Count blocks players place (hoppers, pistons, shulkers…).", true));
	public final BooleanSetting kelp = add(new BooleanSetting("Grown Kelp", "Count fully grown kelp, a sign the chunk stayed loaded.", true));
	public final ModeSetting height = add(new ModeSetting("Height", "Draw the marker at a fixed height or at your feet.", "Player", "Player", "Fixed"));
	public final NumberSetting fixedY = add(new NumberSetting("Marker Y", "Height of the marker in Fixed mode.", 63, -64, 320, 1)
			.visibleWhen(() -> height.is("Fixed")));
	public final BooleanSetting chat = add(new BooleanSetting("Chat Alerts", "Post each flagged chunk in chat with its evidence.", true));
	public final BooleanSetting toast = add(new BooleanSetting("Notifications", "Show a notification for each flagged chunk.", true));

	private final Map<Long, String> flagged = new HashMap<>();
	private final Map<Long, Boolean> announced = new HashMap<>();
	private final Map<String, Integer> counts = new LinkedHashMap<>();
	private double score;

	public SusChunkFinderModule() {
		super("Sus Chunk Finder", "Flags chunks with player-placed blocks or long-loaded growth.", Category.BASEFINDING);
		ChunkScanner.register(this);
		WorldOverlay.register(this::draw);
	}

	@Override
	protected void onEnable() {
		flagged.clear();
		announced.clear();
		ChunkScanner.rescanLoaded();
	}

	@Override
	public boolean active() {
		return isEnabled();
	}

	@Override
	public Predicate<BlockState> filter() {
		boolean placed = placedBlocks.get();
		boolean grown = kelp.get();
		return state -> {
			Block block = state.getBlock();
			if (placed && (PLACED.containsKey(block) || block instanceof ShulkerBoxBlock || state.is(BlockTags.ANVIL))) return true;
			return grown && block == Blocks.KELP;
		};
	}

	@Override
	public void beginChunk(ChunkPos pos) {
		counts.clear();
		score = 0;
	}

	@Override
	public void block(int x, int y, int z, BlockState state) {
		Block block = state.getBlock();
		if (block == Blocks.KELP) {
			if (state.getValue(GrowingPlantHeadBlock.AGE) >= KELP_GROWN_AGE) add("grown kelp", GROWN_KELP);
			return;
		}
		if (block instanceof ShulkerBoxBlock) {
			add("shulker box", SHULKER);
			return;
		}
		if (state.is(BlockTags.ANVIL)) {
			add("anvil", ANVIL);
			return;
		}
		Evidence evidence = PLACED.get(block);
		if (evidence != null) add(evidence.label(), evidence.weight());
	}

	private void add(String label, double weight) {
		counts.merge(label, 1, Integer::sum);
		score += weight;
	}

	@Override
	public void endChunk(ChunkPos pos) {
		long key = pos.toLong();
		if (score < threshold.get()) {
			flagged.remove(key);
			return;
		}
		StringJoiner summary = new StringJoiner(", ");
		counts.forEach((label, n) -> summary.add(n + " " + label + (n == 1 || label.endsWith("kelp") ? "" : "s")));
		flagged.put(key, summary.toString());
		if (announced.putIfAbsent(key, true) == null) announce(pos, summary.toString());
	}

	private void announce(ChunkPos pos, String evidence) {
		int x = pos.getMiddleBlockX();
		int z = pos.getMiddleBlockZ();
		if (chat.get()) ChatUtil.info("Sus chunk at " + x + ", " + z + ": " + evidence);
		if (toast.get()) NotificationManager.get().push("Sus chunk", x + ", " + z, Notification.Kind.INFO);
	}

	@Override
	public void forgetChunk(ChunkPos pos) {
		flagged.remove(pos.toLong());
	}

	@Override
	public void clear() {
		flagged.clear();
		announced.clear();
	}

	private void draw(WorldOverlay.Drawer drawer, float partialTick) {
		if (!isEnabled() || mc.player == null || flagged.isEmpty()) return;
		double y = height.is("Fixed") ? fixedY.get() : Math.floor(mc.player.getPosition(partialTick).y);
		int fill = OogaTheme.accent(0x30);
		int line = OogaTheme.accent(0xD0);
		for (long key : flagged.keySet()) {
			ChunkPos pos = new ChunkPos(key);
			// A flat slab over the whole chunk, easy to spot from a distance.
			AABB slab = new AABB(pos.getMinBlockX(), y, pos.getMinBlockZ(), pos.getMaxBlockX() + 1, y + 0.05, pos.getMaxBlockZ() + 1);
			drawer.box(slab, fill, line);
		}
	}

	@Override
	public String getSuffix() {
		return Integer.toString(flagged.size());
	}
}
