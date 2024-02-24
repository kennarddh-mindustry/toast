package com.github.kennarddh.mindustry.toast.core.handlers.users

import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ClientSide
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.commands.result.CommandResult
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.common.database.tables.Users
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.PlayerReportedGameEvent
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import com.github.kennarddh.mindustry.toast.core.commons.getUserOptionalAndMindustryUserAndUserServerData
import kotlinx.datetime.Clock
import mindustry.gen.Player
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class UserReportHandler : Handler() {
    @Command(["report"])
    @ClientSide
    @Description("Report a player.")
    suspend fun kick(player: Player, target: Player, reason: String): CommandResult {
        newSuspendedTransaction {
            val user = player.getUserOptionalAndMindustryUserAndUserServerData()
            val targetUser = player.getUserOptionalAndMindustryUserAndUserServerData()

            Messenger.publishGameEvent(
                "${ToastVars.server.name}.report",
                GameEvent(
                    ToastVars.server, Clock.System.now().toEpochMilliseconds(),
                    PlayerReportedGameEvent(
                        player.name,
                        user?.get(Users.id)?.value,
                        target.name,
                        targetUser?.get(Users.id)?.value,
                        reason
                    )
                )
            )
        }

        return CommandResult("Successfully reported ${target.name} with the reason ${reason}.")
    }
}