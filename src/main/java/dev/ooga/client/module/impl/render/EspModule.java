package dev.ooga.client.module.impl.render;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.ModeSetting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Outlines entities through walls using Minecraft's own glowing-outline pass, recoloured to
 * Ooga gold. Reusing the vanilla pass keeps it cheap and compatible with shaders.
 */
public class EspModule extends Module {
	private static final int GOLD = 0xF2C14E;

	public final BooleanSetting players = add(new BooleanSetting("Players", "Outline other players.", true));
	public final BooleanSetting hostiles = add(new BooleanSetting("Hostiles", "Outline hostile mobs.", false));
	public final BooleanSetting items = add(new BooleanSetting("Items", "Outline dropped items.", false));
	public final ModeSetting color = add(new ModeSetting("Color", "Gold, or each entity's team colour.", "Gold", "Gold", "Team"));

	public EspModule() {
		super("ESP", "See players and mobs through walls.", Category.RENDER);
	}

	public boolean shouldOutline(Entity entity) {
		if (!isEnabled() || entity == mc.player) return false;
		if (entity instanceof Player) return players.get();
		if (entity instanceof Enemy) return hostiles.get();
		if (entity instanceof ItemEntity) return items.get();
		return false;
	}

	/** @return an RGB colour override, or -1 to keep vanilla's. */
	public int outlineColor(Entity entity) {
		if (!shouldOutline(entity) || color.is("Team")) return -1;
		return GOLD;
	}
}
