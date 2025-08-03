
package de.siphalor.wanderingcollector.mixin;

import com.mojang.authlib.GameProfile;
import de.siphalor.wanderingcollector.LostItemStorage;
import de.siphalor.wanderingcollector.EntityInterfaces;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity implements EntityInterfaces.ServerPlayerEntity {
	@Unique
	private LostItemStorage lostItemStorage = new LostItemStorage();

	public MixinServerPlayerEntity(ServerWorld world, GameProfile profile) {
		super(world, profile);
	}

	@Inject(
			method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;",
			at = @At(value = "RETURN")
	)
	public void onItemDropped(ItemStack stack, boolean dropAtSelf, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) {
		ItemEntity itemEntity = cir.getReturnValue();
		if (itemEntity != null && !retainOwnership) {
			((EntityInterfaces.ItemEntity) itemEntity).wanderingCollector$setFormerOwner(getUuid());
		}
	}

	@Inject(method = "copyFrom", at = @At("RETURN"))
	public void copyFromInject(ServerPlayerEntity other, boolean alive, CallbackInfo callbackInfo) {
		lostItemStorage = ((EntityInterfaces.ServerPlayerEntity) other).wandering_collector$getLostItemStorage();
	}

	@Inject(method = "readCustomData", at = @At("RETURN"))
	public void readCustomDataInject(ReadView view, CallbackInfo callbackInfo) {
		lostItemStorage.read(view);
	}

	@Inject(method = "writeCustomData", at = @At("RETURN"))
	public void writeCustomDataInject(WriteView view, CallbackInfo callbackInfo) {
		lostItemStorage.write(view);
	}

	@Override
	public LostItemStorage wandering_collector$getLostItemStorage() {
		return lostItemStorage;
	}
}