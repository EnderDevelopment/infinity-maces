# Minecraft Custom Maces Plugin

Infinity Maces is a Minecraft plugin that adds six custom maces with unique powers, custom attack damage, crafting recipes, player progression, combat cooldowns, and a command for distributing wood, stone, iron, diamond, netherite, and infinity maces.

## Install

1. Build the plugin with Java 21 and Maven:
   ```bash
   mvn package
   ```
2. Copy the generated `target/plugin.jar` file into your server's `plugins` folder.
3. Start or restart a Paper Minecraft server running version 1.21.11 or a compatible 1.21 release.
4. Ensure the server is using Java 21.
5. Grant trusted players the `custommaces.give` permission if they should be able to use the give command.

## Use

Give a mace with:

```text
/custommace <wood|stone|iron|diamond|netherite|infinity> [player]
```

The command also works with the `/mace` alias. Operators have the required permission by default.

The available maces are:

- Wood Mace: 7.0 attack damage and applies Slowness to the target.
- Stone Mace: 9.0 attack damage and launches the target upward.
- Iron Mace: 12.0 attack damage and grants the wielder temporary Resistance.
- Diamond Mace: 16.0 attack damage and creates a lightning effect at the target.
- Netherite Mace: 21.0 attack damage, ignites the target, and applies Weakness.
- Infinity Mace: 30.0 attack damage, grants the wielder Strength, damages nearby living entities, and knocks them back.

Each mace power has a tier-based cooldown. Maces can also be crafted using their registered recipes. Wood starts the progression, while higher-tier recipes require the appropriate lower-tier custom mace.

Player progression is stored using persistent player data. When a player defeats another player while holding a custom mace and the defeated player is carrying the matching mace tier or the next tier, the killer unlocks that tier and receives the defeated player's custom mace. Custom maces claimed this way are removed from the normal death drops.

---

More Minecraft plugin projects by EnderDevelopment: https://github.com/EnderDevelopment

Project page: https://dash.enderdevelopment.com/community/e0dddd6d-bf39-4eff-9087-bbcccb1132cb?utm_source=github&utm_medium=readme&utm_campaign=auto_publish&utm_content=e0dddd6d-bf39-4eff-9087-bbcccb1132cb
