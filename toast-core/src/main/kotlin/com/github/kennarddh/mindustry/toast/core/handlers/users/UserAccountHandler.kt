package com.github.kennarddh.mindustry.toast.core.handlers.users

import arc.util.Strings
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ClientSide
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ServerSide
import com.github.kennarddh.mindustry.genesis.core.commands.result.CommandResult
import com.github.kennarddh.mindustry.genesis.core.commands.result.CommandResultStatus
import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.genesis.core.commons.priority.PriorityEnum
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.core.menus.Menu
import com.github.kennarddh.mindustry.genesis.core.menus.Menus
import com.github.kennarddh.mindustry.genesis.core.server.packets.annotations.ServerPacketHandler
import com.github.kennarddh.mindustry.genesis.standard.extensions.infoMessage
import com.github.kennarddh.mindustry.genesis.standard.extensions.kickWithoutLogging
import com.github.kennarddh.mindustry.toast.common.*
import com.github.kennarddh.mindustry.toast.common.database.tables.*
import com.github.kennarddh.mindustry.toast.core.commands.validations.MinimumRole
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import com.github.kennarddh.mindustry.toast.core.commons.getUserAndMindustryUserAndUserServerData
import com.github.kennarddh.mindustry.toast.core.commons.getUserOptionalAndMindustryUserAndUserServerData
import com.github.kennarddh.mindustry.toast.core.commons.mindustryServerUserDataWhereClause
import com.password4j.Argon2Function
import com.password4j.Password
import com.password4j.SecureString
import com.password4j.types.Argon2
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import mindustry.game.EventType
import mindustry.gen.Player
import mindustry.net.NetConnection
import mindustry.net.Packets
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

class UserAccountHandler : Handler() {
    private val backingUsers: MutableMap<Player, User> = Collections.synchronizedMap(mutableMapOf())

    val users get() = backingUsers

    private val passwordHashFunctionInstance = Argon2Function.getInstance(
        16384, 10, 4, 64, Argon2.ID
    )

    private val usidHashFunctionInstance = Argon2Function.getInstance(
        16384, 10, 4, 64, Argon2.ID
    )

    private val usernameRegex = """[a-zA-Z0-9_]{1,50}""".toRegex()
    private val passwordRegex = """[a-zA-Z0-9 !@#$%^&*()-_+=\[\]{};:'",.<>/?|`~]{8,50}""".toRegex()
    private val numberOnlyNameRegex = """^\d+$""".toRegex()

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

    @ServerPacketHandler(PriorityEnum.Important)
    fun checkPlayerName(con: NetConnection, packet: Packets.ConnectPacket): Boolean {
        if (packet.name.length > 50) {
            con.kickWithoutLogging("Name cannot be longer than 50 characters long.")

            return false
        }

        if (packet.name.startsWith('#')) {
            con.kickWithoutLogging("Name cannot starts with #.")

            return false
        }

        if (packet.name.matches(numberOnlyNameRegex)) {
            con.kickWithoutLogging("Name cannot only contains numbers.")

            return false
        }

        return true
    }

    @ServerPacketHandler(PriorityEnum.Important)
    suspend fun checkIsSameUserAlreadyJoined(con: NetConnection, packet: Packets.ConnectPacket): Boolean {
        val user = newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
            Users.join(
                MindustryUserServerData,
                JoinType.INNER,
                onColumn = Users.id,
                otherColumn = MindustryUserServerData.userID
            ).join(
                MindustryUser,
                JoinType.INNER,
                onColumn = MindustryUserServerData.mindustryUserID,
                otherColumn = MindustryUser.id
            )
                .selectOne { (MindustryUser.mindustryUUID eq packet.uuid) and (MindustryUserServerData.server eq ToastVars.server) }
        }

        if (user == null) return true

        if (users.count { it.value.userID == user[Users.id].value } >= 1) {
            con.kickWithoutLogging("There is someone with the same user already on this server.")

            return false
        }

