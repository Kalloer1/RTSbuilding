package com.rtsbuilding.rtsbuilding.mixin;

import com.rtsbuilding.rtsbuilding.client.controller.ClientRtsController;
import com.rtsbuilding.rtsbuilding.client.rendering.util.RaycastHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "snownee.jade.overlay.RayTracing")
public class RayTracingMixin {

    @Inject(method = "fire", at = @At("HEAD"), cancellable = true)
    private void rtsbuilding$fire(CallbackInfo ci) {
        if (!ClientRtsController.get().isEnabled()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 rayDir = RaycastHelper.computeCursorRayDirection(mc);
        double maxDist = mc.player.blockInteractionRange() + 5.0;
        Vec3 rayEnd = camPos.add(rayDir.scale(maxDist));

        ClipContext context = new ClipContext(camPos, rayEnd, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player);
        BlockHitResult rtsHitResult = mc.level.clip(context);

        if (rtsHitResult != null && rtsHitResult.getType() == HitResult.Type.BLOCK) {
            try {
                java.lang.reflect.Field targetField = this.getClass().getDeclaredField("target");
                targetField.setAccessible(true);
                targetField.set(this, rtsHitResult);
            } catch (Exception e) {
            }
            ci.cancel();
        }
    }
}
