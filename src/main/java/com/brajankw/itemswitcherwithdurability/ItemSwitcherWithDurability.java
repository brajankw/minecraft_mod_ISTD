package com.brajankw.itemswitcherwithdurability;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.List;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ItemSwitcherWithDurability.MODID)
public class ItemSwitcherWithDurability
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "itemswitcherwithdurability";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public ItemSwitcherWithDurability()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }


    }

    @SubscribeEvent
    public void onItemBreak(PlayerDestroyItemEvent itemEvent) {
        Item brokenItem = itemEvent.getOriginal().getItem();
        Inventory playerInventory = itemEvent.getEntity().getInventory();
        List<ItemStack> sameTools = playerInventory.items.stream().filter((itemStack) -> {
            return itemStack.getItem().getClass() == brokenItem.getClass();
        }).toList();
        if (!sameTools.isEmpty()) {
            ItemStack newTool = playerInventory.items.stream().filter((itemS) -> itemS != brokenItem.getDefaultInstance() && String.valueOf(itemS.getItem()).equals(String.valueOf(brokenItem))).findFirst().orElse(null);
            if (newTool == null) {
                newTool = sameTools.get(0);
            }
            playerInventory.removeItem(newTool);
            playerInventory.setItem(playerInventory.selected, newTool);
        }
    }

    @SubscribeEvent
    public void onStackEnd(PlayerInteractEvent.RightClickBlock event) {
        ItemStack itemStack = event.getItemStack();
        Inventory playerInventory = event.getEntity().getInventory();
        if (itemStack.getCount() == 1 && itemStack.getItem().getClass() == BlockItem.class) {
            ItemStack newStack = playerInventory.items.stream().filter((itemS) -> itemS != itemStack && String.valueOf(itemS.getItem()).equals(String.valueOf(itemStack.getItem()))).findFirst().orElse(null);
            if (newStack == null) {
                return;
            }
            playerInventory.removeItem(newStack);
            newStack.setCount(newStack.getCount() + 1);
            playerInventory.setItem(playerInventory.selected, newStack);
        }
    }

    @SubscribeEvent
    public void onTooltipEvent(ItemTooltipEvent event) {
        ItemStack itemStack = event.getItemStack();
        Item item = itemStack.getItem();
        if (!(item instanceof TieredItem)) {
            return;
        }
        List<Component> components = event.getToolTip();
        components.add(1, Component.literal("Durability: " + (itemStack.getMaxDamage() - itemStack.getDamageValue()) + "/" + itemStack.getMaxDamage()));
    }
}
