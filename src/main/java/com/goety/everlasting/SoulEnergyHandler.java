package com.goety.everlasting;

import com.Polarice3.Goety.common.capabilities.soulenergy.ISoulEnergy;
import com.Polarice3.Goety.common.capabilities.soulenergy.SEProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

import static com.mojang.text2speech.Narrator.LOGGER;

@Mod.EventBusSubscriber(modid = GoetyEverlasting.MOD_ID)
public class SoulEnergyHandler {

    private static final int CHECK_INTERVAL_SECONDS = 60;
    private static final int SOUL_COST_PER_TOTEM = 50;

    private static final String[] MOD_ABILITY_IDS = {
            "goety_everlasting:leeching",
            "goety_everlasting:rampage",
            "goety_everlasting:soul_armor",
            "goety_everlasting:explosive",
            "goety_everlasting:buff"
    };

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;

        if (player.level().isClientSide) {
            return;
        }

        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        CompoundTag persistentData = player.getPersistentData();
        long lastCheckTime = persistentData.getLong("lastSoulEnergyCheck");
        long currentTime = player.level().getGameTime();

        // 每60秒检查一次
        if (currentTime - lastCheckTime >= CHECK_INTERVAL_SECONDS * 20) {
            persistentData.putLong("lastSoulEnergyCheck", currentTime);

            // 统计玩家身上的图腾数量
            int totemCount = countModTotems(player);

            if (totemCount > 0) {
                int totalCost = totemCount * SOUL_COST_PER_TOTEM;
                boolean success = consumeSoulEnergy(player, totalCost);

                if (!success) {
                    // 灵魂能量不足，移除所有图腾
                    removeAllModTotems(player);
                }
            }
        }
    }

    /**
     * 统计玩家身上本模组图腾的数量
     */
    private static int countModTotems(Player player) {
        int count = 0;

        for (ItemStack stack : player.getInventory().items) {
            if (isModAbilityTotem(stack)) {
                count++;
            }
        }

        if (isModAbilityTotem(player.getOffhandItem())) {
            count++;
        }

        for (ItemStack stack : player.getInventory().armor) {
            if (isModAbilityTotem(stack)) {
                count++;
            }
        }

        return count;
    }

    /**
     * 判断是否是本模组的能力图腾
     */
    private static boolean isModAbilityTotem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null || !"everlastingabilities".equals(itemId.getNamespace())) {
            return false;
        }
        if (!"ability_totem".equals(itemId.getPath())) {
            return false;
        }

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("Ability")) {
            return false;
        }

        String abilityId = tag.getString("Ability");
        for (String modAbilityId : MOD_ABILITY_IDS) {
            if (modAbilityId.equals(abilityId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 移除玩家身上所有的本模组图腾
     */
    private static void removeAllModTotems(Player player) {
        int removedCount = 0;

        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (isModAbilityTotem(stack)) {
                player.getInventory().items.set(i, ItemStack.EMPTY);
                removedCount++;
            }
        }

        if (isModAbilityTotem(player.getOffhandItem())) {
            player.getInventory().offhand.set(0, ItemStack.EMPTY);
            removedCount++;
        }

        for (int i = 0; i < player.getInventory().armor.size(); i++) {
            ItemStack stack = player.getInventory().armor.get(i);
            if (isModAbilityTotem(stack)) {
                player.getInventory().armor.set(i, ItemStack.EMPTY);
                removedCount++;
            }
        }

        if (removedCount > 0) {
            LOGGER.info("Removed {} totems from {}", removedCount, player.getName().getString());
        }
    }

    /**
     * 消耗灵魂能量 - 使用 Goety 官方 API (SEProvider.CAPABILITY)
     */
    private static boolean consumeSoulEnergy(Player player, int amount) {
        Optional<ISoulEnergy> soulEnergyOpt = player.getCapability(SEProvider.CAPABILITY).resolve();

        if (soulEnergyOpt.isPresent()) {
            ISoulEnergy soulEnergy = soulEnergyOpt.get();
            int current = soulEnergy.getSoulEnergy();

            if (current >= amount) {
                soulEnergy.decreaseSE(amount);
                LOGGER.info("Consumed {} soul energy from {}. New: {}",
                        amount, player.getName().getString(), current - amount);
                return true;
            } else {
                LOGGER.warn("{} has insufficient soul energy. Needs: {}, Has: {}",
                        player.getName().getString(), amount, current);
                return false;
            }
        }

        LOGGER.warn("Failed to get SEProvider.CAPABILITY for {}", player.getName().getString());
        return false;
    }
}