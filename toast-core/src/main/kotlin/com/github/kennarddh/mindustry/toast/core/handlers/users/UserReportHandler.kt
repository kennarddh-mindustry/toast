package com.github.kennarddh.mindustry.toast.core.handlers.users

import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.parameters.Vararg
import com.github.kennarddh.mindustry.genesis.core.commands.senders.PlayerCommandSender
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.common.database.Database
import com.github.kennarddh.mindustry.toast.common.database.tables.UserReports
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.GameEvent
import com.github.kennarddh.mindustry.toast.common.messaging.messages.PlayerReportedGameEvent
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import com.github.kennarddh.mindustry.toast.core.commons.safeGetPlayerData
import kotlinx.datetime.Clock
import mindustry.gen.Player
import org.jetbrains.exposed.sql.insertAndGetId

class UserReportHandler : Handler {
    @Command(["report"])
    @Description("Report a player.")
    suspend fun report(sender: PlayerCommandSender, target: Player, @Vararg reason: String) {
        if (sender.player == target) return sender.sendError("Cannot report yourself.")

        val playerData = sender.player.safeGetPlayerData() ?: return
        val targetPlayerData = sender.player.safeGetPlayerData() ?: return

        val playerUserID = playerData.userID
        val targetUserID = targetPlayerData.userID

        val reportID = Database.newTransaction {
            UserReports.insertAndGetId {
                it[this.server] = ToastVars.server
                it[this.reason] = reason

                it[this.mindustryUserID] = playerData.mindustryUserID

                it[this.targetMindustryUserID] = targetPlayerData.mindustryUserID

                if (playerUserID != null)
                    it[this.userID] = playerUserID

                if (targetUserID != null)
                    it[this.targetUserID] = targetUserID
            }
        }

        Messenger.publishGameEvent(
            "${ToastVars.server.name}.report",
            GameEvent(
                ToastVars.server, Clock.System.now(),
                PlayerReportedGameEvent(
                    reportID.value,
                    sender.player.plainName(),
                    target.plainName(),
                )
            )
        )

        sender.sendSuccess("Successfully reported '${target.name}' with the reason '${reason}'.")
    }
}