# ink.mobs

Declarative mob behavior library for [inklang](https://github.com/inklang/ink) — a compiled scripting language for Minecraft Paper servers.

Inspired by [MythicMobs](https://www.mythicmobs.net/), ink.mobs lets you define custom mob equipment, drops, experience, and event-driven skills using a clean, declarative grammar.

## Features

- **Equipment** — Arm your mobs with custom helmets, chestplates, weapons, and more
- **Drops** — Define item drops with chance percentages and stack sizes
- **Experience** — Set custom XP rewards per mob
- **Skills** — Trigger actions on events like spawn, damage, death, and more
- **Threshold conditions** — Skills only fire when conditions are met (e.g., `on_damaged gt 50`)

## Usage

Place `.ink` scripts in your server's `plugins/ink/plugins/` directory:

```ink
mob ArmoredZombie {
  equipment {
    head: DIAMOND_HELMET
    chest: DIAMOND_CHESTPLATE
    main: IRON_SWORD
  }

  drops {
    DIAMOND 10
    IRON_INGOT 50 2
    ROTTEN_FLESH 80 1
  }

  experience: 50

  skills {
    on_spawn {
      sound(entity, "ENTITY_ZOMBIE_AMBIENT", 1.0, 1.0)
      ignite(entity, 5)
    }

    on_damaged gt 5 {
      particle_effect(entity, "DAMAGE_INDICATOR", 0.5, 0.5, 0.5, 0.01, 10)
    }
  }
}
```

## Built-in Skills

| Skill | Description |
|-------|-------------|
| `particle_effect(entity, effect, ox, oy, oz, speed, count)` | Spawn a particle effect |
| `sound(entity, sound, volume, pitch)` | Play a sound |
| `explosion(entity, radius, setFire, breakBlocks)` | Create an explosion |
| `damage(entity, amount)` | Deal damage to the entity |
| `heal(entity, amount)` | Heal the entity |
| `teleport(entity, x, y, z)` | Teleport the entity |
| `summon(entity, mobType, x, y, z)` | Summon a new entity |
| `set_velocity(entity, x, y, z)` | Set entity velocity |
| `ignite(entity, seconds)` | Set entity on fire |
| `extinguish(entity)` | Extinguish the entity |
| `set_health(entity, health)` | Set entity health |
| `set_max_health(entity, maxHealth)` | Set max health |
| `speed_boost(entity, amplifier, ticks)` | Apply speed potion |
| `jump_boost(entity, amplifier, ticks)` | Apply jump boost |
| `add_tag(entity, tag)` | Add a tag to the entity |
| `has_tag(entity, tag)` | Check if entity has tag |
| `remove_tag(entity, tag)` | Remove a tag |
| `remove_entity(entity)` | Remove the entity |
| `play_effect(entity, effectName)` | Play an entity effect |

## Supported Events

| Event | Description |
|-------|-------------|
| `on_spawn` | When the mob spawns |
| `on_death` | When the mob dies |
| `on_damage` | When the mob deals damage |
| `on_damaged [op threshold]` | When the mob takes damage (with optional condition) |
| `on_target` | When the mob acquires a target |
| `on_explode` | When the mob explodes |
| `on_interact` | When a player interacts with the mob |
| `on_enter_combat` | When the mob enters combat |
| `on_leave_combat` | When the mob exits combat |
| `on_tick every N` | Every N ticks |

## Package

**Name:** `ink.mobs`
**Version:** `0.1.0`
**Repository:** https://github.com/inklang/brushogun

## License

MIT
