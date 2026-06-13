package com.goety.everlasting;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Random;

@Mod.EventBusSubscriber(modid = GoetyEverlasting.MOD_ID)
public class DropHandler {

    private static final Random RANDOM = new Random();
    private static final double DROP_CHANCE = 0.05; // 5% 掉落概率
    private static final int APOSTLE_DROP_LEVEL = 10; // 使徒掉落的图腾等级

    // 所有能力的ID列表
    private static final String[] ABILITY_IDS = {
            "goety_everlasting:leeching",
            "goety_everlasting:rampage",
            "goety_everlasting:soul_armor",
            "goety_everlasting:explosive",
            "goety_everlasting:buff"
    };

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        LivingEntity killedEntity = event.getEntity();
        Level world = killedEntity.level();

        // 检查是否在服务端
        if (world.isClientSide) {
            return;
        }

        // 检查击杀者是否为玩家
        boolean killedByPlayer = event.getSource().getEntity() instanceof Player;
        if (!killedByPlayer) {
            return;
        }

        // 检查是否为 Apostle（使徒）
        if (isApostle(killedEntity)) {
            dropRandomTotem(killedEntity, APOSTLE_DROP_LEVEL);
            return;
        }

        // 检查是否为村民或灾厄村民（5% 概率掉落）
        boolean isVillagerLike = killedEntity instanceof AbstractVillager || killedEntity instanceof AbstractIllager;
        if (isVillagerLike && RANDOM.nextDouble() < DROP_CHANCE) {
            dropRandomTotem(killedEntity, 1);
        }
    }

    /**
     * 判断是否为使徒
     */
    private static boolean isApostle(LivingEntity entity) {
        String entityTypeName = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        return "goety:apostle".equals(entityTypeName);
    }

    /**
     * 掉落随机图腾
     * @param entity 掉落实体
     * @param level 图腾等级
     */
    private static void dropRandomTotem(LivingEntity entity, int level) {
        // 随机选择一个能力
        String abilityId = ABILITY_IDS[RANDOM.nextInt(ABILITY_IDS.length)];

        // 获取能力图腾物品
        ItemStack totem = new ItemStack(ForgeRegistries.ITEMS.getValue(
                new ResourceLocation("everlastingabilities", "ability_totem")
        ));

        // 设置能力的NBT数据
        CompoundTag tag = new CompoundTag();
        tag.putString("Ability", abilityId);
        tag.putInt("Level", level);
        totem.setTag(tag);

        // 掉落到地上
        entity.spawnAtLocation(totem);
    }
}