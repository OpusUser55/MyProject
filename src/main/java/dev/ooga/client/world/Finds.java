package dev.ooga.client.world;

import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.impl.basefinding.FinderAlertsModule;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Everything the base finders turn up, in one place: a short in-memory history for the Finds
 * HUD, an optional alert sound, and an optional append-only log at
 * {@code config/ooga/finds.log} so nothing is lost when you log off.
 */
public final class Finds {
	public record Find(String type, String detail, BlockPos pos, String dimension, long time) {
	}

	private static final int HISTORY = 32;
	/** Finds reported since the game started; lets RTP Base Finder notice new ones. */
	private static int total;
	private static final Deque<Find> RECENT = new ArrayDeque<>();
	/** type + position, so the same thing is never reported twice in a session. */
	private static final Set<String> SEEN = new HashSet<>();
	private static final Path LOG = FabricLoader.getInstance().getConfigDir().resolve("ooga").resolve("finds.log");
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "Ooga finds log");
		thread.setDaemon(true);
		return thread;
	});

	private Finds() {
	}

	/**
	 * Records a find. Returns false (and does nothing) if this exact find was already reported
	 * this session, so callers can use it to gate their own chat or toast alerts.
	 */
	public static boolean report(String type, String detail, BlockPos pos) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return false;
		String dimension = mc.level.dimension().identifier().toString();
		if (!SEEN.add(type + '|' + dimension + '|' + pos.asLong())) return false;

		Find find = new Find(type, detail, pos.immutable(), dimension, System.currentTimeMillis());
		RECENT.addFirst(find);
		total++;
		while (RECENT.size() > HISTORY) RECENT.removeLast();

		FinderAlertsModule alerts = FinderAlertsModule.instance();
		if (alerts != null && alerts.sound.get()) {
			mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), alerts.pitch.getFloat(), alerts.volume.getFloat()));
		}
		if (alerts == null || alerts.logToFile.get()) write(find);
		return true;
	}

	public static int total() {
		return total;
	}

	public static List<Find> recent() {
		return new ArrayList<>(RECENT);
	}

	/** Forget everything from this session (on disconnect); the log file is kept. */
	public static void clear() {
		RECENT.clear();
		SEEN.clear();
	}

	private static void write(Find find) {
		Minecraft mc = Minecraft.getInstance();
		ServerData server = mc.getCurrentServer();
		String where = server != null ? server.ip : "singleplayer";
		String line = String.format("%s  %s  %s  %-16s %d %d %d  %s%n",
				LocalDateTime.now().format(TIME), where, find.dimension(), find.type(),
				find.pos().getX(), find.pos().getY(), find.pos().getZ(), find.detail() == null ? "" : find.detail());
		WRITER.execute(() -> {
			try {
				Files.createDirectories(LOG.getParent());
				Files.writeString(LOG, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			} catch (IOException e) {
				ModuleManager.LOGGER.warn("Couldn't write to {}", LOG, e);
			}
		});
	}
}
