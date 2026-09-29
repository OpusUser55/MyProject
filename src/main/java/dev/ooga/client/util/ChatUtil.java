package dev.ooga.client.util;

import dev.ooga.client.ui.OogaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Client-side chat lines, prefixed so they're recognisably from Ooga. Never sent to the server. */
public final class ChatUtil {
	private ChatUtil() {
	}

	public static void info(String message) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		MutableComponent prefix = Component.literal("Ooga ").withStyle(style -> style.withColor(OogaTheme.GOLD & 0xFFFFFF).withBold(true));
		MutableComponent body = Component.literal("» " + message).withStyle(style -> style.withColor(0xD8D9DE));
		mc.player.displayClientMessage(prefix.append(body), false);
	}
}
