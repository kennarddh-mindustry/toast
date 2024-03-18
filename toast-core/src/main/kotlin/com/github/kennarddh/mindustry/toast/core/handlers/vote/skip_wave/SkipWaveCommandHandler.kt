package com.github.kennarddh.mindustry.toast.core.handlers.vote.skip_wave

import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ClientSide
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.core.commands.validations.MinimumRole
import com.github.kennarddh.mindustry.toast.core.handlers.vote.AbstractVoteCommand
import com.github.kennarddh.mindustry.toast.core.handlers.vote.VoteSession
import mindustry.Vars
import mindustry.game.EventType
import mindustry.gen.Call
import mindustry.gen.Player
import kotlin.time.Duration.Companion.minutes

class SkipWaveCommandHandler : AbstractVoteCommand<Byte>("skip wave", 1.minutes) {
    @Command(["skip-wave", "skipwave", "next-wave", "nextwave"])
    @ClientSide
    @Description("Start a skip wave vote.")
    suspend fun skipWave(player: Player, vote: Boolean = true) {
        if (!getIsVoting()) {
            if (vote) {
                start(player, 1)
            } else {
                player.sendMessage("[#ff0000]Cannot vote no for '$name' because there is no '$name' vote session in progress.")
            }

            return
        }

        vote(player, vote)
    }

    @Command(["skip-wave-cancel"])
    @ClientSide
    @MinimumRole(UserRole.Mod)
    @Description("Cancel a skip wave vote.")
    suspend fun cancelCommand(player: Player) {
        cancel(player)
    }

    override fun canPlayerStart(player: Player, session: Byte): Boolean {
        if (!Vars.state.rules.waves) {
            player.sendMessage("[#ff0000]Cannot start '$name' vote because waves is disabled")

            return false
        }

        return true
    }

    override fun getRequiredVotes(): Int = 3

    override suspend fun onSuccess(session: VoteSession<Byte>) {
        Call.sendMessage("[#00ff00]'$name' vote success. Skipping wave.")

        Vars.logic.runWave()
    }

    override suspend fun getSessionDetails(session: VoteSession<Byte>): String {
        return "Type [accent]/skip-wave y[] or [accent]/skip-wave n[] to vote."
    }

    @EventHandler
    suspend fun onWave(event: EventType.WaveEvent) {
        silentCancel()
    }
}