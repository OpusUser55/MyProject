package dev.ooga.client.mixin;

import dev.ooga.client.module.impl.misc.NameProtectModule;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Every formatted string the font draws passes through here, which makes it the one place to hide the username. */
@Mixin(StringDecomposer.class)
public abstract class StringDecomposerMixin {
	@ModifyVariable(require = 0, method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
			at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private static String ooga$nameProtect(String text) {
		return NameProtectModule.filter(text);
	}
}
