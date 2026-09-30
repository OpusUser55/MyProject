package dev.ooga.client.config;

import dev.ooga.client.module.Module;
import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.impl.basefinding.HoleEspModule;
import dev.ooga.client.module.impl.basefinding.LightFinderModule;
import dev.ooga.client.module.impl.basefinding.NewChunksModule;
import dev.ooga.client.module.impl.basefinding.SpawnerFinderModule;
import dev.ooga.client.module.impl.basefinding.StorageEspModule;
import dev.ooga.client.module.impl.basefinding.SusChunkFinderModule;
import dev.ooga.client.module.impl.basefinding.TunnelFinderModule;
import dev.ooga.client.module.impl.client.FindsHudModule;
import dev.ooga.client.module.impl.client.RadarModule;
import dev.ooga.client.module.impl.client.RegionMapModule;
import dev.ooga.client.module.impl.client.SafetyModule;
import dev.ooga.client.module.impl.client.TargetHudModule;
import dev.ooga.client.module.impl.combat.AimAssistModule;
import dev.ooga.client.module.impl.combat.AutoCrystalModule;
import dev.ooga.client.module.impl.combat.AutoTotemModule;
import dev.ooga.client.module.impl.combat.CrystalOptimizerModule;
import dev.ooga.client.module.impl.combat.HitboxModule;
import dev.ooga.client.module.impl.combat.JumpResetModule;
import dev.ooga.client.module.impl.combat.NoHitDelayModule;
import dev.ooga.client.module.impl.combat.TriggerBotModule;
import dev.ooga.client.module.impl.combat.VelocityModule;
import dev.ooga.client.module.impl.misc.AdminDetectorModule;
import dev.ooga.client.module.impl.misc.AutoClickerModule;
import dev.ooga.client.module.impl.misc.FastPlaceModule;
import dev.ooga.client.module.impl.movement.NoFallModule;
import dev.ooga.client.module.impl.movement.StepModule;
import dev.ooga.client.module.impl.render.BlockEspModule;
import dev.ooga.client.module.impl.render.EspModule;
import dev.ooga.client.module.impl.render.FullbrightModule;
import dev.ooga.client.module.impl.render.NametagsModule;
import dev.ooga.client.module.impl.world.FastBreakModule;
import dev.ooga.client.module.impl.world.WaypointsModule;

import java.util.List;

/** Built-in one-click setups. Each turns a set of modules on and another set off. */
public final class Presets {
	public record Preset(String name, String description, List<Class<? extends Module>> on, List<Class<? extends Module>> off, boolean safe) {
	}

	private static final List<Class<? extends Module>> BLATANT = List.of(TriggerBotModule.class, AimAssistModule.class, HitboxModule.class,
			VelocityModule.class, NoFallModule.class, StepModule.class, FastBreakModule.class, FastPlaceModule.class, AutoClickerModule.class,
			NoHitDelayModule.class, CrystalOptimizerModule.class, AutoCrystalModule.class, JumpResetModule.class);

	public static final List<Preset> ALL = List.of(
			new Preset("Legit", "Visuals and safety only; no combat automation.",
					List.of(EspModule.class, NametagsModule.class, StorageEspModule.class, SpawnerFinderModule.class, AutoTotemModule.class,
							AdminDetectorModule.class, FullbrightModule.class),
					BLATANT, true),
			new Preset("Base Hunting", "Every finder, the radar, region map and waypoints.",
					List.of(StorageEspModule.class, SpawnerFinderModule.class, SusChunkFinderModule.class, TunnelFinderModule.class,
							HoleEspModule.class, LightFinderModule.class, NewChunksModule.class, BlockEspModule.class, FindsHudModule.class,
							RadarModule.class, RegionMapModule.class, WaypointsModule.class, FullbrightModule.class, AdminDetectorModule.class),
					BLATANT, true),
			new Preset("PvP", "Combat modules at safe, human-looking settings.",
					List.of(TriggerBotModule.class, AutoTotemModule.class, HitboxModule.class, VelocityModule.class, CrystalOptimizerModule.class,
							NoHitDelayModule.class, TargetHudModule.class, NametagsModule.class, EspModule.class, AdminDetectorModule.class),
					List.of(), true),
			new Preset("Blatant", "Everything on, Safe Mode off. Expect bans on servers with anticheat.",
					BLATANT, List.of(), false));

	private Presets() {
	}

	public static void apply(Preset preset) {
		ModuleManager modules = ModuleManager.get();
		for (Class<? extends Module> type : preset.off()) modules.get(type).setEnabled(false, false);
		for (Class<? extends Module> type : preset.on()) modules.get(type).setEnabled(true, false);
		modules.get(SafetyModule.class).safeMode.set(preset.safe());
		ConfigManager.get().markDirty();
	}
}
