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

package de.dei0.blockprot.bukkit.inventories;

import de.dei0.blockprot.bukkit.BlockProt;
import de.dei0.blockprot.bukkit.Permissions;
import de.dei0.blockprot.bukkit.TranslationKey;
import de.dei0.blockprot.bukkit.Translator;
import de.dei0.blockprot.bukkit.events.BlockAccessMenuEvent;
import de.dei0.blockprot.bukkit.integrations.UltimateTeamsIntegration;
import de.dei0.blockprot.bukkit.nbt.BlockNBTHandler;
import de.dei0.blockprot.bukkit.nbt.FriendHandler;
import de.dei0.blockprot.bukkit.nbt.FriendSupportingHandler;
import de.dei0.blockprot.bukkit.nbt.PlayerInventoryClipboard;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.UUID;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class BlockLockInventory extends BlockProtInventory {
    @Override
    int getSize() {
        return InventoryConstants.doubleLine;
    }

    @NotNull
    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__BLOCK_LOCK);
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        Block block = state.getBlock();
        if (block == null) return;
        ItemStack item = event.getCurrentItem();
        if (item == null) return;

        Player player = (Player) event.getWhoClicked();
        if (BlockProt.getDefaultConfig().isLockable(block.getType()) && event.getSlot() == 0) {
            applyChanges(
                player,
                (handler) -> handler.lockBlock(player),
                null
            );
            closeAndOpen(player, null);
        } else {
            switch (item.getType()) {
                case PLAYER_HEAD -> closeAndOpen(
                    player,
                    new FriendManageInventory().fill(player)
                );
                case EMERALD -> {
                    // Add all team members as friends
                    UltimateTeamsIntegration teamsIntegration = null;
                    for (var integration : BlockProt.getInstance().getIntegrations()) {
                        if (integration instanceof UltimateTeamsIntegration) {
                            teamsIntegration = (UltimateTeamsIntegration) integration;
                            break;
                        }
                    }
                    
                    if (teamsIntegration != null && teamsIntegration.isEnabled()) {
                        if (!teamsIntegration.isInTeam(player)) {
                            player.sendMessage(Translator.get(TranslationKey.MESSAGES__NOT_IN_TEAM));
                            closeAndOpen(player, null);
                            return;
                        }
                        
                        List<UUID> teamMembers = teamsIntegration.getTeamMembers(player);
                        var handler = getNbtHandlerOrNull(block);
                        if (handler != null && handler instanceof FriendSupportingHandler<?>) {
                            int added = 0;
                            FriendSupportingHandler<?> friendHandler = (FriendSupportingHandler<?>) handler;
                            
                            for (UUID memberUUID : teamMembers) {
                                // Don't add the player themselves
                                if (memberUUID.equals(player.getUniqueId())) continue;
                                
                                // Check if already a friend
                                boolean alreadyFriend = friendHandler.getFriends().stream()
                                    .anyMatch(f -> f.getName().equals(memberUUID.toString()));
                                
                                if (!alreadyFriend) {
                                    friendHandler.addFriend(memberUUID.toString());
                                    added++;
                                }
                            }
                            
                            if (added > 0) {
                                String message = Translator.get(TranslationKey.MESSAGES__TEAM_MEMBERS_ADDED)
                                    .replace("%count%", String.valueOf(added));
                                player.sendMessage(message);
                            }
                        }
                    }
                    closeAndOpen(player, null);
                }
                case REDSTONE -> {
                    var handler = getNbtHandlerOrNull(block);
                    closeAndOpen(
                        player,
                        handler == null
                            ? null
                            : new RedstoneSettingsInventory().fill(player, state)
                    );
                }
                case OAK_SIGN -> {
                    var handler = getNbtHandlerOrNull(block);
                    closeAndOpen(
                        player,
                        handler == null
                            ? null
                            : new BlockInfoInventory().fill(player, handler)
                    );
                }
                case KNOWLEDGE_BOOK -> {
                    // Paste
                    var handler = getNbtHandlerOrNull(block);
                    var container = PlayerInventoryClipboard.get(player.getUniqueId().toString());
                    if (handler != null && container != null)
                        handler.pasteNbt(container);
                }
                case PAPER -> {
                    // Copy
                    var handler = getNbtHandlerOrNull(block);
                    if (handler != null) {
                        PlayerInventoryClipboard.set(player.getUniqueId().toString(), handler.getNbtCopy());
                        // The player probably doesn't want to paste the data onto the same container.
                        closeAndOpen(player, null);
                    }
                }
                case NAME_TAG -> {
                    player.closeInventory();
                    new AnvilGUI.Builder()
                        .text("Block name")
                        .title(Translator.get(TranslationKey.INVENTORIES__SET_BLOCK_NAME))
                        .plugin(BlockProt.getInstance())
                        .onClick((Integer slot, AnvilGUI.StateSnapshot snapshot) -> {
                            if (slot != AnvilGUI.Slot.OUTPUT) {
                                return Collections.emptyList();
                            }

                            var invState = InventoryState.get(snapshot.getPlayer().getUniqueId());
                            assert(invState.getBlock() != null);

                            new BlockNBTHandler(invState.getBlock()).setName(snapshot.getText());
                            Inventory inventory = new BlockLockInventory().fill(player, block.getType(), new BlockNBTHandler(block));
                            if (inventory == null) return List.of(AnvilGUI.ResponseAction.close());
                            return List.of(AnvilGUI.ResponseAction.openInventory(inventory));
                        })
                        .open(player);
                }
                case SPYGLASS -> closeAndOpen(player, new BlockInspectContentsInventory(player).fill());
                default -> closeAndOpen(
                    player,
                    null
                );
            }
        }
        event.setCancelled(true);
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {
    }

    public Inventory fill(@NotNull Player player, Material material, BlockNBTHandler handler) {
        final InventoryState state = InventoryState.get(player.getUniqueId());
        if (state == null) return inventory;

        var isNotProtected = handler.isNotProtected();
        // This means the user only has INFO permissions but the block is not locked and can
        // therefore not provide any information.
        if (isNotProtected && state.menuPermissions.size() == 1
            && state.menuPermissions.contains(BlockAccessMenuEvent.MenuPermission.INFO))
            return null;

        if (state.menuPermissions.contains(BlockAccessMenuEvent.MenuPermission.LOCK)) {
            setItemStack(
                0,
                getProperMaterial(material),
                isNotProtected
                    ? TranslationKey.INVENTORIES__LOCK
                    : TranslationKey.INVENTORIES__UNLOCK
            );
        }

        // We insert a 'inspect contents' button if the player does not own the block and has admin permissions.
        var rightItemOffset = !isNotProtected && !handler.isOwner(player.getUniqueId()) && (state.getBlock().getState() instanceof InventoryHolder) && player.hasPermission(Permissions.ADMIN.key()) ? 1 : 0;
        if (rightItemOffset == 1) {
            setItemStack(
                getSize() - 2,
                Material.SPYGLASS,
                TranslationKey.INVENTORIES__INSPECT_CONTENTS
            );
        }

        if (!isNotProtected && state.menuPermissions.contains(BlockAccessMenuEvent.MenuPermission.MANAGER)) {
            var offset = 1;
            setItemStack(
                offset++,
                Material.REDSTONE,
                TranslationKey.INVENTORIES__REDSTONE__SETTINGS
            );
            if (!BlockProt.getDefaultConfig().isFriendFunctionalityDisabled()) {
                setItemStack(
                    offset++,
                    Material.PLAYER_HEAD,
                    TranslationKey.INVENTORIES__FRIENDS__MANAGE
                );
                
                // Add team button if UltimateTeams integration is enabled
                UltimateTeamsIntegration teamsIntegration = null;
                for (var integration : BlockProt.getInstance().getIntegrations()) {
                    if (integration instanceof UltimateTeamsIntegration) {
                        teamsIntegration = (UltimateTeamsIntegration) integration;
                        break;
                    }
                }
                
                if (teamsIntegration != null && teamsIntegration.isEnabled() && teamsIntegration.isInTeam(player)) {
                    setItemStack(
                        offset++,
                        Material.EMERALD,
                        TranslationKey.INVENTORIES__FRIENDS__ADD_TEAM
                    );
                }
            }
            setItemStack(
                offset,
                Material.NAME_TAG,
                TranslationKey.INVENTORIES__SET_BLOCK_NAME
            );
            if (PlayerInventoryClipboard.contains(player.getUniqueId().toString())) {
                setItemStack(
                    getSize() - 4 - rightItemOffset,
                    Material.KNOWLEDGE_BOOK,
                    TranslationKey.INVENTORIES__PASTE_CONFIGURATION
                );
            }
            setItemStack(
                getSize() - 3 - rightItemOffset,
                Material.PAPER,
                TranslationKey.INVENTORIES__COPY_CONFIGURATION
            );
        }

        if (!isNotProtected && state.menuPermissions.contains(BlockAccessMenuEvent.MenuPermission.INFO)) {
            setItemStack(
                getSize() - 2 - rightItemOffset,
                Material.OAK_SIGN,
                TranslationKey.INVENTORIES__BLOCK_INFO
            );
        }
        setBackButton();
        return inventory;
    }
}
