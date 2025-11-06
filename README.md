# BlockProt

[![GitHub Release](https://img.shields.io/github/v/release/dei2004/BlockProt?style=flat-square&label=Latest%20Release)](https://github.com/dei2004/BlockProt/releases)

**BlockProt** is a modern, lightweight Bukkit/Spigot plugin that gives players the ability to protect
chests, furnaces, and many more blocks. Features a clean GUI interface instead of commands,
making it easy for any player to use.

## ✨ Features

- 🔒 **Block Protection** - Protect chests, furnaces, doors, and more
- 👥 **Friend System** - Add friends to access your protected blocks
- 🎮 **GUI Interface** - No commands needed, everything through intuitive menus
- 🔗 **UltimateTeams Integration** - Automatically sync team members with block permissions
- 🌐 **Multi-language** - Supports 15+ languages
- ⚡ **Lightweight** - Minimal performance impact
- 🔧 **Plugin Integrations** - Works with Towny, WorldGuard, Lands, PlaceholderAPI

![image1](https://raw.githubusercontent.com/dei2004/BlockProt/master/images/main_menu.png)

## 📥 Installation

1. Download the latest release from the [Releases page](https://github.com/dei2004/BlockProt/releases)
2. Place the `.jar` file in your server's `plugins` directory
3. Restart your server
4. Done! No additional dependencies required

**Requirements:**
- Java 17 or higher
- Spigot, Paper, or any Spigot fork (1.19.3+)
- Does **not** run on CraftBukkit

## 🔨 Building from Source

```bash
git clone https://github.com/dei2004/BlockProt.git
cd BlockProt
./mvnw clean package
```

The compiled plugin will be in `spigot/target/BlockProt-*.jar`  
See [BUILD.md](BUILD.md) for detailed build instructions.

**Supported Languages:** English, German, Spanish, French, Italian, Japanese, Korean, Polish, Portuguese (BR), Russian, Slovak, Czech, Turkish, Chinese (CN/TW)

## 💬 Support

- **Bug Reports:** [GitHub Issues](https://github.com/dei2004/BlockProt/issues)

### Events

Listen to BlockProt events for custom functionality:
- `BlockAccessEvent` - When a player tries to access a protected block
- `BlockAccessMenuEvent` - When a player opens the protection menu
- `BlockLockOnPlaceEvent` - When a block is locked on placement

See [events package](https://github.com/dei2004/BlockProt/tree/master/spigot/src/main/java/de/dei0/blockprot/bukkit/events) for more.

### Creating Plugin Integrations

Extend `PluginIntegration` for conditional integrations with other plugins:
- Only activates when the target plugin is loaded
- Provides utilities for config loading and event listeners
- Examples: [TownyIntegration](https://github.com/dei2004/BlockProt/blob/master/spigot/src/main/java/de/dei0/blockprot/bukkit/integrations/TownyIntegration.java), [UltimateTeamsIntegration](https://github.com/dei2004/BlockProt/blob/master/spigot/src/main/java/de/dei0/blockprot/bukkit/integrations/UltimateTeamsIntegration.java)


## 📜 License

BlockProt is licensed under **GPLv3**. See [LICENSE](LICENSE) for details.

---

**Maintained by [dei0 (dei2004)](https://github.com/dei2004)** • Originally by spnda
