package de.siphalor.spiceoffabric.mixin;

import de.siphalor.spiceoffabric.SpiceOfFabric;
import de.siphalor.spiceoffabric.config.Config;
import de.siphalor.spiceoffabric.foodhistory.FoodHistory;
import de.siphalor.spiceoffabric.util.IHungerManager;
import de.siphalor.spiceoffabric.util.IServerPlayerEntity;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(HungerManager.class)
public abstract class MixinHungerManager implements IHungerManager {

	protected ServerPlayerEntity spiceOfFabric_player = null;

	@Shadow public abstract void add(int int_1, float float_1);

	@Shadow private float foodSaturationLevel;

	protected FoodHistory spiceOfFabric_foodHistory = new FoodHistory();

	@Override
	public void spiceOfFabric_setPlayer(ServerPlayerEntity serverPlayerEntity) {
		spiceOfFabric_player = serverPlayerEntity;
	}

	@Override
	public void spiceOfFabric_clearHistory() {
		spiceOfFabric_foodHistory.reset();
	}

	@Override
	public void spiceOfFabric_setSaturationLevel(float level) {
		foodSaturationLevel = level;
	}

	@Override
	public FoodHistory spiceOfFabric_getFoodHistory() {
		return spiceOfFabric_foodHistory;
	}

	@Override
	public void spiceOfFabric_setFoodHistory(FoodHistory foodHistory) {
		spiceOfFabric_foodHistory = foodHistory;
	}

	@Inject(method = "eat", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/item/Item;getFoodComponent()Lnet/minecraft/item/FoodComponent;"), cancellable = true)
    public void onItemEat(Item item, ItemStack stack, CallbackInfo callbackInfo) {
		Config.setHungerExpressionValues(spiceOfFabric_foodHistory.getTimesEaten(stack), Objects.requireNonNull(item.getFoodComponent()).getHunger(), item.getFoodComponent().getSaturationModifier(), stack.getMaxUseTime());
		int hunger = Config.getHungerValue() + item.getFoodComponent()).getHunger()/2;
		float saturation = Config.getSaturationValue()*2;
		add(hunger, saturation);
		if(spiceOfFabric_player != null) {
			SpiceOfFabric.onEaten(spiceOfFabric_player, spiceOfFabric_foodHistory, stack);
		}
		callbackInfo.cancel();
	}

	@Inject(method = "fromTag", at = @At("RETURN"))
	public void onDeserialize(CompoundTag compoundTag, CallbackInfo callbackInfo) {
		if(compoundTag.contains(SpiceOfFabric.FOOD_HISTORY_ID, 10)) {
			spiceOfFabric_foodHistory = FoodHistory.read(compoundTag.getCompound(SpiceOfFabric.FOOD_HISTORY_ID));
		}
		((IServerPlayerEntity) spiceOfFabric_player).spiceOfFabric_scheduleFoodHistorySync();
		/*if(Config.carrotEnabled.value && spiceOfFabric_foodHistory.carrotHistory == null) {
			spiceOfFabric_player.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(Config.startHearts.value * 2);
			spiceOfFabric_foodHistory.carrotHistory = new HashSet<>();
		}*/
	}

	@Inject(method = "toTag", at = @At("RETURN"))
	public void onSerialize(CompoundTag compoundTag, CallbackInfo callbackInfo) {
		compoundTag.put(SpiceOfFabric.FOOD_HISTORY_ID, spiceOfFabric_foodHistory.write(new CompoundTag()));
	}
}
