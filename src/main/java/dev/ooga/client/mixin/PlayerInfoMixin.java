package dev.ooga.client.mixin;

import dev.ooga.client.module.impl.misc.SkinProtectModule;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Skin Protect: hands out the default skin for players it hides. */
@Mixin(PlayerInfo.class)
public abstract class PlayerInfoMixin {
	@Inject(method = "getSkin", at = @At("RETURN"), cancellable = true, require = 0)
	private void ooga$skinProtect(CallbackInfoReturnable<PlayerSkin> cir) {
		PlayerInfo self = (PlayerInfo) (Object) this;
		if (SkinProtectModule.hides(self.getProfile().name())) cir.setReturnValue(DefaultPlayerSkin.get(self.getProfile()));
	}
}
