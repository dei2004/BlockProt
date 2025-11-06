/*
 * Copyright (C) 2021 - 2025 dei0 (dei2004)
 * This file is part of BlockProt <https://github.com/dei2004/BlockProt>.
 *
 * BlockProt is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BlockProt is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BlockProt.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.dei0.blockprot.bukkit.integrations;

import de.dei0.blockprot.bukkit.BlockProt;
import de.dei0.blockprot.bukkit.nbt.BlockNBTHandler;
import de.dei0.blockprot.bukkit.nbt.FriendSupportingHandler;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTTileEntity;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Integration for the UltimateTeams plugin using reflection.
 * Allows players to add all their team members as friends to protected blocks.
 * Automatically removes players from chest friend lists when they leave/are kicked from teams.
 * Uses reflection to avoid compile-time dependency on UltimateTeams classes.
 *
 * @since 1.2.5
 */
public class UltimateTeamsIntegration extends PluginIntegration implements Listener {
    
    @Nullable
    private Plugin ultimateTeamsPlugin = null;
    
    // Reflected methods
    @Nullable
    private Method getTeamStorageUtilMethod = null;
    @Nullable
    private Method findTeamByMemberMethod = null;
    @Nullable
    private Method getOwnerMethod = null;
    @Nullable
    private Method getMembersMethod = null;
    @Nullable
    private Method isInTeamMethod = null;
    @Nullable
    private Method isTeamOwnerMethod = null;

    /**
     * Creates a new UltimateTeams integration.
     */
    public UltimateTeamsIntegration() {
        super("ultimateteams");
    }

    @Override
    public boolean isEnabled() {
        return ultimateTeamsPlugin != null && ultimateTeamsPlugin.isEnabled() && getTeamStorageUtilMethod != null;
    }

    @Override
    @Nullable
    public Plugin getPlugin() {
        return BlockProt.getInstance().getPlugin("UltimateTeams");
    }

    @Override
    public void enable() {
        @Nullable final Plugin plugin = getPlugin();
        if (plugin == null || !plugin.isEnabled()) {
            return;
        }
        
        this.ultimateTeamsPlugin = plugin;
        
        try {
            // Get the TeamsStorage utility class
            getTeamStorageUtilMethod = plugin.getClass().getMethod("getTeamStorageUtil");
            
            // Get TeamsStorage methods
            Class<?> teamsStorageClass = Class.forName("dev.xf3d3.ultimateteams.utils.TeamsStorage");
            findTeamByMemberMethod = teamsStorageClass.getMethod("findTeamByMember", UUID.class);
            isInTeamMethod = teamsStorageClass.getMethod("isInTeam", Player.class);
            isTeamOwnerMethod = teamsStorageClass.getMethod("isTeamOwner", Player.class);
            
            // Get Team methods
            Class<?> teamClass = Class.forName("dev.xf3d3.ultimateteams.models.Team");
            getOwnerMethod = teamClass.getMethod("getOwner");
            getMembersMethod = teamClass.getMethod("getMembers");
            
        } catch (Exception e) {
            BlockProt.getInstance().getLogger().warning("Failed to setup UltimateTeams integration via reflection: " + e.getMessage());
            ultimateTeamsPlugin = null;
            return;
        }
        
        // Register event listener to handle team member removal
        registerListener(this);
    }

