package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ClientSide
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.core.menus.Menu
import com.github.kennarddh.mindustry.genesis.core.menus.Menus
import com.github.kennarddh.mindustry.genesis.standard.extensions.infoMessage
import com.github.kennarddh.mindustry.toast.common.*
import com.github.kennarddh.mindustry.toast.common.database.tables.*
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import com.github.kennarddh.mindustry.toast.core.commons.getMindustryUserServerData
import com.password4j.Argon2Function
import com.password4j.Password
import com.password4j.SecureString
import com.password4j.types.Argon2
import kotlinx.coroutines.launch
import mindustry.game.EventType
import mindustry.gen.Player
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class UserAccountHandler : Handler() {
    private val passwordHashFunctionInstance = Argon2Function.getInstance(
        16384, 10, 4, 64, Argon2.ID
    )

    private val usidHashFunctionInstance = Argon2Function.getInstance(
        16384, 10, 4, 64, Argon2.ID
    )

    private val usernameRegex = """[a-zA-Z0-9_]{1,50}""".toRegex()
    private val passwordRegex = """[a-zA-Z0-9 !@#$%^&*()-_+=\[\]{};:'",.<>/?|`~]{8,50}""".toRegex()

    private val registerMenu = Menus(
        mapOf
            (
            "username" to Menu("Register 1/3", "Username", 50),
            "password" to Menu("Register 2/3", "Password", 50),
            "confirmPassword" to Menu("Register 3/3", "Confirm Password", 50)
        )
    )

    private val loginMenu = Menus(
        mapOf
            (
            "username" to Menu("Login 1/2", "Username", 50),
            "password" to Menu("Login 2/2", "Password", 50),
        )
    )

    @EventHandler
    fun onPlayerJoin(event: EventType.PlayerJoin) {
        val player = event.player
        val ip = player.con.address.packIP()
        val name = player.name

        CoroutineScopes.Main.launch {
            newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
                val mindustryUser = MindustryUser.insertIfNotExistAndGet({
                    MindustryUser.mindustryUUID eq player.uuid()
                }) {
                    it[this.mindustryUUID] = player.uuid()
                }

                val ipAddress = IPAddresses.insertIfNotExistAndGet({
                    IPAddresses.ipAddress eq ip
                }) {
                    it[this.ipAddress] = ip
                }

                val mindustryName = MindustryNames.insertIfNotExistAndGet({
                    MindustryNames.name eq name
                }) {
                    it[this.name] = name
                    it[this.strippedName] = player.plainName()
                }

                MindustryUserIPAddresses.insertIfNotExistAndGet({
                    MindustryUserIPAddresses.mindustryUserID eq mindustryUser[MindustryUser.id]
                    MindustryUserIPAddresses.ipAddressID eq ipAddress[IPAddresses.id]
                }) {
                    it[this.mindustryUserID] = mindustryUser[MindustryUser.id]
                    it[this.ipAddressID] = ipAddress[IPAddresses.id]
                }

                MindustryUserMindustryNames.insertIfNotExistAndGet({
                    MindustryUserMindustryNames.mindustryUserID eq mindustryUser[MindustryUser.id]
                    MindustryUserMindustryNames.mindustryNameID eq mindustryName[MindustryNames.id]
                }) {
                    it[this.mindustryUserID] = mindustryUser[MindustryUser.id]
                    it[this.mindustryNameID] = mindustryName[MindustryNames.id]
                }

                val mindustryUserServerDataCanBeNull = player.getMindustryUserServerData()

                val mindustryUserServerData = if (mindustryUserServerDataCanBeNull == null) {
                    // New user server data
                    Logger.info("USID: ${player.usid()}")

                    val hashedUSID = Password.hash(SecureString(player.usid().toCharArray()))
                        .addRandomSalt(64)
                        .with(usidHashFunctionInstance)

                    Logger.info("HASHED USID: ${hashedUSID.result.length}, ${hashedUSID.result}")

                    MindustryUserServerData.insert {
                        it[this.mindustryUserID] = mindustryUser[MindustryUser.id]
                        it[this.server] = ToastVars.server
                        it[this.mindustryUSID] = hashedUSID.result
                    }.resultedValues!!.first()
                } else {
                    val storedUSID = mindustryUserServerDataCanBeNull[MindustryUserServerData.mindustryUSID]

                    val mindustryUserServerData =
                        if (!Password.check(player.usid(), storedUSID).with(usidHashFunctionInstance)) {
                            // USID is not same as stored usid, Invalidate login.
                            player.infoMessage(
                                "[#ff0000]Your login was invalidated. Either there is server's ip update or your account got stolen by other player. If this happen too often without any announcements, likely that your user was stolen."
                            )

                            val hashedUSID =
                                Password.hash(SecureString(player.usid().toCharArray()))
                                    .addRandomSalt(64)
                                    .with(usidHashFunctionInstance)

                            MindustryUserServerData.update({
                                MindustryUserServerData.mindustryUserID eq mindustryUser[MindustryUser.id]
                                MindustryUserServerData.server eq ToastVars.server
                            }) {
                                it[this.mindustryUSID] = hashedUSID.result
                                it[this.userID] = null
                            }

                            // Return updated user server data
                            player.getMindustryUserServerData()!!
                        } else {
                            mindustryUserServerDataCanBeNull
                        }

                    mindustryUserServerData
                }

                // TODO: Check for kick and ban

                val userID = mindustryUserServerData[MindustryUserServerData.userID]

                if (userID != null) {
                    val user = Users.selectOne {
                        Users.id eq userID
                    }!!

                    if (user[Users.role] >= UserRole.Admin) {
                        player.admin = true
                    }
                }
            }
        }
    }

    @Command(["register"])
    @ClientSide
    fun register(player: Player) {
        CoroutineScopes.Main.launch {
            val mindustryUserServerData = newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
                MindustryUserServerData
                    .join(
                        MindustryUser,
                        JoinType.INNER,
                        onColumn = MindustryUserServerData.mindustryUserID,
                        otherColumn = MindustryUser.id
                    )
                    .selectOne {
                        MindustryUser.mindustryUUID eq player.uuid()
                        MindustryUserServerData.server eq ToastVars.server
                    }!!
            }

            if (mindustryUserServerData[MindustryUserServerData.userID] != null)
                return@launch player.sendMessage(
                    "[#ff0000]You are already logged in."
                )

            val output = registerMenu.open(player)
                ?: return@launch player.infoMessage(
                    "[#ff0000]Register canceled."
                )

            val username = output["username"]!!
            val password = output["password"]!!
            val confirmPassword = output["confirmPassword"]!!

            if (!username.matches(usernameRegex))
                return@launch player.infoMessage(
                    "[#ff0000]Invalid username. Username may only contains lowercase, uppercase, and numbers."
                )

            if (!password.matches(passwordRegex))
                return@launch player.infoMessage(
                    "[#ff0000]Invalid password. Password may only contains lowercase, uppercase, symbols, and numbers."
                )

            if (!confirmPassword.matches(passwordRegex))
                return@launch player.infoMessage(
                    "[#ff0000]Invalid confirm password. Confirm password may only contains lowercase, uppercase, symbols, and numbers."
                )

            if (confirmPassword != password) return@launch player.infoMessage(
                "[#ff0000]Confirm password is not same as password."
            )

            newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
                if (Users.exists { Users.username eq username })
                    return@newSuspendedTransaction player.infoMessage(
                        "[#ff0000]Your username is already taken."
                    )

                val hashedPassword = Password.hash(SecureString(password.toCharArray()))
                    .addRandomSalt(64)
                    .with(passwordHashFunctionInstance)

                Users.insert {
                    it[this.username] = username
                    it[this.password] = hashedPassword.result
                    it[this.role] = UserRole.Player
                }

                player.infoMessage(
                    "[#00ff00]Register success. Login with /login to use your account."
                )
            }
        }
    }

    @Command(["login"])
    @ClientSide
    fun login(player: Player) {
        CoroutineScopes.Main.launch {
            val mindustryUserServerData = newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
                player.getMindustryUserServerData()!!
            }

            if (mindustryUserServerData[MindustryUserServerData.userID] != null)
                return@launch player.sendMessage(
                    "[#ff0000]You are already logged in."
                )

            val output = loginMenu.open(player)
                ?: return@launch player.infoMessage(
                    "[#ff0000]Login canceled."
                )

            val username = output["username"]!!
            val password = output["password"]!!

            if (!username.matches(usernameRegex))
                return@launch player.infoMessage(
                    "[#ff0000]Invalid username. Username may only contains lowercase, uppercase, and numbers."
                )

            if (!password.matches(passwordRegex))
                return@launch player.infoMessage(
                    "[#ff0000]Invalid password. Password may only contains lowercase, uppercase, symbols, and numbers."
                )

            newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
                val user = Users.selectOne { Users.username eq username }
                    ?: return@newSuspendedTransaction player.infoMessage(
                        "[#ff0000]User not found."
                    )

                if (
                    !Password.check(password, user[Users.password])
                        .with(passwordHashFunctionInstance)
                )
                    return@newSuspendedTransaction player.infoMessage(
                        "[#ff0000]Wrong password."
                    )

                MindustryUserServerData
                    .join(
                        MindustryUser,
                        JoinType.INNER,
                        onColumn = MindustryUserServerData.mindustryUserID,
                        otherColumn = MindustryUser.id
                    )
                    .update({
                        MindustryUser.mindustryUUID eq player.uuid()
                        MindustryUserServerData.server eq ToastVars.server
                    }) {
                        it[MindustryUserServerData.userID] = user[Users.id]
                    }

                player.infoMessage(
                    "[#00ff00]Login success. You are now logged in as ${user[Users.username]}."
                )
            }
        }
    }

    @Command(["logout"])
    @ClientSide
    fun logout(player: Player) {
        CoroutineScopes.Main.launch {
            newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
                val mindustryUserServerData = player.getMindustryUserServerData()!!

                val userID = mindustryUserServerData[MindustryUserServerData.userID]
                    ?: return@newSuspendedTransaction player.infoMessage(
                        "[#ff0000]You are not logged in."
                    )

                val user = Users.selectOne { Users.id eq userID }!!

                MindustryUserServerData
                    .update({
                        MindustryUserServerData.userID eq userID
                        MindustryUserServerData.server eq ToastVars.server
                    }) {
                        it[MindustryUserServerData.userID] = null
                    }

                player.infoMessage(
                    "[#00ff00]Logout success. You are now no longer logged in as ${user[Users.username]}."
                )
            }
        }
    }
}