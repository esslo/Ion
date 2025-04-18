package net.horizonsend.ion.server.features.starship.subsystem.weapon.primary

import net.horizonsend.ion.server.configuration.StarshipWeapons
import net.horizonsend.ion.server.features.multiblock.type.starship.weapon.turret.HeavyTurretMultiblock
import net.horizonsend.ion.server.features.starship.active.ActiveStarship
import net.horizonsend.ion.server.features.starship.subsystem.weapon.TurretWeaponSubsystem
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.Vec3i
import net.kyori.adventure.text.Component
import org.bukkit.block.BlockFace
import java.util.concurrent.TimeUnit

class HeavyTurretWeaponSubsystem(
    ship: ActiveStarship,
    pos: Vec3i,
    face: BlockFace,
    override val multiblock: HeavyTurretMultiblock
) : TurretWeaponSubsystem(ship, pos, face) {
	override val balancing: StarshipWeapons.StarshipWeapon = starship.balancing.weapons.heavyTurret

	override val inaccuracyRadians: Double get() = Math.toRadians(balancing.inaccuracyRadians)
	override val powerUsage: Int get() = balancing.powerUsage / 3
	override var fireCooldownNanos: Long = TimeUnit.MILLISECONDS.toNanos(balancing.fireCooldownMillis)


	override fun getName(): Component {
		return Component.text("Heavy Turret")
	}
}
