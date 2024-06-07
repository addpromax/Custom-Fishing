/*
 *  Copyright (C) <2022> <XiaoMoMi>
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

package net.momirealms.customfishing.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.momirealms.customfishing.api.CustomFishingPlugin;
import net.momirealms.customfishing.api.scheduler.CancellableTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;

/**
 * A scheduler implementation for "synchronous" tasks using Folia's RegionScheduler.
 */
public class FoliaSchedulerImpl implements SyncScheduler {

    private final CustomFishingPlugin plugin;

    public FoliaSchedulerImpl(CustomFishingPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Runs a "synchronous" task on the region thread using Folia's RegionScheduler.
     *
     * @param runnable The task to run.
     * @param location The location associated with the task.
     */
    @Override
    public void runSyncTask(Runnable runnable, Location location) {
        if (location == null) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, runnable);
        } else {
            Bukkit.getRegionScheduler().execute(plugin, location, runnable);
        }
    }

    /**
     * Runs a "synchronous" task repeatedly with a specified delay and period using Folia's RegionScheduler.
     *
     * @param runnable The task to run.
     * @param location The location associated with the task.
     * @param delay    The delay in ticks before the first execution.
     * @param period   The period between subsequent executions in ticks.
     * @return A CancellableTask for managing the scheduled task.
     */
    @Override
    public CancellableTask runTaskSyncTimer(Runnable runnable, Location location, long delay, long period) {
        if (location == null) {
            return new FoliaCancellableTask(Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (scheduledTask -> runnable.run()), delay, period));
        }
        return new FoliaCancellableTask(Bukkit.getRegionScheduler().runAtFixedRate(plugin, location, (scheduledTask -> runnable.run()), delay, period));
    }

    /**
     * Runs a "synchronous" task with a specified delay using Folia's RegionScheduler.
     *
     * @param runnable The task to run.
     * @param location The location associated with the task.
     * @param delay    The delay in ticks before the task execution.
     * @return A CancellableTask for managing the scheduled task.
     */
    @Override
    public CancellableTask runTaskSyncLater(Runnable runnable, Location location, long delay) {
        if (delay == 0) {
            if (location == null) {
                return new FoliaCancellableTask(Bukkit.getGlobalRegionScheduler().run(plugin, (scheduledTask -> runnable.run())));
            }
            return new FoliaCancellableTask(Bukkit.getRegionScheduler().run(plugin, location, (scheduledTask -> runnable.run())));
        }
        if (location == null) {
            return new FoliaCancellableTask(Bukkit.getGlobalRegionScheduler().runDelayed(plugin, (scheduledTask -> runnable.run()), delay));
        }
        return new FoliaCancellableTask(Bukkit.getRegionScheduler().runDelayed(plugin, location, (scheduledTask -> runnable.run()), delay));
    }

    /**
     * Represents a scheduled task using Folia's RegionScheduler that can be cancelled.
     */
    public static class FoliaCancellableTask implements CancellableTask {

        private final ScheduledTask scheduledTask;

        public FoliaCancellableTask(ScheduledTask scheduledTask) {
            this.scheduledTask = scheduledTask;
        }

        @Override
        public void cancel() {
            this.scheduledTask.cancel();
        }

        @Override
        public boolean isCancelled() {
            return this.scheduledTask.isCancelled();
        }
    }
}
