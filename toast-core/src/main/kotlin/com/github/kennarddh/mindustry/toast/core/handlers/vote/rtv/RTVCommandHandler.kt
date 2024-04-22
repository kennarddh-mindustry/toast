package com.github.kennarddh.mindustry.toast.core.handlers.vote.rtv

import arc.Events
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.commands.senders.PlayerCommandSender
import com.github.kennarddh.mindustry.genesis.core.commons.runOnMindustryThread
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.core.commands.validations.MinimumRole
import com.github.kennarddh.mindustry.toast.core.commons.entities.Entities
import com.github.kennarddh.mindustry.toast.core.handlers.vote.AbstractVoteCommand
import com.github.kennarddh.mindustry.toast.core.handlers.vote.VoteSession
import mindustry.game.EventType.GameOverEvent
import mindustry.game.Team
import mindustry.gen.Call
import kotlin.math.ceil
import kotlin.time.Duration.Companion.minutes

class RTVCommandHandler : AbstractVoteCommand<Byte>("rtv", 2.minutes) {
    @Command(["rtv", "change-map"])
    @Description("Start a 'rtv'/'change-map' vote.")
    suspend fun rtv(sender: PlayerCommandSender, vote: Boolean = true) {
        if (!getIsVoting()) {
            if (vote) {
                start(sender.player, 1)
            } else {
                sender.player.sendMessage("[#ff0000]Cannot vote no for '$name' because there is no '$name' vote session in progress.")
            }

            return
        }

        vote(sender.player, vote)
    }

    @Command(["rtv-cancel", "change-map-cancel"])
    @MinimumRole(UserRole.Mod)
    @Description("Cancel a 'rtv' vote.")
    suspend fun cancelCommand(sender: PlayerCommandSender) {
        cancel(sender.player)
    }

    override fun getRequiredVotes(): Int = ceil(Entities.players.size * 3f / 4f).toInt()

    override suspend fun onSuccess(session: VoteSession<Byte>) {
        Call.sendMessage("[#00ff00]RTV success. Changing map.")

        runOnMindustryThread {
            Events.fire(GameOverEvent(Team.derelict))
        }
    }

    override suspend fun getSessionDetails(session: VoteSession<Byte>): String {
        return "Type [accent]/rtv y[] or [accent]/rtv n[] to vote."
    }
}