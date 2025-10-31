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

package net.momirealms.customfishing.bukkit.loot;

import net.momirealms.customfishing.api.BukkitCustomFishingPlugin;
import net.momirealms.customfishing.api.mechanic.loot.Loot;
import net.momirealms.customfishing.api.mechanic.loot.LootGroupProbabilityCalculator;
import net.momirealms.customfishing.api.mechanic.loot.LootType;
import net.momirealms.customfishing.api.mechanic.loot.operation.WeightOperation;
import net.momirealms.customfishing.api.mechanic.requirement.ConditionalElement;
import net.momirealms.customfishing.api.mechanic.requirement.Requirement;
import net.momirealms.customfishing.common.helper.ExpressionHelper;
import net.momirealms.customfishing.common.util.Pair;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于分组的概率计算器实现
 */
public class BukkitLootGroupProbabilityCalculator implements LootGroupProbabilityCalculator {

    private final BukkitCustomFishingPlugin plugin;
    private final Map<String, Map<String, GroupProbabilityInfo>> probabilityCache = new ConcurrentHashMap<>();

    // 反射访问的字段
    private Field lootConditionsField;
    private Field groupMembersMapField;

    public BukkitLootGroupProbabilityCalculator(BukkitCustomFishingPlugin plugin) {
        this.plugin = plugin;
        initReflection();
    }

