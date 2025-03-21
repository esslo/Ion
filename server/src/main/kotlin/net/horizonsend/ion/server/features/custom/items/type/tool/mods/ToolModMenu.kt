package net.horizonsend.ion.server.features.custom.items.type.tool.mods

import net.horizonsend.ion.common.extensions.userError
import net.horizonsend.ion.common.utils.text.plainText
import net.horizonsend.ion.server.IonServer
import net.horizonsend.ion.server.features.custom.items.CustomItem
import net.horizonsend.ion.server.features.custom.items.CustomItemRegistry.customItem
import net.horizonsend.ion.server.features.custom.items.component.ModManager
import net.horizonsend.ion.server.features.nations.gui.playerClicker
import net.horizonsend.ion.server.listener.SLEventListener
import org.bukkit.Material.AIR
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryInteractEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack
import java.util.UUID
import kotlin.math.ceil

class ToolModMenu(
	private val viewer: Player,
	private val itemStack: ItemStack,
	private val customItem: CustomItem,
	private val modManager: ModManager
) : InventoryHolder {
	private val inventorySize = (ceil((modManager.getMods(itemStack).size + 1).toDouble() / 9.0) * 9).toInt()
	private val internalInventory = IonServer.server.createInventory(this, inventorySize)

	override fun getInventory(): Inventory = internalInventory

	fun open() {
		// Will return CRAFTING if none is open
		if (viewer.openInventory.type != InventoryType.CRAFTING && viewer.openInventory.type != InventoryType.CREATIVE) return

		val view = viewer.openInventory(internalInventory) ?: return

		setup(view)
	}

	private fun setup(view: InventoryView) {
		view.title = "Tool Modifications"

		populateMods()
	}

	private fun populateMods() {
		val mods = modManager.getMods(itemStack)

		var index = 0
		for (mod in mods) {
			val modCustomItem = mod.modItem.get() ?: continue
			internalInventory.setItem(index, modCustomItem.constructItemStack())

			index++
		}
	}

	private fun rebuildFromContents() {
		rebuildFromContents(internalInventory.contents.toList())
	}

	private fun rebuildFromContents(contents: Collection<ItemStack?>) {
		val existingMods = modManager.getMods(itemStack).toMutableList()

		val nonItemMods = modManager
			.getMods(itemStack)
			.filter { it.modItem.get() == null }

		val mods = contents
			.mapNotNull { it?.customItem }
			.filterIsInstance<net.horizonsend.ion.server.features.custom.items.type.tool.mods.ModificationItem>()
			.mapTo(mutableSetOf()) { it.modification }
			.plus(nonItemMods)
			.toTypedArray()

		val newMods = mods.toMutableList()

		// Remove all the mods that were present
		newMods.removeAll(existingMods)

		// Remove all the mods that still are present
		existingMods.removeAll(mods.toSet())

		modManager.setMods(itemStack, customItem, mods)

		// Handle the removal / addition of mods
		existingMods.forEach { it.onRemove(itemStack) }
		newMods.forEach { it.onAdd(itemStack) }
	}

	/**
	 * Fired when the inventory is closed
	 **/
	fun handleClose(event: InventoryCloseEvent) {
		rebuildFromContents()
	}

	/**
	 * Events from the player's clicked inventory while the mod menu is on top
	 **/
	fun handlePlayerClick(event: InventoryClickEvent) {
		val currentItem = event.currentItem
		val cursorItem = event.cursor
		val index = event.slot

		if (event.click != ClickType.SHIFT_LEFT && event.click != ClickType.SHIFT_RIGHT) return

		when {
			currentItem != null && cursorItem.type == AIR -> handleAddItem(index, currentItem, event)
			currentItem == null && cursorItem.type != AIR -> handleRemoveItem(index, event)
			else -> event.isCancelled = true
		}
	}

	/**
	 * Events from the player's clicked mod menu
	 **/
	fun handleClick(event: InventoryClickEvent) {
		val currentItem = event.currentItem
		val cursorItem = event.cursor
		val index = event.slot

		if (event.isCancelled) return

		when {
			event.click == ClickType.NUMBER_KEY -> handleNumberKey(index, currentItem, event)
			currentItem == null && cursorItem.type != AIR -> handleAddItem(index, cursorItem, event)
			currentItem != null && cursorItem.type == AIR -> handleRemoveItem(index, event)
			currentItem != null && cursorItem.type != AIR -> handleSwapItem(index, currentItem, cursorItem, event)
			else -> event.isCancelled = true
		}
	}

	fun handleDrag(event: InventoryDragEvent) {
		// If dragging something outside the inventory
		if (event.rawSlots.none { it < inventorySize }) return

		when {
			// Unstackable item can't be split across multiple slots
			event.cursor == null && event.oldCursor.maxStackSize == 1 -> {
				// Will only end up in a single slot
				val slot = event.rawSlots.first()
				handleAddItem(slot, event.newItems[slot]!!, event)
			}
			else -> event.isCancelled = true
		}
	}

	private fun handleNumberKey(slotNumber: Int, currentItem: ItemStack?, event: InventoryClickEvent) {
		val playerItem = viewer.inventory.getItem(event.hotbarButton) ?: ItemStack(AIR)

		when {
			currentItem == null && playerItem.type != AIR -> handleAddItem(slotNumber, playerItem, event)
			currentItem != null && playerItem.type == AIR -> handleRemoveItem(slotNumber, event)
			currentItem != null && playerItem.type != AIR -> handleSwapItem(slotNumber, currentItem, playerItem, event)
			else -> event.isCancelled = true
		}
	}

	private fun handleAddItem(slot: Int, item: ItemStack, event: InventoryInteractEvent) {
		if (!canAdd(item, event.whoClicked as Player)) {
			event.isCancelled = true
			return
		}

		rebuildFromContents(listOf(*internalInventory.contents, item))
	}

	private fun handleRemoveItem(slot: Int, event: InventoryClickEvent) {
		// Rebuild with the item removed
		rebuildFromContents(internalInventory.contents.subtract(setOf(internalInventory.contents[slot])))
	}

	private fun handleSwapItem(slot: Int, currentItem: ItemStack, new: ItemStack, event: InventoryClickEvent) {
		if (!canAdd(new, event.playerClicker)) {
			event.isCancelled = true
			return
		}

		val modified = internalInventory.contents.toMutableList()
		modified[slot] = new

		rebuildFromContents(modified)
	}

	private fun canAdd(itemStack: ItemStack, player: Player): Boolean {
		val customItem = itemStack.customItem
		if (customItem !is ModificationItem) {
			return false
		}

		val mod: ItemModification = customItem.modification

		if (!mod.applicationPredicates.any { predicate -> predicate.canApplyTo(this.customItem) }) {
			player.userError("${mod.displayName.plainText()} cannot be used on this tool!")
			return false
		}

		if (this.modManager.getMods(this.itemStack).size >= this.modManager.maxMods) {
			player.userError("Mod limit reached!")
			return false
		}

		return this.modManager.getMods(this.itemStack).none { existingMod ->
			val incompatible = existingMod.incompatibleWithMods.contains(mod::class)

			if (incompatible) {
				player.userError("${mod.displayName.plainText()} is incompatible with ${existingMod.displayName.plainText()}!")
			}

			// Already installed
			if (existingMod == mod) {
				player.userError("${mod.displayName.plainText()} is already installed!")
				return@none true
			}

			incompatible
		}
	}

	companion object : SLEventListener() {
		private val inventories = mutableMapOf<UUID, net.horizonsend.ion.server.features.custom.items.type.tool.mods.ToolModMenu>()

		fun create(viewer: Player, itemStack: ItemStack, customItem: CustomItem, manager: ModManager): net.horizonsend.ion.server.features.custom.items.type.tool.mods.ToolModMenu {
			val holder = net.horizonsend.ion.server.features.custom.items.type.tool.mods.ToolModMenu(viewer, itemStack, customItem, manager)
			net.horizonsend.ion.server.features.custom.items.type.tool.mods.ToolModMenu.Companion.inventories[viewer.uniqueId] = holder

			return holder
		}

		@EventHandler
		fun onInventoryClick(event: InventoryClickEvent) {
			val inventory = event.clickedInventory ?: return

			val holder = inventory.holder

			if (holder is net.horizonsend.ion.server.features.custom.items.type.tool.mods.ToolModMenu) {
				holder.handleClick(event)
			}

			if (holder is Player) {
				val topHolder = holder.openInventory.topInventory.holder

				if (topHolder is net.horizonsend.ion.server.features.custom.items.type.tool.mods.ToolModMenu) {
					topHolder.handlePlayerClick(event)
				}

				return
			}
		}

		@EventHandler
		fun onInventoryDrag(event: InventoryDragEvent) {
			val inventory = event.inventory

			val holder = inventory.holder

			if (holder is net.horizonsend.ion.server.features.custom.items.type.tool.mods.ToolModMenu) {
				holder.handleDrag(event)
			}
		}

		@EventHandler
		fun onInventoryClose(event: InventoryCloseEvent) {
			val inventory = event.inventory
			val holder = inventory.holder

			if (holder is net.horizonsend.ion.server.features.custom.items.type.tool.mods.ToolModMenu) {
				holder.handleClose(event)
			}
		}

		@EventHandler
		fun onPlayerDropItem(event: PlayerDropItemEvent) {
			val player = event.player
			val holder = player.openInventory.topInventory.holder

			if (holder is net.horizonsend.ion.server.features.custom.items.type.tool.mods.ToolModMenu) {
				// Assume they dropped the item

				if (event.itemDrop.itemStack.itemMeta == holder.itemStack.itemMeta) {
					player.closeInventory()
				}
			}
		}
	}
}
