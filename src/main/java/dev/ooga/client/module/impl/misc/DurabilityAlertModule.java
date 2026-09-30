package dev.ooga.client.module.impl.misc;

import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.notify.Notification;
import dev.ooga.client.ui.notify.NotificationManager;
import dev.ooga.client.util.ChatUtil;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/** Warns once when worn armor or the held tool drops below a durability threshold. */
public class DurabilityAlertModule extends Module {
	private static final EquipmentSlot[] SLOTS = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};

	public final NumberSetting threshold = add(new NumberSetting("Threshold", "Warn below this much durability left.", 15, 1, 50, 1, "%"));
	public final BooleanSetting chat = add(new BooleanSetting("Chat", "Also warn in chat.", false));
	public final BooleanSetting sound = add(new BooleanSetting("Sound", "Play a warning sound.", true));

	/** The item we last warned about per slot, so each item warns once until repaired or swapped. */
	private final Map<EquipmentSlot, ItemStack> warned = new EnumMap<>(EquipmentSlot.class);

	public DurabilityAlertModule() {
		super("Durability Alert", "Warns before your armor or tool breaks.", Category.MISC);
		enableByDefault();
	}

	@Override
	public void onTick() {
		if (mc.player == null) return;
		for (EquipmentSlot slot : SLOTS) {
			ItemStack stack = mc.player.getItemBySlot(slot);
			if (stack.isEmpty() || !stack.isDamageableItem()) {
				warned.remove(slot);
				continue;
			}
			int left = stack.getMaxDamage() - stack.getDamageValue();
			int percent = Math.round(100f * left / stack.getMaxDamage());
			if (percent >= threshold.getInt()) {
				warned.remove(slot);
				continue;
			}
			ItemStack last = warned.get(slot);
			if (last != null && ItemStack.isSameItem(last, stack)) continue;
			warned.put(slot, stack.copy());

			String message = stack.getHoverName().getString() + " at " + percent + "% (" + left + " uses left)";
			NotificationManager.get().push("Low durability", message, Notification.Kind.DISABLED);
			if (chat.get()) ChatUtil.info(message);
			if (sound.get()) mc.player.playSound(SoundEvents.ANVIL_LAND, 0.4f, 1.8f);
		}
	}
}
