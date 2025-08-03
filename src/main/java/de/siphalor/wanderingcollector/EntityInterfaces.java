package de.siphalor.wanderingcollector;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.village.TradeOfferList;

import java.util.UUID;

public class EntityInterfaces {

    public interface ItemEntity {
        void wanderingCollector$setFormerOwner(UUID playerUuid);
        UUID wanderingCollector$getFormerOwner();
    }

    public interface ServerPlayerEntity {
        LostItemStorage wandering_collector$getLostItemStorage();
    }

    public interface WanderingTraderEntity {
        TradeOfferList wandering_collector$getOffers(PlayerEntity playerEntity);
    }

    public interface MerchantInventory {
        void wandering_collector$setPlayer(PlayerEntity playerEntity);
    }
}