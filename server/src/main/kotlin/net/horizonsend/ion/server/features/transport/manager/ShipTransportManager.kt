package net.horizonsend.ion.server.features.transport.manager

import net.horizonsend.ion.server.features.starship.Starship
import net.horizonsend.ion.server.features.starship.movement.StarshipMovement
import net.horizonsend.ion.server.features.transport.NewTransport
import net.horizonsend.ion.server.features.transport.filters.manager.FilterCache
import net.horizonsend.ion.server.features.transport.filters.manager.ShipFilterCache
import net.horizonsend.ion.server.features.transport.manager.extractors.ShipExtractorManager
import net.horizonsend.ion.server.features.transport.manager.holders.ShipCacheHolder
import net.horizonsend.ion.server.features.transport.nodes.cache.ItemTransportCache
import net.horizonsend.ion.server.features.transport.nodes.cache.PowerTransportCache
import net.horizonsend.ion.server.features.transport.nodes.cache.SolarPanelCache
import net.horizonsend.ion.server.features.transport.nodes.inputs.InputManager
import net.horizonsend.ion.server.features.transport.nodes.inputs.ShipInputManager
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.Vec3i
import org.bukkit.World

class ShipTransportManager(val starship: Starship) : TransportManager<ShipCacheHolder<*>>() {
	override val extractorManager: ShipExtractorManager = ShipExtractorManager(this)
	override val filterCache: FilterCache = ShipFilterCache(this)
	val inputManager = ShipInputManager(this)

	override fun getWorld(): World = starship.world

	override val powerNodeManager = ShipCacheHolder(this) { PowerTransportCache(it) }
	override val solarPanelManager = ShipCacheHolder(this) { SolarPanelCache(it) }
	override val itemPipeManager = ShipCacheHolder(this) { ItemTransportCache(it) }
//	override val fluidNodeManager = ShipCacheHolder(this) { FluidTransportCache(it) }

	override val cacheHolders: Array<ShipCacheHolder<*>> = arrayOf(
		powerNodeManager,
		solarPanelManager,
		itemPipeManager,
//		fluidNodeManager
	)

	override val tickedHolders: Array<ShipCacheHolder<*>> = arrayOf(
		powerNodeManager,
		itemPipeManager,
//		fluidNodeManager
	)

	fun processLoad() {
		cacheHolders.forEach { it.handleLoad() }
		extractorManager.loadExtractors()
		NewTransport.registerTransportManager(this)
	}

	fun onDestroy() {
		cacheHolders.forEach { it.release() }
		NewTransport.removeTransportManager(this)
		extractorManager.releaseExtractors()
	}

	fun displace(movement: StarshipMovement) {
		cacheHolders.forEach { it.displace(movement) }
	}

	override fun getInputProvider(): InputManager {
		return inputManager
	}

	override fun getGlobalCoordinate(localVec3i: Vec3i): Vec3i {
		return starship.getGlobalCoordinate(localVec3i)
	}

	override fun getLocalCoordinate(globalVec3i: Vec3i): Vec3i {
		return starship.getLocalCoordinate(globalVec3i)
	}

	fun clearData() {
		extractorManager.extractors.clear()
		filterCache.filters.clear()
	}
}
