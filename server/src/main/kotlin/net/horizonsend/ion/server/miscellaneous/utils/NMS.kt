package net.horizonsend.ion.server.miscellaneous.utils

import net.minecraft.core.Direction
import net.minecraft.world.level.block.Block as MinecraftBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.block.Block as BukkitBlock
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.craftbukkit.CraftChunk
import org.bukkit.craftbukkit.block.data.CraftBlockData
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.Vec3i

//region Access Extensions
inline val BlockData.nms: BlockState get() = (this as CraftBlockData).state
inline val LevelChunk.cbukkit: CraftChunk get() = level.world.getChunkAt(locX, locZ) as CraftChunk
//endregion

//region misc
val Direction.blockFace
	get() = when (this) {
		Direction.DOWN -> BlockFace.DOWN
		Direction.UP -> BlockFace.UP
		Direction.NORTH -> BlockFace.NORTH
		Direction.SOUTH -> BlockFace.SOUTH
		Direction.WEST -> BlockFace.WEST
		Direction.EAST -> BlockFace.EAST
	}
//endregion

//region Block Data
fun BukkitBlock.getNMSBlockData(): BlockState {
	return world.minecraft.getChunk(x shr 4, z shr 4).getBlockState(x and 15, y, z and 15)
}

/**
 * Will attempt to get the block in a thread safe manner.
 * If the chunk is not loaded or it's outside of the valid Y range, will return null.
 */
fun getNMSBlockSateSafe(world: World, x: Int, y: Int, z: Int): BlockState? {
	if (y < world.minHeight || y > world.maxHeight) {
		return null
	}

	return try {
		val chunk: LevelChunk = world.minecraft.getChunkIfLoaded(x shr 4, z shr 4) ?: return null

		chunk.getBlockState(x and 15, y, z and 15)
	} catch (indexOutOfBounds: IndexOutOfBoundsException) {
		null
	}
}

fun getNMSBlockSateSafe(world: World, pos: Vec3i): BlockState? {
	val (x, y, z) = pos

	return getNMSBlockSateSafe(world, x, y, z)
}

fun MinecraftBlock.isAir(): Boolean = this == Blocks.AIR || this == Blocks.CAVE_AIR || this == Blocks.VOID_AIR || this == Blocks.LIGHT

fun World.getChunkAtIfLoaded(chunkX: Int, chunkZ: Int): Chunk? = minecraft.getChunkIfLoaded(chunkX, chunkZ)?.cbukkit

fun World.setNMSBlockData(x: Int, y: Int, z: Int, data: BlockState, applyPhysics: Boolean = false): Boolean {
	getBlockAt(x, y, z).setBlockData(data.createCraftBlockData(), applyPhysics)
	return true
}
//endregion
