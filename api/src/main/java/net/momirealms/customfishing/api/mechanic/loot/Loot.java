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

package net.momirealms.customfishing.api.mechanic.loot;

import net.momirealms.customfishing.api.mechanic.effect.LootBaseEffect;
import net.momirealms.customfishing.api.mechanic.misc.value.MathValue;
import net.momirealms.customfishing.api.mechanic.misc.value.TextValue;
import net.momirealms.customfishing.api.mechanic.statistic.StatisticsKeys;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public interface Loot {

    class DefaultProperties {
        public static boolean DEFAULT_INSTANT_GAME = false;
        public static boolean DEFAULT_DISABLE_GAME = false;
        public static boolean DEFAULT_DISABLE_STATS = false;
        public static boolean DEFAULT_SHOW_IN_FINDER = true;
    }

    LootType DEFAULT_TYPE = LootType.ITEM;
    MathValue<Player> DEFAULT_SCORE = MathValue.plain(0);

    /**
     * Check if this loot triggers an instant game.
     *
     * @return True if it triggers an instant game, false otherwise.
     */
    boolean instantGame();

    /**
     * Check if games are disabled for this loot.
     *
     * @return True if games are disabled, false otherwise.
     */
    boolean disableGame();

    /**
     * Check if statistics recording is disabled for this loot.
     *
     * @return True if statistics are disabled, false otherwise.
     */
    boolean disableStats();

    /**
     * Check if this loot should be displayed in the finder tool.
     *
     * @return True if it should be shown in the finder, false otherwise.
     */
    boolean showInFinder();

    /**
     * Check if players can't grab the loot
     *
     * @return True if players can't grab the loot, false otherwise.
     */
    boolean preventGrabbing();

    /**
     * If the loot item should go directly into inventory
     *
     * @return True if loot go directly into inventory
     */
    MathValue<Player> toInventory();

    /**
     * Get the unique identifier for this loot.
     *
     * @return The unique ID of the loot.
     */
    String id();

    /**
     * Get the type of this loot.
     *
     * @return The type of the loot.
     */
    LootType type();

    /**
     * Get the display nickname for this loot.
     *
     * @return The nickname of the loot.
     */
    @NotNull
    String nick();

    /**
     * Get the display lore for this loot.
     *
     * @return The lore of the loot, or empty list if not set.
     */
    @NotNull
    List<String> lore();

    /**
     * Get the statistics key associated with this loot.
     *
     * @return The statistics key for this loot.
     */
    StatisticsKeys statisticKey();

    /**
     * Get the score value for this loot.
     *
     * @return The score associated with the loot.
     */
    MathValue<Player> score();

    /**
     * Get the groups this loot belongs to.
     *
     * @return An array of group names.
     */
    String[] lootGroup();

    /**
     * Get the base effect associated with this loot.
     *
     * @return The base effect for the loot.
     */
    LootBaseEffect baseEffect();

    /**
     * Get the custom data
     *
     * @return custom data
     */
    Map<String, TextValue<Player>> customData();

    /**
     * Create a new builder for constructing a Loot instance.
     *
     * @return A new Loot builder.
     */
    static Builder builder() {
        return new LootImpl.BuilderImpl();
    }

    /**
     * Builder interface for constructing instances of Loot.
     */
    interface Builder {

        /**
         * Set the type of the loot.
         *
         * @param type The type of the loot.
         * @return The builder instance.
         */
        Builder type(LootType type);

        /**
         * Specify whether the loot triggers an instant game.
         *
         * @param instantGame True if it should trigger an instant game.
         * @return The builder instance.
         */
        Builder instantGame(boolean instantGame);

        /**
         * Specify whether games are disabled for this loot.
         *
         * @param disableGame True if games should be disabled.
         * @return The builder instance.
         */
        Builder disableGame(boolean disableGame);

        /**
         * Specify whether players are prevented from grabbing the loot
         *
         * @param preventGrabbing True if grabbing should be prevented.
         * @return The builder instance.
         */
        Builder preventGrabbing(boolean preventGrabbing);

        /**
         * Specify whether statistics recording is disabled for this loot.
         *
         * @param disableStatistics True if statistics should be disabled.
         * @return The builder instance.
         */
        Builder disableStatistics(boolean disableStatistics);

        /**
         * Specify whether the loot should be shown in the finder tool.
         *
         * @param showInFinder True if it should be shown in the finder.
         * @return The builder instance.
         */
        Builder showInFinder(boolean showInFinder);

        /**
         * Set the unique ID for the loot.
         *
         * @param id The unique identifier.
         * @return The builder instance.
         */
        Builder id(String id);

        /**
         * Set the nickname for the loot.
         *
         * @param nick The nickname.
         * @return The builder instance.
         */
        Builder nick(String nick);

        /**
         * Set the lore for the loot.
         *
         * @param lore The lore.
         * @return The builder instance.
         */
        Builder lore(List<String> lore);

        /**
         * Set the statistics key for the loot.
         *
         * @param statisticsKeys The statistics key.
         * @return The builder instance.
         */
        Builder statisticsKeys(StatisticsKeys statisticsKeys);

        /**
         * Set the score for the loot.
         *
         * @param score The score value.
         * @return The builder instance.
         */
        Builder score(MathValue<Player> score);

        /**
         * Set the groups that the loot belongs to.
         *
         * @param groups An array of group names.
         * @return The builder instance.
         */
        Builder groups(String[] groups);

        /**
         * Set the base effect for the loot.
         *
         * @param lootBaseEffect The base effect.
         * @return The builder instance.
         */
        Builder lootBaseEffect(LootBaseEffect lootBaseEffect);

        /**
         * Set the custom data
         *
         * @param customData the custom data
         * @return The builder instance.
         */
        Builder customData(Map<String, TextValue<Player>> customData);

        /**
         * Set if the loot go directly into inventory
         *
         * @param toInventory go directly into the inventory
         * @return The builder instance.
         */
        Builder toInventory(MathValue<Player> toInventory);

        /**
         * Build and return the Loot instance.
         *
         * @return The constructed Loot instance.
         */
        Loot build();
    }
}
