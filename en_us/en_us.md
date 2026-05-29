[toc]

## Items

### <font color=green>| </font> Flying Sword
---

* Right-click to summon; it automatically attacks monsters within 15 blocks of the player. Every 10 attacks, **1 point of saturation** is consumed.
* Damage scales with the player's attack power.

![image](../img/fly.gif)

<br>

### <font color=green>| </font> Coin
---

* Right-click to use; consumes **1 coin** per use.
* Compatible with Touhou Little Maid — place it in the maid's inventory and switch her working mode to "Railgun".

![image](../img/coin1.png)

<br>

### <font color=green>| </font> Colorful Coin
---

* Right-click to use; consumes **durability** instead of the item itself.
* Compatible with Touhou Little Maid.
* Can break terrain (configurable in the config file). Does **not** break claimed land from FTB Chunks.
* Enchantable:
  * **Quick Charge**: Reduces charge time by 20% per level.
  * **Power**: Increases damage by 25% per level.
  * **Unbreaking**: Reduces durability consumption.
  * **Mending**: Works as in vanilla Minecraft.

<br>

## <font color=green>| </font> Configuration
---

* Path: `config/Akatzumatool/akatzumatool.toml`
* Modify damage values, enable/disable terrain destruction, configure damage whitelist, etc.
* Entity damage whitelist:
  * `whitelist = ["touhou_little_maid:maid", "minecraft:cat"]`
  * The maid is whitelisted by default and will **not** take damage from this mod's projectiles.
  * Use the CraftTweaker mod to look up entity registry names.
