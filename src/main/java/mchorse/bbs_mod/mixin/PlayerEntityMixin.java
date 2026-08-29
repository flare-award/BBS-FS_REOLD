package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.morphing.IMorphProvider;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * For some unknown reason to me, if these methods are used in {@link PlayerEntityMorphMixin}
 * then the world will be locked for some reason... by extracting write/read NBT method to
 * a separate mixin fixes it...
 */
@Mixin(PlayerEntity.class)
public class PlayerEntityMixin
{
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    public void onWriteCustomDataToNbt(NbtCompound nbt, CallbackInfo info)
    {
        if (this instanceof IMorphProvider provider)
        {
            nbt.put("BBSMorph", provider.getMorph().toNbt());
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    public void onReadCustomDataFromNbt(NbtCompound nbt, CallbackInfo info)
    {
        if (this instanceof IMorphProvider provider)
        {
            if (nbt.contains("BBSMorph"))
            {
                provider.getMorph().fromNbt(nbt.getCompound("BBSMorph"));
            }
        }
    }

    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    public void onGetDimensions(CallbackInfoReturnable<EntityDimensions> info)
    {
        if (this instanceof IMorphProvider provider)
        {
            Form form = provider.getMorph().getForm();

            if (form != null && form.hitbox.get())
            {
                PlayerEntity player = (PlayerEntity) (Object) this;
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
