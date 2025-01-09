package net.horizonsend.ion.server.features.starship

import net.horizonsend.ion.common.utils.text.colors.HEColorScheme.Companion.HE_MEDIUM_GRAY
import net.horizonsend.ion.common.utils.text.ofChildren
import net.horizonsend.ion.server.configuration.ConfigurationFiles
import net.horizonsend.ion.server.configuration.StarshipBalancing
import net.horizonsend.ion.server.features.custom.items.CustomItemRegistry
import net.horizonsend.ion.server.features.progression.Levels
import net.horizonsend.ion.server.features.sidebar.SidebarIcon
import net.horizonsend.ion.server.features.starship.destruction.SinkProvider
import net.horizonsend.ion.server.features.world.IonWorld
import net.horizonsend.ion.server.features.world.WorldFlag
import net.horizonsend.ion.server.miscellaneous.utils.updateDisplayName
import net.horizonsend.ion.server.miscellaneous.utils.updateLore
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.empty
import net.kyori.adventure.text.Component.newline
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.AQUA
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.function.Supplier

enum class StarshipType(
	val displayName: String,
	val icon: String = SidebarIcon.GENERIC_STARSHIP_ICON.text,
	val color: String,
	val dynmapIcon: String = "anchor",

	val minSize: Int,
	val maxSize: Int,

	val minLevel: Int,
	val overridePermission: String,

	val containerPercent: Double,
	val concretePercent: Double = 0.3,
	val crateLimitMultiplier: Double,

	val menuItemRaw: Supplier<ItemStack>,
	val displayInMainMenu: Boolean = true,
	val menuSubclasses: Supplier<List<StarshipType>> = Supplier { listOf() },
	val typeCategory: TypeCategory,

	val eventShip: Boolean = false,
	val powerOverrider: Double = 1.0,

	val requiredWorldFlags: Set<WorldFlag> = setOf(),
	val disallowedWorldFlags: Set<WorldFlag> = setOf(),

	val maxMiningLasers: Int = 0,
	val miningLaserTier: Int = 0,

	val sinkProvider: SinkProvider.SinkProviders = SinkProvider.SinkProviders.STANDARD,

	val balancingSupplier: Supplier<StarshipBalancing>
) {
	SPEEDER(
		displayName = "Speeder",
		minSize = 25,
		maxSize = 100,
		minLevel = 1,
		containerPercent = 0.25,
		concretePercent = 0.0,
		crateLimitMultiplier = 0.125,
		menuItemRaw = { ItemStack(Material.DEAD_BUSH) },
		menuSubclasses = { listOf(AI_SPEEDER) },
		typeCategory = TypeCategory.SPECIALTY,
		color = "#ffff32",
		overridePermission = "ion.ships.override.1",
		powerOverrider = 0.0,
		balancingSupplier = ConfigurationFiles.starshipBalancing()::speeder
	),
	AI_SPEEDER(
		displayName = "Speeder",
		minSize = 25,
		maxSize = 100,
		minLevel = 1000,
		containerPercent = 0.5,
		crateLimitMultiplier = 0.125,
		menuItemRaw = { ItemStack(Material.SPONGE) },
		displayInMainMenu = false,
		typeCategory = TypeCategory.SPECIALTY,
		color = "#ffff32",
		powerOverrider = 0.0,
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.speeder",
		balancingSupplier = ConfigurationFiles.starshipBalancing()::speeder
	),
	STARFIGHTER(
		displayName = "Starfighter",
		icon = SidebarIcon.STARFIGHTER_ICON.text,
		minSize = 350,
		maxSize = 500,
		minLevel = 1,
		containerPercent = 0.025,
		crateLimitMultiplier = 0.5,
		menuItemRaw = { ItemStack(Material.IRON_NUGGET) },
		menuSubclasses = { listOf(AI_STARFIGHTER) },
		typeCategory = TypeCategory.WAR_SHIP,
		color = "#ff8000",
		overridePermission = "ion.ships.override.1",
		dynmapIcon = "starfighter",
		balancingSupplier = ConfigurationFiles.starshipBalancing()::starfighter
	),
	AI_STARFIGHTER(
		displayName = "Starfighter",
		icon = SidebarIcon.AI_STARFIGHTER_ICON.text,
		minSize = 150,
		maxSize = 500,
		minLevel = 1000,
		containerPercent = 0.5,
		crateLimitMultiplier = 0.5,
		menuItemRaw = { ItemStack(Material.SPONGE) },
		displayInMainMenu = false,
		typeCategory = TypeCategory.WAR_SHIP,
		color = "#ff8000",
		dynmapIcon = "starfighter",
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.starfighter",
		balancingSupplier = ConfigurationFiles.starshipBalancing()::aiStarfighter
	),
	GUNSHIP(
		displayName = "Gunship",
		icon = SidebarIcon.GUNSHIP_ICON.text,
		minSize = 500,
		maxSize = 2000,
		minLevel = 10,
		containerPercent = 0.025,
		crateLimitMultiplier = 0.5,
		menuItemRaw = { ItemStack(Material.IRON_INGOT) },
		menuSubclasses = { listOf(AI_GUNSHIP) },
		typeCategory = TypeCategory.WAR_SHIP,
		color = "#ff4000",
		overridePermission = "ion.ships.override.10",
		dynmapIcon = "gunship",
		balancingSupplier = ConfigurationFiles.starshipBalancing()::gunship
	),
	AI_GUNSHIP(
		displayName = "Gunship",
		icon = SidebarIcon.AI_GUNSHIP_ICON.text,
		minSize = 500,
		maxSize = 2000,
		minLevel = 1000,
		containerPercent = 0.5,
		crateLimitMultiplier = 0.5,
		menuItemRaw = { ItemStack(Material.SPONGE) },
		displayInMainMenu = false,
		typeCategory = TypeCategory.WAR_SHIP,
		color = "#ff4000",
		dynmapIcon = "gunship",
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.gunship",
		balancingSupplier = ConfigurationFiles.starshipBalancing()::aiGunship
	),
	CORVETTE(
		displayName = "Corvette",
		icon = SidebarIcon.CORVETTE_ICON.text,
		minSize = 2000,
		maxSize = 4000,
		minLevel = 20,
		containerPercent = 0.025,
		crateLimitMultiplier = 0.5,
		menuItemRaw = { ItemStack(Material.IRON_TRAPDOOR) },
		menuSubclasses = { listOf(AI_CORVETTE, AI_CORVETTE_LOGISTIC) },
		typeCategory = TypeCategory.WAR_SHIP,
		color = "#ff0000",
		overridePermission = "ion.ships.override.20",
		dynmapIcon = "corvette",
		maxMiningLasers = 1,
		miningLaserTier = 1,
		balancingSupplier = ConfigurationFiles.starshipBalancing()::corvette
	),
	AI_CORVETTE(
		displayName = "Corvette",
		icon = SidebarIcon.AI_CORVETTE_ICON.text,
		minSize = 2000,
		maxSize = 4000,
		minLevel = 1000,
		containerPercent = 0.5,
		crateLimitMultiplier = 0.5,
		menuItemRaw = { ItemStack(Material.SPONGE) },
		displayInMainMenu = false,
		typeCategory = TypeCategory.WAR_SHIP,
		color = "#ff0000",
		dynmapIcon = "corvette",
		maxMiningLasers = 1,
		miningLaserTier = 1,
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.corvette",
		balancingSupplier = ConfigurationFiles.starshipBalancing()::aiCorvette
	),
	AI_CORVETTE_LOGISTIC(
		displayName = "Logistic Corvette",
		icon = SidebarIcon.AI_CORVETTE_ICON.text,
		minSize = 2000,
		maxSize = 4000,
		minLevel = 1000,
		containerPercent = 0.5,
		crateLimitMultiplier = 0.5,
		menuItemRaw = { ItemStack(Material.IRON_DOOR) },
		displayInMainMenu = false,
		typeCategory = TypeCategory.WAR_SHIP,
		color = "#ff0000",
		dynmapIcon = "corvette",
		maxMiningLasers = 1,
		miningLaserTier = 1,
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.corvette",
		balancingSupplier = ConfigurationFiles.starshipBalancing()::aiCorvetteLogistic
	),
	FRIGATE(
		displayName = "Frigate",
		icon = SidebarIcon.FRIGATE_ICON.text,
		minSize = 4000,
		maxSize = 8000,
		minLevel = 40,
		containerPercent = 0.025,
		crateLimitMultiplier = 0.5,
		menuItemRaw = { ItemStack(Material.IRON_DOOR) },
		menuSubclasses = { listOf(AI_FRIGATE, AI_CORVETTE_LOGISTIC) },
		typeCategory = TypeCategory.WAR_SHIP,
		color = "#c00000",
		overridePermission = "ion.ships.override.40",
		dynmapIcon = "frigate",
		maxMiningLasers = 1,
		miningLaserTier = 1,
		balancingSupplier = ConfigurationFiles.starshipBalancing()::frigate
	),
	AI_FRIGATE(
		displayName = "Frigate",
		icon = SidebarIcon.AI_FRIGATE_ICON.text,
		minSize = 4000,
		maxSize = 8000,
		minLevel = 1000,
		containerPercent = 0.5,
		crateLimitMultiplier = 0.5,
		menuItemRaw = { ItemStack(Material.SPONGE) },
		displayInMainMenu = false,
		typeCategory = TypeCategory.WAR_SHIP,
		color = "#c00000",
		dynmapIcon = "frigate",
		maxMiningLasers = 1,
		miningLaserTier = 1,
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.frigate",
		balancingSupplier = ConfigurationFiles.starshipBalancing()::aiFrigate
	),
	DESTROYER(
		displayName = "Destroyer",
		icon = SidebarIcon.DESTROYER_ICON.text,
		minSize = 8000,
		maxSize = 12000,
		minLevel = 60,
		containerPercent = 0.025,
		crateLimitMultiplier = 0.5,
		menuItemRaw = { ItemStack(Material.IRON_BLOCK) },
		menuSubclasses = { listOf(AI_DESTROYER) },
		typeCategory = TypeCategory.WAR_SHIP,
		color = "#800000",
		overridePermission = "ion.ships.override.60",
		dynmapIcon = "destroyer",
		maxMiningLasers = 1,
		miningLaserTier = 1,
		balancingSupplier = ConfigurationFiles.starshipBalancing()::destroyer
	),
	AI_DESTROYER(
		displayName = "Destroyer",
		icon = SidebarIcon.AI_DESTROYER_ICON.text,
		minSize = 8000,
		maxSize = 12000,
		minLevel = 1000,
		containerPercent = 0.5,
		crateLimitMultiplier = 0.5,
		menuItemRaw = { ItemStack(Material.SPONGE) },
		displayInMainMenu = false,
		typeCategory = TypeCategory.WAR_SHIP,
		color = "#800000",
		dynmapIcon = "destroyer",
		maxMiningLasers = 1,
		miningLaserTier = 1,
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.destroyer",
		balancingSupplier = ConfigurationFiles.starshipBalancing()::aiDestroyer
	),
	CRUISER(
		displayName = "Cruiser",
		icon = SidebarIcon.BATTLECRUISER_ICON.text,
		minSize = 12000,
		maxSize = 16000,
		minLevel = 70,
		containerPercent = 0.025,
		crateLimitMultiplier = 0.5,
		menuItemRaw = { CustomItemRegistry.STEEL_PLATE.constructItemStack() },
		menuSubclasses = { listOf(AI_CRUISER) },
		typeCategory = TypeCategory.WAR_SHIP,
		color = "#FFD700",
		overridePermission = "ion.ships.override.70",
		dynmapIcon = "cruiser",
		maxMiningLasers = 1,
		miningLaserTier = 1,
		sinkProvider = SinkProvider.SinkProviders.CRUISER,
		balancingSupplier = ConfigurationFiles.starshipBalancing()::cruiser,
	),
	AI_CRUISER(
		displayName = "Cruiser",
		icon = SidebarIcon.AI_BATTLECRUISER_ICON.text,
		minSize = 12000,
		maxSize = 16000,
		minLevel = 1000,
		containerPercent = 0.5,
		crateLimitMultiplier = 0.5,
		menuItemRaw = { ItemStack(Material.SPONGE) },
		displayInMainMenu = false,
		typeCategory = TypeCategory.WAR_SHIP,
		color = "#FFD700",
		dynmapIcon = "cruiser",
		maxMiningLasers = 1,
		miningLaserTier = 1,
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.cruiser",
		sinkProvider = SinkProvider.SinkProviders.CRUISER,
		balancingSupplier = ConfigurationFiles.starshipBalancing()::aiCruiser
	),
	BATTLECRUISER(
		displayName = "Battlecruiser",
		icon = SidebarIcon.BATTLESHIP_ICON.text,
		minSize = 16000,
		maxSize = 20000,
		minLevel = 80,
		containerPercent = 0.025,
		crateLimitMultiplier = 0.0,
		menuItemRaw = { CustomItemRegistry.STEEL_BLOCK.constructItemStack() },
		menuSubclasses = { listOf(AI_BATTLECRUISER) },
		typeCategory = TypeCategory.WAR_SHIP,
		color = "#0c5ce8",
		dynmapIcon = "battlecruiser",
		maxMiningLasers = 1,
		miningLaserTier = 1,
		overridePermission = "ion.ships.override.80",
		sinkProvider = SinkProvider.SinkProviders.BATTLECRUISER,
		requiredWorldFlags = setOf(WorldFlag.SPACE_WORLD),
		balancingSupplier = ConfigurationFiles.starshipBalancing()::battlecruiser
	),
	AI_BATTLECRUISER(
		displayName = "Battlecruiser",
		icon = SidebarIcon.AI_BATTLESHIP_ICON.text,
		minSize = 12000,
		maxSize = 20000,
		minLevel = 1000,
		containerPercent = 0.5,
		crateLimitMultiplier = 0.5,
		menuItemRaw = { ItemStack(Material.SPONGE) },
		displayInMainMenu = false,
		typeCategory = TypeCategory.WAR_SHIP,
		color = "#0c5ce8",
		dynmapIcon = "battlecruiser",
		maxMiningLasers = 1,
		miningLaserTier = 1,
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.battlecruiser",
		sinkProvider = SinkProvider.SinkProviders.BATTLECRUISER,
		balancingSupplier = ConfigurationFiles.starshipBalancing()::aiBattlecruiser
	),
	BATTLESHIP(
		displayName = "Battleship",
		icon = SidebarIcon.BATTLESHIP_ICON.text,
		minSize = 20000,
		maxSize = 32000,
		minLevel = 1000,
		containerPercent = 0.015,
		crateLimitMultiplier = 0.5,
		menuItemRaw = { CustomItemRegistry.STEEL_MODULE.constructItemStack() },
		menuSubclasses = { listOf(AI_BATTLESHIP) },
		typeCategory = TypeCategory.WAR_SHIP,
		color = "#0c1cff",
		overridePermission = "ion.ships.override.battleship",
		balancingSupplier = ConfigurationFiles.starshipBalancing()::battleship
	),
	AI_BATTLESHIP(
		displayName = "Battleship",
		icon = SidebarIcon.AI_BATTLESHIP_ICON.text,
		minSize = 20000,
		maxSize = 32000,
		minLevel = 1000,
		containerPercent = 0.5,
		crateLimitMultiplier = 0.5,
		menuItemRaw = { ItemStack(Material.SPONGE) },
		displayInMainMenu = false,
		typeCategory = TypeCategory.WAR_SHIP,
		color = "#0c1cff",
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.battleship",
		balancingSupplier = ConfigurationFiles.starshipBalancing()::aiBattleship
	),
	DREADNOUGHT(
		displayName = "Dreadnought",
		icon = SidebarIcon.DREADNOUGHT_ICON.text,
		minSize = 32000,
		maxSize = 48000,
		minLevel = 1000,
		containerPercent = 0.015,
		crateLimitMultiplier = 0.5,
		menuItemRaw = { CustomItemRegistry.STEEL_ASSEMBLY.constructItemStack() },
		menuSubclasses = { listOf(AI_DREADNOUGHT) },
		typeCategory = TypeCategory.WAR_SHIP,
		color = "#320385",
		overridePermission = "ion.ships.override.dreadnought",
		balancingSupplier = ConfigurationFiles.starshipBalancing()::dreadnought
	),
	AI_DREADNOUGHT(
		displayName = "Dreadnought",
		icon = SidebarIcon.AI_DREADNOUGHT_ICON.text,
		minSize = 32000,
		maxSize = 48000,
		minLevel = 1000,
		containerPercent = 0.5,
		crateLimitMultiplier = 0.5,
		menuItemRaw = { ItemStack(Material.SPONGE) },
		displayInMainMenu = false,
		typeCategory = TypeCategory.WAR_SHIP,
		color = "#320385",
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.dreadnought",
		balancingSupplier = ConfigurationFiles.starshipBalancing()::aiDreadnought
	),
	TANK(
		displayName = "Tank",
		icon = SidebarIcon.STARFIGHTER_ICON.text,
		minSize = 50,
		maxSize = 500,
		minLevel = 100,
		containerPercent = 0.025,
		crateLimitMultiplier = 0.0,
		concretePercent = 0.0,
		menuItemRaw = { CustomItemRegistry.GAS_CANISTER_EMPTY.constructItemStack() },
		typeCategory = TypeCategory.SPECIALTY,
		color = "#ff8000",
		overridePermission = "ion.ships.tank",
		dynmapIcon = "starfighter",
		balancingSupplier = ConfigurationFiles.starshipBalancing()::tank
	),
	SHUTTLE(
		displayName = "Shuttle",
		icon = SidebarIcon.SHUTTLE_ICON.text,
		minSize = 100,
		maxSize = 1000,
		minLevel = 1,
		containerPercent = 0.045,
		crateLimitMultiplier = 1.0,
		menuItemRaw = { ItemStack(Material.PRISMARINE_SHARD) },
		menuSubclasses = { listOf(AI_SHUTTLE) },
		typeCategory = TypeCategory.TRADE_SHIP,
		color = "#008033",
		overridePermission = "ion.ships.override.1",
		powerOverrider = 0.7,
		maxMiningLasers = 1,
		miningLaserTier = 1,
		dynmapIcon = "shuttle",
		balancingSupplier = ConfigurationFiles.starshipBalancing()::shuttle
	),
	AI_SHUTTLE(
		displayName = "Shuttle",
		icon = SidebarIcon.AI_SHUTTLE_ICON.text,
		minSize = 100,
		maxSize = 1000,
		minLevel = 1000,
		containerPercent = 0.045,
		crateLimitMultiplier = 1.0,
		menuItemRaw = { ItemStack(Material.SPONGE) },
		displayInMainMenu = false,
		typeCategory = TypeCategory.TRADE_SHIP,
		color = "#008033",
		powerOverrider = 0.7,
		maxMiningLasers = 1,
		miningLaserTier = 1,
		dynmapIcon = "shuttle",
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.shuttle",
		balancingSupplier = ConfigurationFiles.starshipBalancing()::aiShuttle
	),
	TRANSPORT(
		displayName = "Transport",
		icon = SidebarIcon.TRANSPORT_ICON.text,
		minSize = 1000,
		maxSize = 2000,
		minLevel = 10,
		containerPercent = 0.045,
		crateLimitMultiplier = 1.0,
		menuItemRaw = { ItemStack(Material.PRISMARINE_CRYSTALS) },
		menuSubclasses = { listOf(AI_TRANSPORT) },
		typeCategory = TypeCategory.TRADE_SHIP,
		color = "#008066",
		overridePermission = "ion.ships.override.10",
		powerOverrider = 0.7,
		maxMiningLasers = 1,
		miningLaserTier = 2,
		dynmapIcon = "transport",
		balancingSupplier = ConfigurationFiles.starshipBalancing()::transport
	),
	AI_TRANSPORT(
		displayName = "Transport",
		icon = SidebarIcon.AI_TRANSPORT_ICON.text,
		minSize = 1000,
		maxSize = 2000,
		minLevel = 1000,
		containerPercent = 0.045,
		crateLimitMultiplier = 1.0,
		menuItemRaw = { ItemStack(Material.SPONGE) },
		displayInMainMenu = false,
		typeCategory = TypeCategory.TRADE_SHIP,
		color = "#008066",
		powerOverrider = 0.7,
		maxMiningLasers = 1,
		miningLaserTier = 2,
		dynmapIcon = "transport",
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.transport",
		balancingSupplier = ConfigurationFiles.starshipBalancing()::aiTransport
	),
	LIGHT_FREIGHTER(
		displayName = "Light Freighter",
		icon = SidebarIcon.LIGHT_FREIGHTER_ICON.text,
		minSize = 2000,
		maxSize = 4000,
		minLevel = 20,
		containerPercent = 0.045,
		crateLimitMultiplier = 1.0,
		menuItemRaw = { ItemStack(Material.PRISMARINE_SLAB) },
		menuSubclasses = { listOf(AI_LIGHT_FREIGHTER) },
		typeCategory = TypeCategory.TRADE_SHIP,
		color = "#008099",
		overridePermission = "ion.ships.override.20",
		powerOverrider = 0.7,
		maxMiningLasers = 2,
		miningLaserTier = 2,
		dynmapIcon = "light_freighter",
		balancingSupplier = ConfigurationFiles.starshipBalancing()::lightFreighter
	),
	AI_LIGHT_FREIGHTER(
		displayName = "Light Freighter",
		icon = SidebarIcon.AI_LIGHT_FREIGHTER_ICON.text,
		minSize = 2000,
		maxSize = 4000,
		minLevel = 1000,
		containerPercent = 0.045,
		crateLimitMultiplier = 1.0,
		menuItemRaw = { ItemStack(Material.SPONGE) },
		displayInMainMenu = false,
		typeCategory = TypeCategory.TRADE_SHIP,
		color = "#008099",
		powerOverrider = 0.7,
		maxMiningLasers = 2,
		miningLaserTier = 2,
		dynmapIcon = "light_freighter",
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.light_freighter",
		balancingSupplier = ConfigurationFiles.starshipBalancing()::aiLightFreighter
	),
	MEDIUM_FREIGHTER(
		displayName = "Medium Freighter",
		icon = SidebarIcon.MEDIUM_FREIGHTER_ICON.text,
		minSize = 4000,
		maxSize = 8000,
		minLevel = 40,
		containerPercent = 0.045,
		crateLimitMultiplier = 1.0,
		menuItemRaw = { ItemStack(Material.PRISMARINE_STAIRS) },
		typeCategory = TypeCategory.TRADE_SHIP,
		color = "#0080cc",
		powerOverrider = 0.7,
		maxMiningLasers = 4,
		miningLaserTier = 3,
		dynmapIcon = "medium_freighter",
		overridePermission = "ion.ships.ai.medium_freighter",
		balancingSupplier = ConfigurationFiles.starshipBalancing()::mediumFreighter
	),
	HEAVY_FREIGHTER(
		displayName = "Heavy Freighter",
		icon = SidebarIcon.HEAVY_FREIGHTER_ICON.text,
		minSize = 8000,
		maxSize = 12000,
		minLevel = 60,
		containerPercent = 0.045,
		crateLimitMultiplier = 1.0,
		menuItemRaw = { ItemStack(Material.PRISMARINE) },
		menuSubclasses = { listOf(AI_HEAVY_FREIGHTER) },
		typeCategory = TypeCategory.TRADE_SHIP,
		color = "#0080ff",
		overridePermission = "ion.ships.override.60",
		powerOverrider = 0.7,
		maxMiningLasers = 6,
		miningLaserTier = 3,
		dynmapIcon = "heavy_freighter",
		balancingSupplier = ConfigurationFiles.starshipBalancing()::heavyFreighter
	),
	AI_HEAVY_FREIGHTER(
		displayName = "Heavy Freighter",
		icon = SidebarIcon.AI_HEAVY_FREIGHTER_ICON.text,
		minSize = 8000,
		maxSize = 12000,
		minLevel = 1000,
		containerPercent = 0.045,
		crateLimitMultiplier = 1.0,
		menuItemRaw = { ItemStack(Material.SPONGE) },
		displayInMainMenu = false,
		typeCategory = TypeCategory.TRADE_SHIP,
		color = "#0080ff",
		powerOverrider = 0.7,
		maxMiningLasers = 6,
		miningLaserTier = 3,
		dynmapIcon = "heavy_freighter",
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.heavy_freighter",
		balancingSupplier = ConfigurationFiles.starshipBalancing()::aiHeavyFreighter
	),
	BARGE(
		displayName = "Barge",
		icon = SidebarIcon.BARGE_ICON.text,
		minSize = 16000,
		maxSize = 20000,
		minLevel = 80,
		containerPercent = 0.075,
		crateLimitMultiplier = 0.0,
		menuItemRaw = { ItemStack(Material.SEA_LANTERN) },
		menuSubclasses = { listOf(AI_BARGE) },
		typeCategory = TypeCategory.TRADE_SHIP,
		color = "#0c5ce8",
		dynmapIcon = "barge",
		maxMiningLasers = 10,
		miningLaserTier = 4,
		overridePermission = "ion.ships.override.80",
		sinkProvider = SinkProvider.SinkProviders.BARGE,
		requiredWorldFlags = setOf(WorldFlag.SPACE_WORLD),
		balancingSupplier = ConfigurationFiles.starshipBalancing()::barge
	),
	AI_BARGE(
		displayName = "Barge",
		icon = SidebarIcon.BARGE_ICON.text,
		minSize = 16000,
		maxSize = 20000,
		minLevel = 1000,
		containerPercent = 0.075,
		crateLimitMultiplier = 0.0,
		menuItemRaw = { ItemStack(Material.SPONGE) },
		displayInMainMenu = false,
		typeCategory = TypeCategory.TRADE_SHIP,
		color = "#0c5ce8",
		dynmapIcon = "barge",
		maxMiningLasers = 10,
		miningLaserTier = 4,
		concretePercent = 0.0,
		overridePermission = "ion.ships.ai.barge",
		sinkProvider = SinkProvider.SinkProviders.BARGE,
		balancingSupplier = ConfigurationFiles.starshipBalancing()::barge
	),
	PLATFORM(
		displayName = "Platform",
		minSize = 25,
		maxSize = 2000000,
		minLevel = 1,
		containerPercent = 100.0,
		crateLimitMultiplier = 100.0,
		concretePercent = 0.0,
		menuItemRaw = { ItemStack(Material.BEDROCK) },
		typeCategory = TypeCategory.MISC,
		color = "#ffffff",
		overridePermission = "ion.ships.platform",
		powerOverrider = 0.0,
		balancingSupplier = ConfigurationFiles.starshipBalancing()::platformBalancing
	),
	UNIDENTIFIEDSHIP(
		displayName = "UnidentifiedShip",
		minSize = 25,
		maxSize = 250000,
		minLevel = 1000,
		containerPercent = 100.0,
		concretePercent = 0.0,
		crateLimitMultiplier = 100.0,
		menuItemRaw = { ItemStack(Material.MUD_BRICKS) },
		typeCategory = TypeCategory.MISC,
		color = "#d0e39d",
		overridePermission = "ion.ships.eventship",
		eventShip = true,
		powerOverrider = 2.0,
		balancingSupplier = ConfigurationFiles.starshipBalancing()::eventShipBalancing
	),
	AI_SHIP(
		displayName = "AI Ship",
		minSize = 50,
		maxSize = 48000,
		minLevel = 1000,
		containerPercent = 0.025,
		concretePercent = 0.0,
		crateLimitMultiplier = 0.5,
		menuItemRaw = { ItemStack(Material.SCULK) },
		typeCategory = TypeCategory.MISC,
		color = "#d000d0",
		overridePermission = "ion.ships.aiship",
		balancingSupplier = ConfigurationFiles.starshipBalancing()::eventShipBalancing
	);

	val displayNameMiniMessage: String get() = "<$color>$displayName</$color>"
	val displayNameComponent: Component get() = text(displayName, TextColor.fromHexString(color))

	val menuItem: ItemStack get() = menuItemRaw.get()
		.updateDisplayName(displayNameComponent)
		.updateLore(listOf(
			ofChildren(text("Minimum Block Count: ", HE_MEDIUM_GRAY), text(minSize, AQUA), newline()),
			ofChildren(text("Maximum Block Count: ", HE_MEDIUM_GRAY), text(maxSize, AQUA), newline()),
			empty(),
			text("Right click to view subclasses", AQUA),
			text("Left click to select", AQUA),
		))

	fun canUse(player: Player): Boolean =
		player.hasPermission("starships.anyship") ||
			player.hasPermission(overridePermission) ||
			Levels[player] >= minLevel

	fun canPilotIn(world: IonWorld): Boolean {
		val flags = world.configuration.flags

		if (requiredWorldFlags.toMutableSet().subtract(flags).isNotEmpty()) return false
		return disallowedWorldFlags.none { flags.contains(it) }
	}

	companion object {
		fun getUnlockedTypes(player: Player): List<StarshipType> = entries
			.filter { it.canUse(player) }
			.filter { !it.eventShip.and(!player.hasPermission("ion.ships.eventship")) }
			.sortedBy { it.minLevel }

	}
}
