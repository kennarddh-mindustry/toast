package com.github.kennarddh.mindustry.toast.core.handlers.users

import com.github.kennarddh.mindustry.genesis.core.GenesisAPI
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ClientSide
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ServerSide
import com.github.kennarddh.mindustry.genesis.core.commands.result.CommandResult
import com.github.kennarddh.mindustry.genesis.core.commands.result.CommandResultStatus
import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.core.menus.Menu
import com.github.kennarddh.mindustry.genesis.core.menus.Menus
import com.github.kennarddh.mindustry.genesis.standard.extensions.infoMessage
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUser
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUserServerData
import com.github.kennarddh.mindustry.toast.common.database.tables.Users
import com.github.kennarddh.mindustry.toast.common.exists
import com.github.kennarddh.mindustry.toast.common.selectOne
import com.github.kennarddh.mindustry.toast.core.commands.validations.MinimumRole
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import com.github.kennarddh.mindustry.toast.core.commons.getUserAndMindustryUserAndUserServerData
import com.github.kennarddh.mindustry.toast.core.commons.mindustryServerUserDataWhereClause
import com.password4j.Argon2Function
import com.password4j.Password
import com.password4j.SecureString
import com.password4j.types.Argon2
import mindustry.game.EventType
import mindustry.gen.Player
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.util.*

class UserAccountHandler : Handler() {
    val users: MutableMap<Player, User> = Collections.synchronizedMap(mutableMapOf())

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
        GenesisAPI.commandRegistry.removeCommand("admin")
        GenesisAPI.commandRegistry.removeCommand("admins")
    }

    @EventHandler
    fun onPlayerLeave(event: EventType.PlayerLeave) {
        users.remove(event.player)
    }

    @Command(["register"])
    @ClientSide
    @Description("Register.")
    suspend fun register(player: Player) {
        if (users[player]!!.userID != null)
            return player.sendMessage(
                "[#ff0000]You are already logged in."
            )

        val output = registerMenu.open(player)
            ?: return player.infoMessage(
                "[#ff0000]Register canceled."
            )

        val username = output["username"]!!
        val password = output["password"]!!
        val confirmPassword = output["confirmPassword"]!!

        if (!username.matches(usernameRegex))
            return player.infoMessage(
                "[#ff0000]Invalid username. Username may only contains lowercase, uppercase, and numbers. Min length is 1 and max is 50 characters."
            )

        if (!password.matches(passwordRegex))
            return player.infoMessage(
                "[#ff0000]Invalid password. Password may only contains lowercase, uppercase, symbols, and numbers. Min length is 8 and max is 50 characters."
            )

        if (!confirmPassword.matches(passwordRegex))
            return player.infoMessage(
                "[#ff0000]Invalid confirm password. Confirm password may only contains lowercase, uppercase, symbols, and numbers."
            )

        if (confirmPassword != password) return player.infoMessage(
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

    @Command(["login"])
    @ClientSide
    @Description("Login.")
    suspend fun login(player: Player) {
        if (users[player]!!.userID != null)
            return player.sendMessage(
                "[#ff0000]You are already logged in."
            )

        val output = loginMenu.open(player)
            ?: return player.infoMessage(
                "[#ff0000]Login canceled."
            )

        val username = output["username"]!!
        val password = output["password"]!!

        if (!username.matches(usernameRegex))
            return player.infoMessage(
                "[#ff0000]Invalid username. Username may only contains lowercase, uppercase, and numbers. Min length is 1 and max is 50 characters."
            )

        if (!password.matches(passwordRegex))
            return player.infoMessage(
                "[#ff0000]Invalid password. Password may only contains lowercase, uppercase, symbols, and numbers. Min length is 8 and max is 50 characters."
            )

        newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
            val user = Users.selectOne { Users.username eq username }
                ?: return@newSuspendedTransaction player.infoMessage(
                    "[#ff0000]User not found."
                )

            if (users.count { it.value.userID == user[Users.id].value } >= 1) {
                return@newSuspendedTransaction player.infoMessage(
                    "[#ff0000]There is someone with the same user already on this server."
                )
            }

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
                .update({ player.mindustryServerUserDataWhereClause }) {
                    it[MindustryUserServerData.userID] = user[Users.id]
                }

            users[player]!!.userID = user[Users.id].value

            player.infoMessage(
                "[#00ff00]Login success. You are now logged in as ${user[Users.username]}."
            )
        }
    }

    @Command(["logout"])
    @ClientSide
    @Description("Logout.")
    suspend fun logout(player: Player) {
        if (users[player]!!.userID == null)
            return player.sendMessage(
                "[#ff0000]You are not logged in."
            )

        newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
            MindustryUserServerData
                .update({
                    MindustryUserServerData.userID eq users[player]!!.userID
                    MindustryUserServerData.server eq ToastVars.server
                }) {
                    it[userID] = null
                }

            users[player]!!.userID = null

            player.infoMessage(
                "[#00ff00]Logout success. You are now no longer logged in."
            )
        }
    }

    @Command(["changerole", "change-role"])
    @ClientSide
    @ServerSide
    @MinimumRole(UserRole.Admin)
    @Description("Change someone's role.")
    suspend fun changeRole(player: Player? = null, target: Player, newRole: UserRole): CommandResult =
        newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
            val targetUser =
                target.getUserAndMindustryUserAndUserServerData() ?: return@newSuspendedTransaction CommandResult(
                    "Target is not logged in.",
                    CommandResultStatus.Failed
                )

            if (player != null) {
                val playerUser = player.getUserAndMindustryUserAndUserServerData()!!

                if (playerUser[Users.role] <= targetUser[Users.role])
                    return@newSuspendedTransaction CommandResult(
                        "Your role must be higher than target's role to change target's role.",
                        CommandResultStatus.Failed
                    )

                if (playerUser[Users.role] <= newRole)
                    return@newSuspendedTransaction CommandResult(
                        "Your role must be higher than new role.",
                        CommandResultStatus.Failed
                    )
            }

            Users.update({ Users.id eq targetUser[Users.id] }) {
                it[this.role] = newRole
            }

            return@newSuspendedTransaction CommandResult("Successfully changed ${targetUser[Users.username]} to $newRole.")
        }
}