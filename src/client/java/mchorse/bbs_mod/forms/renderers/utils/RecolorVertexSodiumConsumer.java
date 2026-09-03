package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.utils.colors.Color;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import org.lwjgl.system.MemoryStack;

/**
 * Sodium writes batches of vertices straight into the buffer with its memory intrinsics
 * (see {@link VertexBufferWriter#push}), which walks past every {@code color(...)} call a
 * decorating consumer could hook. Wrapping a consumer that supports the fast path would
 * therefore silently drop those vertices unless the wrapper hands the batch through — this
 * delegates to the wrapped consumer when it is itself a {@link VertexBufferWriter}, which is
 * what Sodium's patched buffer builders are.
 */
public class RecolorVertexSodiumConsumer extends RecolorVertexConsumer implements VertexBufferWriter
{
    public RecolorVertexSodiumConsumer(VertexConsumer consumer, Color color)
    {
        super(consumer, color);

        newColor = color;
    }

    @Override
    public void push(MemoryStack stack, long ptr, int count, VertexFormat format)
    {
        if (this.consumer instanceof VertexBufferWriter writer)
        {
            writer.push(stack, ptr, count, format);
        }
    }

    @Override
    public boolean canUseIntrinsics()
    {
        return this.consumer instanceof VertexBufferWriter writer && writer.canUseIntrinsics();
    }
}
