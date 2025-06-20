package net.horizonsend.ion.server.command.nations

import co.aikar.commands.BukkitCommandExecutionContext
import co.aikar.commands.InvalidCommandArgument
import co.aikar.commands.PaperCommandManager
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.Description
import co.aikar.commands.annotation.Optional
import co.aikar.commands.annotation.Subcommand
import net.horizonsend.ion.common.database.DbObject
import net.horizonsend.ion.common.database.Oid
import net.horizonsend.ion.common.database.cache.nations.NationCache
import net.horizonsend.ion.common.database.cache.nations.SettlementCache
import net.horizonsend.ion.common.database.schema.misc.SLPlayerId
import net.horizonsend.ion.common.database.schema.nations.Nation
import net.horizonsend.ion.common.database.schema.nations.Settlement
import net.horizonsend.ion.common.database.schema.nations.spacestation.NationSpaceStation
import net.horizonsend.ion.common.database.schema.nations.spacestation.PlayerSpaceStation
import net.horizonsend.ion.common.database.schema.nations.spacestation.SettlementSpaceStation
import net.horizonsend.ion.common.database.schema.nations.spacestation.SpaceStationCompanion
import net.horizonsend.ion.common.database.slPlayerId
import net.horizonsend.ion.common.database.uuid
import net.horizonsend.ion.common.extensions.alert
import net.horizonsend.ion.common.extensions.userError
import net.horizonsend.ion.common.utils.miscellaneous.toCreditsString
import net.horizonsend.ion.common.utils.text.colors.HEColorScheme.Companion.HE_DARK_GRAY
import net.horizonsend.ion.common.utils.text.colors.HEColorScheme.Companion.HE_MEDIUM_GRAY
import net.horizonsend.ion.common.utils.text.formatPaginatedMenu
import net.horizonsend.ion.common.utils.text.isAlphanumeric
import net.horizonsend.ion.common.utils.text.lineBreak
import net.horizonsend.ion.common.utils.text.lineBreakWithCenterText
import net.horizonsend.ion.common.utils.text.ofChildren
import net.horizonsend.ion.common.utils.text.template
import net.horizonsend.ion.server.features.cache.PlayerCache
import net.horizonsend.ion.server.features.cache.trade.EcoStations
import net.horizonsend.ion.server.features.nations.NATIONS_BALANCE
import net.horizonsend.ion.server.features.nations.region.Regions
import net.horizonsend.ion.server.features.nations.region.types.RegionCapturableStation
import net.horizonsend.ion.server.features.nations.region.types.RegionSolarSiegeZone
import net.horizonsend.ion.server.features.player.CombatTimer
import net.horizonsend.ion.server.features.space.Space
import net.horizonsend.ion.server.features.space.body.CachedMoon
import net.horizonsend.ion.server.features.space.body.CachedStar
import net.horizonsend.ion.server.features.space.spacestations.CachedNationSpaceStation
import net.horizonsend.ion.server.features.space.spacestations.CachedPlayerSpaceStation
import net.horizonsend.ion.server.features.space.spacestations.CachedSettlementSpaceStation
import net.horizonsend.ion.server.features.space.spacestations.CachedSpaceStation
import net.horizonsend.ion.server.features.space.spacestations.CachedSpaceStation.Companion.calculateCost
import net.horizonsend.ion.server.features.space.spacestations.SpaceStationCache
import net.horizonsend.ion.server.features.starship.hyperspace.HyperspaceBeaconManager
import net.horizonsend.ion.server.features.world.IonWorld.Companion.ion
import net.horizonsend.ion.server.features.world.WorldFlag
import net.horizonsend.ion.server.miscellaneous.utils.Notify
import net.horizonsend.ion.server.miscellaneous.utils.VAULT_ECO
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.distance
import net.horizonsend.ion.server.miscellaneous.utils.slPlayerId
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.empty
import net.kyori.adventure.text.Component.newline
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.AQUA
import net.kyori.adventure.text.format.NamedTextColor.GOLD
import net.kyori.adventure.text.format.NamedTextColor.GRAY
import net.kyori.adventure.text.format.NamedTextColor.GREEN
import net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.litote.kmongo.Id
import org.litote.kmongo.deleteOneById
import org.litote.kmongo.setValue
import java.time.Duration
import java.util.UUID
import kotlin.reflect.KClass

@CommandAlias("spacestation|nspacestation|nstation|sstation|station|nationspacestation")
object SpaceStationCommand : net.horizonsend.ion.server.command.SLCommand() {

	private val lastStationFormedTimeMs = mutableMapOf<UUID, Long>()
	private val STATION_FORMATION_COOLDOWN = Duration.ofMinutes(30L)

	/**
	 * Checks if the player is in station creation cooldown
	 * @return true if the player has formed a station within STATION_FORMATION_COOLDOWN false otherwise
	 * @param player the player to check the station creation cooldown
	 */
	private fun checkStationCreationCooldown(player: Player): Boolean {
		val lastTime = lastStationFormedTimeMs[player.uniqueId]
		return lastTime != null && System.currentTimeMillis() < lastTime + STATION_FORMATION_COOLDOWN.toMillis()
	}

