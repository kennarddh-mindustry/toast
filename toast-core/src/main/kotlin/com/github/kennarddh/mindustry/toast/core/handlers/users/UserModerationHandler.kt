package com.github.kennarddh.mindustry.toast.core.handlers.users

import com.github.kennarddh.mindustry.genesis.core.GenesisAPI
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ClientSide
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ServerSide
import com.github.kennarddh.mindustry.genesis.core.commands.result.CommandResult
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.common.CoroutineScopes
import com.github.kennarddh.mindustry.toast.common.PunishmentType
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUser
import com.github.kennarddh.mindustry.toast.common.database.tables.UserPunishments
import com.github.kennarddh.mindustry.toast.common.database.tables.Users
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.PlayerKickedGameEvent
import com.github.kennarddh.mindustry.toast.core.commands.validations.MinimumRole
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import com.github.kennarddh.mindustry.toast.core.commons.getMindustryUser
import com.github.kennarddh.mindustry.toast.core.commons.getUserAndMindustryUser
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mindustry.gen.Player
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.time.Duration

class UserModerationHandler : Handler() {
    override suspend fun onInit() {
        GenesisAPI.commandRegistry.removeCommand("kick")
    }

    @Command(["kick"])
    @MinimumRole(UserRole.Mod)
    @ClientSide
    @ServerSide
    suspend fun kick(player: Player? = null, target: Player, duration: Duration, reason: String) {
        Logger.info("${if (player == null) "Server" else player.name} kicked ${target.name}/${target.uuid()} for $duration with the reason \"$reason\"")

        target.kick("You were kicked for the reason:\n$reason")

        val punishmentID = newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
            val mindustryUser = target.getMindustryUser()!!
            val targetMindustryUser = target.getMindustryUser()!!
            val user = target.getUserAndMindustryUser()
            val targetUser = target.getUserAndMindustryUser()

            UserPunishments.insertAndGetId {
                it[this.reason] = reason
                it[this.endAt] =
                    Clock.System.now().plus(duration).toLocalDateTime(TimeZone.UTC)
                it[this.type] = PunishmentType.Kick

                it[this.mindustryUserID] = mindustryUser[MindustryUser.id]
                it[this.targetMindustryUserID] = targetMindustryUser[MindustryUser.id]

                if (user != null)
                    it[this.userID] = user[Users.id]

                if (targetUser != null)
                    it[this.targetUserID] = targetUser[Users.id]
            }
        }

        Messenger.publishGameEvent(
            GameEvent(
                ToastVars.server,
                Clock.System.now().toEpochMilliseconds(),
                PlayerKickedGameEvent(punishmentID.value, target.name)
            )
        )

        CommandResult("Successfully kicked ${target.name}/${target.uuid()} for $duration with the reason \"$reason\"")
    }
}