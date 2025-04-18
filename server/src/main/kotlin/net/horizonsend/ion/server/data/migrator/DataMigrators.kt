package net.horizonsend.ion.server.data.migrator

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers
import net.horizonsend.ion.server.IonServerComponent
import net.horizonsend.ion.server.data.migrator.types.item.MigratorResult
import net.horizonsend.ion.server.data.migrator.types.item.legacy.LegacyCustomItemMigrator
import net.horizonsend.ion.server.data.migrator.types.item.modern.migrator.AspectMigrator
import net.horizonsend.ion.server.data.migrator.types.item.modern.migrator.LegacyNameFixer
import net.horizonsend.ion.server.data.migrator.types.item.modern.migrator.ReplacementMigrator
import net.horizonsend.ion.server.features.custom.items.CustomItemRegistry
import net.horizonsend.ion.server.features.custom.items.CustomItemRegistry.customItem
import net.horizonsend.ion.server.features.custom.items.component.CustomComponentTypes.Companion.MOD_MANAGER
import net.horizonsend.ion.server.miscellaneous.registrations.legacy.LegacyPowerArmorModule
import net.horizonsend.ion.server.miscellaneous.registrations.persistence.NamespacedKeys
import net.horizonsend.ion.server.miscellaneous.utils.isPipedInventory
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.craftbukkit.inventory.CraftBlockInventoryHolder
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.persistence.PersistentDataType

@Suppress("UnstableApiUsage")
object DataMigrators : IonServerComponent() {
	override fun onEnable() {
		registerDataVersions()
	}

	private val dataVersions = mutableListOf<DataVersion>()
	private val lastDataVersion get() = dataVersions.lastIndex

