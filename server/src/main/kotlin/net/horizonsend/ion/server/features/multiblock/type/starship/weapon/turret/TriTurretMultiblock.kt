package net.horizonsend.ion.server.features.multiblock.type.starship.weapon.turret

import net.horizonsend.ion.server.configuration.StarshipWeapons
import net.horizonsend.ion.server.features.multiblock.shape.MultiblockShape
import net.horizonsend.ion.server.features.starship.active.ActiveStarship
import net.horizonsend.ion.server.features.starship.subsystem.weapon.TurretWeaponSubsystem
import net.horizonsend.ion.server.features.starship.subsystem.weapon.secondary.TriTurretWeaponSubsystem
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.Vec3i
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import org.bukkit.Material.GRINDSTONE
import org.bukkit.Material.IRON_TRAPDOOR
import org.bukkit.block.BlockFace

sealed class TriTurretMultiblock : TurretMultiblock() {
	override fun createSubsystem(starship: ActiveStarship, pos: Vec3i, face: BlockFace): TurretWeaponSubsystem {
		return TriTurretWeaponSubsystem(starship, pos, getFacing(pos, starship), this)
	}

	protected abstract fun getYFactor(): Int

	override val displayName: Component get() = text("Tri Turret (${if (getYFactor() == 1) "Top" else "Bottom"})")
	override val description: Component get() = text("Rotating weapon system effective against large targets. Can be auto-targeting.")

	override fun getBalancing(starship: ActiveStarship): StarshipWeapons.StarshipWeapon = starship.balancing.weapons.triTurret

	override fun buildFirePointOffsets(): List<Vec3i> = listOf(
		Vec3i(-2, getYFactor() * 4, +3),
		Vec3i(+0, getYFactor() * 4, +4),
		Vec3i(+2, getYFactor() * 4, +3)
	)

	override fun MultiblockShape.buildStructure() {
		y(getYFactor() * 2) {
			z(-1) {
				x(+0).sponge()
			}

			z(+0) {
				x(-1).sponge()
				x(+1).sponge()
			}

			z(+1) {
				x(+0).sponge()
			}
		}

		y(getYFactor() * 3) {
			z(-3) {
				x(-1).anyStairs()
				x(+0).terracottaOrDoubleSlab()
				x(+1).anyStairs()
			}

			z(-2) {
				x(-2).ironBlock()
				x(-1..+1) { this.anyConcrete() }
				x(+2).ironBlock()
			}

			z(-1) {
				x(-3).anyStairs()
				x(-2..+2) { this.anyConcrete() }
				x(+3).anyStairs()
			}

			z(+0) {
				x(-3..-2) { terracottaOrDoubleSlab() }
				x(-1..+1) { this.anyConcrete() }
				x(+2..+3) { terracottaOrDoubleSlab() }
			}

			z(+1) {
				x(-3).anyStairs()
				x(-2).terracottaOrDoubleSlab()
				x(-1).anyConcrete()
				x(+0).terracottaOrDoubleSlab()
				x(+1).anyConcrete()
				x(+2).terracottaOrDoubleSlab()
				x(+3).anyStairs()
			}

			z(+2) {
				x(-2).ironBlock()
				x(-1).anyConcrete()
				x(+0).terracottaOrDoubleSlab()
				x(+1).anyConcrete()
				x(+2).ironBlock()
			}

			z(+3) {
				x(-1).anyStairs()
				x(+0).anyStairs()
				x(+1).anyStairs()
			}
		}

		y(getYFactor() * 4) {
			z(-3) {
				x(+0).anyStairs()
			}

			z(-2) {
				x(-2).anySlab()
				x(-1).anyStairs()
				x(+0).terracottaOrDoubleSlab()
				x(+1).anyStairs()
				x(+2).anySlab()
			}

			z(-1) {
				x(-2).terracottaOrDoubleSlab()
				x(-1).terracottaOrDoubleSlab()
				x(+0).terracottaOrDoubleSlab()
				x(+1).terracottaOrDoubleSlab()
				x(+2).terracottaOrDoubleSlab()
			}

			z(+0) {
				x(-3).anyStairs()
				x(-2).type(GRINDSTONE)
				x(-1).anyStairs()
				x(+0).terracottaOrDoubleSlab()
				x(+1).anyStairs()
				x(+2).type(GRINDSTONE)
				x(+3).anyStairs()
			}

			z(+1) {
				x(-2).endRod()
				x(-1).type(IRON_TRAPDOOR)
				x(+0).type(GRINDSTONE)
				x(+1).type(IRON_TRAPDOOR)
				x(+2).endRod()
			}

			z(+2) {
				x(-2).endRod()
				x(-1).type(IRON_TRAPDOOR)
				x(+0).endRod()
				x(+1).type(IRON_TRAPDOOR)
				x(+2).endRod()
			}

			z(+3) {
				x(+0).endRod()
			}
		}
	}
}

object TopTriTurretMultiblock : TriTurretMultiblock() {
	override fun getYFactor(): Int = 1
	override fun getPilotOffset(): Vec3i = Vec3i(+0, +3, +2)
}

object BottomTriTurretMultiblock : TriTurretMultiblock() {
	override fun getYFactor(): Int = -1
	override fun getPilotOffset(): Vec3i = Vec3i(+0, -4, +2)
}
