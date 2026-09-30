package dev.ooga.client.module.impl.misc;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.NumberSetting;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adds a grouped summary of a shulker box's (or any container item's) contents to its tooltip:
 * every item totalled across stacks, biggest first. Hold Shift to see the whole list.
 */
public class ShulkerPeekModule extends Module {
	public final NumberSetting lines = add(new NumberSetting("Lines", "Items listed before \"hold Shift for more\".", 5, 1, 27, 1));

	public ShulkerPeekModule() {
		super("Shulker Peek", "Totals of what's inside shulker boxes, in the tooltip.", Category.MISC);
		enableByDefault();
		hideFromList();
		ItemTooltipCallback.EVENT.register((stack, context, flag, tooltip) -> {
			if (isEnabled()) addSummary(stack, tooltip);
		});
	}

	private void addSummary(ItemStack stack, List<Component> tooltip) {
		ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
		if (contents == null) return;
		NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
		contents.copyInto(items);

		Map<String, Integer> totals = new LinkedHashMap<>();
		int stacks = 0;
		for (ItemStack item : items) {
			if (item.isEmpty()) continue;
			stacks++;
			totals.merge(item.getHoverName().getString(), item.getCount(), Integer::sum);
		}
		if (totals.isEmpty()) return;

		List<Map.Entry<String, Integer>> sorted = new ArrayList<>(totals.entrySet());
		sorted.sort((a, b) -> b.getValue() - a.getValue());
		boolean all = mc.getWindow() != null && (InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
				|| InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT));
		int shown = all ? sorted.size() : Math.min(sorted.size(), lines.getInt());

		tooltip.add(Component.literal(stacks + "/27 slots used").withStyle(s -> s.withColor(0x7A7D86)));
		for (int i = 0; i < shown; i++) {
			Map.Entry<String, Integer> e = sorted.get(i);
			tooltip.add(Component.literal(String.format(" %,d ", e.getValue())).withStyle(s -> s.withColor(0xF2C14E))
					.append(Component.literal(e.getKey()).withStyle(s -> s.withColor(0xD8D9DE))));
		}
		if (shown < sorted.size()) {
			tooltip.add(Component.literal(" …" + (sorted.size() - shown) + " more (hold Shift)").withStyle(s -> s.withColor(0x7A7D86)));
		}
	}
}
