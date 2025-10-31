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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Interface for calculating loot group-based fishing probabilities
 * 基于分组的钓鱼概率计算器接口
 */
public interface LootGroupProbabilityCalculator {

    /**
     * 计算指定战利品在所有分组中的概率
     * Calculates the probability of catching a specific loot item in all loot groups
     *
     * @param lootId the identifier of the loot item
     * @return a map of group IDs to their respective group probability information
     */
    @NotNull
    Map<String, GroupProbabilityInfo> calculateGroupProbabilities(@NotNull String lootId);

    /**
     * 获取指定战利品所属的所有分组
     * Gets all loot groups that contain the specified loot item
     *
     * @param lootId the identifier of the loot item
     * @return a list of group IDs
     */
    @NotNull
    List<String> getLootGroups(@NotNull String lootId);

    /**
     * 获取指定分组中指定战利品的概率
     * Gets the probability of a specific loot in a specific group
     *
     * @param lootId the identifier of the loot item
     * @param groupId the group identifier
     * @return the probability info, or null if the loot is not in this group
     */
    @Nullable
    GroupProbabilityInfo getGroupProbability(@NotNull String lootId, @NotNull String groupId);

    /**
     * 清除概率缓存（用于重载配置后）
     * Clears the probability cache (use after config reload)
     */
    void clearCache();

    /**
     * Data class containing detailed probability information for a loot group
     * 分组概率信息数据类
     */
    class GroupProbabilityInfo {
        private final String groupId;
        private final double probability;
        private final String conditionsDescription;

        public GroupProbabilityInfo(String groupId, double probability, String conditionsDescription) {
            this.groupId = groupId;
            this.probability = probability;
            this.conditionsDescription = conditionsDescription;
        }

        /**
         * Gets the group ID
         * 获取分组ID
         *
         * @return the group identifier
         */
        @NotNull
        public String getGroupId() {
            return groupId;
        }

        /**
         * Gets the probability as a percentage
         * 获取概率百分比
         *
         * @return the probability percentage
         */
        public double getProbability() {
            return probability;
        }

        /**
         * Gets the conditions description for this group
         * 获取该分组的条件描述
         *
         * @return the conditions description, or null if no specific conditions
         */
        @Nullable
        public String getConditionsDescription() {
            return conditionsDescription;
        }

        /**
         * Gets the probability as a formatted percentage string
         * 获取格式化的概率字符串
         *
         * @return formatted probability string (e.g., "15.50%")
         */
        @NotNull
        public String getFormattedProbability() {
            return String.format("%.2f%%", probability);
        }
    }
}

