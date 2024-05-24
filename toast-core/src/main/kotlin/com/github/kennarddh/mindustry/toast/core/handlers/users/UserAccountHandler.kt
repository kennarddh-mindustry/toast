package com.github.kennarddh.mindustry.toast.core.handlers.users

import com.github.kennarddh.mindustry.genesis.core.Genesis
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.parameters.Vararg
import com.github.kennarddh.mindustry.genesis.core.commands.senders.CommandSender
import com.github.kennarddh.mindustry.genesis.core.commands.senders.PlayerCommandSender
import com.github.kennarddh.mindustry.genesis.core.commands.senders.ServerCommandSender
import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.genesis.core.commons.runOnMindustryThreadSuspended
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.core.menus.Menu
import com.github.kennarddh.mindustry.genesis.core.menus.Menus
import com.github.kennarddh.mindustry.toast.common.Permission
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.common.clearRoleEffect
import com.github.kennarddh.mindustry.toast.common.database.Database
import com.github.kennarddh.mindustry.toast.common.database.tables.*
import com.github.kennarddh.mindustry.toast.common.extensions.exists
import com.github.kennarddh.mindustry.toast.common.extensions.selectOne
import com.github.kennarddh.mindustry.toast.common.extensions.unpackIP
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.PlayerRoleChangedGameEvent
import com.github.kennarddh.mindustry.toast.core.commands.validations.MinimumRole
import com.github.kennarddh.mindustry.toast.core.commons.*
import com.github.kennarddh.mindustry.toast.core.commons.entities.Entities
import com.github.kennarddh.mindustry.toast.core.commons.extensions.getPermissions
import com.password4j.Argon2Function
import com.password4j.Password
import com.password4j.SecureString
import com.password4j.types.Argon2
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import mindustry.gen.Player
import org.jetbrains.exposed.sql.*

class UserAccountHandler : Handler {
    private val passwordHashFunctionInstance = Argon2Function.getInstance(
        4096, 10, 2, 64, Argon2.ID
    )

    private val usernameRegex = """[a-zA-Z0-9_]{1,50}""".toRegex()
    private val passwordRegex = """[a-zA-Z0-9 !@#$%^&*()-_+=\[\]{};:'",.<>/?|`~]{8,50}""".toRegex()

    private val registerMenu = Menus(
        mapOf(
            "username" to Menu("Register 1/3", "Username", 50),
            "password" to Menu("Register 2/3", "Password", 50),
            "confirmPassword" to Menu("Register 3/3", "Confirm Password", 50)
        )
    )

    private val loginMenu = Menus(
        mapOf(
            "username" to Menu("Login 1/2", "Username", 50),
            "password" to Menu("Login 2/2", "Password", 50),
        )
    )

    override suspend fun onInit() {
        Genesis.commandRegistry.removeCommand("admin")
        Genesis.commandRegistry.removeCommand("admins")
    }

    @Command(["register"])
    @Description("Register.")
    suspend fun register(sender: PlayerCommandSender) {
        val playerData = Entities.players[sender.player]
            ?: return sender.sendError("Cannot find user. Please try again later.")

        if (playerData.userID != null)
            return sender.sendError("You are already logged in.")

        val output = registerMenu.open(sender.player)
            ?: return sender.sendError("Register canceled.")

        val username = output["username"]!!
        val password = output["password"]!!
        val confirmPassword = output["confirmPassword"]!!

        if (!username.matches(usernameRegex))
            return sender.sendError(
                "Invalid username. Username may only contains lowercase, uppercase, and numbers. Min length is 1 and max is 50 characters."
            )

        if (!password.matches(passwordRegex))
            return sender.sendError(
                "Invalid password. Password may only contains lowercase, uppercase, symbols, and numbers. Min length is 8 and max is 50 characters.",
            )

        if (!confirmPassword.matches(passwordRegex))
            return sender.sendError(
                "Invalid confirm password. Confirm password may only contains lowercase, uppercase, symbols, and numbers. Min length is 8 and max is 50 characters.",
            )

        if (confirmPassword != password)
            return sender.sendError("Confirm password is not same as password.")

        Database.newTransaction {
            if (Users.exists { Users.username eq username })
                return@newTransaction sender.sendError(
                    "Your username is already taken.",
                )

            val hashedPassword = Password.hash(SecureString(password.toCharArray()))
                .addRandomSalt(64)
                .with(passwordHashFunctionInstance)

            Users.insert {
                it[this.username] = username
                it[this.password] = hashedPassword.result
                it[this.role] = UserRole.Player
            }

            sender.sendSuccess("Register success. Login with /login to use your account.")
        }
    }

