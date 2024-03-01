package com.github.kennarddh.mindustry.toast.core.handlers.users

import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ClientSide
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.commands.result.CommandResult
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.PlayerReportedGameEvent
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import com.github.kennarddh.mindustry.toast.core.commons.safeGetPlayerData
import kotlinx.datetime.Clock
import mindustry.gen.Player

class UserReportHandler : Handler {
    @Command(["report"])
    @ClientSide
    @Description("Report a player.")
    suspend fun report(player: Player, target: Player, reason: String): CommandResult? {
        val playerData = player.safeGetPlayerData() ?: return null
        val targetPlayerData = player.safeGetPlayerData() ?: return null

        Messenger.publishGameEvent(
            "${ToastVars.server.name}.report",
            GameEvent(
                ToastVars.server, Clock.System.now(),
                PlayerReportedGameEvent(
                    player.plainName(),
                    playerData.userID,
                    target.plainName(),
                    targetPlayerData.userID,
                    reason
                )
            )
        )

        return CommandResult("Successfully reported ${target.name} with the reason ${reason}.")
    }
}