/*
 *  Copyright (C) <2024> <XiaoMoMi>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.momirealms.customfishing.bukkit.integration;

import net.momirealms.customfishing.api.BukkitCustomFishingPlugin;
import net.momirealms.customfishing.api.integration.*;
import net.momirealms.customfishing.bukkit.block.BukkitBlockManager;
import net.momirealms.customfishing.bukkit.entity.BukkitEntityManager;
import net.momirealms.customfishing.bukkit.integration.block.ItemsAdderBlockProvider;
import net.momirealms.customfishing.bukkit.integration.block.OraxenBlockProvider;
import net.momirealms.customfishing.bukkit.integration.enchant.AdvancedEnchantmentsProvider;
import net.momirealms.customfishing.bukkit.integration.enchant.VanillaEnchantmentsProvider;
import net.momirealms.customfishing.bukkit.integration.entity.ItemsAdderEntityProvider;
import net.momirealms.customfishing.bukkit.integration.entity.MythicEntityProvider;
import net.momirealms.customfishing.bukkit.integration.item.*;
import net.momirealms.customfishing.bukkit.integration.level.*;
import net.momirealms.customfishing.bukkit.integration.papi.CompetitionPapi;
import net.momirealms.customfishing.bukkit.integration.papi.CustomFishingPapi;
import net.momirealms.customfishing.bukkit.integration.papi.StatisticsPapi;
import net.momirealms.customfishing.bukkit.integration.quest.BattlePassQuest;
import net.momirealms.customfishing.bukkit.integration.quest.BeautyFishingQuest;
import net.momirealms.customfishing.bukkit.integration.quest.BetonQuestQuest;
import net.momirealms.customfishing.bukkit.integration.quest.ClueScrollsQuest;
import net.momirealms.customfishing.bukkit.integration.region.WorldGuardRegion;
import net.momirealms.customfishing.bukkit.integration.season.AdvancedSeasonsProvider;
import net.momirealms.customfishing.bukkit.integration.season.CustomCropsSeasonProvider;
import net.momirealms.customfishing.bukkit.integration.season.RealisticSeasonsProvider;
import net.momirealms.customfishing.bukkit.integration.shop.ShopGUIHook;
import net.momirealms.customfishing.bukkit.item.BukkitItemManager;
import net.momirealms.customfishing.bukkit.item.SNBTItemProvider;
import net.momirealms.customfishing.common.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BukkitIntegrationManager implements IntegrationManager {
    private static BukkitIntegrationManager instance;
    private final BukkitCustomFishingPlugin plugin;
    private final HashMap<String, LevelerProvider> levelerProviders = new HashMap<>();
    private final HashMap<String, EnchantmentProvider> enchantmentProviders = new HashMap<>();
    private SeasonProvider seasonProvider;
    private boolean hasFloodGate;
    private boolean hasGeyser;

    public BukkitIntegrationManager(BukkitCustomFishingPlugin plugin) {
        this.plugin = plugin;
        try {
            this.load();
        } catch (Throwable e) {
            plugin.getPluginLogger().warn("Failed to load integrations", e);
        } finally {
            instance = this;
        }
    }

    public static BukkitIntegrationManager instance() {
        return instance;
    }

    public boolean hasFloodGate() {
        return hasFloodGate;
    }

    public boolean hasGeyser() {
        return hasGeyser;
    }

    @Override
    public void disable() {
        this.enchantmentProviders.clear();
        this.levelerProviders.clear();
    }

    @Override
    public void load() {
        registerEnchantmentProvider(new VanillaEnchantmentsProvider());
        registerItemProvider(new SNBTItemProvider());
        if (isHooked("ItemsAdder")) {
            registerItemProvider(new ItemsAdderItemProvider());
            registerBlockProvider(new ItemsAdderBlockProvider());
            registerEntityProvider(new ItemsAdderEntityProvider());
        }
        if (isHooked("CraftEngine")) {
            try {
                Class<?> ceItemProviderClass = Class.forName("net.momirealms.customfishing.bukkit.integration.item.CraftEngineItemProvider");
                Constructor<?> itemProviderConstructor = ceItemProviderClass.getDeclaredConstructor();
                itemProviderConstructor.setAccessible(true);
                ItemProvider itemProvider = (ItemProvider) itemProviderConstructor.newInstance();
                registerItemProvider(itemProvider);
            } catch (ReflectiveOperationException e) {
                plugin.getPluginLogger().warn("Failed to hook CraftEngine", e);
            }
        }
        if (isHooked("Nexo")) {
            try {
                Class<?> nexoItemProviderClass = Class.forName("net.momirealms.customfishing.bukkit.integration.item.NexoItemProvider");
                Constructor<?> itemProviderConstructor = nexoItemProviderClass.getDeclaredConstructor();
                itemProviderConstructor.setAccessible(true);
                ItemProvider itemProvider = (ItemProvider) itemProviderConstructor.newInstance();
                registerItemProvider(itemProvider);
                Class<?> nexoBlockProviderClass = Class.forName("net.momirealms.customfishing.bukkit.integration.block.NexoBlockProvider");
                Constructor<?> nexoBlockProviderConstructor = nexoBlockProviderClass.getDeclaredConstructor();
                nexoBlockProviderConstructor.setAccessible(true);
                BlockProvider blockProvider = (BlockProvider) nexoBlockProviderConstructor.newInstance();
                registerBlockProvider(blockProvider);
            } catch (ReflectiveOperationException e) {
                plugin.getPluginLogger().warn("Failed to hook Nexo", e);
            }
        }
        if (isHooked("MMOItems")) {
            registerItemProvider(new MMOItemsItemProvider());
        }
        if (isHooked("EcoItems")) {
            registerItemProvider(new EcoItemsProvider());
        }
        if (isHooked("Oraxen", "1")) {
            registerItemProvider(new OraxenItemProvider());
            registerBlockProvider(new OraxenBlockProvider());
        }
        if (isHooked("Zaphkiel")) {
            registerItemProvider(new ZaphkielItemProvider());
        }
        if (isHooked("NeigeItems")) {
            registerItemProvider(new NeigeItemsItemProvider());
        }
        if (isHooked("ExecutableItems")) {
            registerItemProvider(new ExecutableItemProvider());
        }
        if (isHooked("MythicMobs", "5")) {
            registerItemProvider(new MythicMobsItemProvider());
            registerEntityProvider(new MythicEntityProvider());
        }
        if (isHooked("EcoJobs")) {
            registerLevelerProvider(new EcoJobsLevelerProvider());
        }
        if (isHooked("EcoSkills")) {
            registerLevelerProvider(new EcoSkillsLevelerProvider());
        }
        if (isHooked("Jobs")) {
            registerLevelerProvider(new JobsRebornLevelerProvider());
        }
        if (isHooked("MMOCore")) {
            registerLevelerProvider(new MMOCoreLevelerProvider());
        }
        if (isHooked("mcMMO")) {
            try {
                registerItemProvider(new McMMOTreasureProvider());
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                plugin.getPluginLogger().warn("Failed to initialize mcMMO Treasure");
            }
            registerLevelerProvider(new McMMOLevelerProvider());
        }
        if (isHooked("AureliumSkills")) {
            registerLevelerProvider(new AureliumSkillsProvider());
        }
        if (isHooked("AuraSkills")) {
            registerLevelerProvider(new AuraSkillsLevelerProvider());
            registerItemProvider(new AuraSkillItemProvider());
        }
        if (isHooked("AdvancedEnchantments")) {
            registerEnchantmentProvider(new AdvancedEnchantmentsProvider());
        }
        if (isHooked("RealisticSeasons")) {
            registerSeasonProvider(new RealisticSeasonsProvider());
        } else if (isHooked("AdvancedSeasons")) {
            registerSeasonProvider(new AdvancedSeasonsProvider());
        } else if (isHooked("CustomCrops")) {
            registerSeasonProvider(new CustomCropsSeasonProvider());
        }
        if (isHooked("Vault")) {
            VaultHook.init();
        }
        if (isHooked("BattlePass")){
            BattlePassQuest battlePassQuest = new BattlePassQuest();
            battlePassQuest.register();
        }
        if (isHooked("ClueScrolls")) {
            ClueScrollsQuest clueScrollsQuest = new ClueScrollsQuest();
            clueScrollsQuest.register();
        }
        if (isHooked("BetonQuest", "2")) {
            BetonQuestQuest.register();
        }
        if (isHooked("WorldGuard", "7")) {
            WorldGuardRegion.register();
        }
        if (isHooked("PlaceholderAPI")) {
            new CustomFishingPapi(plugin).load();
            new CompetitionPapi(plugin).load();
            new StatisticsPapi(plugin).load();
        }
        if (isHooked("ShopGUIPlus")) {
            ShopGUIHook.register();
        }
        if (isHooked("BeautyQuests")) {
            BeautyFishingQuest.register();
        }
        if (Bukkit.getPluginManager().getPlugin("Geyser-Spigot") != null) {
            this.hasGeyser = true;
        }
        if (Bukkit.getPluginManager().getPlugin("floodgate") != null) {
            this.hasFloodGate = true;
        }
    }

    private boolean isHooked(String hooked) {
        if (Bukkit.getPluginManager().getPlugin(hooked) != null) {
            plugin.getPluginLogger().info(hooked + " hooked!");
            return true;
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private boolean isHooked(String hooked, String... versionPrefix) {
        Plugin p = Bukkit.getPluginManager().getPlugin(hooked);
        if (p != null) {
            String ver = p.getDescription().getVersion();
            for (String prefix : versionPrefix) {
                if (ver.startsWith(prefix)) {
                    plugin.getPluginLogger().info(hooked + " hooked!");
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean registerLevelerProvider(@NotNull LevelerProvider leveler) {
        if (levelerProviders.containsKey(leveler.identifier())) return false;
        levelerProviders.put(leveler.identifier(), leveler);
        return true;
    }

    @Override
    public boolean unregisterLevelerProvider(@NotNull String id) {
        return levelerProviders.remove(id) != null;
    }

    @Override
    public boolean registerEnchantmentProvider(@NotNull EnchantmentProvider enchantment) {
        if (enchantmentProviders.containsKey(enchantment.identifier())) return false;
        enchantmentProviders.put(enchantment.identifier(), enchantment);
        return true;
    }

    @Override
    public boolean unregisterEnchantmentProvider(@NotNull String id) {
        return enchantmentProviders.remove(id) != null;
    }

    @Override
    @Nullable
    public LevelerProvider getLevelerProvider(String plugin) {
        return levelerProviders.get(plugin);
    }

    @Override
    @Nullable
    public EnchantmentProvider getEnchantmentProvider(String id) {
        return enchantmentProviders.get(id);
    }

    @Override
    public List<Pair<String, Short>> getEnchantments(ItemStack itemStack) {
        ArrayList<Pair<String, Short>> list = new ArrayList<>();
        for (EnchantmentProvider enchantmentProvider : enchantmentProviders.values()) {
            list.addAll(enchantmentProvider.getEnchants(itemStack));
        }
        return list;
    }

    @Nullable
    @Override
    public SeasonProvider getSeasonProvider() {
        return seasonProvider;
    }

    @Override
    public boolean registerSeasonProvider(@NotNull SeasonProvider season) {
        if (this.seasonProvider != null) return false;
        this.seasonProvider = season;
        return true;
    }

    @Override
    public boolean unregisterSeasonProvider() {
        if (this.seasonProvider == null) return false;
        this.seasonProvider = null;
        return true;
    }

    @Override
    public boolean registerEntityProvider(@NotNull EntityProvider entity) {
        return ((BukkitEntityManager) plugin.getEntityManager()).registerEntityProvider(entity);
    }

    @Override
    public boolean unregisterEntityProvider(@NotNull String id) {
        return ((BukkitEntityManager) plugin.getEntityManager()).unregisterEntityProvider(id);
    }

    @Override
    public boolean registerItemProvider(@NotNull ItemProvider item) {
        return ((BukkitItemManager) plugin.getItemManager()).registerItemProvider(item);
    }

    @Override
    public boolean unregisterItemProvider(@NotNull String id) {
        return ((BukkitItemManager) plugin.getItemManager()).unregisterItemProvider(id);
    }

    @Override
    public boolean registerBlockProvider(@NotNull BlockProvider block) {
        return ((BukkitBlockManager) plugin.getBlockManager()).registerBlockProvider(block);
    }

    @Override
    public boolean unregisterBlockProvider(@NotNull String id) {
        return ((BukkitBlockManager) plugin.getBlockManager()).unregisterBlockProvider(id);
    }
}
