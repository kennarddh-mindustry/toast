package com.github.kennarddh.mindustry.toast.core.handlers.vote.skip_wave

import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.commands.senders.PlayerCommandSender
import com.github.kennarddh.mindustry.genesis.core.commons.runOnMindustryThread
import com.github.kennarddh.mindustry.genesis.core.commons.runOnMindustryThreadSuspended
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.standard.commands.parameters.validations.numbers.GTE
import com.github.kennarddh.mindustry.genesis.standard.commands.parameters.validations.numbers.LTE
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.core.commands.validations.MinimumRole
import com.github.kennarddh.mindustry.toast.core.commons.entities.Entities
import com.github.kennarddh.mindustry.toast.core.commons.entities.PlayerData
import com.github.kennarddh.mindustry.toast.core.commons.safeGetPlayerData
import com.github.kennarddh.mindustry.toast.core.handlers.vote.AbstractVoteCommand
import com.github.kennarddh.mindustry.toast.core.handlers.vote.VoteSession
import mindustry.Vars
import mindustry.game.EventType
import mindustry.gen.Call
import kotlin.time.Duration.Companion.minutes

class SkipWaveCommandHandler : AbstractVoteCommand<SkipWaveVoteObjective>("skip wave", 1.minutes, 1.minutes) {
    @Command(["skip_wave", "next_wave", "sw", "nw"])
    @Description("Start a 'skip wave' vote.")
    suspend fun skipWave(sender: PlayerCommandSender, @LTE(5) @GTE(1) amountOfWave: Int = 1) {
        val playerData = sender.player.safeGetPlayerData() ?: return

        start(playerData, SkipWaveVoteObjective(amountOfWave))
    }


    @Command(["skip_wave_vote", "next_wave_vote", "swv", "nwv"])
    @Description("Vote for 'skip wave' vote.")
    suspend fun voteCommand(sender: PlayerCommandSender, vote: Boolean) {
        val playerData = sender.player.safeGetPlayerData() ?: return

        vote(playerData, vote)
    }

    @Command(["skip_wave_cancel", "next_wave_cancel", "swc", "nwc"])
    @MinimumRole(UserRole.Mod)
    @Description("Cancel a 'skip wave' vote.")
    suspend fun cancelCommand(sender: PlayerCommandSender) {
        val playerData = sender.player.safeGetPlayerData() ?: return

        cancel(playerData)
    }

    override suspend fun canPlayerStart(playerData: PlayerData, objective: SkipWaveVoteObjective): Boolean {
        return runOnMindustryThreadSuspended {
            if (!Vars.state.rules.waves) {
                playerData.player.sendMessage("[#ff0000]Cannot start '$name' vote because waves is disabled")

                return@runOnMindustryThreadSuspended false
            }

            return@runOnMindustryThreadSuspended true
        }
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
        return "Type [accent]/swv y[] or [accent]/swv n[] to vote for skipping ${session.objective.amountOfWave} waves."
    }

    @EventHandler
    suspend fun onWave(event: EventType.WaveEvent) {
        silentCancel()
    }
}