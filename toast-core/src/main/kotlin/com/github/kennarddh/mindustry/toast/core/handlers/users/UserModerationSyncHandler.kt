package com.github.kennarddh.mindustry.toast.core.handlers.users

import com.github.kennarddh.mindustry.genesis.core.GenesisAPI
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.standard.extensions.kickWithoutLogging
import com.github.kennarddh.mindustry.toast.common.PunishmentType
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUser
import com.github.kennarddh.mindustry.toast.common.database.tables.UserPunishments
import com.github.kennarddh.mindustry.toast.common.database.tables.Users
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.PlayerPunishedGameEvent
import com.github.kennarddh.mindustry.toast.common.selectOne
import com.github.kennarddh.mindustry.toast.common.toDisplayString
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.alias

class UserModerationSyncHandler : Handler() {
    override suspend fun onInit() {
        Messenger.listenGameEvent("${ToastVars.server.name}Server") {
            val data = it.data

            if (it.server == ToastVars.server) return@listenGameEvent
            if (data !is PlayerPunishedGameEvent) return@listenGameEvent

            val targetUserAlias = Users.alias("targetUser")
            val targetMindustryUserAlias = MindustryUser.alias("targetMindustryUser")

            val userPunishment = UserPunishments
                .join(
                    targetUserAlias,
                    JoinType.LEFT,
                    onColumn = UserPunishments.targetUserID,
                    otherColumn = targetUserAlias[Users.id]
                )
                .join(
                    targetMindustryUserAlias,
                    JoinType.LEFT,
                    onColumn = UserPunishments.targetMindustryUserID,
                    otherColumn = targetMindustryUserAlias[MindustryUser.id]
                )
                .selectOne {
                    UserPunishments.id eq data.userPunishmentID
                }!!


            for ((player, user) in GenesisAPI.getHandler<UserAccountHandler>()!!.users) {
                if (
                    (userPunishment[UserPunishments.targetMindustryUserID] != null && player.uuid() == userPunishment[targetMindustryUserAlias[MindustryUser.mindustryUUID]]) ||
                    (userPunishment[UserPunishments.targetUserID] != null && user.userID != null && user.userID == userPunishment[UserPunishments.targetUserID]!!.value)
                ) {
                    val reason = userPunishment[UserPunishments.reason]
                    val type = userPunishment[UserPunishments.type]

                    when (type) {
                        PunishmentType.Kick -> {
                            val duration = userPunishment[UserPunishments.endAt]!!
                                .toInstant(TimeZone.UTC)
                                .minus(
                                    userPunishment[UserPunishments.punishedAt].toInstant(TimeZone.UTC)
                                )

                            player.kickWithoutLogging(
                                """
                                [#ff0000]You were kicked in other server for the reason
                                []$reason
                                [#00ff00]You can join again in ${duration.toDisplayString()}.
                                [#00ff00]Appeal in Discord.
                                """.trimIndent()
                            )
                        }

                        PunishmentType.VoteKick -> {
                            val duration = userPunishment[UserPunishments.endAt]!!
                                .toInstant(TimeZone.UTC)
                                .minus(
                                    userPunishment[UserPunishments.punishedAt].toInstant(TimeZone.UTC)
                                )

                            player.kickWithoutLogging(
                                """
                                [#ff0000]You were vote kicked in other server for the reason
                                []$reason
                                [#00ff00]You can join again in ${duration.toDisplayString()}.
                                [#00ff00]Appeal in Discord.
                                """.trimIndent()
                            )
                        }

                        PunishmentType.Ban -> {
                            player.kickWithoutLogging(
                                """
                                [#ff0000]You were banned in other server for the reason
                                []$reason
                                [#00ff00]Appeal in Discord.
                                """.trimIndent()
                            )
                        }

                        PunishmentType.Mute -> TODO("Mute")
                    }
                }
            }
        }
    }
}