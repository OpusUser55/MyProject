package dev.ooga.client.module.impl.combat;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.module.impl.client.FriendsModule;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/** Makes other entities a little easier to hit by growing the area your crosshair picks them in. */
public class HitboxModule extends Module {
	private static HitboxModule instance;

	public final NumberSetting expand = add(new NumberSetting("Expand", "Extra size added on every side.", 0.15, 0.05, 1.0, 0.05, "m"));
	public final BooleanSetting players = add(new BooleanSetting("Players", "Expand players.", true));
	public final BooleanSetting hostiles = add(new BooleanSetting("Hostiles", "Expand hostile mobs.", false));

	public HitboxModule() {
		super("Hitbox", "Bigger hitboxes for easier hits.", Category.COMBAT);
		instance = this;
	}

	/** Extra pick radius for {@code entity}; 0 when the module doesn't apply. */
	public static float extra(Entity entity) {
		HitboxModule self = instance;
		if (self == null || !self.isEnabled() || entity == mc.player || FriendsModule.protects(entity)) return 0f;
		boolean wanted = entity instanceof Player ? self.players.get() : entity instanceof Enemy && self.hostiles.get();
		return wanted ? self.expand.getFloat() : 0f;
	}

	@Override
	public String getSuffix() {
		return expand.format();
	}
}
