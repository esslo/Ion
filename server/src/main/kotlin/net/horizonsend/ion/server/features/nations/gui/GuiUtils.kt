package net.horizonsend.ion.server.features.nations.gui

import co.aikar.commands.ACFBukkitUtil.color
import com.destroystokyo.paper.profile.CraftPlayerProfile
import com.destroystokyo.paper.profile.ProfileProperty
import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.OutlinePane
import com.github.stefvanschie.inventoryframework.pane.Pane
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import net.horizonsend.ion.server.miscellaneous.utils.Skins
import net.horizonsend.ion.server.miscellaneous.utils.updateDisplayName
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.util.Optional
import java.util.UUID
import java.util.concurrent.TimeUnit

fun GuiItem.name(text: String?): GuiItem = apply { if (text != null) item.updateDisplayName(text) }

fun GuiItem.lore(text: String): GuiItem {
	val meta = item.itemMeta
	meta.lore = text.split("\n")
	item.itemMeta = meta
	return this
}

fun GuiItem.lore(vararg lines: String): GuiItem {
	lore(lines.joinToString("\n"))
	return this
}

fun staticPane(x: Int, y: Int, length: Int, height: Int) = StaticPane(x, y, length, height)

fun outlinePane(x: Int, y: Int, length: Int, height: Int) = OutlinePane(x, y, length, height)

fun StaticPane.withItem(item: GuiItem, x: Int, z: Int): StaticPane {
	addItem(item, x, z); return this
}

fun OutlinePane.withItem(item: GuiItem): OutlinePane {
	addItem(item); return this
}

fun OutlinePane.withItems(items: Iterable<GuiItem>): OutlinePane {
	items.forEach { addItem(it) }; return this
}

fun guiButton(itemStack: ItemStack, action: (InventoryClickEvent.() -> Unit)? = null) =
	GuiItem(itemStack.ensureServerConversions()) {
		it.isCancelled = true; action?.invoke(it)
	}

fun guiButton(type: Material, action: (InventoryClickEvent.() -> Unit)? = null) =
	guiButton(item(type).ensureServerConversions(), action)

fun item(type: Material) = ItemStack(type, 1)

private val skinCache: LoadingCache<UUID, Optional<Skins.SkinData>> = CacheBuilder.newBuilder()
	.expireAfterAccess(1, TimeUnit.HOURS)
	.build(
		CacheLoader.from { id ->
			Optional.ofNullable(Skins[id!!])
		}
	)

fun skullItem(uuid: UUID, playerName: String) = skullItem(uuid, playerName, text(playerName))

fun skullItem(uuid: UUID, playerName: String, itemName: Component): ItemStack =
	item(Material.PLAYER_HEAD).updateDisplayName(itemName).also { item ->
		val meta = item.itemMeta as SkullMeta

		skinCache[uuid].ifPresent { skin ->
			meta.playerProfile = Bukkit.createProfile(uuid, playerName).also { profile ->
				profile.setProperty(ProfileProperty("textures", skin.value, skin.signature))
			}
		}

		item.itemMeta = meta
	}.ensureServerConversions()

fun skullItem(itemName: String, uuid: UUID, skinData: String): ItemStack =
	item(Material.PLAYER_HEAD).updateDisplayName(itemName).also { item ->
		val meta = item.itemMeta as SkullMeta

		meta.playerProfile = CraftPlayerProfile(uuid, itemName).apply {
			this.setProperty(ProfileProperty("textures", skinData))
		}

		item.itemMeta = meta
	}.ensureServerConversions()

fun gui(rows: Int, title: String) = ChestGui(rows, color(title))

fun ChestGui.withPane(pane: Pane): ChestGui {
	addPane(pane); return this
}

val InventoryClickEvent.playerClicker: Player get() = whoClicked as Player