	/**
	 * Records the time that a player created a station
	 * @param player the player to set the station creation cooldown
	 */
	private fun setStationCreationCooldown(player: Player) {
		lastStationFormedTimeMs[player.uniqueId] = System.currentTimeMillis()
	}

	private fun formatSpaceStationMessage(message: String, vararg params: Any) = template(
		text(message, GRAY),
		paramColor = AQUA,
		useQuotesAroundObjects = false,
		*params
	)

	override fun onEnable(manager: PaperCommandManager) {
		manager.commandContexts.registerContext(CachedSpaceStation::class.java) { c: BukkitCommandExecutionContext ->
			SpaceStationCache[c.popFirstArg()] ?: throw InvalidCommandArgument("No such space station")
		}

		registerAsyncCompletion(manager, "spacestations") { c ->
			val player = c.player

			SpaceStationCache.all().filter { it.hasOwnershipContext(player.slPlayerId) }.map { it.name }
		}

		manager.commandCompletions.setDefaultCompletion("spacestations", CachedSpaceStation::class.java)
	}

	private fun validateName(name: String) {
		if (!name.isAlphanumeric()) {
			throw InvalidCommandArgument("Name must be alphanumeric")
		}

		if (name.length < 3) {
			throw InvalidCommandArgument("Name cannot be less than 3 characters")
		}

		if (name.length > 40) {
			throw InvalidCommandArgument("Name cannot be more than 40 characters")
		}

		if (NationSpaceStation.all().any { it.name.equals(name, ignoreCase = true) }) {
			throw InvalidCommandArgument("A space station named $name already exists")
		}
	}

	private fun checkDimensions(world: World, x: Int, z: Int, radius: Int, cachedStation: CachedSpaceStation<*, *, *>?, sender: Player) {
		failIf(radius !in 15..10_000) { "Radius must be at least 15 and at most 10,000 blocks" }

		val y = 128 // we don't care about comparing height here

		// Check conflicts with planet orbits
		for (planet in Space.getOrbitingPlanets().filter { it.spaceWorld == world }) {
			val padding = 500
			val minDistance = planet.orbitDistance - padding - radius
			val maxDistance = planet.orbitDistance + padding + radius
			val distance = distance(x, y, z, planet.sun.location.x, y, planet.sun.location.z).toInt()

			failIf(distance in minDistance..maxDistance) {
				"This claim would be in the way of ${planet.name}'s orbit"
			}
		}

		// Check conflicts with moon orbits
		for (moon: CachedMoon in Space.getMoons().filter { it.spaceWorld == world }) {
			val padding = 500
			val minDistance = moon.orbitDistance - padding - radius
			val maxDistance = moon.orbitDistance + padding + radius
			val distance = distance(x, y, z, moon.parent.location.x, y, moon.parent.location.z).toInt()

			failIf(distance in minDistance..maxDistance) {
				"This claim would be in the way of ${moon.name}'s orbit"
			}
		}

		// Check conflict with stars
		for (star: CachedStar in Space.getStars().filter { it.spaceWorld == world }) {
			val minDistance = 256
			val distance = distance(x, y, z, star.location.x, y, star.location.z)

			failIf(distance < minDistance) {
				"This claim would be too close to the star ${star.name}"
			}
		}

		// Check conflict with beacons
		HyperspaceBeaconManager.beaconWorlds[world]?.let { beacons ->
			for (beacon in beacons) {
				val minDistance = 3000
				val distance = distance(x, y, z, beacon.spaceLocation.x, y, beacon.spaceLocation.z)

				failIf(distance < minDistance) {
					"This claim would be too close to the hyperspace beacon ${beacon.name}"
				}
			}
		}

		// Check conflicts with other stations
		// (use the database directly, in order to avoid people making
		// another one in the same location before the cache updates)
		for (other in SpaceStationCache.all()) {
			if (other.databaseId == cachedStation?.databaseId) continue
			if (other.world != world.name) continue

			var padding = 0
			if(!other.hasOwnershipContext(sender.slPlayerId)){
				padding = 200
			}

			val minDistance = other.radius + radius + padding
			val distance = distance(x, y, z, other.x, y, other.z)

			failIf(distance < minDistance) {
				"This claim would be too close to the space station ${other.name}"
			}
		}

		// Check conflicts with eco stations
		for (other in EcoStations.getAll()) {
			if (other.world != world.name) continue
			val minDistance = 5000
			val distance = distance(x, y, z, other.x, y, other.z)

			failIf(distance < minDistance) {
				"This claim would be too close to the eco station ${other.name}"
			}
		}

		// Check conflicts with capturable stations
		for (station in Regions.getAllOfInWorld<RegionCapturableStation>(world)) {
			val minDistance = maxOf((NATIONS_BALANCE.capturableStation.radius + radius), 2500)
			val distance = distance(x, y, z, station.x, y, station.z)

			failIf(distance < minDistance) {
				"This claim would be too close to the capturable station ${station.name}"
			}
		}

		// Check conflicts with solar siege zones
		for (station in Regions.getAllOfInWorld<RegionSolarSiegeZone>(world)) {
			val minDistance = maxOf((RegionSolarSiegeZone.RADIUS + radius), 3500)
			val distance = distance(x, y, z, station.x, y, station.z)

			failIf(distance < minDistance) {
				"This claim would be too close to the solar siege zone ${station.name}"
			}
		}
	}

