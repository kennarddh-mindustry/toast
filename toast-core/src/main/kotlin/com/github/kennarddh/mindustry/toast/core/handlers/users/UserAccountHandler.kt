package com.github.kennarddh.mindustry.toast.core.handlers.users

import com.github.kennarddh.mindustry.genesis.core.Genesis
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ClientSide
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ServerSide
import com.github.kennarddh.mindustry.genesis.core.commands.result.CommandResult
import com.github.kennarddh.mindustry.genesis.core.commands.result.CommandResultStatus
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
    @ClientSide
    @Description("Register.")
    suspend fun register(player: Player): CommandResult {
        val playerData = Entities.players[player]
            ?: return CommandResult("Cannot find user. Please try again later.", CommandResultStatus.Failed)

        if (playerData.userID != null)
            return CommandResult("You are already logged in.", CommandResultStatus.Failed)

        val output = registerMenu.open(player)
            ?: return CommandResult("Register canceled.", CommandResultStatus.Failed)

        val username = output["username"]!!
        val password = output["password"]!!
        val confirmPassword = output["confirmPassword"]!!

        if (!username.matches(usernameRegex))
            return CommandResult(
                "Invalid username. Username may only contains lowercase, uppercase, and numbers. Min length is 1 and max is 50 characters.",
                CommandResultStatus.Failed
            )

        if (!password.matches(passwordRegex))
            return CommandResult(
                "Invalid password. Password may only contains lowercase, uppercase, symbols, and numbers. Min length is 8 and max is 50 characters.",
                CommandResultStatus.Failed
            )

        if (!confirmPassword.matches(passwordRegex))
            return CommandResult(
                "Invalid confirm password. Confirm password may only contains lowercase, uppercase, symbols, and numbers. Min length is 8 and max is 50 characters.",
                CommandResultStatus.Failed
            )

        if (confirmPassword != password)
            return CommandResult("Confirm password is not same as password.", CommandResultStatus.Failed)

        return Database.newTransaction {
            if (Users.exists { Users.username eq username })
                return@newTransaction CommandResult(
                    "Your username is already taken.",
                    CommandResultStatus.Failed
                )

            val hashedPassword = Password.hash(SecureString(password.toCharArray()))
                .addRandomSalt(64)
                .with(passwordHashFunctionInstance)

            Users.insert {
                it[this.username] = username
                it[this.password] = hashedPassword.result
                it[this.role] = UserRole.Player
            }

            CommandResult("Register success. Login with /login to use your account.")
        }
    }

    @Command(["login"])
    @ClientSide
    @Description("Login.")
    suspend fun login(player: Player): CommandResult {
        val playerData = Entities.players[player]
            ?: return CommandResult("Cannot find user. Please try again later.", CommandResultStatus.Failed)

        if (playerData.userID != null)
            return CommandResult("You are already logged in.", CommandResultStatus.Failed)

        val output = loginMenu.open(player)
            ?: return CommandResult("Login cancelled.", CommandResultStatus.Failed)

        val username = output["username"]!!
        val password = output["password"]!!

        if (!username.matches(usernameRegex))
            return CommandResult(
                "Invalid username. Username may only contains lowercase, uppercase, and numbers. Min length is 1 and max is 50 characters.",
                CommandResultStatus.Failed
            )

        if (!password.matches(passwordRegex))
            return CommandResult(
                "Invalid password. Password may only contains lowercase, uppercase, symbols, and numbers. Min length is 8 and max is 50 characters.",
                CommandResultStatus.Failed
            )

        return Database.newTransaction {
            val user = Users.selectOne { Users.username eq username }
                ?: return@newTransaction CommandResult(
                    "User not found.", CommandResultStatus.Failed
                )

            if (Entities.players.values.find { it.userID == user[Users.id].value } != null) {
                return@newTransaction CommandResult(
                    "There is someone with the same user already on this server.",
                    CommandResultStatus.Failed
                )
            }

            if (!Password.check(password, user[Users.password]).with(passwordHashFunctionInstance))
                return@newTransaction CommandResult(
                    "Wrong password.", CommandResultStatus.Failed
                )

            MindustryUserServerData
                .join(
                    MindustryUser,
                    JoinType.INNER,
                    onColumn = MindustryUserServerData.mindustryUserID,
                    otherColumn = MindustryUser.id
                )
                .update({
                    (MindustryUser.mindustryUUID eq player.uuid()) and (MindustryUserServerData.server eq ToastVars.server)
                }) {
                    it[MindustryUserServerData.userID] = user[Users.id]
                }

            playerData.userID = user[Users.id].value
            playerData.userID = user[Users.id].value

            runOnMindustryThreadSuspended {
                player.clearRoleEffect()

                user[Users.role].applyRoleEffect(player)

                player.applyName(user[Users.role])
            }

            CommandResult(
                "Login success. You are now logged in as ${user[Users.username]}."
            )
        }
    }

    @Command(["logout"])
    @ClientSide
    @Description("Logout.")
    suspend fun logout(player: Player): CommandResult {
        val playerData = Entities.players[player]
            ?: return CommandResult("Cannot find user. Please try again later.", CommandResultStatus.Failed)

        if (playerData.userID == null)
            return CommandResult("You are not logged in.", CommandResultStatus.Failed)

        return Database.newTransaction {
            MindustryUserServerData
                .update({
                    MindustryUserServerData.userID eq playerData.userID
                    MindustryUserServerData.server eq ToastVars.server
                }) {
                    it[userID] = null
                }

            playerData.userID = null

            runOnMindustryThreadSuspended {
                player.clearRoleEffect()
                player.applyName(null)
            }

            CommandResult("Logout success. You are now no longer logged in.")
        }
    }

    @Command(["changerole", "change-role"])
    @ClientSide
    @ServerSide
    @MinimumRole(UserRole.Admin)
    @Description("Change someone's role.")
    suspend fun changeRole(player: Player? = null, target: Player, newRole: UserRole): CommandResult? {
        val targetPlayerData = target.safeGetPlayerData() ?: return null
        val targetRole = targetPlayerData.role
            ?: return CommandResult("Target is not registered.", CommandResultStatus.Failed)

        val targetUserID = targetPlayerData.userID

        if (targetUserID == null) {
            Logger.warn("ChangeRole. Invalid state, targetUserID is null.")

            return CommandResult("Error Occurred. Invalid state, targetUserID is null.")
        }

        if (player != null) {
            val playerData = player.safeGetPlayerData() ?: return null
            val playerRole = playerData.role

            if (playerRole == null) {
                Logger.warn("Player is not registered. Check if MinimumRole annotation is correct and exist. ChangeRole.")

                return CommandResult("Error Occurred. Player is not registered.", CommandResultStatus.Failed)
            }

            if (playerRole <= targetRole)
                return CommandResult(
                    "Your role must be higher than target's role to change target's role.",
                    CommandResultStatus.Failed
                )

            if (playerRole <= newRole)
                return CommandResult(
                    "Your role must be higher than new role.",
                    CommandResultStatus.Failed
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

        return CommandResult("Successfully changed ${target.plainName()} role to $newRole.")
    }

    @Command(["user"])
    @ClientSide
    @ServerSide
    @Description("Get data about a user.")
    suspend fun getUserData(player: Player? = null, target: Player? = player): CommandResult? {
        if (player == null && target == null) return CommandResult(
            "Target must not be null on server",
            CommandResultStatus.Failed
        )

        if (target == null) {
            return CommandResult(
                "There is an error. Please report this and explain what make this happen. Error code: NULL_TARGET_COMMAND",
                CommandResultStatus.Failed
            )
        }

        return Database.newTransaction {
            val targetUser = target.getUser()
                ?: return@newTransaction CommandResult("Target is not logged in.", CommandResultStatus.Failed)

            val targetPlayerData = target.safeGetPlayerData() ?: return@newTransaction null

            val permissions =
                if (player == null)
                    Permission.all
                else
                    player.safeGetPlayerData()?.fullPermissions ?: return@newTransaction null

            val targetUUIDs: Set<String> =
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

                    uuids.map { it[MindustryUser.mindustryUUID] }.toSet()
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


            return@newTransaction CommandResult(
                """
                Info for ${targetUser[Users.username]}.
                Total XP: $targetTotalXP
                UUIDs: ${if (permissions.contains(Permission.ViewUUID)) targetUUIDs.joinToString(", ") else "No Permission"}
                IPs: ${if (permissions.contains(Permission.ViewIP)) targetIPs.joinToString(", ") else "No Permission"}
                Mindustry names: ${
                    if (permissions.contains(Permission.ViewMindustryNamesHistory)) targetMindustryNames.joinToString(
                        ", "
                    ) else "No Permission"
                }
                Discord ID: ${targetUser[Users.discordID]}
                """.trimIndent()
            )
        }
    }
}