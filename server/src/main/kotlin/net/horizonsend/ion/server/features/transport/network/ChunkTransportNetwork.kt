package net.horizonsend.ion.server.features.transport.network

import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import net.horizonsend.ion.server.IonServer
import net.horizonsend.ion.server.features.multiblock.util.BlockSnapshot
import net.horizonsend.ion.server.features.multiblock.util.getBlockSnapshotAsync
import net.horizonsend.ion.server.features.transport.ChunkTransportManager
import net.horizonsend.ion.server.features.transport.node.NodeFactory
import net.horizonsend.ion.server.features.transport.node.TransportNode
import net.horizonsend.ion.server.features.transport.node.power.PowerExtractorNode
import net.horizonsend.ion.server.miscellaneous.registrations.persistence.NamespacedKeys.NODES
import net.horizonsend.ion.server.miscellaneous.registrations.persistence.NamespacedKeys.POWER_TRANSPORT
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.toBlockKey
import net.horizonsend.ion.server.miscellaneous.utils.seconds
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import java.util.concurrent.ConcurrentHashMap

abstract class ChunkTransportNetwork(val manager: ChunkTransportManager) {
	val nodes: ConcurrentHashMap<Long, TransportNode> = ConcurrentHashMap()
	val extractors: ConcurrentHashMap<Long, PowerExtractorNode> = ConcurrentHashMap()

	val world get() = manager.chunk.world

	val pdc get() = manager.chunk.inner.persistentDataContainer

	protected abstract val namespacedKey: NamespacedKey
	abstract val nodeFactory: NodeFactory<*>

// val grids: Nothing = TODO("ChunkTransportNetwork system")

	open fun setup() {}

	/**
	 * Handle the creation / loading of the node into memory
	 **/
	open suspend fun createNodeFromBlock(block: BlockSnapshot) {
		val key = toBlockKey(block.x, block.y, block.z)

		nodeFactory.create(key, block)
	}

	abstract fun processBlockRemoval(key: Long)
	abstract fun processBlockAddition(key: Long, new: BlockSnapshot)

	/**
	 * Load stored node data from the chunk
	 **/
	fun loadData() {
		val existing = pdc.get(POWER_TRANSPORT, PersistentDataType.TAG_CONTAINER) ?: return

		// Deserialize once
		val nodeData = existing.getOrDefault(NODES, PersistentDataType.TAG_CONTAINER_ARRAY, arrayOf()).mapNotNull {
			runCatching { TransportNode.load(it, this) }.onFailure {
				IonServer.slF4JLogger.error("Error deserializing multiblock data! $it")
				it.printStackTrace()
			}.getOrNull()
		}

		nodeData.forEach { runCatching { it.loadIntoNetwork() }.onFailure {
			IonServer.slF4JLogger.error("Error loading node into network!")
			it.printStackTrace()
		} }
	}

	fun save(adapterContext: PersistentDataAdapterContext) {
		val container = adapterContext.newPersistentDataContainer()

		val serializedNodes: MutableMap<TransportNode, Pair<Int, PersistentDataContainer>> = mutableMapOf()

		nodes.forEach { (_, node) ->
			serializedNodes[node] = nodes.values.indexOf(node) to node.serialize(adapterContext, node)
		}

		container.set(NODES, PersistentDataType.TAG_CONTAINER_ARRAY, serializedNodes.values.seconds().toTypedArray())

		pdc.set(namespacedKey, PersistentDataType.TAG_CONTAINER, container)

		saveAdditional()
	}

	open fun saveAdditional() {}

	/**
	 *
	 **/
	abstract suspend fun tick()

	/**
	 * Builds the transportNetwork TODO better documentation
	 **/
	fun build() = manager.scope.launch {
//		collectAllNodes().join()
		buildRelations()
		finalizeNodes()
		buildGraph()
	}

	/**
	 *
	 **/
	private fun collectAllNodes(): Job = manager.scope.launch {
		// Parallel collect the nodes of each section
		manager.chunk.sections.map { (y, _) ->
			launch { collectSectionNodes(y) }
		}.joinAll()
	}

	/**
	 * Collect all nodes in this chunk section
	 *
	 * Iterate the section for possible nodes, handle creation
	 **/
	suspend fun collectSectionNodes(sectionY: Int) {
		val originX = manager.chunk.originX
		val originY = sectionY.shl(4) - manager.chunk.inner.world.minHeight
		val originZ = manager.chunk.originZ

		for (x: Int in 0..15) {
			val realX = originX + x

			for (y: Int in 0..15) {
				val realY = originY + y

				for (z: Int in 0..15) {
					val realZ = originZ + z

					val snapshot = getBlockSnapshotAsync(manager.chunk.world, realX, realY, realZ) ?: continue

					createNodeFromBlock(snapshot)
				}
			}
		}
	}

	/**
	 * Get the neighbors of a node
	 **/
	suspend fun buildRelations() {
		for ((key, node) in nodes) {
			node.buildRelations(key)
		}
	}

	/**
	 * Consolidates network nodes where possible
	 *
	 * e.g. a straight section may be represented as a single node
	 **/
	private fun finalizeNodes() {
//		nodes.forEach { (_, node) ->
//
//		}
	}

	/**
	 *
	 **/
	private fun buildGraph() {
		//TODO
	}

	fun getNode(x: Int, y: Int, z: Int): TransportNode? {
		val key = toBlockKey(x, y, z)
		return nodes[key]
	}
}
