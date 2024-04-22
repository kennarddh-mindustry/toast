package com.github.kennarddh.mindustry.toast.core.handlers.vote.skip_wave

import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.commands.senders.PlayerCommandSender
import com.github.kennarddh.mindustry.genesis.core.commons.runOnMindustryThread
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.standard.commands.parameters.validations.numbers.GTE
import com.github.kennarddh.mindustry.genesis.standard.commands.parameters.validations.numbers.LTE
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.core.commands.validations.MinimumRole
import com.github.kennarddh.mindustry.toast.core.commons.entities.Entities
import com.github.kennarddh.mindustry.toast.core.handlers.vote.AbstractVoteCommand
import com.github.kennarddh.mindustry.toast.core.handlers.vote.VoteSession
import mindustry.Vars
import mindustry.game.EventType
import mindustry.gen.Call
import mindustry.gen.Player
import kotlin.time.Duration.Companion.minutes

class SkipWaveCommandHandler : AbstractVoteCommand<SkipWaveVoteObjective>("skip wave", 1.minutes) {
    @Command(["skip-wave", "next-wave", "sw", "nw"])
    @Description("Start a 'skip wave' vote.")
    suspend fun skipWave(sender: PlayerCommandSender, @LTE(5) @GTE(1) amountOfWave: Int = 1) {
        start(sender.player, SkipWaveVoteObjective(amountOfWave))
    }


    @Command(["skip-wave-vote", "next-wave-vote", "swv", "nwv"])
    @Description("Vote for 'skip wave' vote.")
    suspend fun voteCommand(sender: PlayerCommandSender, vote: Boolean) {
        vote(sender.player, vote)
    }

    @Command(["skip-wave-cancel", "next-wave-cancel", "swc", "nwc"])
    @MinimumRole(UserRole.Mod)
    @Description("Cancel a 'skip wave' vote.")
    suspend fun cancelCommand(sender: PlayerCommandSender) {
        cancel(sender.player)
    }

    override fun canPlayerStart(player: Player, objective: SkipWaveVoteObjective): Boolean {
        if (!Vars.state.rules.waves) {
            player.sendMessage("[#ff0000]Cannot start '$name' vote because waves is disabled")

            return false
        }

        return true
    }

    override suspend fun onSuccess(session: VoteSession<SkipWaveVoteObjective>) {
        Call.sendMessage("[#00ff00]'$name' vote success. Skipping wave.")

        runOnMindustryThread {
            repeat(session.objective.amountOfWave) {
                Vars.logic.runWave()
            }
        }
    }

    override fun getRequiredVotes(): Int = Entities.players.size.coerceIn(1..3)

    override suspend fun getSessionDetails(session: VoteSession<SkipWaveVoteObjective>): String {
        return "Type [accent]/skip-wave-vote y[] or [accent]/skip-wave-vote n[] to vote."
    }

    @EventHandler
    suspend fun onWave(event: EventType.WaveEvent) {
        silentCancel()
    }
}