package net.horizonsend.ion.server.features.starship.control.movement

import net.horizonsend.ion.server.features.starship.control.controllers.ai.util.PathfindingController
import net.horizonsend.ion.server.miscellaneous.utils.Vec3i
import net.horizonsend.ion.server.miscellaneous.utils.component1
import net.horizonsend.ion.server.miscellaneous.utils.component2
import net.horizonsend.ion.server.miscellaneous.utils.distanceSquared
import net.horizonsend.ion.server.miscellaneous.utils.minecraft
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.chunk.LevelChunk
import org.bukkit.Location
import org.bukkit.World
import kotlin.math.abs


/*
 * Pathfinding:
 *
 * Uses level chunk sections as nodes, if filled, they are marked as unnavigable
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
object AIPathfinding {
	const val MAX_A_STAR_ITERATIONS = 20

	data class SectionNode(val world: World, val location: Vec3i, val navigable: Boolean) {
		val center: Vec3i = Vec3i(
			location.x.shl(4) + 8,
			location.y.shl(4) + world.minHeight + 8,
			location.z.shl(4) + 8
		)
	}


	/** Searches the chunk for being navigable */
	fun checkNavigability(world: World, chunk: LevelChunk, yRange: Iterable<Int>): Set<SectionNode> {
		val sectionNodes = mutableSetOf<SectionNode>()
		val levelChunkSections = chunk.sections

		val chunkX = chunk.pos.x
		val chunkZ = chunk.pos.z

		for (sectionPos in yRange) {
			val section = levelChunkSections.getOrNull(sectionPos)
			val pos = Vec3i(chunkX, sectionPos, chunkZ)

			// Section is not inside the build limit, and is not navigable
			if (section == null) {
				sectionNodes += SectionNode(world, pos, false)
				continue
			}

			// Section is only air, navigable
			if (section.hasOnlyAir()) {
				sectionNodes += SectionNode(world, pos, true)
				continue
			}

			// Section has blocks, not navigable
			sectionNodes += SectionNode(world, pos, false)
		}

		return sectionNodes
	}

	/** Takes a list of sections across multiple chunks, and returns those sections being searched */
	fun searchSections(world: World, sections: Collection<Vec3i>, loadChunks: Boolean = false): Set<SectionNode> {
		val chunkMap = sections.groupBy { ChunkPos(it.x, it.z) }
		val nmsWorld = world.minecraft

		val sectionNodes = mutableSetOf<SectionNode>()

		for ((chunkPos, sectionsList) in chunkMap) {
			val (x, z) = chunkPos

			val chunk = nmsWorld.getChunkIfLoaded(x, z) ?: if (loadChunks) nmsWorld.getChunk(x, z) else null

			if (chunk == null) {
				for (sectionPos in sections) {
					// Don't try to load chunks
					sectionNodes.add(SectionNode(world, sectionPos, false))
				}

				continue
			}

			val nodes = checkNavigability(world, chunk, sectionsList.map { it.y })

			sectionNodes.addAll(nodes)
		}

		return sectionNodes
	}

	/** Gets the chunks that should be searched for pathfinding */
	private fun getSurroundingChunkPositions(controller: PathfindingController, radius: Int = 5): List<Vec3i> {
		val center = Vec3i(controller.getSectionOrigin())

		val centerChunkX = center.x
		val centerSectionY = center.y
		val centerChunkZ = center.z

		val xRange = (-radius + centerChunkX)..(+radius + centerChunkX)
		val yRange = (-radius + centerSectionY)..(+radius + centerSectionY)
		val zRange = (-radius + centerChunkZ)..(+radius + centerChunkZ)

		val positions = mutableListOf<Vec3i>()

		for (x in xRange) for (z in zRange) for (y in yRange) {
			positions += Vec3i(x, y, z)
		}

		return positions
	}

	/** Adjusts the tracked sections when the AI has moved */
	fun adjustTrackedSections(controller: PathfindingController, searchDistance: Int, loadChunks: Boolean = false) {
		if (controller.trackedSections.isEmpty()) {
			val allSurrounding = getSurroundingChunkPositions(controller, searchDistance)
			val navigable = searchSections(controller.getWorld(), allSurrounding, loadChunks)
			controller.trackedSections.addAll(navigable)
			return
		}

		val new = getSurroundingChunkPositions(controller)

		// Remove all the tracked sections that are no longer tracked
		controller.trackedSections.removeIf { !new.contains(it.location) }

		val existingPositions = controller.trackedSections.map { it.location }

		// Non-searched new sections
		val toSearch = new.toMutableSet()
		toSearch.removeIf { existingPositions.contains(it) }

		val navigable = searchSections(controller.getWorld(), toSearch, loadChunks)

		controller.trackedSections.addAll(navigable)
	}

	// Begin A* Implementation
	fun findNavigationNodes(controller: PathfindingController, destination: Vec3i, searchDistance: Int): List<SectionNode> {
		val allNodes = controller.trackedSections.toSet()

		val currentPos = controller.getCenter()
		val originNodeLocation = controller.getSectionOrigin()

		val destinationNodeLocation = getDestinationNode(currentPos, destination, searchDistance)
		val destinationNode = allNodes.first { it.location == destinationNodeLocation }

		val closedNodes = controller.trackedSections.filter { !it.navigable }.toMutableList()
		val openNodes = mutableListOf(allNodes.first { it.location == originNodeLocation })

		val originNode = openNodes.first()

		var currentNode: SectionNode = originNode

		var iterations = 0
		while (currentNode != destinationNode && iterations <= MAX_A_STAR_ITERATIONS) {
			iterations++
//			closedNodes += currentNode

			val nextNode = searchNeighbors(currentNode, allNodes, closedNodes, destinationNode) ?: return openNodes
			currentNode = nextNode
			openNodes += nextNode

			continue
		}

		return openNodes
	}

	fun searchNeighbors(
		previousNode: SectionNode,
		allNodes: Collection<SectionNode>,
		closedNodes: Collection<SectionNode>,
		destinationNode: SectionNode,
	): SectionNode? {
		val neighbors = getNeighborNodes(previousNode, allNodes)

		if (neighbors.isEmpty()) throw PathfindingException("No neighbors for $previousNode!")

		return neighbors
			.filter { it.navigable }
			.filter { !closedNodes.contains(it) }
			.associateWith { getEstimatedCostEuclidean(it, destinationNode) }
			.minByOrNull { it.value }?.key
	}

	fun getNeighborNodes(node: SectionNode, all: Collection<SectionNode>): Set<SectionNode> {
		val nodes = mutableSetOf<SectionNode>()
		val neighbors = getNeighbors(node)

		return all.filterTo(nodes) { neighbors.contains(it.location) }
	}

	fun getNeighbors(node: SectionNode): Set<Vec3i> {
		val (x, y, z) = node.location
		val sections = mutableSetOf<Vec3i>()

		for (face in ADJACENT.values()) {
			val newX = x + face.modX
			val newY = y + face.modY
			val newZ = z + face.modZ

			sections += Vec3i(newX, newY, newZ)
		}

		return sections
	}

	/** Gets a destination node location */
	fun getDestinationNode(origin: Location, destination: Vec3i, searchDistance: Int): Vec3i {
		val vector = destination.toVector().subtract(origin.toVector()).normalize().multiply((searchDistance - 1).shl(4))

		val (x, y, z) = Vec3i(origin.clone().add(vector))
		val chunkX = x.shr(4)
		val chunkZ = z.shr(4)
		val sectionMinY = (y - origin.world.minHeight).shr(4)

		return Vec3i(chunkX, sectionMinY, chunkZ)
	}

	// Use squares for estimation
	fun getEstimatedCostEuclidean(node: SectionNode, destinationNode: SectionNode): Int =
		distanceSquared(node.location, destinationNode.location)

	fun getEstimatedCostManhattan(node: SectionNode, destination: Vec3i): Double {
		val origin = node.center.toVector()

		destination.toVector()

		val xDiff = abs(origin.x - destination.x)
		val yDiff = abs(origin.y - destination.y)
		val zDiff = abs(origin.z - destination.z)

		return xDiff + yDiff + zDiff
	}

	class PathfindingException(message: String?) : Exception(message)

	enum class ADJACENT(val modX: Int, val modY: Int, val modZ: Int) {
		NORTH(0, 0, -1),
		UP_NORTH(0, 1, -1),
		DOWN_NORTH(0, -1, -1),
		NORT_EAST(1, 0, -1),
		NORT_WEST(-1, 0, -1),
		UP_NORT_EAST(1, 1, -1),
		UP_NORT_WEST(-1, 1, -1),
		DOWN_NORT_EAST(1, -1, -1),
		DOWN_NORT_WEST(-1, -1, -1),
		EAST(1, 0, 0),
		UP_EAST(1, 1, 0),
		DOWN_EAST(1, -1, 0),
		SOUTH(0, 0, 1),
		UP_SOUTH(0, 1, 1),
		DOWN_SOUTH(0, -1, 1),
		SOUTH_EAST(1, 0, 1),
		SOUTH_WEST(-1, 0, 1),
		UP_SOUTH_EAST(1, 1, 1),
		UP_SOUTH_WEST(-1, 1, 1),
		DOWN_SOUTH_EAST(1, -1, 1),
		DOWN_SOUTH_WEST(-1, -1, 1),
		WEST(-1, 0, 0),
		UP_WEST(-1, 1, 0),
		DOWN_WEST(-1, -1, 0),
		UP(0, 1, 0),
		DOWN(0, -1, 0),
	}
}