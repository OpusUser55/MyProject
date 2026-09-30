package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Eats the best food in your hotbar when hunger drops, then goes back to your slot. */
public class AutoEatModule extends Module {
	public final NumberSetting hunger = add(new NumberSetting("Hunger", "Eat at or below this many hunger points (20 is full).", 14, 1, 19, 1));
	public final BooleanSetting avoidBad = add(new BooleanSetting("Avoid Bad Food", "Skip rotten flesh, spider eyes, raw chicken, pufferfish and poisonous potatoes.", true));
	public final BooleanSetting pauseInScreens = add(new BooleanSetting("Pause In Menus", "Don't eat while a screen is open.", true));

	private int previousSlot = -1;
	private boolean eating;

	public AutoEatModule() {
		super("Auto Eat", "Eats automatically when you get hungry.", Category.MISC);
	}

	@Override
	protected void onDisable() {
		stop();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.gameMode == null) return;
		if (pauseInScreens.get() && mc.screen != null) {
			stop();
			return;
		}
		boolean hungry = mc.player.getFoodData().getFoodLevel() <= hunger.getInt() && !mc.player.getAbilities().instabuild;
		if (eating) {
			// Keep holding use until the food is eaten or we're no longer hungry.
			if (!hungry || !isFood(mc.player.getMainHandItem())) stop();
			else mc.options.keyUse.setDown(true);
			return;
		}
		if (!hungry || mc.player.isUsingItem()) return;
		int slot = bestFoodSlot();
		if (slot < 0) return;
		Inventory inventory = mc.player.getInventory();
		previousSlot = inventory.getSelectedSlot();
		inventory.setSelectedSlot(slot);
		mc.options.keyUse.setDown(true);
		eating = true;
	}

	private void stop() {
		if (!eating) return;
		eating = false;
		mc.options.keyUse.setDown(false);
		if (mc.player != null && previousSlot >= 0) mc.player.getInventory().setSelectedSlot(previousSlot);
		previousSlot = -1;
	}

	private int bestFoodSlot() {
		Inventory inventory = mc.player.getInventory();
		int best = -1;
		int bestNutrition = 0;
		for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!isFood(stack)) continue;
			int nutrition = stack.get(DataComponents.FOOD).nutrition();
			if (nutrition > bestNutrition) {
				bestNutrition = nutrition;
				best = slot;
			}
		}
		return best;
	}

	private boolean isFood(ItemStack stack) {
		FoodProperties food = stack.get(DataComponents.FOOD);
		if (food == null) return false;
		if (!avoidBad.get()) return true;
		return !stack.is(Items.ROTTEN_FLESH) && !stack.is(Items.SPIDER_EYE) && !stack.is(Items.CHICKEN)
				&& !stack.is(Items.PUFFERFISH) && !stack.is(Items.POISONOUS_POTATO) && !stack.is(Items.SUSPICIOUS_STEW);
	}
}