	@Subcommand("create nation")
    fun createNation(sender: Player, name: String, radius: Int, @Optional cost: Int?) {
		if (checkStationCreationCooldown(sender)) {
			sender.userError("You must wait ${STATION_FORMATION_COOLDOWN.toMinutes() - Duration.ofMillis(System.currentTimeMillis() -
					lastStationFormedTimeMs[sender.uniqueId]!!).toMinutes()} minutes before you can claim another station")
			return
		}

		val nation: Oid<Nation> = requireNationIn(sender)
		requireNationPermission(sender, nation, SpaceStationCache.SpaceStationPermission.CREATE_STATION.nation)

		create(sender, name, radius, cost, nation, NationSpaceStation.Companion)
		setStationCreationCooldown(sender)

		Notify.chatAndEvents(formatSpaceStationMessage(
			"{0} established space station {1}, for their nation, {2}, in {3}",
			text(sender.name, LIGHT_PURPLE),
			name,
			getNationName(nation),
			sender.world.name,
		))
	}

	@Subcommand("create settlement")
    fun createSettlement(sender: Player, name: String, radius: Int, @Optional cost: Int?) {
		if (checkStationCreationCooldown(sender)) {
			sender.userError("You must wait ${STATION_FORMATION_COOLDOWN.toMinutes() - Duration.ofMillis(System.currentTimeMillis() -
					lastStationFormedTimeMs[sender.uniqueId]!!).toMinutes()} minutes before you can claim another station")
			return
		}

		val nation: Oid<Settlement> = requireSettlementIn(sender)
		requireSettlementPermission(sender, nation, SpaceStationCache.SpaceStationPermission.CREATE_STATION.settlement)

		create(sender, name, radius, cost, nation, SettlementSpaceStation.Companion)
		setStationCreationCooldown(sender)

		Notify.chatAndEvents(formatSpaceStationMessage(
			"{0} established space station {1}, for their settlement, {2}, in {3}",
			text(sender.name, LIGHT_PURPLE),
			name,
			getSettlementName(nation),
			sender.world.name,
		))
	}

	@Subcommand("create personal")
    fun createPersonal(sender: Player, name: String, radius: Int, @Optional cost: Int?) {
		if (checkStationCreationCooldown(sender)) {
			sender.userError("You must wait ${STATION_FORMATION_COOLDOWN.toMinutes() - Duration.ofMillis(System.currentTimeMillis() -
					lastStationFormedTimeMs[sender.uniqueId]!!).toMinutes()} minutes before you can claim another station")
			return
		}

		create(sender, name, radius, cost, sender.slPlayerId, PlayerSpaceStation.Companion)
		setStationCreationCooldown(sender)

		Notify.chatAndEvents(formatSpaceStationMessage(
			"{0} established the personal space station {1} in {2}",
			text(sender.name, LIGHT_PURPLE),
			name,
			sender.world.name,
		))
	}

	// Check settlement / nation permissions in their own version
	fun <Owner: DbObject>create(
		sender: Player,
		name: String,
		radius: Int,
		@Optional cost: Int?,
		owner: Id<Owner>,
		companion: SpaceStationCompanion<Owner, *>)
	{
		requireEconomyEnabled()

		failIf(!sender.hasPermission("nations.spacestation.create")) {
			"You can't create space stations here!"
		}

		failIf(!sender.world.ion.hasFlag(WorldFlag.ALLOW_SPACE_STATIONS)) { "You can't create space stations in this world!" }

		failIf(CombatTimer.isNpcCombatTagged(sender) || CombatTimer.isPvpCombatTagged(sender)) { "You are currently in combat!" }

		validateName(name)

		val location = sender.location
		val world = location.world
		val x = location.blockX
		val z = location.blockZ
		checkDimensions(world, x, z, radius, null, sender)

		val realCost = calculateCost(0, radius)
		requireMoney(sender, realCost, "create a space station")

		failIf(cost != realCost) {
			"You must acknowledge the cost of creating a space station to create one. " +
				"The cost is ${realCost.toCreditsString()}. Run the command: " +
				"/nstation $name $radius $realCost"
		}

		companion.create(
			owner,
			name,
			world.name,
			x,
			z,
			radius,
			SpaceStationCompanion.TrustLevel.MANUAL
		)

		VAULT_ECO.withdrawPlayer(sender, realCost.toDouble())
	}

	private fun requireStationOwnership(player: SLPlayerId, station: CachedSpaceStation<*, *, *>) {
		if (!station.hasOwnershipContext(player)) fail { "Your ${station.ownershipType} doesn't own ${station.name}" }
	}

	private fun requirePermission(
        player: SLPlayerId,
        station: CachedSpaceStation<*, *, *>,
        permission: SpaceStationCache.SpaceStationPermission
	) {
		if (!station.hasPermission(player, permission)) fail { "You don't have permission $permission" }
	}

	@Subcommand("abandon")
	@Description("Delete a space station")
    fun onAbandon(sender: Player, station: CachedSpaceStation<*, *, *>) = asyncCommand(sender) {
		requireStationOwnership(sender.slPlayerId, station)
		requirePermission(sender.slPlayerId, station, SpaceStationCache.SpaceStationPermission.DELETE_STATION)

		station.abandon()
		lastStationFormedTimeMs[sender.uniqueId] = 0L

		Notify.chatAndEvents(formatSpaceStationMessage(
			"{0} {1} abandoned space station {2}",
			text(station.ownershipType, GRAY),
			text(station.ownerName, LIGHT_PURPLE),
			station.name
		))
	}

	@Subcommand("resize")
	@Description("Resize the station")
    fun onResize(sender: Player, station: CachedSpaceStation<*, *, *>, newRadius: Int, @Optional cost: Int?) {
		requireEconomyEnabled()

		requireStationOwnership(sender.slPlayerId, station)
		requirePermission(sender.slPlayerId, station, SpaceStationCache.SpaceStationPermission.MANAGE_STATION)
		val stationName = station.name

		val world = Bukkit.getWorld(station.world) ?: fail { "Could not find station world; please contact staff" }
		val x = station.x
		val z = station.z
		checkDimensions(world, x, z, newRadius, station, sender)

		val realCost = calculateCost(station.radius, newRadius)
		requireMoney(sender, realCost, "create a space station")

		if (newRadius < station.radius) {
			sender.alert("WARNING: Shrinking a station will not refund its initial cost!")
		}

		failIf(cost != realCost) {
			"You must acknowledge the cost of resizing a space station to resize one. " +
				"The cost is ${realCost.toCreditsString()}. Run the command: " +
				"/nstation resize ${station.name} $newRadius $realCost"
		}

		station.changeRadius(newRadius)

		VAULT_ECO.withdrawPlayer(sender, realCost.toDouble())

		sender.sendMessage(formatSpaceStationMessage(
			"Resized {0} to {1}",
			stationName,
			newRadius
		))
	}

	@Subcommand("set trustlevel")
	@CommandCompletion("MANUAL|NATION|ALLY")
	@Description("Change the setting for can build in the station")
    fun onSetTrustLevel(sender: Player, station: CachedSpaceStation<*, *, *>, trustLevel: SpaceStationCompanion.TrustLevel) {
		requireStationOwnership(sender.slPlayerId, station)
		val stationName = station.name
		failIf(station.trustLevel == trustLevel) { "$stationName's trust level is already $trustLevel" }

		station.changeTrustLevel(trustLevel)

		sender.sendMessage(formatSpaceStationMessage(
			"Set trust level of {0} to {1}",
			stationName,
			trustLevel,
		))
	}

	/**
	 * Lists all stations that a player has access to
	 * @param sender the player to check station access for
	 * @param currentPage the current station to look at
	 */
	@Subcommand("list")
	fun onList(sender: Player, @Optional currentPage: Int?) {
		val ownedStations = getOwnedStationList(sender, Any::class)
		val trustedStations = getTrustedStationList(sender, Any::class)
		sender.sendMessage(formatStationList(ownedStations, trustedStations, currentPage ?: 1))
	}

	/**
	 * Lists all stations that a player, or their settlement or nation, explicitly owns
	 * @param sender the player to check station access for
	 * @param currentPage the current station to look at
	 */
	@Subcommand("list owned")
	fun onListOwned(sender: Player, @Optional currentPage: Int?) {
		val ownedStations = getOwnedStationList(sender, Any::class)
		sender.sendMessage(formatStationList(ownedStations, listOf(), currentPage ?: 1, " owned"))
	}

	/**
	 * Lists all stations that a player explicitly owns
	 * @param sender the player to check station access for
	 * @param currentPage the current station to look at
	 */
	@Subcommand("list owned player")
	fun onListOwnedPlayer(sender: Player, @Optional currentPage: Int?) {
		val ownedStations = getOwnedStationList(sender, CachedPlayerSpaceStation::class)
		sender.sendMessage(formatStationList(ownedStations, listOf(), currentPage ?: 1, " owned player"))
	}

	/**
	 * Lists all stations that a player's settlement explicitly owns
	 * @param sender the player to check station access for
	 * @param currentPage the current station to look at
	 */
	@Subcommand("list owned settlement")
	fun onListOwnedSettlement(sender: Player, @Optional currentPage: Int?) {
		val ownedStations = getOwnedStationList(sender, CachedSettlementSpaceStation::class)
		sender.sendMessage(formatStationList(ownedStations, listOf(), currentPage ?: 1, " owned settlement"))
	}

	/**
	 * Lists all stations that a player's nation explicitly owns
	 * @param sender the player to check station access for
	 * @param currentPage the current station to look at
	 */
	@Subcommand("list owned nation")
	fun onListOwnedNation(sender: Player, @Optional currentPage: Int?) {
		val ownedStations = getOwnedStationList(sender, CachedNationSpaceStation::class)
		sender.sendMessage(formatStationList(ownedStations, listOf(), currentPage ?: 1, " owned nation"))
	}

	/**
	 * Lists all stations that a player, or their settlement or nation, is trusted to
	 * @param sender the player to check station access for
	 * @param currentPage the current station to look at
	 */
	@Subcommand("list trusted")
	fun onListTrusted(sender: Player, @Optional currentPage: Int?) {
		val trustedStations = getTrustedStationList(sender, Any::class)
		sender.sendMessage(formatStationList(listOf(), trustedStations, currentPage ?: 1, " trusted"))
	}

	/**
	 * Lists all stations that a player is trusted to
	 * @param sender the player to check station access for
	 * @param currentPage the current station to look at
	 */
	@Subcommand("list trusted player")
	fun onListTrustedPlayer(sender: Player, @Optional currentPage: Int?) {
		val trustedStations = getTrustedStationList(sender, CachedPlayerSpaceStation::class)
		sender.sendMessage(formatStationList(listOf(), trustedStations, currentPage ?: 1, " trusted player"))
	}

	/**
	 * Lists all stations that a player's settlement is trusted to
	 * @param sender the player to check station access for
	 * @param currentPage the current station to look at
	 */
	@Subcommand("list trusted settlement")
	fun onListTrustedSettlement(sender: Player, @Optional currentPage: Int?) {
		val trustedStations = getTrustedStationList(sender, CachedSettlementSpaceStation::class)
		sender.sendMessage(formatStationList(listOf(), trustedStations, currentPage ?: 1, " trusted settlement"))
	}

	/**
	 * Lists all stations that a player's nation is trusted to
	 * @param sender the player to check station access for
	 * @param currentPage the current station to look at
	 */
	@Subcommand("list trusted nation")
	fun onListTrustedNation(sender: Player, @Optional currentPage: Int?) {
		val trustedStations = getTrustedStationList(sender, CachedNationSpaceStation::class)
		sender.sendMessage(formatStationList(listOf(), trustedStations, currentPage ?: 1, " trusted nation"))
	}

	/**
	 * Formats a paginated menu containing space station data
	 * @param ownedStations stations that are owned by the player
	 * @param trustedStations stations that are trusted to the player
	 * @param currentPage the current page of the paginated menu
	 * @param subcommand the subcommand that the user used
	 */
	private fun formatStationList(
		ownedStations: List<CachedSpaceStation<*, *, *>>,
		trustedStations: List<CachedSpaceStation<*, *, *>>,
		currentPage: Int,
		subcommand: String = ""
	): Component {
		val header = lineBreakWithCenterText(text("Space Stations With Access"), 6)
		val body = formatPaginatedMenu(
			ownedStations.count() + trustedStations.count(),
			"/spacestation list$subcommand",
			currentPage
		) { page ->
			val combinedStations = ownedStations + trustedStations
			val station = combinedStations[page]
			val x = station.x
			val z = station.z

			template(
				text("{0} at {1} {2} {3} in {4}{5}{6}"),
				station.name,
				x, z,
				template(text("with radius {0}", GREEN), station.radius),
				station.world,
				template(text(" owned by {0}", AQUA), station.ownerName),
				if (station in trustedStations) text(" (Trusted)", GOLD) else empty()
			)
		}

		return ofChildren(header, newline(), body)
	}

	/**
	 * Gets the stations that the player (or their settlement/nation) owns
	 * @param sender the player to check
	 * @param clazz the type of space station to check for (Any for default)
	 */
	private fun <T : Any> getOwnedStationList(sender: Player, clazz: KClass<T>): List<CachedSpaceStation<*, *, *>> {
		return SpaceStationCache.all().filter { station ->
			station.hasOwnershipContext(sender.slPlayerId) &&
			if (clazz != Any::class) station::class == clazz else true
		}
	}

	/**
	 * Gets the stations that the player (or their settlement/nation) is trusted to
	 * @param sender the player to check
	 * @param clazz the type of space station to check for (Any for default)
	 */
	private fun <T : Any> getTrustedStationList(sender: Player, clazz: KClass<T>): List<CachedSpaceStation<*, *, *>> {
		val playerData = PlayerCache.getIfOnline(sender)

		return SpaceStationCache.all().filter { station ->
			(station.trustedPlayers.contains(sender.slPlayerId) ||
					station.trustedSettlements.contains(playerData?.settlementOid) ||
					station.trustedNations.contains(playerData?.nationOid)) &&
					if (clazz != Any::class) station::class == clazz else true
		}
	}

	@Subcommand("trusted list")
    fun onTrustedList(sender: Player, station: CachedSpaceStation<*, *, *>) {
		requireStationOwnership(sender.slPlayerId, station)
		val stationName: String = station.name

		val trustedPlayers: String = station.trustedPlayers.map(::getPlayerName).sorted().joinToString()
		sender.sendRichMessage("<Gold>Trusted players in $stationName: <aqua>$trustedPlayers")

		val trustedSettlements: String = station.trustedSettlements.map(::getSettlementName).sorted().joinToString()
		sender.sendRichMessage("<Gold>Trusted settlements in $stationName: <aqua>$trustedSettlements")

		val trustedNations: String = station.trustedNations.map(::getNationName).sorted().joinToString()
		sender.sendRichMessage("<Gold>Trusted nations in $stationName: <aqua>$trustedNations")
	}

	@Subcommand("trusted add player")
	@Description("Give a player build access to the station")
	@CommandCompletion("@players")
    fun onTrustedAddPlayer(sender: Player, station: CachedSpaceStation<*, *, *>, player: String) = asyncCommand(sender) {
		requireStationOwnership(sender.slPlayerId, station)
		val stationName = station.name

		val playerId: SLPlayerId = resolveOfflinePlayer(player).slPlayerId
		val playerName: String = getPlayerName(playerId)

		requirePermission(sender.slPlayerId, station, SpaceStationCache.SpaceStationPermission.MANAGE_STATION)

		failIf(station.trustedPlayers.contains(playerId)) {
			"$playerName is already trusted in $stationName"
		}

		station.trustPlayer(playerId)

		sender.sendMessage(formatSpaceStationMessage("Added {0} to {1}", playerName, stationName))

		Notify.playerCrossServer(
			playerId.uuid,
			formatSpaceStationMessage("You were added to the space station {0} by {1}", stationName, sender.name)
		)
	}

	@Subcommand("trusted add settlement")
	@Description("Give a settlement build access to the station")
	@CommandCompletion("@settlements")
    fun onTrustedAddSettlement(sender: Player, station: CachedSpaceStation<*, *, *>, settlement: String) = asyncCommand(sender) {
		requireStationOwnership(sender.slPlayerId, station)
		val stationName = station.name

		val settlementId: Oid<Settlement> = SettlementCache.getByName(settlement) ?: fail {
			"Settlement $settlement not found"
		}

		requirePermission(sender.slPlayerId, station, SpaceStationCache.SpaceStationPermission.MANAGE_STATION)

		failIf(station.trustedSettlements.contains(settlementId)) {
			"$settlement is already trusted in $stationName"
		}

		station.trustSettlement(settlementId)

		sender.sendMessage(formatSpaceStationMessage("Added {0} to {1}", settlement, stationName))

		Notify.settlementCrossServer(
			settlementId,
			formatSpaceStationMessage("Your settlement was added to the space station {0} by {1}", stationName, sender.name)
		)
	}

	@Subcommand("trusted add nation")
	@Description("Give a nation build access to the station")
	@CommandCompletion("@nations")
    fun onTrustedAddNation(sender: Player, station: CachedSpaceStation<*, *, *>, nation: String) = asyncCommand(sender) {
		requireStationOwnership(sender.slPlayerId, station)
		val stationName = station.name

		val nationId: Oid<Nation> = NationCache.getByName(nation) ?: fail {
			"Nation $nation not found"
		}

		requirePermission(sender.slPlayerId, station, SpaceStationCache.SpaceStationPermission.MANAGE_STATION)

		failIf(station.trustedNations.contains(nationId)) {
			"$nation is already trusted in $stationName"
		}

		station.trustNation(nationId)

		sender.sendMessage(formatSpaceStationMessage("Added {0} to {1}", nation, stationName))

		Notify.nationCrossServer(
			nationId,
			formatSpaceStationMessage("Your settlement was added to the space station {0} by {1}", stationName, sender.name)
		)
	}

	@Subcommand("trusted remove player")
	@Description("Revoke a player's build access to the station")
	@CommandCompletion("@players")
    fun onTrustedRemovePlayer(sender: Player, station: CachedSpaceStation<*, *, *>, player: String) = asyncCommand(sender) {
		requireStationOwnership(sender.slPlayerId, station)
		val stationName = station.name

		val playerId: SLPlayerId = resolveOfflinePlayer(player).slPlayerId
		val playerName: String = getPlayerName(playerId)

		requirePermission(sender.slPlayerId, station, SpaceStationCache.SpaceStationPermission.MANAGE_STATION)

		failIf(!station.trustedPlayers.contains(playerId)) {
			"$playerName isn't trusted in $stationName"
		}

		station.unTrustPlayer(playerId)

		sender.sendMessage(formatSpaceStationMessage("Removed {0} from {1}", playerName, stationName))

		Notify.playerCrossServer(
			playerId.uuid,
			formatSpaceStationMessage("You were removed from station the space station {0} by {1}", stationName, sender.name)
		)
	}


	@Subcommand("trusted remove settlement")
	@Description("Give a settlement build access to the station")
	@CommandCompletion("@settlements")
    fun onTrustedRemoveSettlement(sender: Player, station: CachedSpaceStation<*, *, *>, settlement: String) = asyncCommand(sender) {
		requireStationOwnership(sender.slPlayerId, station)
		val stationName = station.name

		val settlementId: Oid<Settlement> = SettlementCache.getByName(settlement) ?: fail {
			"Settlement $settlement not found"
		}

		requirePermission(sender.slPlayerId, station, SpaceStationCache.SpaceStationPermission.MANAGE_STATION)

		failIf(!station.trustedSettlements.contains(settlementId)) {
			"$settlement isn't trusted in $stationName"
		}

		station.unTrustSettlement(settlementId)

		sender.sendMessage(formatSpaceStationMessage("Removed {0} from {1}", settlement, stationName))

		Notify.settlementCrossServer(
			settlementId,
			formatSpaceStationMessage("Your settlement was removed from station the space station {0} by {1}", stationName, sender.name)
		)
	}

	@Subcommand("trusted remove nation")
	@Description("Give a nation build access to the station")
	@CommandCompletion("@nations")
    fun onTrustedRemoveNation(sender: Player, station: CachedSpaceStation<*, *, *>, nation: String) = asyncCommand(sender) {
		requireStationOwnership(sender.slPlayerId, station)
		val stationName = station.name

		val nationId: Oid<Nation> = NationCache.getByName(nation) ?: fail {
			"Nation $nation not found"
		}

		requirePermission(sender.slPlayerId, station, SpaceStationCache.SpaceStationPermission.MANAGE_STATION)

		failIf(!station.trustedNations.contains(nationId)) {
			"$nation isn't trusted in $stationName"
		}

		station.unTrustNation(nationId)

		sender.sendMessage(formatSpaceStationMessage("Removed {0} from {1}", nation, stationName))

		Notify.nationCrossServer(
			nationId,
			formatSpaceStationMessage("Your settlement was removed from station the space station {0} by {1}", stationName, sender.name)
		)
	}

	@Subcommand("set name")
	@Description("Rename the station")
	@CommandCompletion("@nothing")
    fun onRename(sender: Player, station: CachedSpaceStation<*, *, *>, newName: String) = asyncCommand(sender) {
		requireStationOwnership(sender.slPlayerId, station)

		val oldName = station.name
		validateName(newName)
		station.rename(newName)

		sender.sendMessage(formatSpaceStationMessage("Renamed {0} to {1}", oldName, newName))
		Notify.chatAndGlobal(formatSpaceStationMessage("Space station {0} has been renamed to {1} by {2}", oldName, newName, sender.name))
	}

	@Subcommand("info")
	fun onInfo(sender: CommandSender, station: CachedSpaceStation<*, *, *>) {
		val colon = text(": ", HE_DARK_GRAY)

		sender.sendMessage(ofChildren(
			lineBreakWithCenterText(text(station.name, AQUA)), newline(),
			text("Owner", HE_MEDIUM_GRAY), colon, text(station.ownerName, AQUA), newline(),
			text("Radius", HE_MEDIUM_GRAY), colon, text(station.radius, AQUA), newline(),
			text("World", HE_MEDIUM_GRAY), colon, text(station.world, AQUA), newline(),
			text("Location", HE_MEDIUM_GRAY), colon, text(station.x, AQUA), text(", ", HE_MEDIUM_GRAY), text(station.z, AQUA), newline(),
			lineBreak(36)
		))
	}

	@Subcommand("transfer")
	@CommandCompletion("@spacestations personal|settlement|nation @nothing")
	fun onTransfer(sender: Player, station: CachedSpaceStation<*, *, *>, destination: String, @Optional confirm: String?) {
		requireStationOwnership(sender.slPlayerId, station)
		requirePermission(sender.slPlayerId, station, SpaceStationCache.SpaceStationPermission.DELETE_STATION)

		failIf(confirm != "confirm") {
			"To transfer the station, confirm this choice. Use /spacestation transfer ${station.name} $destination confirm."
		}

		when (destination.lowercase()) {
			"personal" -> transferPersonal(sender, station)
			"settlement" -> transferSettlement(sender, station)
			"nation" -> transferNation(sender, station)
			else -> fail { "The destination must be personal, settlement, or nation!" }
		}
	}

	private fun transferPersonal(sender: Player, station: CachedSpaceStation<*, *, *>) {
		if (station.companion == PlayerSpaceStation.Companion) fail { "${station.name} is already a personal space station!" }

		station.companion.col.deleteOneById(station.databaseId)

		val id = PlayerSpaceStation.create(
			sender.slPlayerId,
			station.name,
			station.world,
			station.x,
			station.z,
			station.radius,
			SpaceStationCompanion.TrustLevel.MANUAL
		)

		PlayerSpaceStation.updateById(id, setValue(PlayerSpaceStation::trustedPlayers, station.trustedPlayers))
		PlayerSpaceStation.updateById(id, setValue(PlayerSpaceStation::trustedSettlements, station.trustedSettlements))
		PlayerSpaceStation.updateById(id, setValue(PlayerSpaceStation::trustedNations, station.trustedNations))
		PlayerSpaceStation.updateById(id, setValue(PlayerSpaceStation::trustLevel, station.trustLevel))

		@Suppress("UNCHECKED_CAST")
		Notify.chatAndEvents(when (station.companion) {
			NationSpaceStation.Companion -> formatSpaceStationMessage(
				"{0} of {1} transferred the nation space station {2} in {3} to themself",
				text(sender.name, LIGHT_PURPLE),
				getNationName(station.owner as Oid<Nation>),
				station.name,
				station.world,
			)

			SettlementSpaceStation.Companion -> formatSpaceStationMessage(
				"{0} of {1} transferred the settlement space station {2} in {3} to themself",
				text(sender.name, LIGHT_PURPLE),
				getSettlementName(station.owner as Oid<Settlement>),
				station.name,
				station.world,
			)

			else -> throw NotImplementedError()
		})
	}

	private fun transferSettlement(sender: Player, station: CachedSpaceStation<*, *, *>) {
		if (station.companion == SettlementSpaceStation.Companion) fail { "${station.name} is already a settlement space station!" }

		val settlement = requireSettlementIn(sender)
		requireSettlementPermission(sender, settlement, SpaceStationCache.SpaceStationPermission.CREATE_STATION.settlement)

		station.companion.col.deleteOneById(station.databaseId)

		val id = SettlementSpaceStation.create(
			settlement,
			station.name,
			station.world,
			station.x,
			station.z,
			station.radius,
			SpaceStationCompanion.TrustLevel.MANUAL
		)

		SettlementSpaceStation.updateById(id, setValue(SettlementSpaceStation::trustedPlayers, station.trustedPlayers))
		SettlementSpaceStation.updateById(id, setValue(SettlementSpaceStation::trustedSettlements, station.trustedSettlements))
		SettlementSpaceStation.updateById(id, setValue(SettlementSpaceStation::trustedNations, station.trustedNations))
		SettlementSpaceStation.updateById(id, setValue(SettlementSpaceStation::trustLevel, station.trustLevel))

		@Suppress("UNCHECKED_CAST")
		Notify.chatAndEvents(when (station.companion) {
			NationSpaceStation.Companion -> formatSpaceStationMessage(
				"{0} of {1} transferred the nation space station {2} in {3} to the settlement {4}",
				text(sender.name, LIGHT_PURPLE),
				getNationName(station.owner as Oid<Nation>),
				station.name,
				station.world,
				getSettlementName(settlement)
			)

			PlayerSpaceStation.Companion -> formatSpaceStationMessage(
				"{0} transferred their personal space station {1} in {2} to the settlement {3}",
				text(sender.name, LIGHT_PURPLE),
				station.name,
				station.world,
				getSettlementName(settlement)
			)

			else -> throw NotImplementedError()
		})
	}

	private fun transferNation(sender: Player, station: CachedSpaceStation<*, *, *>) {
		if (station.companion == NationSpaceStation.Companion) fail { "${station.name} is already a nation space station!" }

		val nation = requireNationIn(sender)
		requireNationPermission(sender, nation, SpaceStationCache.SpaceStationPermission.CREATE_STATION.nation)

		station.companion.col.deleteOneById(station.databaseId)

		val id = NationSpaceStation.create(
			nation,
			station.name,
			station.world,
			station.x,
			station.z,
			station.radius,
			SpaceStationCompanion.TrustLevel.MANUAL
		)

		NationSpaceStation.updateById(id, setValue(NationSpaceStation::trustedPlayers, station.trustedPlayers))
		NationSpaceStation.updateById(id, setValue(NationSpaceStation::trustedSettlements, station.trustedSettlements))
		NationSpaceStation.updateById(id, setValue(NationSpaceStation::trustedNations, station.trustedNations))
		NationSpaceStation.updateById(id, setValue(NationSpaceStation::trustLevel, station.trustLevel))

		@Suppress("UNCHECKED_CAST")
		Notify.chatAndEvents(when (station.companion) {
			SettlementSpaceStation.Companion -> formatSpaceStationMessage(
				"{0} of {1} transferred the settlement space station {2} in {3} to the nation {4}",
				text(sender.name, LIGHT_PURPLE),
				getSettlementName(station.owner as Oid<Settlement>),
				station.name,
				station.world,
				getNationName(nation)
			)

			PlayerSpaceStation.Companion -> formatSpaceStationMessage(
				"{0} transferred their personal space station {1} in {2} to the nation {3}",
				text(sender.name, LIGHT_PURPLE),
				station.name,
				station.world,
				getNationName(nation)
			)

			else -> throw NotImplementedError()
		})
	}
}

