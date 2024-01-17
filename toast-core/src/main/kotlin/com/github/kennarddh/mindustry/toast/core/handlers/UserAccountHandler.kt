package com.github.kennarddh.mindustry.toast.core.handlers

import arc.util.Strings
import com.github.kennarddh.mindustry.toast.core.commons.*
import com.github.kennarddh.mindustry.toast.core.commons.database.tables.*
import com.github.kennarddh.mindustry.toast.core.commons.menus.Menu
import com.github.kennarddh.mindustry.toast.core.commons.menus.Menus
import com.password4j.Argon2Function
import com.password4j.Password
import com.password4j.SaltGenerator
import com.password4j.SecureString
import com.password4j.types.Argon2
import kennarddh.genesis.core.commands.annotations.ClientSide
import kennarddh.genesis.core.commands.annotations.Command
import kennarddh.genesis.core.events.annotations.EventHandler
import kennarddh.genesis.core.handlers.Handler
import kotlinx.coroutines.launch
import mindustry.game.EventType
import mindustry.gen.Player
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class UserAccountHandler : Handler() {
    private val argon2FunctionInstance = Argon2Function.getInstance(
        15360, 6, 2, 64, Argon2.ID
    )

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
            "username" to Menu("Register 1/3", "Username", 50),
            "password" to Menu("Register 2/3", "Password", 50),
        )
    )

    @EventHandler
    fun onPlayerJoin(event: EventType.PlayerJoin) {
        val player = event.player
        val ip = player.con.address.packIP()
        val name = player.name
        val strippedName = Strings.stripColors(name)

        CoroutineScopes.Main.launch {
            newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
                val mindustryUser = MindustryUser.selectAll().where {
                    MindustryUser.mindustryUUID eq player.uuid()
                }.firstOrNull()

                val mindustryUserID = if (mindustryUser == null) {
                    MindustryUser.insertAndGetId {
                        it[this.mindustryUUID] = player.uuid()
                    }
                } else {
                    mindustryUser[MindustryUser.id]
                }

                val ipAddress = IPAddresses.selectAll().where {
                    IPAddresses.ipAddress eq ip
                }.firstOrNull()

                val ipAddressID = if (ipAddress == null) {
                    IPAddresses.insertAndGetId {
                        it[this.ipAddress] = ip
                    }
                } else {
                    ipAddress[IPAddresses.id]
                }

                val mindustryName = MindustryNames.selectOne {
                    MindustryNames.name eq name
                }

                val mindustryNameID = if (mindustryName == null) {
                    MindustryNames.insertAndGetId {
                        it[this.name] = name
                        it[this.strippedName] = strippedName
                    }
                } else {
                    mindustryName[MindustryNames.id]
                }

                if (!MindustryUserIPAddresses.exists {
                        MindustryUserIPAddresses.mindustryUserID eq mindustryUserID
                        MindustryUserIPAddresses.ipAddressID eq ipAddressID
                    }) {
                    MindustryUserIPAddresses.insertIgnore {
                        it[this.mindustryUserID] = mindustryUserID
                        it[this.ipAddressID] = ipAddressID
                    }
                }

                if (!MindustryUserMindustryNames.exists {
                        MindustryUserMindustryNames.mindustryUserID eq mindustryUserID
                        MindustryUserMindustryNames.mindustryNameID eq mindustryNameID
                    }) {
                    MindustryUserMindustryNames.insertIgnore {
                        it[this.mindustryUserID] = mindustryUserID
                        it[this.mindustryNameID] = mindustryNameID
                    }
                }

                if (!MindustryUserServerData.exists { MindustryUserServerData.mindustryUserID eq mindustryUserID }) {
                    MindustryUserServerData.insertIgnore {
                        it[this.mindustryUserID] = mindustryUserID
                        it[this.server] = Server.Survival
                    }
                }

                // TODO: Check for kick and ban

                val userID = mindustryUser?.get(MindustryUser.userID)

                if (userID != null) {
                    val user = Users.selectAll().where {
                        Users.id eq userID
                    }.first()

                    // TODO: Handle more role
                    when (user[Users.role]) {
                        UserRole.Owner -> player.admin = true
                        UserRole.CoOwner -> player.admin = true
                        UserRole.Admin -> player.admin = true
                        UserRole.Mod -> TODO()
                        UserRole.Player -> Unit
                    }
                }
            }
        }
    }

    @Command(["register"])
    @ClientSide
    fun register(player: Player) {
        CoroutineScopes.Main.launch {
            val output = registerMenu.open(player)
                ?: return@launch player.infoMessage(
                    "[#ff0000]Register canceled."
                )

            val username = output["username"]!!
            val password = output["password"]!!
            val confirmPassword = output["confirmPassword"]!!

            if (confirmPassword != password) return@launch player.infoMessage(
                "[#ff0000]Confirm password is not same as password."
            )

            newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
                if (Users.exists { Users.username eq username })
                    return@newSuspendedTransaction player.infoMessage(
                        "[#ff0000]Your username is already taken."
                    )

                val hashedPassword =
                    Password.hash(SecureString(password.toCharArray())).addSalt(SaltGenerator.generate(64))
                        .with(argon2FunctionInstance)

                Users.insertIgnore {
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
            val output = loginMenu.open(player)
                ?: return@launch player.infoMessage(
                    "[#ff0000]Login canceled."
                )

            val username = output["username"]!!
            val password = output["password"]!!

            newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
                val mindustryUser = MindustryUser.selectOne { MindustryUser.mindustryUUID eq player.uuid() }!!

                if (mindustryUser[MindustryUser.userID] != null)
                    return@newSuspendedTransaction player.infoMessage(
                        "[#ff0000]You are already registered."
                    )

                val user = Users.selectOne { Users.username eq username }
                    ?: return@newSuspendedTransaction player.infoMessage(
                        "[#ff0000]User not found."
                    )

                if (!Password.check(password, user[Users.password]).with(argon2FunctionInstance)
                )
                    return@newSuspendedTransaction player.infoMessage(
                        "[#ff0000]Wrong password."
                    )

                MindustryUser.update({ MindustryUser.mindustryUUID eq player.uuid() }) {
                    it[userID] = user[Users.id]
                }

                player.infoMessage(
                    "[#00ff00]Login success. You are now logged in as ${user[Users.username]}."
                )
            }
        }
    }
}