    /**
     * 初始化反射字段访问
     */
    private void initReflection() {
        try {
            Class<?> lootManagerClass = BukkitLootManager.class;
            lootConditionsField = lootManagerClass.getDeclaredField("lootConditions");
            lootConditionsField.setAccessible(true);

            groupMembersMapField = lootManagerClass.getDeclaredField("groupMembersMap");
            groupMembersMapField.setAccessible(true);

            plugin.debug("LootGroupProbabilityCalculator 反射初始化成功");
        } catch (Exception e) {
            plugin.debug("LootGroupProbabilityCalculator 反射初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 存储每个分组的总权重（用于解析 {{group_xxx}} 占位符）
    private final Map<String, Double> groupTotalWeights = new ConcurrentHashMap<>();
    
    // 存储所有实际分组的 ID（在配置文件中有独立层级的分组）
    private final Set<String> actualGroupIds = ConcurrentHashMap.newKeySet();

    @Override
    @NotNull
    public Map<String, GroupProbabilityInfo> calculateGroupProbabilities(@NotNull String lootId) {
        // 检查缓存
        Map<String, GroupProbabilityInfo> cached = probabilityCache.get(lootId);
        if (cached != null) {
            return new HashMap<>(cached);
        }

        Map<String, GroupProbabilityInfo> result = new HashMap<>();

        try {
            // 获取loot信息
            Optional<Loot> lootOpt = plugin.getLootManager().getLoot(lootId);
            if (lootOpt.isEmpty()) {
                return result;
            }

            Loot loot = lootOpt.get();
            if (loot.type() != LootType.ITEM) {
                return result;
            }

            // 获取lootConditions
            @SuppressWarnings("unchecked")
            LinkedHashMap<String, ConditionalElement<List<Pair<String, WeightOperation>>, Player>> lootConditions =
                    (LinkedHashMap<String, ConditionalElement<List<Pair<String, WeightOperation>>, Player>>) 
                    lootConditionsField.get(plugin.getLootManager());
            
            if (lootConditions == null || lootConditions.isEmpty()) {
                return result;
            }

            // 第一遍：收集所有实际分组 ID 和预计算总权重
            synchronized (groupTotalWeights) {
                if (groupTotalWeights.isEmpty()) {
                    plugin.debug("[LootGroup] 开始预计算所有分组的总权重");
                    collectActualGroupIds(lootConditions);
                    calculateAllGroupTotalWeights(lootConditions);
                    plugin.debug("[LootGroup] 预计算完成，共计算了 " + groupTotalWeights.size() + " 个分组");
                    plugin.debug("[LootGroup] 实际分组数量: " + actualGroupIds.size() + " (" + String.join(", ", actualGroupIds) + ")");
                }
            }

            // 第二遍：计算当前 loot 在各分组中的概率
            for (Map.Entry<String, ConditionalElement<List<Pair<String, WeightOperation>>, Player>> entry : lootConditions.entrySet()) {
                String groupId = entry.getKey();
                ConditionalElement<List<Pair<String, WeightOperation>>, Player> conditionalElement = entry.getValue();

                // 递归计算该分组及其子分组的概率
                calculateGroupProbabilitiesRecursive(lootId, groupId, conditionalElement, result, "");
            }
            
            // 过滤结果：只保留实际分组的概率，移除辅助性标签（no_star, silver_star 等）
            result.entrySet().removeIf(entry -> !actualGroupIds.contains(entry.getKey()));

            // 缓存结果
            if (!result.isEmpty()) {
                probabilityCache.put(lootId, result);
            }

        } catch (Exception e) {
            plugin.debug("[LootGroup] 计算分组概率时出错: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }
    
    /**
     * 收集所有实际分组的 ID（在配置文件中有独立层级的分组）
     */
    private void collectActualGroupIds(Map<String, ConditionalElement<List<Pair<String, WeightOperation>>, Player>> lootConditions) {
        for (Map.Entry<String, ConditionalElement<List<Pair<String, WeightOperation>>, Player>> entry : lootConditions.entrySet()) {
            String groupId = entry.getKey();
            actualGroupIds.add(groupId);
            
            // 递归收集子分组
            ConditionalElement<List<Pair<String, WeightOperation>>, Player> conditionalElement = entry.getValue();
            Map<String, ConditionalElement<List<Pair<String, WeightOperation>>, Player>> subGroups = conditionalElement.getSubElements();
            if (subGroups != null && !subGroups.isEmpty()) {
                collectActualGroupIds(subGroups);
            }
        }
    }
    
    /**
     * 预计算所有分组的总权重（用于解析 {{group_xxx}} 占位符）
     * 直接从已展开的战利品数据中统计组合分组的总权重
     */
    private void calculateAllGroupTotalWeights(Map<String, ConditionalElement<List<Pair<String, WeightOperation>>, Player>> lootConditions) {
        for (Map.Entry<String, ConditionalElement<List<Pair<String, WeightOperation>>, Player>> entry : lootConditions.entrySet()) {
            String groupId = entry.getKey();
            ConditionalElement<List<Pair<String, WeightOperation>>, Player> conditionalElement = entry.getValue();
            
            // 用于统计各种组合分组的总权重
            Map<String, Double> combinedGroupWeights = new HashMap<>();
            
            List<Pair<String, WeightOperation>> operations = conditionalElement.getElement();
            
            if (operations != null) {
                // 遍历所有操作，统计每个战利品的权重
                for (Pair<String, WeightOperation> pair : operations) {
                    String lootId = pair.left();
                    double weight = extractBaseWeightOnly(pair.right());
                    
                    // 只处理固定权重（跳过表达式）
                    if (weight > 0) {
                        // 获取该战利品所属的所有分组
                        Optional<Loot> lootOpt = plugin.getLootManager().getLoot(lootId);
                        if (lootOpt.isPresent() && lootOpt.get().type() == LootType.ITEM) {
                            List<String> lootGroups = Arrays.asList(lootOpt.get().lootGroup());
                            
                            // 为所有可能的组合分组累加权重
                            // 例如：如果鱼属于 [ocean_fish, no_star]，则累加到 no_star&ocean_fish（按字母顺序）
                            if (lootGroups.size() >= 2) {
                                // 生成所有可能的组合（2个分组的组合）
                                for (int i = 0; i < lootGroups.size(); i++) {
                                    for (int j = i + 1; j < lootGroups.size(); j++) {
                                        // 按字母顺序排序，确保键的一致性
                                        String group1 = lootGroups.get(i);
                                        String group2 = lootGroups.get(j);
                                        String combo = group1.compareTo(group2) < 0 ? 
                                            group1 + "&" + group2 : group2 + "&" + group1;
                                        combinedGroupWeights.put(combo, 
                                            combinedGroupWeights.getOrDefault(combo, 0.0) + weight);
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 存储所有计算出的组合分组权重
                for (Map.Entry<String, Double> cgEntry : combinedGroupWeights.entrySet()) {
                    String combo = cgEntry.getKey();
                    double totalWeight = cgEntry.getValue();
                    if (totalWeight > 0) {
                        groupTotalWeights.put(combo, totalWeight);
                        plugin.debug("[LootGroup] 预计算组合分组 " + combo + " 的总权重: " + totalWeight);
                    }
                }
            }
            
            // 递归处理子分组
            Map<String, ConditionalElement<List<Pair<String, WeightOperation>>, Player>> subGroups = conditionalElement.getSubElements();
            if (subGroups != null && !subGroups.isEmpty()) {
                calculateAllGroupTotalWeights(subGroups);
            }
        }
    }
    
    /**
     * 只提取基础权重（PlainMathValueImpl），不处理动态占位符
     */
    private double extractBaseWeightOnly(WeightOperation operation) {
        try {
            Field[] fields = operation.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(operation);
                
                if (field.getName().equals("arg") && value != null) {
                    // 只处理 PlainMathValueImpl
                    if (value.getClass().getSimpleName().equals("PlainMathValueImpl")) {
                        Field valueField = value.getClass().getDeclaredField("value");
                        valueField.setAccessible(true);
                        return valueField.getDouble(value);
                    }
                    // 跳过 ExpressionMathValueImpl（可能包含占位符）
                    return 0.0;
                }
                
                if (field.getType() == double.class || field.getType() == Double.class) {
                    if (value != null) {
                        return ((Number) value).doubleValue();
                    }
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return 0.0;
    }

    /**
     * 递归计算分组概率
     */
    private void calculateGroupProbabilitiesRecursive(
            String lootId,
            String groupId,
            ConditionalElement<List<Pair<String, WeightOperation>>, Player> conditionalElement,
            Map<String, GroupProbabilityInfo> result,
            String parentConditions) {

        try {
            // 获取当前分组的条件描述
            String conditions = getConditionsDescription(conditionalElement);
            String fullConditions = parentConditions.isEmpty() ? conditions :
                    (conditions.isEmpty() ? parentConditions : parentConditions + ", " + conditions);

            // 计算当前分组的权重
            Map<String, Double> weights = new HashMap<>();
            List<Pair<String, WeightOperation>> operations = conditionalElement.getElement();

            if (operations != null && !operations.isEmpty()) {
                for (Pair<String, WeightOperation> pair : operations) {
                    String targetId = pair.left();
                    double weight = extractBaseWeight(pair.right());

                    // 处理group_for_each操作
                    if (targetId.startsWith("group_for_each:") || targetId.contains("&")) {
                        applyGroupOperation(targetId.replace("group_for_each:", ""), weight, weights);
                    } else {
                        weights.put(targetId, weights.getOrDefault(targetId, 0.0) + weight);
                    }
                }
            }

            // 如果当前分组包含目标loot，计算概率
            if (weights.containsKey(lootId) && weights.get(lootId) > 0) {
                double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();
                double probability = totalWeight > 0 ? (weights.get(lootId) / totalWeight * 100.0) : 0.0;

                result.put(groupId, new GroupProbabilityInfo(groupId, probability, fullConditions));
                plugin.debug("[LootGroup] ✓ 分组 " + groupId + " 中的 " + lootId + " 概率: " + String.format("%.2f%%", probability));
            }

            // 递归处理子分组
            Map<String, ConditionalElement<List<Pair<String, WeightOperation>>, Player>> subGroups = conditionalElement.getSubElements();
            if (subGroups != null && !subGroups.isEmpty()) {
                for (Map.Entry<String, ConditionalElement<List<Pair<String, WeightOperation>>, Player>> subEntry : subGroups.entrySet()) {
                    calculateGroupProbabilitiesRecursive(lootId, subEntry.getKey(), subEntry.getValue(), result, fullConditions);
                }
            }

        } catch (Exception e) {
            plugin.getPluginLogger().warn("处理分组 " + groupId + " 时出错: " + e.getMessage());
        }
    }

    /**
     * 提取基础权重值 - 直接从源代码层面提取，无需创建 Context
     */
    private double extractBaseWeight(WeightOperation operation) {
        try {
            // 通过反射获取权重值
            Field[] fields = operation.getClass().getDeclaredFields();
            
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(operation);
                
                // 如果是 MathValue 类型，尝试提取数值
                if (field.getName().equals("arg") && value != null) {
                    // 1. 从 PlainMathValueImpl 提取固定值
                    if (value.getClass().getSimpleName().equals("PlainMathValueImpl")) {
                        Field valueField = value.getClass().getDeclaredField("value");
                        valueField.setAccessible(true);
                        double result = valueField.getDouble(value);
                        return result;
                    }
                    
                    // 2. 从 ExpressionMathValueImpl 提取表达式并直接计算
                    if (value.getClass().getSimpleName().equals("ExpressionMathValueImpl")) {
                        try {
                            // 获取 raw 字段 (TextValue)
                            Field rawField = value.getClass().getDeclaredField("raw");
                            rawField.setAccessible(true);
                            Object textValue = rawField.get(value);
                            
                            if (textValue != null) {
                                String textValueType = textValue.getClass().getSimpleName();
                                
                                // 两种 TextValue 都有 raw 字段
                                if (textValueType.equals("PlainTextValueImpl") || textValueType.equals("PlaceholderTextValueImpl")) {
                                    Field rawStringField = textValue.getClass().getDeclaredField("raw");
                                    rawStringField.setAccessible(true);
                                    String expression = (String) rawStringField.get(textValue);
                                    
                                    // 如果是 PlaceholderTextValueImpl，处理内部占位符
                                    if (textValueType.equals("PlaceholderTextValueImpl")) {
                                        // 替换 {{group_xxx}} 占位符为实际权重
                                        if (expression.contains("{{group_")) {
                                            String resolvedExpression = resolveGroupPlaceholders(expression);
                                            if (resolvedExpression != null && !resolvedExpression.contains("{{")) {
                                                try {
                                                    return ExpressionHelper.evaluate(resolvedExpression);
                                                } catch (Exception e) {
                                                    plugin.getPluginLogger().warn("计算解析后的表达式失败: " + resolvedExpression + " - " + e.getMessage());
                                                }
                                            } else {
                                                plugin.getPluginLogger().warn("无法解析表达式中的分组占位符: " + expression);
                                            }
                                            return 1.0;
                                        }
                                        // 检查是否包含玩家占位符
                                        if (expression.contains("%")) {
                                            return 1.0;
                                        }
                                    }
                                    
                                    // 直接使用 ExpressionHelper 计算表达式（不需要 Context）
                                    double result = ExpressionHelper.evaluate(expression);
                                    return result;
                                }
                            }
                        } catch (Exception e) {
                            plugin.getPluginLogger().warn("计算表达式失败: " + e.getMessage());
                        }
                        // 如果无法计算，使用默认权重
                        return 1.0;
                    }
                    
                    // 3. 其他 MathValue 类型
                    return 1.0;
                }
                
                // 兼容直接的 double 字段
                if (field.getType() == double.class || field.getType() == Double.class) {
                    if (value != null) {
                        return ((Number) value).doubleValue();
                    }
                }
            }
        } catch (Exception e) {
            plugin.getPluginLogger().warn("提取权重失败: " + e.getMessage());
        }
        return 0.0;
    }

    /**
     * 解析 {{group_xxx}} 占位符，替换为实际的分组总权重
     */
    private String resolveGroupPlaceholders(String expression) {
        String result = expression;
        
        // 查找所有 {{group_xxx}} 占位符
        int start = 0;
        while ((start = result.indexOf("{{group_", start)) != -1) {
            int end = result.indexOf("}}", start);
            if (end == -1) break;
            
            String placeholder = result.substring(start, end + 2);
            String groupName = result.substring(start + 8, end); // 8 = "{{group_".length()
            
            // 标准化分组名称：如果包含 &，对分组排序
            String normalizedGroupName = normalizeGroupName(groupName);
            
            // 查找分组权重
            Double groupWeight = groupTotalWeights.get(normalizedGroupName);
            if (groupWeight != null && groupWeight > 0) {
                result = result.replace(placeholder, String.valueOf(groupWeight));
            } else {
                // 无法找到分组权重，返回 null
                plugin.getPluginLogger().warn("无法找到分组 " + normalizedGroupName + " 的总权重 (原始: " + groupName + ")");
                return null;
            }
            
            start = end + 2;
        }
        
        return result;
    }
    
    /**
     * 标准化分组名称：对用 & 连接的多个分组进行字母排序
     * 例如：ocean_fish&no_star 或 no_star&ocean_fish 都会返回 lava_fish&no_star (按字母顺序)
     */
    private String normalizeGroupName(String groupName) {
        if (!groupName.contains("&")) {
            return groupName;
        }
        
        String[] groups = groupName.split("&");
        java.util.Arrays.sort(groups);
        return String.join("&", groups);
    }
    
    /**
     * 应用群组操作
     */
    private void applyGroupOperation(String groupSpec, double weightValue, Map<String, Double> weights) {
        try {
            String[] requiredGroups = groupSpec.split("&");

            // 查找所有匹配的战利品
            for (Loot loot : plugin.getLootManager().getRegisteredLoots()) {
                if (loot.type() == LootType.ITEM) {
                    List<String> lootGroups = Arrays.asList(loot.lootGroup());

                    // 检查是否包含所有必需的群组
                    boolean hasAllGroups = true;
                    for (String requiredGroup : requiredGroups) {
                        if (!lootGroups.contains(requiredGroup.trim())) {
                            hasAllGroups = false;
                            break;
                        }
                    }

                    if (hasAllGroups) {
                        String lootId = loot.id();
                        weights.put(lootId, weights.getOrDefault(lootId, 0.0) + weightValue);
                    }
                }
            }
        } catch (Exception e) {
            plugin.debug("应用群组操作失败: " + e.getMessage());
        }
    }

    /**
     * 获取条件描述（简化版本，只返回基本信息）
     */
    private String getConditionsDescription(ConditionalElement<?, Player> conditionalElement) {
        try {
            Requirement<Player>[] requirements = conditionalElement.getRequirements();
            if (requirements == null || requirements.length == 0) {
                return "";
            }
            
            // 简单地返回条件数量，具体条件解析较为复杂，暂时省略
            return requirements.length + " 个条件";
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    @NotNull
    public List<String> getLootGroups(@NotNull String lootId) {
        Optional<Loot> lootOpt = plugin.getLootManager().getLoot(lootId);
        if (lootOpt.isPresent()) {
            return Arrays.asList(lootOpt.get().lootGroup());
        }
        return Collections.emptyList();
    }

    @Override
    @Nullable
    public GroupProbabilityInfo getGroupProbability(@NotNull String lootId, @NotNull String groupId) {
        Map<String, GroupProbabilityInfo> probabilities = calculateGroupProbabilities(lootId);
        return probabilities.get(groupId);
    }

    @Override
    public void clearCache() {
        probabilityCache.clear();
        plugin.debug("LootGroupProbabilityCalculator 缓存已清除");
    }
}