        return true
    }


    @ServerPacketHandler(PriorityEnum.High)
    suspend fun onConnectPacket(con: NetConnection, packet: Packets.ConnectPacket): Boolean {
        val ip = con.address.packIP()

        return newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
            val mindustryUser = MindustryUser.insertIfNotExistAndGet({
                MindustryUser.mindustryUUID eq packet.uuid
            }) {
                it[this.mindustryUUID] = packet.uuid
            }

            MindustryUserIPAddresses.insertIfNotExistAndGet({
                (MindustryUserIPAddresses.mindustryUserID eq mindustryUser[MindustryUser.id]) and
                        (MindustryUserIPAddresses.ipAddress eq ip)
            }) {
                it[this.mindustryUserID] = mindustryUser[MindustryUser.id]
                it[this.ipAddress] = ip
            }

            MindustryUserMindustryNames.insertIfNotExistAndGet({
                (MindustryUserMindustryNames.mindustryUserID eq mindustryUser[MindustryUser.id]) and
                        (MindustryUserMindustryNames.name eq packet.name)
            }) {
                it[this.mindustryUserID] = mindustryUser[MindustryUser.id]
                it[this.name] = packet.name
                it[this.strippedName] = Strings.stripColors(packet.name)
            }

            val mindustryUserServerDataCanBeNull = MindustryUserServerData
                .join(
                    MindustryUser,
                    JoinType.INNER,
                    onColumn = MindustryUserServerData.mindustryUserID,
                    otherColumn = MindustryUser.id
                )
                .selectOne {
                    (MindustryUser.mindustryUUID eq packet.uuid) and (MindustryUserServerData.server eq ToastVars.server)
                }

            val mindustryUserServerData = if (mindustryUserServerDataCanBeNull == null) {
                // New user server data
                val hashedUSID = Password.hash(SecureString(packet.usid.toCharArray()))
                    .addRandomSalt(64)
                    .with(usidHashFunctionInstance)

                MindustryUserServerData.insert {
                    it[this.mindustryUserID] = mindustryUser[MindustryUser.id]
                    it[this.server] = ToastVars.server
                    it[this.mindustryUSID] = hashedUSID.result
                }.resultedValues!!.first()
            } else {
                val storedUSID = mindustryUserServerDataCanBeNull[MindustryUserServerData.mindustryUSID]

                val mindustryUserServerData =
                    if (!Password.check(packet.usid, storedUSID).with(usidHashFunctionInstance)) {
                        // USID is not same as stored usid, Invalidate login.
                        con.infoMessage(
                            "[#ff0000]Your login was invalidated. Either there is server's ip update or your account got stolen by other player. If this happen too often without any announcements, likely that your user was stolen."
                        )

                        val hashedUSID = Password.hash(SecureString(packet.usid.toCharArray()))
                            .addRandomSalt(64)
                            .with(usidHashFunctionInstance)

                        MindustryUserServerData.update({
                            (MindustryUserServerData.mindustryUserID eq mindustryUser[MindustryUser.id]) and
                                    (MindustryUserServerData.server eq ToastVars.server)
                        }) {
                            it[this.mindustryUSID] = hashedUSID.result
                            it[this.userID] = null
                        }

                        // Return updated user server data
                        MindustryUserServerData
                            .join(
                                MindustryUser,
                                JoinType.INNER,
                                onColumn = MindustryUserServerData.mindustryUserID,
                                otherColumn = MindustryUser.id
                            )
                            .selectOne {
                                (MindustryUser.mindustryUUID eq packet.uuid) and (MindustryUserServerData.server eq ToastVars.server)
                            }!!
                    } else {
                        mindustryUserServerDataCanBeNull
                    }

                mindustryUserServerData
            }

            val userID = mindustryUserServerData[MindustryUserServerData.userID]

            val targetUserAlias = Users.alias("targetUser")
            val targetMindustryUserAlias = MindustryUser.alias("targetMindustryUser")

            val userPunishmentsQuery = UserPunishments
                .join(
                    Users,
                    JoinType.LEFT,
                    onColumn = UserPunishments.userID,
                    otherColumn = Users.id
                )
                .join(
                    targetUserAlias,
                    JoinType.LEFT,
                    onColumn = UserPunishments.targetUserID,
                    otherColumn = targetUserAlias[Users.id]
                )
                .join(
                    MindustryUser,
                    JoinType.LEFT,
                    onColumn = UserPunishments.mindustryUserID,
                    otherColumn = MindustryUser.id
                )
                .join(
                    targetMindustryUserAlias,
                    JoinType.LEFT,
                    onColumn = UserPunishments.targetMindustryUserID,
                    otherColumn = targetMindustryUserAlias[MindustryUser.id]
                )
                .selectAll()

            if (userID != null)
                userPunishmentsQuery.andWhere {
                    UserPunishments.targetUserID.isNull() or (UserPunishments.targetUserID eq userID.value)
                }

            userPunishmentsQuery.andWhere {
                UserPunishments.targetMindustryUserID.isNull() or (UserPunishments.targetMindustryUserID eq mindustryUser[MindustryUser.id])
            }

            // Check is still not ended and has not been pardoned
            userPunishmentsQuery.andWhere {
                (UserPunishments.endAt greaterEq CurrentDateTime) or UserPunishments.pardonedAt.isNotNull()
            }

            for (userPunishment in userPunishmentsQuery) {
                if (userPunishment[UserPunishments.type] == PunishmentType.Ban) {
                    con.kickWithoutLogging(
                        """
                            [#ff0000]You were banned for the reason
                            []${userPunishment[UserPunishments.reason]}
                            [#00ff00]Appeal in Discord.
                            """.trimIndent()
                    )

                    return@newSuspendedTransaction false
                } else if (userPunishment[UserPunishments.type] == PunishmentType.Kick) {
                    val kickTimeLeft =
                        userPunishment[UserPunishments.endAt]!!.toInstant(TimeZone.UTC).minus(Clock.System.now())

                    con.kickWithoutLogging(
                        """
                            [#ff0000]You were kicked for the reason
                            []${userPunishment[UserPunishments.reason]}
                            [#00ff00]You can join again in $kickTimeLeft.
                            [#00ff00]Appeal in Discord.
                            """.trimIndent()
                    )

                    return@newSuspendedTransaction false
                } else if (userPunishment[UserPunishments.type] == PunishmentType.VoteKick) {
                    val kickTimeLeft =
                        userPunishment[UserPunishments.endAt]!!.toInstant(TimeZone.UTC).minus(Clock.System.now())

                    con.kickWithoutLogging(
                        """
                            [#ff0000]You were vote kicked for the reason
                            []${userPunishment[UserPunishments.reason]}
                            [#00ff00]You can join again in $kickTimeLeft.
                            [#00ff00]Appeal in Discord.
                            """.trimIndent()
                    )

                    return@newSuspendedTransaction false
                }
            }

            return@newSuspendedTransaction true
        }
    }

    @EventHandler
    suspend fun onPlayerConnect(event: EventType.PlayerConnect) {
        val player = event.player

        newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
            val userAndMindustryUserAndUserServerData = player.getUserOptionalAndMindustryUserAndUserServerData()

            val userID = userAndMindustryUserAndUserServerData?.get(Users.id)?.value

            val mindustryUserID = userAndMindustryUserAndUserServerData?.get(MindustryUser.id)!!.value

            backingUsers[player] = User(userID, mindustryUserID, player)

            if (userID != null) {
                if (userAndMindustryUserAndUserServerData[Users.role] >= UserRole.Admin) {
                    player.admin = true
                }
            }
        }
    }

    @EventHandler
    fun onPlayerLeave(event: EventType.PlayerLeave) {
        backingUsers.remove(event.player)
    }

    @Command(["register"])
    @ClientSide
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

            users.remove(player)

            player.infoMessage(
                "[#00ff00]Logout success. You are now no longer logged in."
            )
        }
    }

    @Command(["changeRole"])
    @ClientSide
    @ServerSide
    @MinimumRole(UserRole.Admin)
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