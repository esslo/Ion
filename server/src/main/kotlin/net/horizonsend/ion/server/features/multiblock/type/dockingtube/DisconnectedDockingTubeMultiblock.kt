package net.horizonsend.ion.server.features.multiblock.type.dockingtube

import com.manya.pdc.DataTypes
import net.horizonsend.ion.common.extensions.information
import net.horizonsend.ion.common.extensions.successActionMessage
import net.horizonsend.ion.common.extensions.userError
import net.horizonsend.ion.server.features.multiblock.shape.MultiblockShape
import net.horizonsend.ion.server.features.starship.active.ActiveStarships
import net.horizonsend.ion.server.miscellaneous.registrations.persistence.NamespacedKeys
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.Vec3i
import net.horizonsend.ion.server.miscellaneous.utils.getFacing
import net.horizonsend.ion.server.miscellaneous.utils.getRelativeIfLoaded
import net.horizonsend.ion.server.miscellaneous.utils.isDoor
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Sign
import org.bukkit.block.sign.Side
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType

object DisconnectedDockingTubeMultiblock : DockingTubeMultiblock(
	MiniMessage.miniMessage().deserialize("[Disconnected]").color(TextColor.fromHexString("#FF5555"))
) {
	override val displayName: Component get() = text("Docking Tube (Disconnected)")

	override fun MultiblockShape.RequirementBuilder.tubeStateExtension() = anyButton()

	override fun toggle(sign: Sign, player: Player) {
		if (ActiveStarships.findByBlock(sign.block) != null) {
			player.userError("Cannot toggle tube in an active ship")
			return
		}

		val direction = sign.getFacing().oppositeFace
		val doorTop = sign.block.getRelative(direction)

		val buttons = getButtons(sign.location, direction)

		for (distance in 1..100) {
			// checks if docking tube is too far
			if (distance == 100) {
				player.userError("Other end not found!")
				return
			}

			// needs to be 2 so it's 1 block ahead of the buttons, since the door is already 1 block behind to start with
			val block = doorTop.getRelativeIfLoaded(direction, distance + 2) ?: break

			// if it's not air, it is either the destination or an obstruction
			val blockType = block.type
			if (blockType != Material.AIR) {
				// if it's not a door it's an obstruction
				if (!blockType.isDoor) {
					player.userError(
						"&Docking tube is blocked or the other end is missing/misaligned. Distance: $distance"
					)
					return
				}

				// get the location the other side would have a sign at
				// doesn't actually have to be a sign block
				// if it's unloaded then tell them too move closer
				val otherSignBlock = block.getRelativeIfLoaded(direction) ?: run {
					player.userError("Door on other end is too far.")
					return
				}

				val otherSign: Sign? = otherSignBlock.state as? Sign

				if (otherSign == null) {
					player.information("Warning: the other end of the docking tube does not have a valid sign")
				}

				val otherSignLocation = otherSignBlock.location

				// if the other side's sign is not a valid closed docking tube it can't connect
				if (!signMatchesStructure(otherSignLocation, direction.oppositeFace)) {
					player.userError(
						"Docking tube on the other end is not valid or is not aligned correctly."
					)
					return
				}

				val buttonStates = buttons.map { it.state }
				val otherButtons = getButtons(otherSignLocation, direction.oppositeFace).map { it.state }

				for (i in 0..distance) {
					buttons.forEach { button ->
						button.getRelative(direction, i).setType(Material.GLASS, false)
					}
				}

				player.successActionMessage("Docking tube connected.")

				sign.persistentDataContainer.set(
					NamespacedKeys.MULTIBLOCK,
					PersistentDataType.STRING,
					ConnectedDockingTubeMultiblock::class.simpleName!!
				)

				sign.persistentDataContainer.set(
					NamespacedKeys.TUBE_BUTTONS,
					DataTypes.list(StoredButtonDataType.Companion),
					(buttonStates).map {
						val loc = Vec3i(it.location) - Vec3i(sign.location)

						val (leftRight, upDown) = relativeCoordinateToOffset(direction, loc)

						StoredButtonDataType(
							leftRight,
							upDown,
							it.type
						)
					}
				)

				otherSign?.let {
					otherSign.persistentDataContainer.set(
						NamespacedKeys.TUBE_BUTTONS,
						DataTypes.list(StoredButtonDataType.Companion),
						otherButtons.map {
							val loc = Vec3i(it.location) - Vec3i(sign.location)

							val (leftRight, upDown) = relativeCoordinateToOffset(direction.oppositeFace, loc)

							StoredButtonDataType(
								leftRight,
								upDown,
								it.type
							)
						}
					)

					otherSign.persistentDataContainer.set(
						NamespacedKeys.MULTIBLOCK,
						PersistentDataType.STRING,
						ConnectedDockingTubeMultiblock::class.simpleName!!
					)

					otherSign.getSide(Side.FRONT).line(3, ConnectedDockingTubeMultiblock.stateText)
					otherSign.update(false, false)
				}

				sign.getSide(Side.FRONT).line(3, ConnectedDockingTubeMultiblock.stateText)
				sign.update(false, false)

				sign.world.playSound(sign.location, Sound.BLOCK_PISTON_EXTEND, 1.0f, 1.5f)
				return
			}

			val buttonRelatives = buttons.map { it.getRelative(direction, distance) }

			for (buttonRelative in buttonRelatives) {
				if (buttonRelative.type == Material.AIR) continue

				player.userError("Blocked at ${Vec3i(buttonRelative.location)}")
				return
			}
		}

		player.userError("Other end not found!")
	}
}
