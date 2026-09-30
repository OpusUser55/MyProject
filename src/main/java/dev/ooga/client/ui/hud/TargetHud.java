package dev.ooga.client.ui.hud;

import dev.ooga.client.module.impl.client.TargetHudModule;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.render.OogaFonts;
import dev.ooga.client.ui.render.OogaFonts.Weight;
import dev.ooga.client.ui.render.Render2D;
import dev.ooga.client.util.Anim;
import dev.ooga.client.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** The current target: face, name, a health bar that drains smoothly, distance and gear. */
public class TargetHud extends HudElement {
	private static final float W = 128f;
	private static final float H = 38f;
	private static final EquipmentSlot[] GEAR = {EquipmentSlot.MAINHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND};

	private final TargetHudModule module;
	private final Anim health = new Anim(1f, 10f);
	private LivingEntity last;

	public TargetHud(TargetHudModule module) {
		super("target", "Target HUD", Anchor.CENTER, 0.5f, Anchor.START, 0.62f);
		this.module = module;
	}

	@Override
	public boolean isVisible() {
		return module.isEnabled() && module.target() != null;
	}

	@Override
	public NumberSetting scaleSetting() {
		return module.scale;
	}

	@Override
	protected void render(GuiGraphics g, float x, float y, float delta) {
		Minecraft mc = Minecraft.getInstance();
		LivingEntity target = module.target();
		float scale = module.scale.getFloat();
		width = W * scale;
		height = H * scale;
		if (target == null || mc.player == null) return;

		float ratio = Math.max(0f, Math.min(1f, target.getHealth() / Math.max(1f, target.getMaxHealth())));
		if (target != last) {
			health.snap(ratio);
			last = target;
		}
		float shown = health.update(ratio);

		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		Render2D.roundRect(g, 0, 0, W, H, OogaTheme.RADIUS_CARD, 0xE00E0F12);
		Render2D.outline(g, 0, 0, W, H, OogaTheme.RADIUS_CARD, OogaTheme.BORDER);

		float face = 24f;
		if (target instanceof Player player && mc.getConnection() != null) {
			PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
			if (info != null) PlayerFaceRenderer.draw(g, info.getSkin(), 5, 5, (int) face);
			else Render2D.roundRect(g, 5, 5, face, face, 3f, OogaTheme.SURFACE_CONTROL);
		} else {
			Render2D.roundRect(g, 5, 5, face, face, 3f, OogaTheme.SURFACE_CONTROL);
			OogaFonts.drawCentered(g, target.getName().getString().substring(0, 1), 5 + face / 2f, 13f, OogaTheme.TEXT_SECONDARY, Weight.SEMIBOLD, 1f);
		}

		float tx = 5 + face + 5;
		String name = OogaFonts.trim(target.getName().getString(), Weight.SEMIBOLD, 1f, W - tx - 30);
		OogaFonts.draw(g, name, tx, 5, OogaTheme.TEXT, Weight.SEMIBOLD);
		String dist = String.format("%.1fm", target.distanceTo(mc.player));
		OogaFonts.draw(g, dist, W - 5 - OogaFonts.width(dist, Weight.REGULAR, 0.75f), 6, OogaTheme.TEXT_SECONDARY, Weight.REGULAR, 0.75f);

		// Gear row.
		float gx = tx;
		g.pose().pushMatrix();
		g.pose().translate(gx, 15f);
		g.pose().scale(0.6f, 0.6f);
		int i = 0;
		for (EquipmentSlot slot : GEAR) {
			ItemStack stack = target.getItemBySlot(slot);
			if (stack.isEmpty()) continue;
			g.renderItem(stack, i * 16, 0);
			i++;
		}
		g.pose().popMatrix();

		// Health bar.
		float barX = tx, barY = H - 8f, barW = W - tx - 5, barH = 4f;
		Render2D.roundRect(g, barX, barY, barW, barH, 2f, OogaTheme.SURFACE_CONTROL);
		int color = ColorUtil.lerp(0xFFE5484D, 0xFF46C37B, ratio);
		if (shown > 0.01f) Render2D.roundRect(g, barX, barY, Math.max(barH, barW * shown), barH, 2f, color);
		float hp = target.getHealth() + target.getAbsorptionAmount();
		String hpText = hp >= 10 ? Integer.toString(Math.round(hp)) : String.format("%.1f", hp);
		OogaFonts.draw(g, hpText + " ♥", W - 5 - OogaFonts.width(hpText + " ♥", Weight.SEMIBOLD, 0.7f), barY - 7.5f, color, Weight.SEMIBOLD, 0.7f);
		g.pose().popMatrix();
	}
}
