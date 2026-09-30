package dev.ooga.client.mixin;

import dev.ooga.client.module.ModuleManager;
import dev.ooga.client.module.impl.combat.CrystalOptimizerModule;
import dev.ooga.client.module.impl.combat.MaceSwapModule;
import dev.ooga.client.module.impl.render.HitParticlesModule;
import dev.ooga.client.module.impl.combat.ShieldBreakerModule;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Weapon swaps for a single hit. Runs before {@code attack} syncs the held slot, so the
 * server sees the swapped weapon for this very attack.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
	@Inject(method = "attack", at = @At("HEAD"), require = 0)
	private void ooga$beforeAttack(Player player, Entity target, CallbackInfo ci) {
		ModuleManager modules = ModuleManager.get();
		modules.get(ShieldBreakerModule.class).beforeAttack(target);
		modules.get(MaceSwapModule.class).beforeAttack(target);
	}

	@Inject(method = "attack", at = @At("TAIL"), require = 0)
	private void ooga$afterAttack(Player player, Entity target, CallbackInfo ci) {
		ModuleManager.get().get(HitParticlesModule.class).onHit(target);
		ModuleManager.get().get(CrystalOptimizerModule.class).afterAttack(target);
	}
}
