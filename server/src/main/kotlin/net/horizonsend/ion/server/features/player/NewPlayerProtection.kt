package net.horizonsend.ion.server.features.player

import co.aikar.commands.PaperCommandManager
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import net.horizonsend.ion.common.database.cache.nations.NationCache
import net.horizonsend.ion.common.database.cache.nations.SettlementCache
import net.horizonsend.ion.common.database.schema.misc.SLPlayer
import net.horizonsend.ion.common.database.uuid
import net.horizonsend.ion.common.extensions.alertAction
import net.horizonsend.ion.common.extensions.success
import net.horizonsend.ion.common.extensions.userError
import net.horizonsend.ion.common.utils.luckPerms
import net.horizonsend.ion.server.configuration.ConfigurationFiles
import net.horizonsend.ion.server.features.cache.PlayerCache
import net.horizonsend.ion.server.features.progression.PlayerXPLevelCache
import net.horizonsend.ion.server.features.world.IonWorld.Companion.hasFlag
import net.horizonsend.ion.server.features.world.WorldFlag
import net.horizonsend.ion.server.miscellaneous.utils.Tasks
import net.horizonsend.ion.server.miscellaneous.utils.slPlayerId
import net.luckperms.api.node.NodeEqualityPredicate
import net.luckperms.api.node.types.PermissionNode
import net.luckperms.api.node.types.SuffixNode
import net.luckperms.api.util.Tristate
import org.bukkit.Bukkit
import org.bukkit.Statistic.PLAY_ONE_MINUTE
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import kotlin.math.pow

@CommandAlias("removeprotection")
object NewPlayerProtection : net.horizonsend.ion.server.command.SLCommand(), Listener {
	private val lpUserManager = luckPerms.userManager

	private val oldProtectionIndicator = SuffixNode.builder("&6★&r", 0).build()
	private val oldAlternateProtectionIndicator = SuffixNode.builder(" &6★ &r", 0).build()
	private val newerDldAlternateProtectionIndicator = SuffixNode.builder("<gold>★<reset>", 0).build()

	private val protectionIndicator = SuffixNode.builder("<gold> ★<reset>", 0).build()
	private val removeProtectionPermission = PermissionNode.builder("ion.core.protection.removed").build()

	override fun onEnable(manager: PaperCommandManager) {
		Tasks.syncRepeat(5 * 20 * 60, 5 * 20 * 60) {
			for (player in Bukkit.getOnlinePlayers())
				player.updateProtection()
		}
	}

	@Default
	@Suppress("unused") // Command
	fun onRemoveProtection(sender: Player) {
		onRemoveProtection(sender, sender.name)
	}

	@CommandPermission("ion.core.protection.removeothers")
	@Subcommand("other")
	fun onRemoveProtection(sender: Player, target: String) {
		val id = SLPlayer[target]?._id ?: fail { "Unable to remove new player protection from $target, the player does not exist." }
		lpUserManager.modifyUser(id.uuid) {
			it.data().run {
				add(removeProtectionPermission)
				remove(protectionIndicator)
			}
		}.thenAccept { t ->
			sender.success("Removed new player protection from $target.")
		}
	}

	@CommandPermission("ion.core.protection.giveothers")
	@CommandAlias("giveprotection")
	fun onGiveProtection(sender: Player, target: String) {
		val lpUser = lpUserManager.getUser(target)

		if (lpUser == null) {
			sender.userError(
				"Unable to give new player protection to $target, the player does not exist."
			)
			return
		}

		lpUser.data().run {
			remove(removeProtectionPermission)
			add(protectionIndicator)
		}

		lpUserManager.saveUser(lpUser)

		sender.success("Gave new player protection to $target.")
	}

	fun Player.updateProtection() {
		if (!ConfigurationFiles.legacySettings().master) return

		val lpUser = lpUserManager.getUser(uniqueId)!!

		lpUser.data().run {
			if (contains(oldProtectionIndicator, NodeEqualityPredicate.IGNORE_EXPIRY_TIME) == Tristate.TRUE) {
				remove(oldProtectionIndicator)
			}

			if (contains(oldAlternateProtectionIndicator, NodeEqualityPredicate.IGNORE_EXPIRY_TIME) == Tristate.TRUE) {
				remove(oldAlternateProtectionIndicator)
			}

			if (contains(newerDldAlternateProtectionIndicator, NodeEqualityPredicate.IGNORE_EXPIRY_TIME) == Tristate.TRUE) {
				remove(newerDldAlternateProtectionIndicator)
			}

			if (hasProtection()) {
				add(protectionIndicator)
			} else {
				remove(protectionIndicator)
			}
		}

		lpUserManager.saveUser(lpUser)
	}

	fun Player.hasProtection(): Boolean {
		if (hasMetadata("NPC")) return false

		val player = PlayerCache[this]
		val playerLevel = PlayerXPLevelCache[this]

		if (hasPermission("ion.core.protection.removed")) return false // If protection has been removed by staff.
		if (player.nationOid?.let { SettlementCache[NationCache[it].capital].leader == slPlayerId } == true) return false // If owns
		return getStatistic(PLAY_ONE_MINUTE) / 72000.0 <= 48.0.pow((100.0 - playerLevel.level) * 0.01) // If playtime is less then 48^((100-x)*0.001) hours
	}

//	fun UUID.hasProtection(): CompletableFuture<Boolean?> {
//		val future = CompletableFuture<Boolean?>()
//
//		Tasks.async {
//			val player = SLPlayer[this]
//
//			if (player == null) {
//				future.complete(null)
//				return@async
//			}
//
//			val playerLevel = player.level
//
//			val protectionRemoved = luckPerms.userManager.loadUser(this).get().distinctNodes.filterIsInstance<PermissionNode>().any {
//				it.permission == "ion.core.protection.removed"
//			}
//
//			if (protectionRemoved) {
//				future.complete(false)
//				return@async
//			}
//
//			if (player.nation?.let { SettlementCache[NationCache[it].capital].leader == this.slPlayerId } == true) {
//				future.complete(false)
//				return@async
//			}
//
//			val offlinePlayer = findOfflinePlayer(this)
//
//			if (offlinePlayer == null) {
//				future.complete(null)
//				return@async
//			}
//
//			val playTime = offlinePlayer.getStatistic(PLAY_ONE_MINUTE) / 72000.0 <= 48.0.pow((100.0 - playerLevel) * 0.01) // If playtime is less then 48^((100-x)*0.001) hours
//			future.complete(playTime)
//		}
//
//		return future
//	}

	@EventHandler
	fun onPlayerHurtNoob(event: EntityDamageByEntityEvent) {
		if (event.entity !is Player || event.damager !is Player) return

		if ((event.entity as Player).hasProtection() && !event.entity.world.hasFlag(WorldFlag.ARENA)) event.damager.alertAction(
			"The player you are attacking has new player protection!\n" +
				"Attacking them for any reason other than self defense is against the rules"
		)
	}
}
