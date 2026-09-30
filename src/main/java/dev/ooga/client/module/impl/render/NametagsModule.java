package dev.ooga.client.module.impl.render;

import dev.ooga.client.OogaClient;
import dev.ooga.client.module.Category;
import dev.ooga.client.module.Module;
import dev.ooga.client.module.impl.client.FriendsModule;
import dev.ooga.client.module.setting.BooleanSetting;
import dev.ooga.client.module.setting.NumberSetting;
import dev.ooga.client.render.Projector;
import dev.ooga.client.ui.OogaTheme;
import dev.ooga.client.ui.render.GlowRenderer;
import dev.ooga.client.ui.render.OogaFonts;
import dev.ooga.client.ui.render.OogaFonts.Weight;
import dev.ooga.client.ui.render.Render2D;
import dev.ooga.client.util.ColorUtil;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Clean, readable name tags drawn on the HUD over players (and optionally mobs), visible
 * through walls at any distance: name, health and distance, plus held item and armour.
 */
public class NametagsModule extends Module {
	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	public final BooleanSetting players = add(new BooleanSetting("Players", "Tags over other players.", true));
	public final BooleanSetting hostiles = add(new BooleanSetting("Hostiles", "Tags over hostile mobs.", false));
	public final BooleanSetting droppedItems = add(new BooleanSetting("Dropped Items", "Tags over items on the ground: name and count.", false));
	public final BooleanSetting health = add(new BooleanSetting("Health", "Show health (including absorption).", true));
	public final BooleanSetting distance = add(new BooleanSetting("Distance", "Show distance in blocks.", true));
	public final BooleanSetting ping = add(new BooleanSetting("Ping", "Show each player's latency.", false));
	public final NumberSetting background = add(new NumberSetting("Background", "Opacity of the tag's backing.", 0.78, 0.0, 1.0, 0.05));
	public final BooleanSetting items = add(new BooleanSetting("Items", "Show held item and armour above the tag.", true));
	public final BooleanSetting hideVanilla = add(new BooleanSetting("Hide Vanilla", "Hide Minecraft's own name tags for entities that get one of ours.", true));
	public final NumberSetting scale = add(new NumberSetting("Scale", "Tag size.", 1.0, 0.5, 2.0, 0.05, "x"));
	public final NumberSetting range = add(new NumberSetting("Range", "Maximum distance.", 256, 16, 512, 8, "m"));

	private record Tag(Entity entity, float x, float y, double distance) {
	}

	public NametagsModule() {
		super("Nametags", "Readable tags with health and gear, through walls.", Category.RENDER);
		HudElementRegistry.addFirst(Identifier.fromNamespaceAndPath(OogaClient.MOD_ID, "nametags"), this::render);
	}

	public boolean wants(Entity entity) {
		if (!isEnabled() || mc.player == null || entity == mc.player) return false;
		if (entity instanceof Player) return players.get();
		if (entity instanceof Enemy) return hostiles.get();
		if (entity instanceof net.minecraft.world.entity.item.ItemEntity) return droppedItems.get();
		return false;
	}

	/** Whether vanilla should skip its own tag for this entity. */
	public boolean hidesVanilla(Entity entity) {
		return hideVanilla.get() && wants(entity);
	}

