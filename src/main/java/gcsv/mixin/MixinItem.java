package gcsv.mixin;


import gcsv.GCSVMod;
import gcsv.IDeathItem;
import net.minecraft.entity.ItemEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
abstract class MixinItem implements IDeathItem {
    @Shadow
    private int itemAge;
    @Unique
    private short numExpirations;

    @Inject(at = @At("HEAD"), method = "writeCustomDataToNbt")
    public void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        if (this.numExpirations != 0)
            nbt.putShort("NumExpirations", this.numExpirations);
    }

    @Inject(at = @At("HEAD"), method = "readCustomDataFromNbt")
    public void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("NumExpirations"))
            this.numExpirations = nbt.getShort("NumExpirations");
    }

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "net/minecraft/entity/ItemEntity.discard ()V")
    )
    private void discardDueToAge(ItemEntity instance) {
        if (this.numExpirations == 0) {
            instance.discard();
            return;
        }

        final int finalExpiration = GCSVMod.INSTANCE.properties.deathItemTicks / 6000;
        if (this.numExpirations == finalExpiration)
            this.itemAge = 6000 - (GCSVMod.INSTANCE.properties.deathItemTicks % 6000);
        else if (this.numExpirations < finalExpiration)
            this.itemAge = 0;

        this.numExpirations++;
        if (this.itemAge >= 6000)
            instance.discard();
    }

    @Override
    public void gcsv$markAsDeathItem() {
        this.numExpirations = 1;
    }
}
