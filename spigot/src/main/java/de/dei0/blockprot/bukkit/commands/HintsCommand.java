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

package de.dei0.blockprot.bukkit.commands;

import de.dei0.blockprot.bukkit.TranslationKey;
import de.dei0.blockprot.bukkit.Translator;
import de.dei0.blockprot.bukkit.nbt.PlayerSettingsHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HintsCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return false;
        var settings = new PlayerSettingsHandler(player);
        if (!settings.hasPlayerInteractedWithMenu()) {
            settings.setHasPlayerInteractedWithMenu(true);
            sender.sendMessage(
                Translator.get(TranslationKey.MESSAGES__DISABLED_HINTS));
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return null;
    }
}
