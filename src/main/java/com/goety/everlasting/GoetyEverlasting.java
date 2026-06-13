package com.goety.everlasting;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(GoetyEverlasting.MOD_ID)
public class GoetyEverlasting {
    public static final String MOD_ID = "goety_everlasting";

    public GoetyEverlasting() {
        // 注册掉落处理器
        MinecraftForge.EVENT_BUS.register(new DropHandler());
    }
}