    @Command(["login"])
    @Description("Login.")
    suspend fun login(sender: PlayerCommandSender) {
        val playerData = Entities.players[sender.player]
            ?: return sender.sendError("Cannot find user. Please try again later.")

        if (playerData.userID != null)
            return sender.sendError("You are already logged in.")

        val output = loginMenu.open(sender.player)
            ?: return sender.sendError("Login cancelled.")

        val username = output["username"]!!
        val password = output["password"]!!

        if (!username.matches(usernameRegex))
            return sender.sendError(
                "Invalid username. Username may only contains lowercase, uppercase, and numbers. Min length is 1 and max is 50 characters.",
            )

        if (!password.matches(passwordRegex))
            return sender.sendError(
                "Invalid password. Password may only contains lowercase, uppercase, symbols, and numbers. Min length is 8 and max is 50 characters.",
            )

        Database.newTransaction {
            val user = Users.selectOne { Users.username eq username }
                ?: return@newTransaction sender.sendError(
                    "User not found."
                )

            if (Entities.players.values.find { it.userID == user[Users.id].value } != null) {
                return@newTransaction sender.sendError(
                    "There is someone with the same user already on this server.",
                )
            }

            if (!Password.check(password, user[Users.password]).with(passwordHashFunctionInstance))
                return@newTransaction sender.sendError("Wrong password.")

            MindustryUserServerData
                .join(
                    MindustryUser,
                    JoinType.INNER,
                    onColumn = MindustryUserServerData.mindustryUserID,
                    otherColumn = MindustryUser.id
                )
                .update({
                    (MindustryUser.mindustryUUID eq sender.player.uuid()) and (MindustryUserServerData.server eq ToastVars.server)
                }) {
                    it[MindustryUserServerData.userID] = user[Users.id]
                }

            playerData.userID = user[Users.id].value
            playerData.role = user[Users.role]

            runOnMindustryThreadSuspended {
                sender.player.clearRoleEffect()

                user[Users.role].applyRoleEffect(sender.player)

                sender.player.applyName(user[Users.role])
            }

            sender.sendSuccess(
                "Login success. You are now logged in as ${user[Users.username]}."
            )
        }
    }

    @Command(["changerole", "change_role"])
    @MinimumRole(UserRole.Admin)
    @Description("Change someone's role.")
    suspend fun changeRole(sender: CommandSender, target: Player, @Vararg newRole: UserRole) {
        val targetPlayerData = target.safeGetPlayerData() ?: return

        val targetRole = targetPlayerData.role
            ?: return sender.sendError("Target is not registered.")

        val targetUserID = targetPlayerData.userID

        if (targetUserID == null) {
            Logger.warn("ChangeRole. Invalid state, targetUserID is null.")

            return sender.sendError("Error Occurred. Invalid state, targetUserID is null.")
        }

        if (sender is PlayerCommandSender) {
            val playerData = sender.player.safeGetPlayerData() ?: return
            val playerRole = playerData.role

            if (playerRole == null) {
                Logger.warn("Player is not registered. Check if MinimumRole annotation is correct and exist. ChangeRole.")

                return sender.sendError("Error Occurred. Player is not registered.")
            }

            if (playerRole <= targetRole)
                return sender.sendError(
                    "Your role must be higher than target's role to change target's role.",
                )

            if (playerRole <= newRole)
                return sender.sendError(
                    "Your role must be higher than new role.",
                )
        }

        Database.newTransaction {
            Users.update({ Users.id eq targetPlayerData.userID }) {
                it[this.role] = newRole
            }
        }

        runOnMindustryThreadSuspended {
            target.clearRoleEffect()
            newRole.applyRoleEffect(target)
            target.applyName(newRole)
        }

        targetPlayerData.role = newRole

        CoroutineScopes.Main.launch {
            Messenger.publishGameEvent(
                "${ToastVars.server.name}.player.role.changed",
                GameEvent(
                    ToastVars.server, Clock.System.now(),
                    PlayerRoleChangedGameEvent(targetUserID)
                )
            )
        }

        sender.sendSuccess("Successfully changed ${target.plainName()} role to $newRole.")
    }

    @Command(["logout"])
    @Description("Logout.")
    suspend fun logout(sender: PlayerCommandSender) {
        val playerData = Entities.players[sender.player]
            ?: return sender.sendError("Cannot find user. Please try again later.")

        if (playerData.userID == null)
            return sender.sendError("You are not logged in.")

        Database.newTransaction {
            MindustryUserServerData
                .update({
                    MindustryUserServerData.userID eq playerData.userID
                    MindustryUserServerData.server eq ToastVars.server
                }) {
                    it[userID] = null
                }

            playerData.userID = null
            playerData.role = null

            runOnMindustryThreadSuspended {
                sender.player.clearRoleEffect()
                sender.player.applyName(null)
            }

            sender.sendSuccess("Logout success. You are now no longer logged in.")
        }
    }

