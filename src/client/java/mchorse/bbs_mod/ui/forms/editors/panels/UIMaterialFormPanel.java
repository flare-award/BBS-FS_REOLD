package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.framework.elements.UISection;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UISliderTrackpad;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.function.BiConsumer;

/**
 * Material tab of the form editor. It always edits the form currently picked
 * in the editor's form list on the left - pick a body part (or a model nested
 * through body parts) there, and this tab edits that one: its colors.
 *
 * <p>The Rendering and Shaders (PBR) sections were removed from this tab; the
 * underlying form values still exist and are still read by the renderers and by
 * {@code FormMaterials}, so existing films keep rendering exactly as before.
 */
public class UIMaterialFormPanel extends UIFormPanel
{
    public UIColor color;
    public UIColor colorOverlay;
    public UISliderTrackpad lighting;
    public UISliderTrackpad hue;
    public UISliderTrackpad saturation;

    public UIMaterialFormPanel(UIForm editor)
    {
        super(editor);

        this.color = new UIColor((c) -> this.setColor(Color.rgba(c))).withAlpha();
        this.color.tooltip(UIKeys.FORMS_EDITORS_MATERIAL_COLOR_TOOLTIP);
        this.colorOverlay = new UIColor((c) -> this.form.colorOverlay.set(Color.rgba(c))).withAlpha();
        this.colorOverlay.tooltip(UIKeys.FORMS_EDITORS_MATERIAL_COLOR_OVERLAY_TOOLTIP);
        this.lighting = this.createSlider((form, v) -> form.lighting.set(v));
        this.lighting.tooltip(UIKeys.FORMS_EDITORS_MATERIAL_LIGHTING_TOOLTIP);
        this.hue = this.createSlider((form, v) -> form.hue.set(v), -180D, 180D, 1D);
        this.hue.tooltip(UIKeys.FORMS_EDITORS_MATERIAL_HUE_TOOLTIP);
        this.saturation = this.createSlider((form, v) -> form.saturation.set(v), 0D, 2D, 0.05D);
        this.saturation.tooltip(UIKeys.FORMS_EDITORS_MATERIAL_SATURATION_TOOLTIP);

        UISection colorSection = this.section(UIKeys.FORMS_EDITORS_MATERIAL_SECTION_COLOR, "material_color", true);

        colorSection.fields.add(
            UI.labelRow(UIKeys.FORMS_EDITORS_MATERIAL_COLOR, this.color),
            UI.labelRow(UIKeys.FORMS_EDITORS_MATERIAL_COLOR_OVERLAY, this.colorOverlay),
            UI.labelRow(UIKeys.FORMS_EDITORS_MATERIAL_HUE, this.hue),
            UI.labelRow(UIKeys.FORMS_EDITORS_MATERIAL_SATURATION, this.saturation),
            UI.labelRow(UIKeys.FORMS_EDITORS_MATERIAL_LIGHTING, this.lighting)
        );

        this.options.add(colorSection);
    }

    /** A 0..1 slider that moves in hundredths and writes into the edited form. */
    private UISliderTrackpad createSlider(BiConsumer<Form, Float> setter)
    {
        return this.createSlider(setter, 0D, 1D, 0.01D);
    }

    private UISliderTrackpad createSlider(BiConsumer<Form, Float> setter, double min, double max, double step)
    {
        UISliderTrackpad slider = new UISliderTrackpad((v) -> setter.accept(this.form, v.floatValue()));

        slider.limit(min, max);
        slider.snap(step);

        return slider;
    }

    private void setColor(Color value)
    {
        ValueColor colorValue = this.getColorValue();

        if (colorValue != null)
        {
            colorValue.set(value);
        }
    }

    /**
     * The form's tint color, when its kind has one (model, billboard and most
     * others do). It isn't part of the base form, hence the lookup by id.
     */
    private ValueColor getColorValue()
    {
        if (this.form != null && this.form.get("color") instanceof ValueColor value)
        {
            return value;
        }

        return null;
    }

    @Override
    public void startEdit(Form form)
    {
        super.startEdit(form);

        ValueColor colorValue = this.getColorValue();

        this.color.setEnabled(colorValue != null);
        this.color.setColor(colorValue == null ? Colors.WHITE : colorValue.get().getARGBColor());
        this.colorOverlay.setColor(form.colorOverlay.get().getARGBColor());
        this.hue.setValue(form.hue.get());
        this.saturation.setValue(form.saturation.get());
        this.lighting.setValue(form.lighting.get());
    }
}
