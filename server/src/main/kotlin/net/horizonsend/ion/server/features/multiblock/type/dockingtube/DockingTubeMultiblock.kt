package net.horizonsend.ion.server.features.multiblock.type.dockingtube

import net.horizonsend.ion.server.features.multiblock.Multiblock
import net.horizonsend.ion.server.features.multiblock.shape.MultiblockShape
import net.horizonsend.ion.server.features.multiblock.type.DisplayNameMultilblock
import net.horizonsend.ion.server.features.multiblock.type.InteractableMultiblock
import net.horizonsend.ion.server.miscellaneous.utils.axis
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.Vec3i
import net.horizonsend.ion.server.miscellaneous.utils.rightFace
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Axis
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Sign
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent

abstract class DockingTubeMultiblock(val stateText: Component) : Multiblock(), InteractableMultiblock, DisplayNameMultilblock {
	override val name = "dockingtube"

	override val signText = arrayOf(
		MiniMessage.miniMessage().deserialize("<dark_blue>Docking"),
		MiniMessage.miniMessage().deserialize("<dark_blue>Tube"),
		null,
		stateText
	)

	override val description: Component get() = text("Creates a glass pathway between structures that also have docking tubes.")

	override fun MultiblockShape.buildStructure() {
		at(0, 0, 0).anyDoor()

		z(+1) {
			y(-2) {
				x(0).tubeStateExtension()
			}

			y(-1) {
				x(-1).tubeStateExtension()
				x(+1).tubeStateExtension()
			}

			y(+0) {
				x(-1).tubeStateExtension()
				x(+1).tubeStateExtension()
			}

			y(+1) {
				x(0).tubeStateExtension()
			}
		}
	}

	abstract fun MultiblockShape.RequirementBuilder.tubeStateExtension()

	override fun onSignInteract(sign: Sign, player: Player, event: PlayerInteractEvent) = toggle(sign, player)

	abstract fun toggle(sign: Sign, player: Player)

	fun getButtons(sign: Location, direction: BlockFace): List<Block> {
		val buttons = ArrayList<Block>()
		val right = direction.rightFace
		val left = right.oppositeFace
		val doorTopFront = sign.block.getRelative(direction).getRelative(direction)
		val doorBottomFront = doorTopFront.getRelative(BlockFace.DOWN)
		buttons.add(doorTopFront.getRelative(right))
		buttons.add(doorTopFront.getRelative(left))
		buttons.add(doorTopFront.getRelative(BlockFace.UP))
		buttons.add(doorBottomFront.getRelative(right))
		buttons.add(doorBottomFront.getRelative(left))
		buttons.add(doorBottomFront.getRelative(BlockFace.DOWN))
		return buttons
	}

	fun relativeOffsetToCoordinate(facing: BlockFace, leftRight: Int, upDown: Int): Vec3i {
		val y = upDown
		var x = 0
		var z = 0

		when (facing.axis) {
			Axis.X -> { z = leftRight }
			Axis.Z -> { x = leftRight }
			else -> { throw NotImplementedError() }
		}

		return Vec3i(x, y, z)
	}

	fun relativeCoordinateToOffset(facing: BlockFace, offset: Vec3i): Pair<Int, Int> {
		val (x, y, z) = offset

		val leftRight: Int = when (facing.axis) {
			Axis.X -> z
			Axis.Z -> x
			else -> { throw NotImplementedError() }
		}

		return leftRight to y
	}
}
