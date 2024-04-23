package com.github.kennarddh.mindustry.toast.core.handlers.users

import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.parameters.Vararg
import com.github.kennarddh.mindustry.genesis.core.commands.senders.PlayerCommandSender
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
    @Description("Report a player.")
    suspend fun report(sender: PlayerCommandSender, target: Player, @Vararg reason: String) {
        val playerData = sender.player.safeGetPlayerData() ?: return
        val targetPlayerData = sender.player.safeGetPlayerData() ?: return

        Messenger.publishGameEvent(
            "${ToastVars.server.name}.report",
            GameEvent(
                ToastVars.server, Clock.System.now(),
                PlayerReportedGameEvent(
                    sender.player.plainName(),
                    playerData.userID,
                    target.plainName(),
                    targetPlayerData.userID,
                    reason
                )
            )
        )

        sender.sendSuccess("Successfully reported ${target.name} with the reason ${reason}.")
    }
}