	private fun registerDataVersions() {
		registerDataVersion(DataVersion.builder(0).build()) // Base server version

		registerDataVersion(DataVersion
			.builder(1)
			.addMigrator(LegacyCustomItemMigrator(
				predicate = { it.type == Material.BOW && it.itemMeta.hasCustomModelData() && it.itemMeta.customModelData == 1 && it.customItem == null },
				converter = { MigratorResult.Replacement(CustomItemRegistry.BLASTER_PISTOL.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = { it.type == Material.BOW && it.itemMeta.hasCustomModelData() && it.itemMeta.customModelData == 2 && it.customItem == null },
				converter = { MigratorResult.Replacement(CustomItemRegistry.BLASTER_RIFLE.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = { it.type == Material.BOW && it.itemMeta.hasCustomModelData() && it.itemMeta.customModelData == 3 && it.customItem == null },
				converter = { MigratorResult.Replacement(CustomItemRegistry.BLASTER_SNIPER.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = { it.type == Material.BOW && it.itemMeta.hasCustomModelData() && it.itemMeta.customModelData == 4 && it.customItem == null },
				converter = { MigratorResult.Replacement(CustomItemRegistry.BLASTER_CANNON.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.DIAMOND_PICKAXE
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 1
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.POWER_DRILL_BASIC.constructItemStack()) }
			))
			// Start minerals
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.IRON_INGOT
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 1
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.ALUMINUM_INGOT.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.IRON_ORE
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 1
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.ALUMINUM_ORE.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.IRON_BLOCK
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 1
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.ALUMINUM_BLOCK.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.IRON_INGOT
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 2
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.CHETHERITE.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.IRON_ORE
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 2
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.CHETHERITE_ORE.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.IRON_BLOCK
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 2
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.CHETHERITE_BLOCK.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.IRON_INGOT
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 3
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.TITANIUM_INGOT.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.IRON_ORE
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 3
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.TITANIUM_ORE.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.IRON_BLOCK
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 3
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.TITANIUM_BLOCK.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.IRON_INGOT
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 4
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.URANIUM.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.IRON_ORE
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 4
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.URANIUM_ORE.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.IRON_BLOCK
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 4
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.URANIUM_BLOCK.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.SHEARS
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 1
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.DETONATOR.constructItemStack()) }
			))
			// Minerals end
			// Batteries
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.SNOWBALL
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 7
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.BATTERY_A.constructItemStack(it.amount)) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.SNOWBALL
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 8
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.BATTERY_M.constructItemStack(it.amount)) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.SNOWBALL
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 9
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.BATTERY_G.constructItemStack(it.amount)) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.LEATHER_HELMET
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 1
						&& it.customItem == null
				},
				converter = { old ->
					@Suppress("DEPRECATION")
					val oldMods = old.lore
						?.filter { it.startsWith("Module: ") }
						?.mapNotNull { LegacyPowerArmorModule[it.split(" ")[1]]?.modern?.get() }
						?.toSet()
						?: setOf()

					val new = CustomItemRegistry.POWER_ARMOR_HELMET.constructItemStack()
					CustomItemRegistry.POWER_ARMOR_HELMET.getComponent(MOD_MANAGER).setMods(new, CustomItemRegistry.POWER_ARMOR_HELMET, oldMods.toTypedArray())
					old.getData(DataComponentTypes.DYED_COLOR)?.let { color -> new.setData(DataComponentTypes.DYED_COLOR, color) }
					MigratorResult.Replacement(new)
				}
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.LEATHER_CHESTPLATE
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 1
						&& it.customItem == null
				},
				converter = { old ->
					@Suppress("DEPRECATION")
					val oldMods = old.lore
						?.filter { it.startsWith("Module: ") }
						?.mapNotNull { LegacyPowerArmorModule[it.split(" ")[1]]?.modern?.get() }
						?.toSet()
						?: setOf()

					val new = CustomItemRegistry.POWER_ARMOR_CHESTPLATE.constructItemStack()
					CustomItemRegistry.POWER_ARMOR_CHESTPLATE.getComponent(MOD_MANAGER).setMods(new, CustomItemRegistry.POWER_ARMOR_CHESTPLATE, oldMods.toTypedArray())
					old.getData(DataComponentTypes.DYED_COLOR)?.let { color -> new.setData(DataComponentTypes.DYED_COLOR, color) }
					MigratorResult.Replacement(new)
				}
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.LEATHER_LEGGINGS
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 1
						&& it.customItem == null
				},
				converter = { old ->
					@Suppress("DEPRECATION")
					val oldMods = old.lore
						?.filter { it.startsWith("Module: ") }
						?.mapNotNull { LegacyPowerArmorModule[it.split(" ")[1]]?.modern?.get() }
						?.toSet()
						?: setOf()

					val new = CustomItemRegistry.POWER_ARMOR_LEGGINGS.constructItemStack()
					CustomItemRegistry.POWER_ARMOR_LEGGINGS.getComponent(MOD_MANAGER).setMods(new, CustomItemRegistry.POWER_ARMOR_LEGGINGS, oldMods.toTypedArray())
					old.getData(DataComponentTypes.DYED_COLOR)?.let { color -> new.setData(DataComponentTypes.DYED_COLOR, color) }
					MigratorResult.Replacement(new)
				}
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.LEATHER_BOOTS
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 1
						&& it.customItem == null
				},
				converter = { old ->
					@Suppress("DEPRECATION")
					val oldMods = old.lore
						?.filter { it.startsWith("Module: ") }
						?.mapNotNull { LegacyPowerArmorModule[it.split(" ")[1]]?.modern?.get() }
						?.toSet()
						?: setOf()

					val new = CustomItemRegistry.POWER_ARMOR_BOOTS.constructItemStack()
					CustomItemRegistry.POWER_ARMOR_BOOTS.getComponent(MOD_MANAGER).setMods(new, CustomItemRegistry.POWER_ARMOR_BOOTS, oldMods.toTypedArray())
					old.getData(DataComponentTypes.DYED_COLOR)?.let { color -> new.setData(DataComponentTypes.DYED_COLOR, color) }
					MigratorResult.Replacement(new)
				}
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					(it.type == Material.FLINT_AND_STEEL)
						&& (it.itemMeta.hasCustomModelData())
						&& (it.itemMeta.customModelData == 1)
						&& (it.customItem == null)
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.ARMOR_MODIFICATION_SHOCK_ABSORBING.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.FLINT_AND_STEEL
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 2
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.ARMOR_MODIFICATION_SPEED_BOOSTING.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.FLINT_AND_STEEL
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 3
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.ARMOR_MODIFICATION_ROCKET_BOOSTING.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.FLINT_AND_STEEL
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 4
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.ARMOR_MODIFICATION_NIGHT_VISION.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.FLINT_AND_STEEL
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 5
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.ARMOR_MODIFICATION_ENVIRONMENT.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.FLINT_AND_STEEL
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 6
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.ARMOR_MODIFICATION_PRESSURE_FIELD.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.SHIELD
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 1
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.ENERGY_SWORD_BLUE.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.SHIELD
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 2
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.ENERGY_SWORD_RED.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.SHIELD
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 3
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.ENERGY_SWORD_YELLOW.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.SHIELD
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 4
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.ENERGY_SWORD_GREEN.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.SHIELD
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 5
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.ENERGY_SWORD_PURPLE.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.SHIELD
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 6
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.ENERGY_SWORD_ORANGE.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.SHIELD
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 7
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.ENERGY_SWORD_PINK.constructItemStack()) }
			))
			.addMigrator(LegacyCustomItemMigrator(
				predicate = {
					it.type == Material.SHIELD
						&& it.itemMeta.hasCustomModelData()
						&& it.itemMeta.customModelData == 8
						&& it.customItem == null
				},
				converter = { MigratorResult.Replacement(CustomItemRegistry.ENERGY_SWORD_BLACK.constructItemStack()) }
			))
			.build()
		)

		registerDataVersion(DataVersion
			.builder(2)
			.addMigrator(LegacyNameFixer(
				"DETONATOR", "SMOKE_GRENADE", "PUMPKIN_GRENADE", "GUN_BARREL", "CIRCUITRY", "PISTOL_RECEIVER", "RIFLE_RECEIVER",
				"SMB_RECEIVER", "SNIPER_RECEIVER", "SHOTGUN_RECEIVER", "CANNON_RECEIVER", "ALUMINUM_INGOT", "ALUMINUM_BLOCK", "RAW_ALUMINUM_BLOCK",
				"CHETHERITE", "CHETHERITE_BLOCK", "TITANIUM_INGOT", "TITANIUM_BLOCK", "RAW_TITANIUM_BLOCK", "URANIUM", "URANIUM_BLOCK",
				"RAW_URANIUM_BLOCK", "NETHERITE_CASING", "ENRICHED_URANIUM", "ENRICHED_URANIUM_BLOCK", "URANIUM_CORE", "URANIUM_ROD", "FUEL_ROD_CORE",
				"FUEL_CELL", "FUEL_CONTROL", "REACTIVE_COMPONENT", "REACTIVE_HOUSING", "REACTIVE_PLATING", "REACTIVE_CHASSIS", "REACTIVE_MEMBRANE",
				"REACTIVE_ASSEMBLY", "FABRICATED_ASSEMBLY", "CIRCUIT_BOARD", "MOTHERBOARD", "REACTOR_CONTROL", "SUPERCONDUCTOR", "SUPERCONDUCTOR_BLOCK",
				"SUPERCONDUCTOR_CORE", "STEEL_INGOT", "STEEL_BLOCK", "STEEL_PLATE", "STEEL_CHASSIS", "STEEL_MODULE", "STEEL_ASSEMBLY",
				"REINFORCED_FRAME", "REACTOR_FRAME", "PROGRESS_HOLDER", "BATTLECRUISER_REACTOR_CORE", "BARGE_REACTOR_CORE", "CRUISER_REACTOR_CORE", "UNLOADED_SHELL",
				"LOADED_SHELL", "UNCHARGED_SHELL", "CHARGED_SHELL", "ARSENAL_MISSILE", "PUMPKIN_GRENADE", "UNLOADED_ARSENAL_MISSILE", "ACTIVATED_ARSENAL_MISSILE",
				"GAS_CANISTER_EMPTY",
			))
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.BLASTER_RIFLE)
				.addAdditionalIdentifier("RIFLE")
				.setModel("weapon/blaster/rifle")
				.pullLore(CustomItemRegistry.BLASTER_RIFLE)
				.changeIdentifier("RIFLE", "BLASTER_RIFLE")
				.setDataComponent(DataComponentTypes.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.itemAttributes().build())
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.BLASTER_PISTOL)
				.addAdditionalIdentifier("PISTOL")
				.setModel("weapon/blaster/pistol")
				.pullLore(CustomItemRegistry.BLASTER_PISTOL)
				.changeIdentifier("PISTOL", "BLASTER_PISTOL")
				.setDataComponent(DataComponentTypes.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.itemAttributes().build())
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.BLASTER_SHOTGUN)
				.addAdditionalIdentifier("SHOTGUN")
				.setModel("weapon/blaster/shotgun")
				.pullLore(CustomItemRegistry.BLASTER_SHOTGUN)
				.changeIdentifier("SHOTGUN", "BLASTER_SHOTGUN")
				.setDataComponent(DataComponentTypes.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.itemAttributes().build())
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.BLASTER_SNIPER)
				.addAdditionalIdentifier("SNIPER")
				.setModel("weapon/blaster/sniper")
				.pullLore(CustomItemRegistry.BLASTER_SNIPER)
				.changeIdentifier("SNIPER", "BLASTER_SNIPER")
				.setDataComponent(DataComponentTypes.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.itemAttributes().build())
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.BLASTER_CANNON)
				.addAdditionalIdentifier("CANNON")
				.setModel("weapon/blaster/cannon")
				.pullLore(CustomItemRegistry.BLASTER_CANNON)
				.changeIdentifier("CANNON", "BLASTER_CANNON")
				.setDataComponent(DataComponentTypes.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.itemAttributes().build())
				.build()
			)
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.POWER_DRILL_BASIC))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.POWER_DRILL_ENHANCED))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.POWER_DRILL_ADVANCED))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.POWER_CHAINSAW_BASIC))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.POWER_CHAINSAW_ENHANCED))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.POWER_CHAINSAW_ADVANCED))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.POWER_HOE_BASIC))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.POWER_HOE_ENHANCED))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.POWER_HOE_ADVANCED))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.RANGE_1))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.RANGE_2))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.VEIN_MINER_25))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.SILK_TOUCH_MOD))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.AUTO_SMELT))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.FORTUNE_1))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.FORTUNE_2))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.FORTUNE_3))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.POWER_CAPACITY_25))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.POWER_CAPACITY_50))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.AUTO_REPLANT))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.AUTO_COMPOST))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.RANGE_3))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.EXTENDED_BAR))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.FERTILIZER_DISPENSER))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.PERSONAL_TRANSPORTER))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.AERACH))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.ARET))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.CHANDRA))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.CHIMGARA))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.DAMKOTH))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.DISTERRA))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.EDEN))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.GAHARA))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.HERDOLI))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.ILIUS))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.ISIK))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.KOVFEFE))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.KRIO))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.LIODA))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.LUXITERNA))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.QATRA))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.RUBACIEA))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.TURMS))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.VASK))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.ASTERI))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.HORIZON))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.ILIOS))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.REGULUS))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.SIRIUS))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.PLANET_SELECTOR))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.GAS_CANISTER_EMPTY))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.GAS_CANISTER_HYDROGEN))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.GAS_CANISTER_NITROGEN))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.GAS_CANISTER_METHANE))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.GAS_CANISTER_OXYGEN))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.GAS_CANISTER_CHLORINE))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.GAS_CANISTER_FLUORINE))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.GAS_CANISTER_HELIUM))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.GAS_CANISTER_CARBON_DIOXIDE))
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.BATTLECRUISER_REACTOR_CORE)
				.pullModel(CustomItemRegistry.BATTLECRUISER_REACTOR_CORE)
				.setItemMaterial(Material.WARPED_WART_BLOCK)
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.BARGE_REACTOR_CORE)
				.pullModel(CustomItemRegistry.BARGE_REACTOR_CORE)
				.setItemMaterial(Material.WARPED_WART_BLOCK)
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.CRUISER_REACTOR_CORE)
				.pullModel(CustomItemRegistry.CRUISER_REACTOR_CORE)
				.setItemMaterial(Material.WARPED_WART_BLOCK)
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.ARSENAL_MISSILE)
				.pullModel(CustomItemRegistry.ARSENAL_MISSILE)
				.setItemMaterial(Material.WARPED_FUNGUS_ON_A_STICK)
				.build()
			)
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.UNLOADED_ARSENAL_MISSILE))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.ACTIVATED_ARSENAL_MISSILE))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.UNLOADED_SHELL))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.LOADED_SHELL))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.UNCHARGED_SHELL))
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.CHARGED_SHELL)
				.pullModel(CustomItemRegistry.CHARGED_SHELL)
				.setItemMaterial(Material.WARPED_FUNGUS_ON_A_STICK)
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.STEEL_INGOT)
				.pullModel(CustomItemRegistry.STEEL_INGOT)
				.setItemMaterial(Material.WARPED_FUNGUS_ON_A_STICK)
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.STEEL_BLOCK)
				.pullModel(CustomItemRegistry.STEEL_BLOCK)
				.setItemMaterial(Material.WARPED_WART_BLOCK)
				.build()
			)
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.STEEL_PLATE))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.STEEL_CHASSIS))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.STEEL_MODULE))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.STEEL_ASSEMBLY))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.REINFORCED_FRAME))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.REACTOR_FRAME))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.SUPERCONDUCTOR))
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.SUPERCONDUCTOR_BLOCK)
				.pullModel(CustomItemRegistry.SUPERCONDUCTOR_BLOCK)
				.setItemMaterial(Material.WARPED_WART_BLOCK)
				.build()
			)
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.SUPERCONDUCTOR_CORE))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.CIRCUIT_BOARD))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.MOTHERBOARD))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.REACTOR_CONTROL))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.REACTIVE_COMPONENT))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.REACTIVE_HOUSING))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.REACTIVE_PLATING))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.REACTIVE_CHASSIS))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.REACTIVE_MEMBRANE))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.REACTIVE_ASSEMBLY))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.FABRICATED_ASSEMBLY))
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.NETHERITE_CASING)
				.pullModel(CustomItemRegistry.NETHERITE_CASING)
				.setItemMaterial(Material.WARPED_WART_BLOCK)
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.ENRICHED_URANIUM)
				.pullModel(CustomItemRegistry.ENRICHED_URANIUM)
				.setItemMaterial(Material.WARPED_FUNGUS_ON_A_STICK)
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.ENRICHED_URANIUM_BLOCK)
				.pullModel(CustomItemRegistry.ENRICHED_URANIUM_BLOCK)
				.setItemMaterial(Material.WARPED_WART_BLOCK)
				.build()
			)
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.URANIUM_CORE))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.URANIUM_ROD))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.FUEL_ROD_CORE))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.FUEL_CELL))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.FUEL_CONTROL))
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.URANIUM)
				.pullModel(CustomItemRegistry.URANIUM)
				.setItemMaterial(Material.WARPED_FUNGUS_ON_A_STICK)
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.RAW_URANIUM)
				.pullModel(CustomItemRegistry.RAW_URANIUM)
				.setItemMaterial(Material.WARPED_FUNGUS_ON_A_STICK)
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.URANIUM_ORE)
				.pullModel(CustomItemRegistry.URANIUM_ORE)
				.setItemMaterial(Material.WARPED_WART_BLOCK)
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.URANIUM_BLOCK)
				.pullModel(CustomItemRegistry.URANIUM_BLOCK)
				.setItemMaterial(Material.WARPED_WART_BLOCK)
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.RAW_URANIUM_BLOCK)
				.pullModel(CustomItemRegistry.RAW_URANIUM_BLOCK)
				.setItemMaterial(Material.WARPED_WART_BLOCK)
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.TITANIUM_INGOT)
				.pullModel(CustomItemRegistry.TITANIUM_INGOT)
				.setItemMaterial(Material.WARPED_FUNGUS_ON_A_STICK)
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.RAW_TITANIUM)
				.pullModel(CustomItemRegistry.RAW_TITANIUM)
				.setItemMaterial(Material.WARPED_FUNGUS_ON_A_STICK)
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.TITANIUM_ORE)
				.pullModel(CustomItemRegistry.TITANIUM_ORE)
				.setItemMaterial(Material.WARPED_WART_BLOCK)
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.TITANIUM_BLOCK)
				.pullModel(CustomItemRegistry.TITANIUM_BLOCK)
				.setItemMaterial(Material.WARPED_WART_BLOCK)
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.RAW_TITANIUM_BLOCK)
				.pullModel(CustomItemRegistry.RAW_TITANIUM_BLOCK)
				.setItemMaterial(Material.WARPED_WART_BLOCK)
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.CHETHERITE)
				.pullModel(CustomItemRegistry.CHETHERITE)
				.setItemMaterial(Material.WARPED_FUNGUS_ON_A_STICK)
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.CHETHERITE_ORE)
				.pullModel(CustomItemRegistry.CHETHERITE_ORE)
				.setItemMaterial(Material.WARPED_WART_BLOCK)
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.CHETHERITE_BLOCK)
				.pullModel(CustomItemRegistry.CHETHERITE_BLOCK)
				.setItemMaterial(Material.WARPED_WART_BLOCK)
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.ALUMINUM_INGOT)
				.pullModel(CustomItemRegistry.ALUMINUM_INGOT)
				.setItemMaterial(Material.WARPED_FUNGUS_ON_A_STICK)
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.RAW_ALUMINUM)
				.pullModel(CustomItemRegistry.RAW_ALUMINUM)
				.setItemMaterial(Material.WARPED_FUNGUS_ON_A_STICK)
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.ALUMINUM_ORE)
				.pullModel(CustomItemRegistry.ALUMINUM_ORE)
				.setItemMaterial(Material.WARPED_WART_BLOCK)
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.ALUMINUM_BLOCK)
				.pullModel(CustomItemRegistry.ALUMINUM_BLOCK)
				.setItemMaterial(Material.WARPED_WART_BLOCK)
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.RAW_ALUMINUM_BLOCK)
				.pullModel(CustomItemRegistry.RAW_ALUMINUM_BLOCK)
				.setItemMaterial(Material.WARPED_WART_BLOCK)
				.build()
			)
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.PISTOL_RECEIVER))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.RIFLE_RECEIVER))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.SMB_RECEIVER))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.SNIPER_RECEIVER))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.SHOTGUN_RECEIVER))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.CANNON_RECEIVER))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.GUN_BARREL))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.CIRCUITRY))
			.build()
		)

		registerDataVersion(DataVersion
			.builder(3)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.STANDARD_MAGAZINE)
				.pullModel(CustomItemRegistry.STANDARD_MAGAZINE)
				.pullLore(CustomItemRegistry.STANDARD_MAGAZINE)
				.pullName(CustomItemRegistry.STANDARD_MAGAZINE)
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.SPECIAL_MAGAZINE)
				.pullModel(CustomItemRegistry.SPECIAL_MAGAZINE)
				.pullLore(CustomItemRegistry.SPECIAL_MAGAZINE)
				.pullName(CustomItemRegistry.SPECIAL_MAGAZINE)
				.build()
			)
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.SMOKE_GRENADE))
			.addMigrator(AspectMigrator.fixModel(CustomItemRegistry.DETONATOR))
			.build()
		)

		registerDataVersion(DataVersion.builder(4).build())

		registerDataVersion(DataVersion
			.builder(5)
			.addMigrator(ReplacementMigrator(
				CustomItemRegistry.GUN_BARREL, CustomItemRegistry.CIRCUITRY,
				CustomItemRegistry.PISTOL_RECEIVER, CustomItemRegistry.RIFLE_RECEIVER,
				CustomItemRegistry.SMB_RECEIVER, CustomItemRegistry.SNIPER_RECEIVER,
				CustomItemRegistry.SHOTGUN_RECEIVER, CustomItemRegistry.CANNON_RECEIVER
			))
			.addMigrator(ReplacementMigrator(
				CustomItemRegistry.ALUMINUM_INGOT, CustomItemRegistry.RAW_ALUMINUM, CustomItemRegistry.ALUMINUM_ORE,
				CustomItemRegistry.ALUMINUM_BLOCK, CustomItemRegistry.RAW_ALUMINUM_BLOCK,
				CustomItemRegistry.CHETHERITE, CustomItemRegistry.CHETHERITE_ORE, CustomItemRegistry.CHETHERITE_BLOCK,
				CustomItemRegistry.TITANIUM_INGOT, CustomItemRegistry.RAW_TITANIUM, CustomItemRegistry.TITANIUM_ORE,
				CustomItemRegistry.TITANIUM_BLOCK, CustomItemRegistry.RAW_TITANIUM_BLOCK,
				CustomItemRegistry.URANIUM, CustomItemRegistry.RAW_URANIUM, CustomItemRegistry.URANIUM_ORE,
				CustomItemRegistry.URANIUM_BLOCK, CustomItemRegistry.RAW_URANIUM_BLOCK
			))
			.addMigrator(ReplacementMigrator(
				CustomItemRegistry.REACTIVE_COMPONENT, CustomItemRegistry.REACTIVE_HOUSING, CustomItemRegistry.REACTIVE_PLATING,
				CustomItemRegistry.REACTIVE_CHASSIS, CustomItemRegistry.REACTIVE_MEMBRANE, CustomItemRegistry.REACTIVE_ASSEMBLY,
				CustomItemRegistry.FABRICATED_ASSEMBLY, CustomItemRegistry.CIRCUIT_BOARD, CustomItemRegistry.MOTHERBOARD,
				CustomItemRegistry.REACTOR_CONTROL
			))
			.addMigrator(ReplacementMigrator(
				CustomItemRegistry.SUPERCONDUCTOR, CustomItemRegistry.SUPERCONDUCTOR_BLOCK, CustomItemRegistry.SUPERCONDUCTOR_CORE
			))
			.addMigrator(ReplacementMigrator(
				CustomItemRegistry.STEEL_INGOT, CustomItemRegistry.STEEL_BLOCK, CustomItemRegistry.STEEL_PLATE,
				CustomItemRegistry.STEEL_CHASSIS, CustomItemRegistry.STEEL_MODULE, CustomItemRegistry.STEEL_ASSEMBLY,
				CustomItemRegistry.REINFORCED_FRAME, CustomItemRegistry.REACTOR_FRAME
			))
			.addMigrator(ReplacementMigrator(
				CustomItemRegistry.UNLOADED_SHELL, CustomItemRegistry.LOADED_SHELL, CustomItemRegistry.UNCHARGED_SHELL,
				CustomItemRegistry.CHARGED_SHELL, CustomItemRegistry.ARSENAL_MISSILE, CustomItemRegistry.UNLOADED_ARSENAL_MISSILE,
				CustomItemRegistry.ACTIVATED_ARSENAL_MISSILE
			))
			.build()
		)

		registerDataVersion(DataVersion.builder(6)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.POWER_ARMOR_BOOTS)
				.addConsumer {
					it.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers
						.itemAttributes()
						.addModifier(
							Attribute.ARMOR,
							AttributeModifier(
								NamespacedKeys.key(CustomItemRegistry.POWER_ARMOR_BOOTS.identifier),
								2.0,
								AttributeModifier.Operation.ADD_NUMBER,
								CustomItemRegistry.POWER_ARMOR_BOOTS.slot.group
							)
						)
						.build()
					)
				}
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.POWER_ARMOR_LEGGINGS)
				.addConsumer {
					it.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers
						.itemAttributes()
						.addModifier(
							Attribute.ARMOR,
							AttributeModifier(
								NamespacedKeys.key(CustomItemRegistry.POWER_ARMOR_LEGGINGS.identifier),
								2.0,
								AttributeModifier.Operation.ADD_NUMBER,
								CustomItemRegistry.POWER_ARMOR_LEGGINGS.slot.group
							)
						)
						.build()
					)
				}
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.POWER_ARMOR_CHESTPLATE)
				.addConsumer {
					it.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers
						.itemAttributes()
						.addModifier(
							Attribute.ARMOR,
							AttributeModifier(
								NamespacedKeys.key(CustomItemRegistry.POWER_ARMOR_CHESTPLATE.identifier),
								2.0,
								AttributeModifier.Operation.ADD_NUMBER,
								CustomItemRegistry.POWER_ARMOR_CHESTPLATE.slot.group
							)
						)
						.build()
					)
				}
				.build()
			)
			.addMigrator(AspectMigrator
				.builder(CustomItemRegistry.POWER_ARMOR_HELMET)
				.addConsumer {
					it.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers
						.itemAttributes()
						.addModifier(
							Attribute.ARMOR,
							AttributeModifier(
								NamespacedKeys.key(CustomItemRegistry.POWER_ARMOR_HELMET.identifier),
								2.0,
								AttributeModifier.Operation.ADD_NUMBER,
								CustomItemRegistry.POWER_ARMOR_HELMET.slot.group
							)
						)
						.build()
					)
				}
				.build()
			)
			.build()
		)
	}

	private fun registerDataVersion(dataVersion: DataVersion) {
		dataVersions.add(dataVersion)
	}

	fun migrate(chunk: Chunk) {
		val chunkVersion = chunk.persistentDataContainer.getOrDefault(NamespacedKeys.DATA_VERSION, PersistentDataType.INTEGER, 0)
		if (chunkVersion == lastDataVersion) return

		val toApply = getVersions(chunkVersion)

		val snapshot = chunk.chunkSnapshot

		for (x in 0..15) for (y in chunk.world.minHeight until chunk.world.maxHeight) for (z in 0..15) {
			val type = snapshot.getBlockType(x, y, z)
			if (type.isPipedInventory) {
				val state = chunk.getBlock(x, y, z).state as InventoryHolder
				migrateInventory(state.inventory, toApply)
			}
		}

		chunk.persistentDataContainer.set(NamespacedKeys.DATA_VERSION, PersistentDataType.INTEGER, lastDataVersion)
	}

	fun migrate(player: Player) {
		val playerVersion = player.persistentDataContainer.getOrDefault(NamespacedKeys.PLAYER_DATA_VERSION, PersistentDataType.INTEGER, 0)
		if (playerVersion == lastDataVersion) return

		log.info("Migrating ${player.name}'s inventory from $playerVersion to $lastDataVersion")
		migrateInventory(player.inventory, getVersions(playerVersion).apply { log.info("Applying $size versions") })

		player.persistentDataContainer.set(NamespacedKeys.PLAYER_DATA_VERSION, PersistentDataType.INTEGER, lastDataVersion)
	}

	fun getVersions(dataVersion: Int): List<DataVersion> {
		return dataVersions.subList(dataVersion + 1 /* Inclusive */, lastDataVersion + 1 /* Exclusive */)
	}

	fun migrateInventory(inventory: Inventory, versions: List<DataVersion>) {
		if (inventory.holder is CraftBlockInventoryHolder || inventory.holder is ChestGui || inventory.holder == null) return

		for (dataVersion in versions) {
			dataVersion.migrateInventory(inventory)
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	fun onPlayerLogin(event: PlayerJoinEvent) {
		migrate(event.player)
	}

	@EventHandler(priority = EventPriority.MONITOR)
	fun onChunkLoad(event: ChunkLoadEvent) {
		migrate(event.chunk)
	}

	@EventHandler(priority = EventPriority.MONITOR)
	fun onOpenInventory(event: InventoryOpenEvent) {
		migrateInventory(event.inventory, getVersions(0))
	}
}
