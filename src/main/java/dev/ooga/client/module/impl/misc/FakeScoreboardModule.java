package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.StringSetting;
import dev.ooga.client.util.MoneyFmt;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.ArrayList;
import java.util.List;

/**
 * Swaps the sidebar for one with your own numbers. Everything lives in the client's copy of
 * the scoreboard, so only you see it; the server's board comes back when this is turned off.
 *
 * <p>Each line is a team prefix on an invisible score holder ("§0", "§1", ...), which is how
 * servers draw sidebars without the red score numbers or name clashes.
 */
public class FakeScoreboardModule extends Module {
	private static final String OBJECTIVE = "ooga_fakesb";
	private static final String TEAM_PREFIX = "ooga_sb_";

	public final StringSetting title = add(new StringSetting("Title", "Text at the top of the board.", "Ooga"));
	public final BooleanSetting showMoney = add(new BooleanSetting("Money", "Show the money line.", true));
	public final StringSetting money = add(new StringSetting("Money Amount", "Numbers like 1.5M are formatted; anything else shows as typed.", "1.5M")
			.visibleWhen(showMoney::get));
	public final BooleanSetting showShards = add(new BooleanSetting("Shards", "Show the shards line.", true));
	public final StringSetting shards = add(new StringSetting("Shards Amount", "What the shards line says.", "250")
			.visibleWhen(showShards::get));
	public final BooleanSetting showKills = add(new BooleanSetting("Kills", "Show the kills line.", true));
	public final StringSetting kills = add(new StringSetting("Kills Amount", "What the kills line says.", "67")
			.visibleWhen(showKills::get));
	public final BooleanSetting showDeaths = add(new BooleanSetting("Deaths", "Show the deaths line.", true));
	public final StringSetting deaths = add(new StringSetting("Deaths Amount", "What the deaths line says.", "3")
			.visibleWhen(showDeaths::get));
	public final BooleanSetting showPlaytime = add(new BooleanSetting("Playtime", "Show the playtime line.", true));
	public final StringSetting playtime = add(new StringSetting("Playtime Amount", "What the playtime line says.", "4d 2h")
			.visibleWhen(showPlaytime::get));
	public final StringSetting footer = add(new StringSetting("Footer", "Grey line at the bottom, e.g. a server address. Leave empty to hide.", ""));

	private Objective ours;
	/** The server's sidebar we replaced, restored on disable if it still exists. */
	private Objective serverBoard;
	private final List<String> teams = new ArrayList<>();
	/** What's currently on the board, so we only rebuild when something changed. */
	private String shownKey;

	public FakeScoreboardModule() {
		super("Fake Scoreboard", "Replaces the sidebar with your own numbers.", Category.MISC);
	}

	@Override
	protected void onEnable() {
		shownKey = null;
	}

	@Override
	protected void onDisable() {
		teardown();
	}

	/** Called on joining a world: the old scoreboard (and our objects on it) is gone. */
	public void onWorldJoin() {
		ours = null;
		serverBoard = null;
		teams.clear();
		shownKey = null;
	}

	/** Forces a rebuild on the next tick, e.g. after Fake Pay changes the balance. */
	public void refresh() {
		shownKey = null;
	}

	@Override
	public void onTick() {
		if (!inWorld()) return;
		Scoreboard board = mc.level.getScoreboard();
		List<MutableComponent> lines = buildLines();

		StringBuilder key = new StringBuilder(title.get());
		for (MutableComponent line : lines) key.append('\n').append(line.getString());
		// Rebuild when the content changed, or when the server pushed its own sidebar over ours.
		boolean displayed = ours != null && board.getDisplayObjective(DisplaySlot.SIDEBAR) == ours;
		if (displayed && key.toString().equals(shownKey)) return;

		rebuild(board, lines);
		shownKey = key.toString();
	}

	private void rebuild(Scoreboard board, List<MutableComponent> lines) {
		Objective current = board.getDisplayObjective(DisplaySlot.SIDEBAR);
		if (current != null && !OBJECTIVE.equals(current.getName())) serverBoard = current;

		dropTeams(board);
		Objective old = board.getObjective(OBJECTIVE);
		if (old != null) board.removeObjective(old);

		ours = board.addObjective(OBJECTIVE, ObjectiveCriteria.DUMMY, white(title.get()),
				ObjectiveCriteria.RenderType.INTEGER, false, BlankFormat.INSTANCE);
		board.setDisplayObjective(DisplaySlot.SIDEBAR, ours);

		for (int i = 0; i < lines.size(); i++) {
			String name = TEAM_PREFIX + i;
			PlayerTeam existing = board.getPlayerTeam(name);
			if (existing != null) board.removePlayerTeam(existing);
			PlayerTeam team = board.addPlayerTeam(name);
			team.setPlayerPrefix(lines.get(i));
			teams.add(name);

			// A colour code alone renders as nothing, so only the team prefix shows.
			String holder = "§" + Integer.toHexString(i);
			board.getOrCreatePlayerScore(ScoreHolder.forNameOnly(holder), ours).set(lines.size() - i);
			board.addPlayerToTeam(holder, team);
		}
	}

	private List<MutableComponent> buildLines() {
		List<MutableComponent> lines = new ArrayList<>();
		if (showMoney.get()) lines.add(colored("$ ", 0x00FF00).append(white(displayedMoney())));
		if (showShards.get()) lines.add(colored("★ ", 0xA503FC).append(white(shards.get())));
		if (showKills.get()) lines.add(colored("🗡 ", 0xFF0000).append(white(kills.get())));
		if (showDeaths.get()) lines.add(colored("☠ ", 0xFC7703).append(white(deaths.get())));
		if (showPlaytime.get()) lines.add(colored("⌚ ", 0xFFE600).append(white(playtime.get())));
		if (!footer.get().isBlank()) {
			// A blank spacer above the footer, like most server boards.
			lines.add(Component.literal(" "));
			lines.add(colored(footer.get(), 0xA0A0A0));
		}
		// The sidebar hides an objective with no scores, which would leave the server's gone and nothing shown.
		if (lines.isEmpty()) lines.add(Component.literal(" "));
		return lines;
	}

	private String displayedMoney() {
		Double base = MoneyFmt.parse(money.get());
		if (base == null) return money.get();
		double spent = ModuleManager.get().get(FakePayModule.class).spentOffset();
		return MoneyFmt.format(Math.max(0.0, base - spent));
	}

	private void teardown() {
		shownKey = null;
		if (mc.level == null) {
			ours = null;
			serverBoard = null;
			teams.clear();
			return;
		}

		Scoreboard board = mc.level.getScoreboard();
		dropTeams(board);
		Objective old = board.getObjective(OBJECTIVE);
		if (old != null) board.removeObjective(old);
		ours = null;

		// Only put the server's board back if the server hasn't removed it in the meantime.
		Objective restore = serverBoard != null && board.getObjective(serverBoard.getName()) == serverBoard ? serverBoard : null;
		if (board.getDisplayObjective(DisplaySlot.SIDEBAR) == null) board.setDisplayObjective(DisplaySlot.SIDEBAR, restore);
		serverBoard = null;
	}

	private void dropTeams(Scoreboard board) {
		for (String name : teams) {
			PlayerTeam team = board.getPlayerTeam(name);
			if (team != null) board.removePlayerTeam(team);
		}
		teams.clear();
	}

	private static MutableComponent colored(String text, int rgb) {
		return Component.literal(text).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
	}

	private static MutableComponent white(String text) {
		return colored(text, 0xFFFFFF);
	}
}
