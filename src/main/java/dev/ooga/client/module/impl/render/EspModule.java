package dev.ooga.client.module.impl.render;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.ModeSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.social.FriendStore;
import dev.ooga.client.ui.OogaTheme;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Outlines entities through walls using Minecraft's own glowing-outline pass, recoloured to
 * Ooga gold. Reusing the vanilla pass keeps it cheap and compatible with shaders.
 */
public class EspModule extends Module {
	public final BooleanSetting players = add(new BooleanSetting("Players", "Outline other players.", true));
	public final BooleanSetting hostiles = add(new BooleanSetting("Hostiles", "Outline hostile mobs.", false));
	public final BooleanSetting items = add(new BooleanSetting("Items", "Outline dropped items.", false));
	public final BooleanSetting passive = add(new BooleanSetting("Passive Mobs", "Outline animals and other passive mobs.", false));
	public final BooleanSetting invisible = add(new BooleanSetting("Invisible", "Also outline invisible entities.", true));
	public final NumberSetting range = add(new NumberSetting("Range", "Only outline entities within this distance.", 128, 8, 256, 4, "m"));
	public final ModeSetting color = add(new ModeSetting("Color", "Accent colour, each entity's team colour, or white.", "Accent", "Accent", "Team", "White"));
	public final BooleanSetting friendColor = add(new BooleanSetting("Friend Color", "Outline .friend players in blue.", true));

	public EspModule() {
		super("ESP", "See players and mobs through walls.", Category.RENDER);
	}

	public boolean shouldOutline(Entity entity) {
		if (!isEnabled() || entity == mc.player || mc.player == null) return false;
		if (!invisible.get() && entity.isInvisible()) return false;
		double r = range.get();
		if (entity.distanceToSqr(mc.player) > r * r) return false;
		if (entity instanceof Player) return players.get();
		if (entity instanceof Enemy) return hostiles.get();
		if (entity instanceof ItemEntity) return items.get();
		if (entity instanceof Mob) return passive.get();
		return false;
	}

	/** @return an RGB colour override, or -1 to keep vanilla's. */
	public int outlineColor(Entity entity) {
		if (!shouldOutline(entity)) return -1;
		if (friendColor.get() && entity instanceof Player p && FriendStore.isFriend(p.getName().getString())) return 0x5CC8FF;
		if (color.is("Team")) return -1;
		return color.is("White") ? 0xFFFFFF : OogaTheme.GOLD & 0xFFFFFF;
	}
}
