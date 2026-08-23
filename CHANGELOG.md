# Changelog

## Coolstuff 0.1.1

### Throwable Cakes

- Dispensers now fire cakes as Thrown Cake entities instead of dropping the item.
- Dispensed cakes preserve their fillings and effects.
- Dispensers retain vanilla item-dropping behavior when throwable cakes are disabled.

### Snowballs

- A normally raised shield now blocks 80% of incoming Snowballs; this is separate from the timed parry window.

### Badminton Racket

- A Badminton Racket now breaks when its parry causes a fireball to enter the Black Hole phase.

### Fixes

- Dropped Ghast Cores are now immune to explosion damage, like Nether Stars.
- Ghast Cores can now drop when the explosion of a parried fireball kills the Ghast, not only when direct fireball damage gets the final hit.

- Projectile-to-projectile deflections no longer trigger the PARRY effect, increase the fireball combo, or power up the fireball.

### Commands

- Added `/coolstuff summon` commands for intentionally spawning special Coolstuff encounters.
- Special summons use the mod's actual initialization logic instead of disguised vanilla `/summon` commands or hand-written NBT.
- Added summons for UltraGhasts, Shield Skeletons, Zombie Wolf packs, owned Zombie Wolves, and armored Wolf Jockeys.

### Zombie Wolves

- Fixed Skeletons being afraid of Zombie Wolves.
- Fixed Zombie Wolves attacking Skeletons.
- Wild Zombie Wolf packs now elect a leader while they are calm.
- Pack members loosely gather around their leader without crowding each other.
- Pack formation is suspended while the wolves are aggressive or under attack.
- Zombie Wolves spawned with an owner now stay close to that Zombie.
