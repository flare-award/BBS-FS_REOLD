package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderLayer.class)
public class RenderLayerMixin
{
    /**
     * Since 1.21.1 a layer draws a finished {@code BuiltBuffer} instead of a building
     * {@code BufferBuilder} plus a vertex sorter, so that is the shape of this callback.
     */
    @Inject(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderLayer;startDrawing()V", ordinal = 0, shift = At.Shift.AFTER))
    public void onDraw(BuiltBuffer buffer, CallbackInfo info)
    {
        CustomVertexConsumerProvider.drawLayer((RenderLayer) (Object) this);
    }
}
