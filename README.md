<img width="875" height="150" alt="Coolstuff" src="https://github.com/user-attachments/assets/317a8d63-af9b-400c-81e1-c62436c4c90f" />
# Coolstuff

Coolstuff is a Vanilla+ Minecraft mod with cool and occasionally absurd content. It expands familiar mechanics and mobs with unexpected reactions, satisfying visual effects and increasingly ridiculous consequences.

## AI Disclosure
Coolstuff was developed with the assistance of AI tools, primarily ChatGPT and Codex, which were used to help write and refine parts of the mod’s code.
The mod’s concept, mechanics, game design, textures, models, sounds, testing, balancing, and overall creative direction were created and managed by Wentory.
AI was used as a development tool - not as a replacement for the creative work behind the mod.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.242 or newer in the 21.x line
- Java 21
- Required on both the client and server

## Features

- Fireball parrying with stacking speed, damage and visual phases
- Ghasts that can parry fireballs back
- UltraGhasts that fire bursts of projectiles
- A black hole created after 100 fireball parries
- A Fireball Launcher and Badminton Racket
- Throwable cakes with multiple hidden fillings and effects
- Spore Creepers that leap at their targets and can be shield-parried
- Frostlings and a snowball screen/freezing mechanic
- Shield Skeletons with defensive and parrying behaviour
- Zombie Wolves
- Emissive armor trims
- An in-game configuration screen with individual mechanic toggles and spawn chances

> Warning: black holes are intentionally destructive. Back up important worlds before experimenting with them.

## Installation

1. Install NeoForge for Minecraft 1.21.1.
2. Put the Coolstuff JAR into the `mods` folder.
3. Install the mod on both the client and server when playing multiplayer.

## Configuration

Open **Mods → Coolstuff → Config**. Spawn chances can be changed immediately. Options marked with a yellow `!` require a Minecraft restart because they remove content from registration or resource loading.

## Building from source

```shell
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

The finished JAR is created in `build/libs`.

## License

Coolstuff is available under the [MIT License](LICENSE).
