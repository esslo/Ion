package net.horizonsend.ion.server.features.starship.subsystem.shield

import net.horizonsend.ion.server.features.multiblock.type.particleshield.BoxShieldMultiblock
import net.horizonsend.ion.server.features.starship.active.ActiveStarship
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.Vec3i
import net.horizonsend.ion.server.miscellaneous.utils.leftFace
import net.horizonsend.ion.server.miscellaneous.utils.rightFace
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.block.Sign
import kotlin.collections.set
import kotlin.math.abs

class BoxShieldSubsystem(
	starship: ActiveStarship,
	sign: Sign,
	multiblock: BoxShieldMultiblock
) : ShieldSubsystem(starship, sign, multiblock) {
	override var power: Int = maxPower
		set(value) {
			field = value.coerceIn(0, maxPower)
		}

	private val width: Int
	private val height: Int
	private val length: Int

	private data class RectangularPrism(val min: Vec3i, val max: Vec3i)

	private val cachedShapes = mutableMapOf<BlockFace, RectangularPrism>()

	init {
		val dimensions = sign.getLine(3)
			.replace(",", " ")
			.split(" ")
			.map { it.toInt() }
		width = dimensions[0]
		height = dimensions[1]
		length = dimensions[2]

		cacheShape { face -> face }
		cacheShape { face -> face.rightFace }
		cacheShape { face -> face.leftFace }
		cacheShape { face -> face.oppositeFace }
	}

	override fun containsPosition(world: World, blockPos: Vec3i): Boolean {
		if (starship.world.uid != world.uid) {
			return false
		}

		val shape = getShape()
		val min = shape.min
		val max = shape.max
		val lx = blockPos.x - pos.x
		val ly = blockPos.y - pos.y
		val lz = blockPos.z - pos.z

		return lx >= min.x && ly >= min.y && lz >= min.z && lx <= max.x && ly <= max.y && lz <= max.z
	}

	private fun cacheShape(getRotatedFace: (BlockFace) -> BlockFace) {
		val key = getRotatedFace(this.face)
		val right = key.rightFace

		val dx = (width / 2) * abs(right.modX) + (length / 2) * abs(key.modX)
		val dy = height / 2
		val dz = (width / 2) * abs(right.modZ) + (length / 2) * abs(key.modZ)

		cachedShapes[key] = RectangularPrism(Vec3i(-dx, -dy, -dz), Vec3i(dx, dy, dz))
	}

	private fun getShape(): RectangularPrism {
		return cachedShapes.getValue(face)
	}
}