    /**
     * Gets the team a player belongs to using reflection.
     *
     * @param player The player to check.
     * @return The team object the player is in, or null if not in a team or on error.
     */
    @Nullable
    public Object getPlayerTeam(@NotNull final Player player) {
        if (!isEnabled()) return null;
        
        try {
            Object teamsStorage = getTeamStorageUtilMethod.invoke(ultimateTeamsPlugin);
            Object optionalTeam = findTeamByMemberMethod.invoke(teamsStorage, player.getUniqueId());
            
            // Check if Optional is present
            Method isPresentMethod = optionalTeam.getClass().getMethod("isPresent");
            if ((Boolean) isPresentMethod.invoke(optionalTeam)) {
                Method getMethod = optionalTeam.getClass().getMethod("get");
                return getMethod.invoke(optionalTeam);
            }
        } catch (Exception e) {
            BlockProt.getInstance().getLogger().warning("Error getting player team: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Gets all team members (including the owner) of a player's team using reflection.
     *
     * @param player The player whose team members to get.
     * @return A list of UUIDs of all team members, or an empty list if not in a team or on error.
     */
    @NotNull
    public List<UUID> getTeamMembers(@NotNull final Player player) {
        List<UUID> members = new ArrayList<>();
        
        Object team = getPlayerTeam(player);
        if (team == null) return members;

        try {
            // Add owner
            UUID owner = (UUID) getOwnerMethod.invoke(team);
            members.add(owner);
            
            // Add all other members (members map contains all members except owner)
            @SuppressWarnings("unchecked")
            Map<UUID, ?> membersMap = (Map<UUID, ?>) getMembersMethod.invoke(team);
            members.addAll(membersMap.keySet());
            
        } catch (Exception e) {
            BlockProt.getInstance().getLogger().warning("Error getting team members: " + e.getMessage());
        }
        
        return members;
    }

    /**
     * Checks if a player is in a team using reflection.
     *
     * @param player The player to check.
     * @return True if the player is in a team, false otherwise or on error.
     */
    public boolean isInTeam(@NotNull final Player player) {
        if (!isEnabled()) return false;
        
        try {
            Object teamsStorage = getTeamStorageUtilMethod.invoke(ultimateTeamsPlugin);
            return (Boolean) isInTeamMethod.invoke(teamsStorage, player);
        } catch (Exception e) {
            BlockProt.getInstance().getLogger().warning("Error checking if player is in team: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a player is the team owner using reflection.
     *
     * @param player The player to check.
     * @return True if the player is a team owner, false otherwise or on error.
     */
    public boolean isTeamOwner(@NotNull final Player player) {
        if (!isEnabled()) return false;
        
        try {
            Object teamsStorage = getTeamStorageUtilMethod.invoke(ultimateTeamsPlugin);
            return (Boolean) isTeamOwnerMethod.invoke(teamsStorage, player);
        } catch (Exception e) {
            BlockProt.getInstance().getLogger().warning("Error checking if player is team owner: " + e.getMessage());
            return false;
        }
    }

    @Override
    protected void filterFriendsInternal(@NotNull ArrayList<OfflinePlayer> friends,
                                        @NotNull Player player,
                                        @NotNull Block block) {
        // Allow all friends - no filtering needed for UltimateTeams
    }

    @Override
    public boolean filterFriendByUuid(@NotNull UUID friend,
                                     @NotNull Player player,
                                     @NotNull Block block) {
        // Allow all friends - no filtering needed for UltimateTeams
        return true;
    }

    /**
     * Event handler for when a player is kicked from a team.
     * Removes the kicked player from all protected blocks' friend lists.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeamKick(org.bukkit.event.Event event) {
        // Check if this is a TeamKickEvent using reflection
        if (!event.getClass().getName().equals("dev.xf3d3.ultimateteams.api.events.TeamKickEvent")) {
            return;
        }
        
        try {
            // Get the kicked player using reflection
            Method getKickedPlayerMethod = event.getClass().getMethod("getKickedPlayer");
            OfflinePlayer kickedPlayer = (OfflinePlayer) getKickedPlayerMethod.invoke(event);
            
            if (kickedPlayer != null) {
                removePlayerFromAllProtectedBlocks(kickedPlayer.getUniqueId());
            }
        } catch (Exception e) {
            BlockProt.getInstance().getLogger().warning("Error handling TeamKickEvent: " + e.getMessage());
        }
    }

    /**
     * Event handler for when a player leaves a team.
     * Removes the player from all protected blocks' friend lists.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeamLeave(org.bukkit.event.Event event) {
        // Check if this is a TeamLeaveEvent using reflection
        if (!event.getClass().getName().equals("dev.xf3d3.ultimateteams.api.events.TeamLeaveEvent")) {
            return;
        }
        
        try {
            // Get the player who left using reflection
            Method getPlayerMethod = event.getClass().getMethod("getPlayer");
            Player player = (Player) getPlayerMethod.invoke(event);
            
            if (player != null) {
                removePlayerFromAllProtectedBlocks(player.getUniqueId());
            }
        } catch (Exception e) {
            BlockProt.getInstance().getLogger().warning("Error handling TeamLeaveEvent: " + e.getMessage());
        }
    }

    /**
     * Removes a player from all protected blocks' friend lists across all worlds.
     * This scans all loaded chunks and removes the player from any blocks they were added to.
     *
     * @param playerUUID The UUID of the player to remove.
     */
    private void removePlayerFromAllProtectedBlocks(@NotNull UUID playerUUID) {
        Bukkit.getScheduler().runTaskAsynchronously(BlockProt.getInstance(), () -> {
            int removedCount = 0;
            String playerUUIDString = playerUUID.toString();
            
            // Iterate through all worlds
            for (World world : Bukkit.getWorlds()) {
                // Iterate through all loaded chunks
                for (Chunk chunk : world.getLoadedChunks()) {
                    // Iterate through all tile entities (chests, furnaces, etc.)
                    for (BlockState blockState : chunk.getTileEntities()) {
                        if (blockState instanceof TileState) {
                            try {
                                Block block = blockState.getBlock();
                                NBTTileEntity nbtTile = new NBTTileEntity(blockState);
                                
                                // Check if this block has BlockProt data
                                if (nbtTile.hasTag("blockprot")) {
                                    NBTCompound blockprotData = nbtTile.getCompound("blockprot");
                                    
                                    // Check if it has friends
                                    if (blockprotData.hasTag("friends")) {
                                        NBTCompound friendsCompound = blockprotData.getCompound("friends");
                                        
                                        // Check if this player is in the friends list
                                        if (friendsCompound.hasTag(playerUUIDString)) {
                                            // Remove the player from friends
                                            friendsCompound.removeKey(playerUUIDString);
                                            removedCount++;
                                            
                                            BlockProt.getInstance().getLogger().info(
                                                "Removed player " + playerUUID + " from protected block at " +
                                                block.getX() + ", " + block.getY() + ", " + block.getZ() +
                                                " in world " + world.getName()
                                            );
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                // Silently ignore blocks that can't be processed
                            }
                        }
                    }
                }
            }
            
            final int finalCount = removedCount;
            if (finalCount > 0) {
                BlockProt.getInstance().getLogger().info(
                    "Removed player " + playerUUID + " from " + finalCount + " protected blocks due to team removal."
                );
            }
        });
    }
}
