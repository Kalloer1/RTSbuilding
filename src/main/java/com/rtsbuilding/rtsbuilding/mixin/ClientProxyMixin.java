package com.rtsbuilding.rtsbuilding.mixin;

import com.rtsbuilding.rtsbuilding.client.screen.standalone.BuilderScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "snownee.jade.util.ClientProxy")
public class ClientProxyMixin {

    @Inject(method = "shouldShowAfterGui", at = @At("HEAD"), cancellable = true)
    private static void rtsbuilding$shouldShowAfterGui(Minecraft mc, Screen screen, CallbackInfoReturnable<Boolean> cir) {
        if (screen instanceof BuilderScreen) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "shouldShowWithGui", at = @At("HEAD"), cancellable = true)
    private static void rtsbuilding$shouldShowWithGui(Minecraft mc, Screen screen, CallbackInfoReturnable<Boolean> cir) {
        if (screen instanceof BuilderScreen) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "shouldShowBeforeGui", at = @At("HEAD"), cancellable = true)
    private static void rtsbuilding$shouldShowBeforeGui(Minecraft mc, Screen screen, CallbackInfoReturnable<Boolean> cir) {
        if (screen instanceof BuilderScreen) {
            cir.setReturnValue(true);
        }
    }
}
