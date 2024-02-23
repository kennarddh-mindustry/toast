package com.github.kennarddh.mindustry.toast.core.handlers.users

import arc.util.Strings
import com.github.kennarddh.mindustry.genesis.core.GenesisAPI
import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.genesis.core.commons.priority.PriorityEnum
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.core.server.packets.annotations.ServerPacketHandler
import com.github.kennarddh.mindustry.genesis.standard.extensions.infoMessage
import com.github.kennarddh.mindustry.genesis.standard.extensions.kickWithoutLogging
import com.github.kennarddh.mindustry.toast.common.*
import com.github.kennarddh.mindustry.toast.common.database.tables.*
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import com.github.kennarddh.mindustry.toast.core.commons.getUserOptionalAndMindustryUserAndUserServerData
import com.password4j.Argon2Function
import com.password4j.Password
import com.password4j.SecureString
import com.password4j.types.Argon2
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import mindustry.game.EventType
import mindustry.net.NetConnection
import mindustry.net.Packets
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class UserJoinsHandler : Handler() {
    private val usidHashFunctionInstance = Argon2Function.getInstance(
        4096, 10, 2, 64, Argon2.ID
    )

    private val numberOnlyNameRegex = """^\d+$""".toRegex()

    @ServerPacketHandler(PriorityEnum.Important)
    fun checkPlayerName(con: NetConnection, packet: Packets.ConnectPacket): Boolean {
        if (packet.name.length > 50) {
            con.kickWithoutLogging("Name cannot be longer than 50 characters long.")

            return false
        }

        if (packet.name.lowercase() == "server") {
            con.kickWithoutLogging("Name cannot be the word \"server\".")

            return false
        }

        if (packet.name.contains('`')) {
            con.kickWithoutLogging("Name cannot contains the letter \"`\" (backtick).")

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

        if (GenesisAPI.getHandler<UserAccountHandler>()!!.users.count { it.value.userID == user[Users.id].value } >= 1) {
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

            // Check has not ended
            userPunishmentsQuery.andWhere {
                UserPunishments.endAt.isNull() or (UserPunishments.endAt greaterEq CurrentDateTime)
            }

            // Check has not been pardoned
            userPunishmentsQuery.andWhere {
                UserPunishments.pardonedAt.isNull()
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
                            [#00ff00]You can join again in ${kickTimeLeft.toDisplayString()}.
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
                            [#00ff00]You can join again in ${kickTimeLeft.toDisplayString()}.
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

            GenesisAPI.getHandler<UserAccountHandler>()!!.users[player] = User(userID, mindustryUserID, player)

            if (userID != null) {
                if (userAndMindustryUserAndUserServerData[Users.role] >= UserRole.Admin) {
                    player.admin = true
                }
            }
        }
    }
}