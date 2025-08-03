 package de.siphalor.wanderingcollector.mixin;

import com.mojang.serialization.Codec;
import de.siphalor.wanderingcollector.*;
import de.siphalor.wanderingcollector.EntityInterfaces;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.Merchant;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradedItem;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(WanderingTraderEntity.class)
public abstract class MixinWanderingTraderEntity extends MerchantEntity implements Merchant, EntityInterfaces.WanderingTraderEntity {
	@Unique
	private final Map<UUID, List<TradeOffer>> playerSpecificTrades = new HashMap<>();

	// Hardcoded config values
	@Unique
	private static final int BUY_BACK_TRADES = 10;
	@Unique
	private static final int MIN_PRICE = 32;
	@Unique
	private static final int MAX_PRICE = 64;
	@Unique
	private static final boolean SCALE_PRICE_WITH_COUNT = true;
	@Unique
	private static final float MIN_PRICE_SCALE = 0.3f;
	@Unique
	private static final Random PRICE_RANDOM = new Random();


	@Unique
	private static final Codec<Map<UUID, List<TradeOffer>>> PLAYER_TRADES_CODEC =
			Codec.unboundedMap(Uuids.CODEC, TradeOffer.CODEC.listOf());

	public MixinWanderingTraderEntity(EntityType<? extends MerchantEntity> entityType, World world) {
		super(entityType, world);
	}

	@Unique
	private ItemStack getPriceStack(ItemStack forStack) {
		ItemStack result = new ItemStack(Items.EMERALD);

		int count = randInclusive();
		if (SCALE_PRICE_WITH_COUNT) {
			count = MathHelper.ceil(count * (MIN_PRICE_SCALE + (1 - MIN_PRICE_SCALE) * forStack.getCount() / forStack.getMaxCount()));
		}
		result.setCount(count);
		return result;
	}

	@Unique
	private int randInclusive() {
		return Math.min(MixinWanderingTraderEntity.MIN_PRICE, MixinWanderingTraderEntity.MAX_PRICE) + (int) (MixinWanderingTraderEntity.PRICE_RANDOM.nextFloat() * (Math.abs(MixinWanderingTraderEntity.MIN_PRICE - MixinWanderingTraderEntity.MAX_PRICE)));
	}

	@Override
	public TradeOfferList wandering_collector$getOffers(PlayerEntity playerEntity) {
		TradeOfferList tradeOfferList = new TradeOfferList();
		tradeOfferList.addAll(getOffers());
		List<TradeOffer> offers = playerSpecificTrades.get(playerEntity.getUuid());
		if (offers == null) {
			LostItemStorage storage = ((EntityInterfaces.ServerPlayerEntity) playerEntity).wandering_collector$getLostItemStorage();
			if (storage.isEmpty() || BUY_BACK_TRADES <= 0) {
				offers = Collections.emptyList();
			} else {
				offers = new ArrayList<>(BUY_BACK_TRADES);
				for (ItemStack stack : storage.poll(BUY_BACK_TRADES)) {
					// Create TradedItem from the price stack
					TradedItem buyItem = new TradedItem(getPriceStack(stack).getItem());
					offers.add(new TradeOffer(buyItem, stack, 1, 1, 1F));
				}
			}
			playerSpecificTrades.put(playerEntity.getUuid(), offers);
		}
		tradeOfferList.addAll(offers);
		return tradeOfferList;
	}

	@Override
	public void sendOffers(PlayerEntity playerEntity, Text text, int i) {
		OptionalInt optionalInt = playerEntity.openHandledScreen(new SimpleNamedScreenHandlerFactory((ix, playerInventory, playerEntityx) ->
				new MerchantScreenHandler(ix, playerInventory, this), text)
		);
		if (optionalInt.isPresent()) {
			TradeOfferList tradeOfferList = wandering_collector$getOffers(playerEntity);
			if (!tradeOfferList.isEmpty()) {
				playerEntity.sendTradeOffers(optionalInt.getAsInt(), tradeOfferList, i, this.getExperience(), this.isLeveledMerchant(), this.canRefreshTrades());
			}
		}
	}

	@Inject(method = "readCustomData", at = @At("RETURN"))
	public void readCustomDataInject(ReadView view, CallbackInfo callbackInfo) {
		playerSpecificTrades.clear();
		view.read(WanderingCollector.PLAYER_SPECIFIC_TRADES, PLAYER_TRADES_CODEC)
				.ifPresent(playerSpecificTrades::putAll);
	}

	@Inject(method = "writeCustomData", at = @At("RETURN"))
	public void writeCustomDataInject(WriteView view, CallbackInfo callbackInfo) {
		if (!playerSpecificTrades.isEmpty()) {
			view.put(WanderingCollector.PLAYER_SPECIFIC_TRADES, PLAYER_TRADES_CODEC, playerSpecificTrades);
		}
	}
}