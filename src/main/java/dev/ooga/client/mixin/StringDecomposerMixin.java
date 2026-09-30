package dev.ooga.client.mixin;

import dev.ooga.client.module.impl.misc.NameProtectModule;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Name Protect: every piece of drawn text is decomposed here, so one hook covers it all. */
@Mixin(StringDecomposer.class)
public abstract class StringDecomposerMixin {
	@Inject(method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
			at = @At("HEAD"), cancellable = true, require = 0)
	private static void ooga$nameProtect(String text, int start, Style style, Style reset, FormattedCharSink sink, CallbackInfoReturnable<Boolean> cir) {
		// Only whole strings: a start offset indexes into the original text.
		if (start != 0) return;
		String replaced = NameProtectModule.protect(text);
		if (replaced != null) cir.setReturnValue(StringDecomposer.iterateFormatted(replaced, 0, style, reset, sink));
	}
}