    @Command(["user"])
    @Description("Get data about a user.")
    suspend fun getUserData(sender: CommandSender, @Vararg target: Player? = null) {
        val computedTarget = when (sender) {
            is ServerCommandSender -> {
                target ?: return sender.sendError(
                    "Target must not be null on server",
                )
            }

            is PlayerCommandSender -> {
                target ?: sender.player
            }

            else -> {
                Logger.error("Unknown CommandSender")

                return sender.sendError("Server error occurred, please report.")
            }
        }

        Database.newTransaction {
            val targetUser = computedTarget.getUser()
                ?: return@newTransaction sender.sendError("Target is not logged in.")

            val targetPlayerData = computedTarget.safeGetPlayerData() ?: return@newTransaction

            val permissions = sender.getPermissions() ?: return@newTransaction

            val targetMindustryUsersID: Set<Int> =
                if (permissions.contains(Permission.ViewUUID)) {
                    val uuids = MindustryUser
                        .join(
                            MindustryUserServerData,
                            JoinType.INNER,
                            onColumn = MindustryUser.id,
                            otherColumn = MindustryUserServerData.mindustryUserID
                        )
                        .select(MindustryUser.mindustryUUID)
                        .where { MindustryUserServerData.userID eq targetPlayerData.userID }

                    uuids.map { it[MindustryUser.id].value }.toSet()
                } else {
                    setOf()
                }

            val targetIPs: Set<String> =
                if (permissions.contains(Permission.ViewIP)) {
                    val ips = MindustryUser
                        .join(
                            MindustryUserServerData,
                            JoinType.INNER,
                            onColumn = MindustryUser.id,
                            otherColumn = MindustryUserServerData.mindustryUserID
                        )
                        .join(
                            MindustryUserIPAddresses,
                            JoinType.INNER,
                            onColumn = MindustryUser.id,
                            otherColumn = MindustryUserIPAddresses.mindustryUserID
                        )
                        .select(MindustryUserIPAddresses.ipAddress)
                        .where { MindustryUserServerData.userID eq targetPlayerData.userID }

                    ips.map { it[MindustryUserIPAddresses.ipAddress].unpackIP() }.toSet()
                } else {
                    setOf()
                }

            val targetMindustryNames: Set<String> =
                if (permissions.contains(Permission.ViewMindustryNamesHistory)) {
                    val ips = MindustryUser
                        .join(
                            MindustryUserServerData,
                            JoinType.INNER,
                            onColumn = MindustryUser.id,
                            otherColumn = MindustryUserServerData.mindustryUserID
                        )
                        .join(
                            MindustryUserMindustryNames,
                            JoinType.INNER,
                            onColumn = MindustryUser.id,
                            otherColumn = MindustryUserMindustryNames.mindustryUserID
                        )
                        .select(MindustryUserMindustryNames.strippedName)
                        .where { MindustryUserServerData.userID eq targetPlayerData.userID }

                    ips.map { it[MindustryUserMindustryNames.strippedName] }.toSet()
                } else {
                    setOf()
                }

            val targetTotalXP = MindustryUser
                .join(
                    MindustryUserServerData,
                    JoinType.INNER,
                    onColumn = MindustryUser.id,
                    otherColumn = MindustryUserServerData.mindustryUserID
                )
                .join(
                    MindustryUserMindustryNames,
                    JoinType.INNER,
                    onColumn = MindustryUser.id,
                    otherColumn = MindustryUserMindustryNames.mindustryUserID
                )
                .select(MindustryUserServerData.xp.sum())
                .where { MindustryUserServerData.userID eq targetPlayerData.userID }
                .firstOrNull()
                ?.get(MindustryUserServerData.xp.sum())

            sender.sendSuccess(
                """
                Info for ${targetUser[Users.username]}.
                Total XP: $targetTotalXP
                Mindustry Users ID: ${targetMindustryUsersID.joinToString(", ")}
                IPs: ${if (permissions.contains(Permission.ViewIP)) targetIPs.joinToString(", ") else "No Permission"}
                Mindustry Names: ${
                    if (permissions.contains(Permission.ViewMindustryNamesHistory)) targetMindustryNames.joinToString(
                        ", "
                    ) else "No Permission"
                }
                Discord ID: ${targetUser[Users.discordID]}
                Joined Times: ${targetUser[Users.joinedTimes]}
                """.trimIndent()
            )
        }
    }
}