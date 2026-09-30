package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.util.MoneyFmt;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;

import java.util.Locale;

/**
 * Catches your own {@code /pay <player> <amount>} before it's sent and prints the usual
 * confirmation locally instead. Nothing reaches the server; it only changes what you see.
 */
public class FakePayModule extends Module {
	private static final int WHITE = 0xFFFFFF;
	private static final int GREEN = 0x00FC00;

	public final BooleanSetting sound = add(new BooleanSetting("Sound", "Play the hit marker ding.", true));
	public final BooleanSetting actionBar = add(new BooleanSetting("Action Bar", "Also show it above the hotbar.", true));
	public final BooleanSetting removeFromScoreboard = add(new BooleanSetting("Remove From Scoreboard",
			"Take the amount off Fake Scoreboard's money line.", false).onChange(this::refreshBoard));

	private double spent;

	public FakePayModule() {
		super("Fake Pay", "Fakes /pay locally instead of sending it.", Category.MISC);
	}

	@Override
	protected void onDisable() {
		resetSpent();
	}

	/** Called on joining a world: a fresh session starts from the full balance again. */
	public void resetSpent() {
		spent = 0.0;
		refreshBoard();
	}

	/** How much to knock off the fake balance, 0 when that option is off. */
	public double spentOffset() {
		return isEnabled() && removeFromScoreboard.get() ? spent : 0.0;
	}

	/**
	 * @param command the command without its leading slash, as typed
	 * @return true if it was a /pay we faked, so it must not be sent
	 */
	public boolean handleCommand(String command) {
		if (!isEnabled() || command == null || mc.player == null) return false;

		String[] parts = command.trim().split("\\s+");
		if (parts.length < 3 || !"pay".equals(parts[0].toLowerCase(Locale.ROOT))) return false;

		String target = parts[1];
		Double amount = MoneyFmt.parse(parts[2]);
		if (amount == null || amount <= 0) return false;

		MutableComponent message = Component.literal("You paid " + target + " ").withStyle(s -> s.withColor(WHITE))
				.append(Component.literal("$").withStyle(s -> s.withColor(GREEN)))
				.append(Component.literal(MoneyFmt.format(amount)).withStyle(s -> s.withColor(GREEN)));

		mc.player.displayClientMessage(message, false);
		if (actionBar.get()) mc.player.displayClientMessage(message, true);
		if (sound.get()) mc.player.playSound(SoundEvents.ARROW_HIT_PLAYER, 1.0f, 1.0f);

		if (removeFromScoreboard.get()) {
			spent += amount;
			refreshBoard();
		}
		return true;
	}

	private void refreshBoard() {
		ModuleManager.get().get(FakeScoreboardModule.class).refresh();
	}
}
