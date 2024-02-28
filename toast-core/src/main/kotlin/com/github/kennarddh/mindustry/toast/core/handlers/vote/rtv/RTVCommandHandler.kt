package com.github.kennarddh.mindustry.toast.core.handlers.vote.kick

import arc.Events
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ClientSide
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commons.runOnMindustryThread
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.core.commands.validations.MinimumRole
import com.github.kennarddh.mindustry.toast.core.handlers.vote.AbstractVoteCommand
import com.github.kennarddh.mindustry.toast.core.handlers.vote.VoteSession
import mindustry.game.EventType.GameOverEvent
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Player
import kotlin.time.Duration.Companion.minutes

class RTVCommandHandler : AbstractVoteCommand<Byte>("rtv", 2.minutes) {
    @Command(["rtv", "change-map"])
    @ClientSide
    suspend fun rtv(player: Player) {
        if (!getIsVoting()) {
            start(player, 1)

            return
        }

        vote(player, true)
    }

    @Command(["rtv-cancel"])
    @ClientSide
    @MinimumRole(UserRole.Mod)
    suspend fun cancelCommand(player: Player) {
        cancel(player)
    }

    override suspend fun onSuccess(session: VoteSession<Byte>) {
        Call.sendMessage("[#00ff00]RTV success. Changing map.")

        runOnMindustryThread {
            Events.fire(GameOverEvent(Team.crux))
        }
    }

    override suspend fun getSessionDetails(session: VoteSession<Byte>): String {
        return "Type [accent]/rtv y[] or [accent]/rtv n[] to vote."
    }
}