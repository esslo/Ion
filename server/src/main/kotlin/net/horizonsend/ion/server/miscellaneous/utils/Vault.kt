package net.horizonsend.ion.server.miscellaneous.utils

import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer

/** Registered vault economy service */
val VAULT_ECO: Economy = Bukkit.getServer().servicesManager.getRegistration(Economy::class.java)!!.provider

fun OfflinePlayer.getMoneyBalance(): Double = VAULT_ECO.getBalance(this)

fun OfflinePlayer.hasEnoughMoney(amount: Number): Boolean = VAULT_ECO.has(this, amount.toDouble())

fun OfflinePlayer.depositMoney(amount: Number) = VAULT_ECO.depositPlayer(this, amount.toDouble())

fun OfflinePlayer.withdrawMoney(amount: Number) = VAULT_ECO.withdrawPlayer(this, amount.toDouble())
