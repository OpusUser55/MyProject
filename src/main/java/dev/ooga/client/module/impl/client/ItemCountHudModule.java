package dev.ooga.client.module.impl.client;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.hud.HudManager;
import dev.ooga.client.ui.hud.ItemCountHud;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ItemCountHudModule extends Module {
	public final BooleanSetting totems = add(new BooleanSetting("Totems", "Totems of undying.", true));
	public final BooleanSetting crystals = add(new BooleanSetting("Crystals", "End crystals.", true));
	public final BooleanSetting obsidian = add(new BooleanSetting("Obsidian", "Obsidian blocks.", false));
	public final BooleanSetting xp = add(new BooleanSetting("XP Bottles", "Bottles o' enchanting.", true));
	public final BooleanSetting gapples = add(new BooleanSetting("Golden Apples", "Golden and enchanted golden apples.", true));
	public final BooleanSetting pearls = add(new BooleanSetting("Ender Pearls", "Ender pearls.", true));
	public final BooleanSetting food = add(new BooleanSetting("Food", "Everything edible, as one number.", false));
	public final BooleanSetting hideEmpty = add(new BooleanSetting("Hide Empty", "Skip items you have none of.", true));
	public final BooleanSetting vertical = add(new BooleanSetting("Vertical", "Stack top to bottom instead of left to right.", false));
	public final NumberSetting scale = add(new NumberSetting("Scale", "Size.", 1.0, 0.5, 2.0, 0.05, "x"));

	public ItemCountHudModule() {
		super("Item Count HUD", "How many totems, crystals, pearls and more you're carrying.", Category.HUD);
		hideFromList();
		HudManager.get().register(new ItemCountHud(this));
	}

	/** The items to count, in display order. Food is handled separately by the HUD. */
	public List<Item> tracked() {
		List<Item> items = new ArrayList<>();
		if (totems.get()) items.add(Items.TOTEM_OF_UNDYING);
		if (crystals.get()) items.add(Items.END_CRYSTAL);
		if (obsidian.get()) items.add(Items.OBSIDIAN);
		if (xp.get()) items.add(Items.EXPERIENCE_BOTTLE);
		if (gapples.get()) {
			items.add(Items.GOLDEN_APPLE);
			items.add(Items.ENCHANTED_GOLDEN_APPLE);
		}
		if (pearls.get()) items.add(Items.ENDER_PEARL);
		return items;
	}
}
