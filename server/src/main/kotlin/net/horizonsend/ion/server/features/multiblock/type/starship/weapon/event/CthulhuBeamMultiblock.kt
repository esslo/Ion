package net.horizonsend.ion.server.features.multiblock.type.starship.weapon.event

import net.horizonsend.ion.server.features.multiblock.shape.MultiblockShape
import net.horizonsend.ion.server.features.multiblock.type.starship.weapon.SignlessStarshipWeaponMultiblock
import net.horizonsend.ion.server.features.starship.active.ActiveStarship
import net.horizonsend.ion.server.features.starship.subsystem.weapon.event.CthulhuBeamSubsystem
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.Vec3i
import org.bukkit.block.BlockFace

sealed class CthulhuBeamMutliblock : SignlessStarshipWeaponMultiblock<CthulhuBeamSubsystem>() {
	override val key: String = "cthulhu_beam"
	override fun createSubsystem(starship: ActiveStarship, pos: Vec3i, face: BlockFace): CthulhuBeamSubsystem {
		val adjustedFace = getAdjustedFace(face)
		return CthulhuBeamSubsystem(starship, pos, adjustedFace)
	}

	protected abstract fun getAdjustedFace(originalFace: BlockFace): BlockFace
}

object CthulhuBeamStarshipWeaponMultiblockTop : CthulhuBeamMutliblock() {
	override fun getAdjustedFace(originalFace: BlockFace): BlockFace = BlockFace.UP

	override fun MultiblockShape.buildStructure() {
		at(+0, +0, +0).powerInput()
		at(+0, +1, +0).ironBlock()
		at(+0, +2, +0).lodestone()
	}
}

object CthulhuBeamStarshipWeaponMultiblockBottom : CthulhuBeamMutliblock() {
	override fun getAdjustedFace(originalFace: BlockFace): BlockFace = BlockFace.DOWN

	override fun MultiblockShape.buildStructure() {
		at(+0, +0, +0).powerInput()
		at(+0, -1, +0).ironBlock()
		at(+0, -2, +0).lodestone()
	}
}

object CthulhuBeamStarshipWeaponMultiblockSide : CthulhuBeamMutliblock() {
	override fun getAdjustedFace(originalFace: BlockFace): BlockFace = originalFace

	override fun MultiblockShape.buildStructure() {
		at(+0, +0, +0).powerInput()
		at(+0, +0, +1).ironBlock()
		at(+0, +0, +2).lodestone()
	}
}
