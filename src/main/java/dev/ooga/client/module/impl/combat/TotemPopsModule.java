package dev.ooga.client.module.impl.combat;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.ui.notify.Notification;
import dev.ooga.client.ui.notify.NotificationManager;
import dev.ooga.client.util.ChatUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/** Counts how many totems each player has popped, and tells you when they die after popping. */
public class TotemPopsModule extends Module {
	private static TotemPopsModule instance;

	public final BooleanSetting chat = add(new BooleanSetting("Chat", "Post pops in chat.", true));
	public final BooleanSetting self = add(new BooleanSetting("Include Me", "Count your own pops too.", false));

	private final Map<String, Integer> pops = new HashMap<>();

	public TotemPopsModule() {
		super("Totem Pops", "Counts other players' totem pops.", Category.COMBAT);
		instance = this;
	}

	/** Pops so far for a player name (0 if none), for other modules to display. */
	public static int popsOf(String name) {
		return instance == null || !instance.isEnabled() ? 0 : instance.pops.getOrDefault(name, 0);
	}

	/** Called when the server plays a totem animation on an entity. */
	public static void onPop(Entity entity) {
		TotemPopsModule m = instance;
		if (m == null || !m.isEnabled() || !(entity instanceof Player player)) return;
		if (player == mc.player && !m.self.get()) return;
		String name = player.getGameProfile().name();
		int count = m.pops.merge(name, 1, Integer::sum);
		String text = name + " popped " + count + (count == 1 ? " totem" : " totems");
		if (m.chat.get()) ChatUtil.info(text);
		else NotificationManager.get().push("Totem pop", text, Notification.Kind.INFO);
	}

	@Override
	public void onTick() {
		if (mc.level == null) {
			pops.clear();
			return;
		}
		// Report deaths of players who had popped, then forget them.
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!(entity instanceof Player player) || !player.isDeadOrDying()) continue;
			String name = player.getGameProfile().name();
			Integer count = pops.remove(name);
			if (count != null && chat.get()) ChatUtil.info(name + " died after popping " + count + (count == 1 ? " totem" : " totems"));
		}
	}

	@Override
	protected void onDisable() {
		pops.clear();
	}
}
