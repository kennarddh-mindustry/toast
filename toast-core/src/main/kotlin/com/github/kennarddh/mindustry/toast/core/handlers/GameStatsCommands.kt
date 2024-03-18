package com.github.kennarddh.mindustry.toast.core.handlers

import arc.struct.Seq
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ClientSide
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ServerSide
import com.github.kennarddh.mindustry.genesis.core.commands.result.CommandResult
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import mindustry.entities.Units
import mindustry.game.Team
import mindustry.gen.Player
import mindustry.logic.GlobalVars
import mindustry.logic.LAccess
import mindustry.type.UnitType

class GameStatsCommands : Handler {
    @Command(["count"])
    @ClientSide
    @ServerSide
    fun count(player: Player? = null, unit: UnitType, team: Team? = null): CommandResult {
        val computedTeam = team ?: player?.team() ?: Team.sharded

        val cap = Units.getStringCap(computedTeam)

        var total = 0
        var free = 0
        var flagged = 0
        var unflagged = 0
        var players = 0
        var command = 0
        var logic = 0
        var freeFlagged = 0
        var logicFlagged = 0

        (computedTeam.data().unitCache(unit) ?: Seq.with()).forEach {
            total++

            val ctrl = it.sense(LAccess.controlled).toInt()

            if (it.flag == 0.0) {
                unflagged++
            } else {
                flagged++
                if (ctrl == 0) freeFlagged++
            }

            when (ctrl) {
                GlobalVars.ctrlPlayer -> players++
                GlobalVars.ctrlCommand -> command++
                GlobalVars.ctrlProcessor -> {
                    if (it.flag != 0.0) logicFlagged++
                    logic++
                }

                else -> free++
            }
        }

        return CommandResult(
            """
            ${if (player == null) "" else "[accent]"}${unit.name}:
            Team: ${computedTeam.name}
            Total(Cap): $total($cap)
            Free(Free Flagged): $free($freeFlagged)
            Flagged(Unflagged): $flagged($unflagged)
            Players(Command): $players($command)
            Logic(Logic Flagged): $logic($logicFlagged)
            """.trimIndent()
        )
    }
}