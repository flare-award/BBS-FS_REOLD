package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.morphing.IMorphProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Resizes a morphed player.
 *
 * <p>This used to sit in {@link PlayerEntityMixin}, but since 1.21 {@code PlayerEntity} no longer
 * declares {@code getDimensions} at all - the override that used to handle the sneak pose was
 * folded into {@link Entity#getDimensions}, so there is nothing left on the player to hook.
 * The hook lives on {@code Entity} instead and narrows itself with an {@code instanceof}:
 * {@code IMorphProvider} is only mixed into {@code PlayerEntity}, so no other entity is affected.</p>
 */
@Mixin(Entity.class)
public class EntityMixin
{
    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    public void onGetDimensions(CallbackInfoReturnable<EntityDimensions> info)
    {
        /* IMorphProvider is an interface, so the instanceof narrows to players on its own; the
         * player reference then needs the (Type) (Object) this double cast the other morph mixins
         * use, because this mixin class is not a PlayerEntity in the compiler's eyes. */
        if (this instanceof IMorphProvider provider)
        {
            PlayerEntity player = (PlayerEntity) (Object) this;
            Form form = provider.getMorph().getForm();

            if (form != null && form.hitbox.get())
            {
                EntityDimensions dimensions = info.getReturnValue();
                float height = form.hitboxHeight.get() * (player.isSneaking() ? form.hitboxSneakMultiplier.get() : 1F);
                EntityDimensions morphed = dimensions.fixed()
                    ? EntityDimensions.fixed(form.hitboxWidth.get(), height)
                    : EntityDimensions.changing(form.hitboxWidth.get(), height);

                /* Since 1.21 the eye height travels with the dimensions instead of being answered
                 * by LivingEntity.getActiveEyeHeight(EntityPose, EntityDimensions), which is gone:
                 * setting it here is what keeps a morphed player looking through its own eyes. */
                info.setReturnValue(morphed.withEyeHeight(form.hitboxEyeHeight.get() * height));
            }
        }
    }
}