	private void render(GuiGraphics g, DeltaTracker tracker) {
		if (!isEnabled() || mc.level == null || mc.player == null || mc.options.hideGui) return;
		float partialTick = tracker.getGameTimeDeltaPartialTick(false);
		Vec3 camera = Projector.camera();
		double max = range.get();
		List<Tag> tags = new ArrayList<>();
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!wants(entity) || entity.isRemoved()) continue;
			Vec3 feet = entity.getPosition(partialTick);
			double d = feet.distanceTo(camera);
			if (d > max) continue;
			float[] screen = Projector.toScreen(feet.x, feet.y + entity.getBbHeight() + 0.35, feet.z);
			if (screen == null) continue;
			tags.add(new Tag(entity, screen[0], screen[1], d));
		}
		// Far first, so nearer tags draw on top.
		tags.sort((a, b) -> Double.compare(b.distance(), a.distance()));
		for (Tag tag : tags) draw(g, tag);
	}

	private void draw(GuiGraphics g, Tag tag) {
		Entity entity = tag.entity();
		String name = entity.getName().getString();
		if (entity instanceof net.minecraft.world.entity.item.ItemEntity item) {
			var stack = item.getItem();
			name = stack.getHoverName().getString() + (stack.getCount() > 1 ? " x" + stack.getCount() : "");
		}
		String hp = null;
		int hpColor = 0;
		if (health.get() && entity instanceof LivingEntity living) {
			float value = living.getHealth() + living.getAbsorptionAmount();
			float ratio = Math.min(1f, living.getHealth() / Math.max(1f, living.getMaxHealth()));
			hp = value >= 10 ? Integer.toString(Math.round(value)) : String.format("%.1f", value);
			hpColor = living.getAbsorptionAmount() > 0 ? 0xFFF2C94C : ColorUtil.lerp(0xFFE5484D, 0xFF46C37B, ratio);
		}
		String dist = distance.get() ? Math.round(tag.distance()) + "m" : null;
		if (entity instanceof Player player) {
			int pops = dev.ooga.client.module.impl.combat.TotemPopsModule.popsOf(player.getGameProfile().name());
			if (pops > 0) dist = (dist == null ? "" : dist + " ") + "-" + pops + " totem" + (pops == 1 ? "" : "s");
		}
		if (ping.get() && entity instanceof Player && mc.getConnection() != null) {
			var info = mc.getConnection().getPlayerInfo(entity.getUUID());
			if (info != null) dist = (dist == null ? "" : dist + " ") + info.getLatency() + "ms";
		}

		float gap = 4f, pad = 4f;
		float w = OogaFonts.width(name, Weight.SEMIBOLD);
		if (hp != null) w += gap + OogaFonts.width(hp, Weight.SEMIBOLD);
		if (dist != null) w += gap + OogaFonts.width(dist, Weight.REGULAR, 0.85f);
		w += pad * 2;
		float h = 13f;
		float s = scale.getFloat();

		g.pose().pushMatrix();
		g.pose().translate(tag.x(), tag.y());
		g.pose().scale(s, s);
		float x = -w / 2f, y = -h;
		GlowRenderer.glow(g, x, y, w, h, OogaTheme.RADIUS_CONTROL, FriendsModule.highlights(entity) ? FriendsModule.COLOR : OogaTheme.GOLD, 0.35f, 3f);
		Render2D.roundRect(g, x, y, w, h, OogaTheme.RADIUS_CONTROL, ColorUtil.withAlpha(0x0E0F12, Math.round(255 * background.getFloat())));
		Render2D.outline(g, x, y, w, h, OogaTheme.RADIUS_CONTROL, FriendsModule.highlights(entity) ? FriendsModule.COLOR : entity instanceof Player ? OogaTheme.accent(0x80) : OogaTheme.BORDER);
		float tx = x + pad, ty = y + 2.5f;
		OogaFonts.draw(g, name, tx, ty, OogaTheme.TEXT, Weight.SEMIBOLD);
		tx += OogaFonts.width(name, Weight.SEMIBOLD) + gap;
		if (hp != null) {
			OogaFonts.draw(g, hp, tx, ty, hpColor, Weight.SEMIBOLD);
			tx += OogaFonts.width(hp, Weight.SEMIBOLD) + gap;
		}
		if (dist != null) OogaFonts.draw(g, dist, tx, ty + 0.8f, OogaTheme.TEXT_SECONDARY, Weight.REGULAR, 0.85f);

		if (items.get() && entity instanceof LivingEntity living) drawGear(g, living, y - 2f);
		g.pose().popMatrix();
	}

	/** Held item then armour, centred in a row just above the tag. */
	private void drawGear(GuiGraphics g, LivingEntity living, float bottom) {
		List<ItemStack> stacks = new ArrayList<>();
		ItemStack held = living.getMainHandItem();
		if (!held.isEmpty()) stacks.add(held);
		for (EquipmentSlot slot : ARMOR) {
			ItemStack stack = living.getItemBySlot(slot);
			if (!stack.isEmpty()) stacks.add(stack);
		}
		if (stacks.isEmpty()) return;
		float iconScale = 0.75f;
		float size = 16 * iconScale;
		float x = -stacks.size() * size / 2f;
		g.pose().pushMatrix();
		g.pose().translate(x, bottom - size);
		g.pose().scale(iconScale, iconScale);
		for (int i = 0; i < stacks.size(); i++) {
			g.renderItem(stacks.get(i), i * 16, 0);
			g.renderItemDecorations(mc.font, stacks.get(i), i * 16, 0);
		}
		g.pose().popMatrix();
	}
}
