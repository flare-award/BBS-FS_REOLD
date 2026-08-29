package mchorse.bbs_mod.client;

import mchorse.bbs_mod.BBSMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

public class BBSShaders
{
    private static final Logger LOGGER = LoggerFactory.getLogger("BBS/Shaders");

    private static ShaderProgram model;
    private static ShaderProgram multiLink;
    private static ShaderProgram subtitles;
    private static ShaderProgram selection;
    private static ShaderProgram pixelArt;
    private static ShaderProgram pixelArtText;
    private static ShaderProgram pixelArtTextIntensity;

    private static ShaderProgram pickerPreview;
    private static ShaderProgram pickerBillboard;
    private static ShaderProgram pickerBillboardNoShading;
    private static ShaderProgram pickerParticles;
    private static ShaderProgram pickerModels;

    static
    {
        setup();
    }

    public static void setup()
    {
        if (model != null) model.close();
        if (subtitles != null) subtitles.close();
        if (selection != null) selection.close();
        if (pixelArt != null) pixelArt.close();
        if (pixelArtText != null) pixelArtText.close();
        if (pixelArtTextIntensity != null) pixelArtTextIntensity.close();

        if (pickerPreview != null) pickerPreview.close();
        if (pickerBillboard != null) pickerBillboard.close();
        if (pickerBillboardNoShading != null) pickerBillboardNoShading.close();
        if (pickerParticles != null) pickerParticles.close();
        if (pickerModels != null) pickerModels.close();

        try
        {
            ResourceFactory factory = new ProxyResourceFactory(MinecraftClient.getInstance().getResourceManager());

            /* Each one loads on its own: the old shared try let the first shader that
             * failed take every later one down with it and left the fields null, which
             * then NPE'd somewhere far from here. Naming the program in the message is
             * what tells us which JSON the driver or the resource pack rejected. */
            model = load(factory, "model", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
            multiLink = load(factory, "multilink", VertexFormats.POSITION_TEXTURE_COLOR);
            subtitles = load(factory, "subtitles", VertexFormats.POSITION_TEXTURE_COLOR);
            selection = load(factory, "selection", VertexFormats.POSITION_TEXTURE_COLOR);

            pickerPreview = load(factory, "picker_preview", VertexFormats.POSITION_TEXTURE_COLOR);
            pickerBillboard = load(factory, "picker_billboard", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
            pickerBillboardNoShading = load(factory, "picker_billboard_no_shading", VertexFormats.POSITION_TEXTURE_LIGHT_COLOR);
            pickerParticles = load(factory, "picker_particles", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT);
            pickerModels = load(factory, "picker_models", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
        }
        catch (IOException e)
        {
            LOGGER.error("BBS could not set up its resource factory for the core shaders", e);
        }

        /* Kept in a try of their own: these are a cosmetic nicety, and a driver
         * that refuses to compile them must not take the shaders the editor
         * can't work without (bone picking, models) down with them. Everything
         * asking for these falls back to vanilla's programs when they're null. */
        try
        {
            ResourceFactory factory = new ProxyResourceFactory(MinecraftClient.getInstance().getResourceManager());

            pixelArt = new ShaderProgram(factory, "pixelart", VertexFormats.POSITION_TEXTURE_COLOR);
            pixelArtText = new ShaderProgram(factory, "pixelart_text", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT);
            pixelArtTextIntensity = new ShaderProgram(factory, "pixelart_text_intensity", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT);
        }
        catch (IOException e)
        {
            /* All or nothing: text must not end up half swapped */
            if (pixelArt != null) pixelArt.close();
            if (pixelArtText != null) pixelArtText.close();
            if (pixelArtTextIntensity != null) pixelArtTextIntensity.close();

            pixelArt = pixelArtText = pixelArtTextIntensity = null;

            e.printStackTrace();
        }
    }

    public static ShaderProgram getModel()
    {
        return model;
    }

    public static ShaderProgram getMultilinkProgram()
    {
        return multiLink;
    }

    public static ShaderProgram getSubtitlesProgram()
    {
        return subtitles;
    }

    public static ShaderProgram getSelectionProgram()
    {
        return selection;
    }

    /**
     * Textured UI quads with the seam between texels smoothed, for when the
     * interface is drawn at a fractional scale (see the shader's own comment).
     */
    public static ShaderProgram getPixelArtProgram()
    {
        return pixelArt;
    }

    public static ShaderProgram getPixelArtTextProgram()
    {
        return pixelArtText;
    }

    public static ShaderProgram getPixelArtTextIntensityProgram()
    {
        return pixelArtTextIntensity;
    }

    /**
     * Load one core shader, naming it in the log if it fails and returning null so the
     * caller can fall back instead of crashing somewhere far away from here.
     */
    private static ShaderProgram load(ResourceFactory factory, String name, VertexFormat format)
    {
        try
        {
            return new ShaderProgram(factory, name, format);
        }
        catch (Exception e)
        {
            LOGGER.error("BBS core shader '{}' failed to load - features using it will fall back or be missing", name, e);

            return null;
        }
    }

    public static ShaderProgram getPickerPreviewProgram()
    {
        return pickerPreview;
    }

    public static ShaderProgram getPickerBillboardProgram()
    {
        return pickerBillboard;
    }

    public static ShaderProgram getPickerBillboardNoShadingProgram()
    {
        return pickerBillboardNoShading;
    }

    public static ShaderProgram getPickerParticlesProgram()
    {
        return pickerParticles;
    }

    public static ShaderProgram getPickerModelsProgram()
    {
        return pickerModels;
    }

    private static class ProxyResourceFactory implements ResourceFactory
    {
        private ResourceManager manager;

        public ProxyResourceFactory(ResourceManager manager)
        {
            this.manager = manager;
        }

        @Override
        public Optional<Resource> getResource(Identifier id)
        {
            if (id.getPath().contains("/core/"))
            {
                return this.manager.getResource(Identifier.of(BBSMod.MOD_ID, id.getPath()));
            }

            /* #moj_import always resolves in the minecraft namespace, so our own
             * includes have to be looked up in BBS first, vanilla's second */
            if (id.getPath().contains("/include/"))
            {
                Optional<Resource> resource = this.manager.getResource(Identifier.of(BBSMod.MOD_ID, id.getPath()));

                if (resource.isPresent())
                {
                    return resource;
                }
            }

            return this.manager.getResource(id);
        }
    }
}
