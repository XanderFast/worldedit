/*
 * WorldEdit
 * Copyright (C) 2012 sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

// $Id$

package com.sk89q.worldedit.bukkit;

import com.sk89q.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import com.sk89q.worldedit.LocalPlayer;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldVector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles all events thrown in relation to a Player
 */
public class WorldEditListener implements Listener {

    private WorldEditPlugin plugin;
    private boolean ignoreLeftClickAir = false;
    private final static Pattern cuipattern = Pattern.compile("u00a74u00a75u00a73u00a74([^\\|]*)\\|?(.*)");

    /**
     * Called when a player plays an animation, such as an arm swing
     *
     * @param event Relevant event details
     */

    /**
     * Construct the object;
     *
     * @param plugin
     */
    public WorldEditListener(WorldEditPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.wrapPlayer(event.getPlayer()).dispatchCUIHandshake();
    }

    /**
     * Called when a player leaves a server
     *
     * @param event Relevant event details
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getWorldEdit().markExpire(plugin.wrapPlayer(event.getPlayer()));
    }

    /**
     * Called when a player attempts to use a command
     *
     * @param event Relevant event details
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String[] split = event.getMessage().split(" ");

        if (split.length > 0) {
            split = plugin.getWorldEdit().commandDetection(split);
            split[0] = "/" + split[0];
        }

        final String newMessage = StringUtil.joinString(split, " ");

        if (!newMessage.equals(event.getMessage())) {
            event.setMessage(newMessage);
            plugin.getServer().getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                if (event.getMessage().length() > 0) {
                    plugin.getServer().dispatchCommand(event.getPlayer(),
                            event.getMessage().substring(1));
                }
                event.setCancelled(true);
            }
        }
    }

    /**
     * Called when a player interacts
     *
     * @param event Relevant event details
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.useItemInHand() == Result.DENY) {
            return;
        }

        final LocalPlayer player = plugin.wrapPlayer(event.getPlayer());
        final LocalWorld world = player.getWorld();
        final WorldEdit we = plugin.getWorldEdit();

        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_BLOCK) {
            final Block clickedBlock = event.getClickedBlock();
            final WorldVector pos = new WorldVector(world, clickedBlock.getX(),
                    clickedBlock.getY(), clickedBlock.getZ());

            if (we.handleBlockLeftClick(player, pos)) {
                event.setCancelled(true);
            }

            if (we.handleArmSwing(player)) {
                event.setCancelled(true);
            }

            if (!ignoreLeftClickAir) {
                final int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        ignoreLeftClickAir = false;
                    }
                }, 2);

                if (taskId != -1) {
                    ignoreLeftClickAir = true;
                }
            }
        } else if (action == Action.LEFT_CLICK_AIR) {
            if (ignoreLeftClickAir) {
                return;
            }

            if (we.handleArmSwing(player)) {
                event.setCancelled(true);
            }


        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            final Block clickedBlock = event.getClickedBlock();
            final WorldVector pos = new WorldVector(world, clickedBlock.getX(),
                    clickedBlock.getY(), clickedBlock.getZ());

            if (we.handleBlockRightClick(player, pos)) {
                event.setCancelled(true);
            }

            if (we.handleRightClick(player)) {
                event.setCancelled(true);
            }
        } else if (action == Action.RIGHT_CLICK_AIR) {
            if (we.handleRightClick(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChat(PlayerChatEvent event) {
        Matcher matcher = cuipattern.matcher(event.getMessage());
        if (matcher.find()) {
            String type = matcher.group(1);
            String args = matcher.group(2);

            if( type.equals("v") ) {
                try {
                    plugin.getSession(event.getPlayer()).setCUIVersion(Integer.parseInt(args));
                    event.setCancelled(true);
                } catch(NumberFormatException ignore) {
                }
            }

        }
    }
}
