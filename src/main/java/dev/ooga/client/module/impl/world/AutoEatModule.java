package dev.ooga.client.module.impl.world;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.util.Keys;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Eats from your hotbar when you get hungry, then puts your previous item back in hand. Picks
 * the most filling food, and leaves golden apples and bad food alone unless you allow them.
 */
public class AutoEatModule extends Module {
	public final NumberSetting hunger = add(new NumberSetting("Hunger", "Eat when your food bar drops to this (20 = full).", 14, 1, 19, 1));
	public final BooleanSetting gapples = add(new BooleanSetting("Golden Apples", "Allow eating golden apples.", false));
	public final BooleanSetting badFood = add(new BooleanSetting("Bad Food", "Allow rotten flesh, spider eyes, raw chicken and pufferfish.", false));
	public final BooleanSetting pauseMining = add(new BooleanSetting("Not While Mining", "Wait until you stop holding attack.", true));

	private int previousSlot = -1;
	private int eatTicks;

	public AutoEatModule() {
		super("Auto Eat", "Eats automatically when you get hungry.", Category.WORLD);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.screen != null) {
			stop();
			return;
		}
		Inventory inventory = mc.player.getInventory();

		if (previousSlot != -1) {
			// Eating: keep holding use until the bar rises or the food runs out, with a timeout.
			ItemStack held = inventory.getItem(inventory.getSelectedSlot());
			if (eatTicks++ > 60 || !held.has(DataComponents.FOOD) || mc.player.getFoodData().getFoodLevel() > hunger.getInt()) {
				stop();
			} else {
				Keys.hold(mc.options.keyUse);
			}
			return;
		}

		if (mc.player.getAbilities().instabuild || mc.player.getFoodData().getFoodLevel() > hunger.getInt()) return;
		if (pauseMining.get() && mc.options.keyAttack.isDown()) return;

		int best = -1;
		int bestNutrition = 0;
		for (int i = 0; i < 9; i++) {
			ItemStack stack = inventory.getItem(i);
			FoodProperties food = stack.get(DataComponents.FOOD);
			if (food == null || !allowed(stack)) continue;
			if (food.nutrition() > bestNutrition) {
				bestNutrition = food.nutrition();
				best = i;
			}
		}
		if (best == -1) return;
		previousSlot = inventory.getSelectedSlot();
		inventory.setSelectedSlot(best);
		eatTicks = 0;
		Keys.click(mc.options.keyUse);
		Keys.hold(mc.options.keyUse);
	}

	private boolean allowed(ItemStack stack) {
		if (!gapples.get() && (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE))) return false;
		if (!badFood.get() && (stack.is(Items.ROTTEN_FLESH) || stack.is(Items.SPIDER_EYE) || stack.is(Items.CHICKEN)
				|| stack.is(Items.PUFFERFISH) || stack.is(Items.POISONOUS_POTATO) || stack.is(Items.SUSPICIOUS_STEW))) return false;
		// Chorus fruit teleports you; never eat it by surprise.
		return !stack.is(Items.CHORUS_FRUIT);
	}

	private void stop() {
		if (previousSlot == -1) return;
		Keys.release(mc.options.keyUse);
		if (mc.player != null) mc.player.getInventory().setSelectedSlot(previousSlot);
		previousSlot = -1;
	}

	@Override
	protected void onDisable() {
		stop();
	}
}
