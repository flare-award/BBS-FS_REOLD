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
     * The old injection point, just after {@code startDrawing()}, no longer exists - that
     * method is gone and {@code draw} is the whole body now - so the hook sits at HEAD,
     * which is the same position in the frame: before this layer puts anything on screen.
     */
    @Inject(method = "draw", at = @At("HEAD"))
    public void onDraw(BuiltBuffer buffer, CallbackInfo info)
    {
        CustomVertexConsumerProvider.drawLayer((RenderLayer) (Object) this);
    }
}
