package gcsv.mixin;

import gcsv.IDeathItem;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(PlayerInventory.class)
abstract class MixinPlayerInventory {
    @Redirect(
            method = "dropAll",
            at = @At(value = "INVOKE", target = "net/minecraft/entity/player/PlayerEntity.dropItem (Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;")
    )
    private ItemEntity dropItem(PlayerEntity thisPlayer, ItemStack stack, boolean throwRandomly, boolean retainOwnership) {
        ItemEntity itemEntity = thisPlayer.dropItem(stack, throwRandomly, retainOwnership);
        if (itemEntity != null)
            ((IDeathItem) itemEntity).gcsv$markAsDeathItem();

        return itemEntity;
    }
}
