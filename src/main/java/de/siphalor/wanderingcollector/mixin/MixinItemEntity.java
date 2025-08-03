/*
 * Copyright 2021-2023 Siphalor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 */

package de.siphalor.wanderingcollector.mixin;

import de.siphalor.wanderingcollector.WanderingCollector;
import de.siphalor.wanderingcollector.EntityInterfaces;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Uuids;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(net.minecraft.entity.ItemEntity.class)
public abstract class MixinItemEntity extends Entity implements EntityInterfaces.ItemEntity {
	@Unique
	private static final String FORMER_OWNER_KEY = WanderingCollector.MOD_ID + ":FormerOwner";

	@Unique
	private UUID formerOwner;

	public MixinItemEntity(EntityType<?> type, World world) {
		super(type, world);
	}

	@Override
	public UUID wanderingCollector$getFormerOwner() {
		return formerOwner;
	}

	@Override
	public void wanderingCollector$setFormerOwner(UUID playerUuid) {
		this.formerOwner = playerUuid;
	}

	@Inject(method = "readCustomData", at = @At("RETURN"))
	public void readCustomDataInject(ReadView view, CallbackInfo ci) {
		view.read(FORMER_OWNER_KEY, Uuids.INT_STREAM_CODEC)
				.ifPresent(uuid -> formerOwner = uuid);
	}

	@Inject(method = "writeCustomData", at = @At("RETURN"))
	public void writeCustomDataInject(WriteView view, CallbackInfo ci) {
		if (formerOwner != null) {
			view.putNullable(FORMER_OWNER_KEY, Uuids.INT_STREAM_CODEC, formerOwner);
		}
	}

	@Inject(
			method = "tick",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ItemEntity;discard()V", ordinal = 1)
	)
	public void tickInject(CallbackInfo callbackInfo) {
		WanderingCollector.addStackToThrower((net.minecraft.entity.ItemEntity)(Object) this);
	}

	@Inject(
			method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ItemEntity;discard()V")
	)
	public void onDeathInject(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		WanderingCollector.addStackToThrower((net.minecraft.entity.ItemEntity)(Object) this);
	}
}