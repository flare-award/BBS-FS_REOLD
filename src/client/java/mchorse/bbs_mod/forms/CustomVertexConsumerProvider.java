package mchorse.bbs_mod.forms;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.forms.renderers.utils.RecolorVertexConsumer;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A vertex consumer provider that keeps one long-lived {@link BufferBuilder} per render layer and
 * never lets the layers trip over each other.
 *
 * <p>Since 1.21 the vanilla immediate provider hands out a brand new builder per {@code getBuffer}
 * call and refuses to switch layers before the previous one was drawn ("mismatched begin/end").
 * Forms deliberately keep several layers alive at once — the structure form fills the terrain
 * layers, the item and model forms fill entity layers, and the deferred translucent pass draws them
 * in its own order later — so this provider owns the builders itself and draws them in the order
 * the layer map was built, which is what keeps opaque geometry in front of translucent geometry.</p>
 */
public class CustomVertexConsumerProvider extends VertexConsumerProvider.Immediate
{
    private static Consumer<RenderLayer> runnables;

    private final BufferAllocator fallbackAllocator;
    private final Map<RenderLayer, BufferAllocator> layerAllocators;
    private final Map<RenderLayer, BufferBuilder> builders = new LinkedHashMap<>();

    private Function<VertexConsumer, VertexConsumer> substitute;
    private boolean ui;

    public static void drawLayer(RenderLayer layer)
    {
        if (runnables != null)
        {
            runnables.accept(layer);
        }
    }

    public static void hijackVertexFormat(Consumer<RenderLayer> runnable)
    {
        runnables = runnable;
    }

    public static void clearRunnables()
    {
        runnables = null;
    }

    public CustomVertexConsumerProvider(BufferAllocator fallbackAllocator, SequencedMap<RenderLayer, BufferAllocator> layerAllocators)
    {
        super(fallbackAllocator, layerAllocators);

        this.fallbackAllocator = fallbackAllocator;
        this.layerAllocators = layerAllocators;
    }

    public void setSubstitute(Function<VertexConsumer, VertexConsumer> substitute)
    {
        this.substitute = substitute;

        if (this.substitute == null)
        {
            RecolorVertexConsumer.newColor = null;
        }
    }

    public void setUI(boolean ui)
    {
        this.ui = ui;
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer renderLayer)
    {
        BufferBuilder buffer = this.builders.computeIfAbsent(renderLayer, layer -> new BufferBuilder(
            this.layerAllocators.getOrDefault(layer, this.fallbackAllocator),
            layer.getDrawMode(),
            layer.getVertexFormat()
        ));

        if (this.substitute != null)
        {
            VertexConsumer apply = this.substitute.apply(buffer);

            if (apply != null)
            {
                return apply;
            }
        }

        return buffer;
    }

    /**
     * Translucent layers of buffered forms (blocks, items) defer into the frame's sorted
     * translucent queue instead of drawing immediately — otherwise their semi-transparent
     * pixels write depth mid-frame and occlude forms drawn after them. Active only when the
     * current form renderer published its sort origin (never in picking or UI paths).
     */
    @Override
    public void draw(RenderLayer layer)
    {
        Vector3f origin = FormTranslucentQueue.getSortOrigin();

        /* Text layers defer only inside a recorded group (labels), where the group preserves
         * the text-over-background order. */
        boolean textLayer = FormTranslucentQueue.isGroupOpen() && layer.getVertexFormat() == VertexFormats.POSITION_COLOR_TEXTURE_LIGHT;

        if (origin == null || !FormTranslucentQueue.isActive() || !(textLayer || isDeferrableTranslucent(layer)))
        {
            drawNow(layer, this.builders.get(layer));

            return;
        }

        BufferBuilder builder = this.builders.get(layer);

        if (builder == null || builder.vertexCount == 0)
        {
            return;
        }

        /* Ending and uploading the layer's buffer here is what the immediate provider's own
         * draw would have done — including the vertex layout Iris pins around it. */
        boolean extended = BBSRendering.beginIrisBufferUpload(builder);
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);

        try
        {
            buffer.bind();
            buffer.upload(builder.end());
            VertexBuffer.unbind();
        }
        finally
        {
            BBSRendering.endIrisBufferUpload(extended);
        }

        FormTranslucentQueue.add(new FormTranslucentQueue.RenderLayerCommand(layer, buffer, new Matrix4f(RenderSystem.getModelViewMatrix()), new Vector3f(origin)));
    }

    private static void drawNow(RenderLayer layer, BufferBuilder builder)
    {
        if (builder == null)
        {
            return;
        }

        BuiltBuffer built = builder.endNullable();

        if (built != null)
        {
            BufferRenderer.drawWithGlobalProgram(built);
        }
    }

    private static boolean isDeferrableTranslucent(RenderLayer layer)
    {
        String name = layer.toString();

        return name.contains("translucent") && !name.contains("glint");
    }

    public void draw()
    {
        /* Insertion order: the layer map is built opaque-first, so opaque writes depth before the
         * translucent layers are drawn over it. */
        this.builders.forEach(CustomVertexConsumerProvider::drawNow);

        if (this.ui)
        {
            /* Force back the depth func because it seems like stuff rendered by a vertex
             * consumer is resetting the depth func to GL_LESS, and since this vertex consumer
             * is designed  */
            RenderSystem.depthFunc(GL11.GL_ALWAYS);
        }
    }
}
