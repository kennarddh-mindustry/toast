package com.github.kennarddh.mindustry.toast.core.handlers

import arc.struct.Seq
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.commands.senders.CommandSender
import com.github.kennarddh.mindustry.genesis.core.commands.senders.PlayerCommandSender
import com.github.kennarddh.mindustry.genesis.core.events.annotations.EventHandler
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.common.extensions.toDisplayString
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import mindustry.Vars
import mindustry.entities.Units
import mindustry.game.EventType.PlayEvent
import mindustry.game.Team
import mindustry.logic.GlobalVars
import mindustry.logic.LAccess
import mindustry.type.UnitType

class GameStatsCommands : Handler {
    private var mapStartTime: Instant = Clock.System.now()

    @EventHandler
    fun onPlay(event: PlayEvent) {
        mapStartTime = Clock.System.now()
    }

    @Command(["count"])
    @Description("Shows unit count.")
    fun count(sender: CommandSender, unit: UnitType, team: Team? = null) {
        val computedTeam = team
            ?: if (sender is PlayerCommandSender) sender.player.team() else Team.sharded

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

        sender.sendSuccess(
            """
            ${if (sender is PlayerCommandSender) "" else "[accent]"}${unit.name}:
            Team: ${computedTeam.name}
            Total(Cap): $total($cap)
            Free(Free Flagged): $free($freeFlagged)
            Flagged(Unflagged): $flagged($unflagged)
            Players(Command): $players($command)
            Logic(Logic Flagged): $logic($logicFlagged)
            """.trimIndent()
        )
    }

    @Command(["mapinfo", "map_info"])
    @Description("Shows map info.")
    fun mapInfo(sender: CommandSender, team: Team? = null) {
        val computedTeam = team
            ?: if (sender is PlayerCommandSender) sender.player.team() else Team.sharded

        sender.sendSuccess(with(Vars.state) {
            """
            ${if (sender is PlayerCommandSender) "" else "[accent]"}Name: ${map.name()}${if (sender is PlayerCommandSender) "" else "[accent]"} (by: ${map.author()}${if (sender is PlayerCommandSender) "" else "[accent]"})
            Team: ${computedTeam.name}
            Map Time: ${Clock.System.now().minus(mapStartTime).toDisplayString()}
            Build Speed (Unit Factories): ${rules.buildSpeed(computedTeam)}x (${rules.unitBuildSpeed(computedTeam)}x)
            Build Cost (Refund): ${rules.buildCostMultiplier}x (${rules.deconstructRefundMultiplier}x)
            Block Health (Damage): ${rules.blockHealth(computedTeam)}x (${rules.blockDamage(computedTeam)}x)
            Unit Damage: ${rules.unitDamage(computedTeam)}x
            Core Capture: ${rules.coreCapture}
            Core Incinerates: ${rules.coreIncinerates}
            Core Modifies Unit Cap: ${rules.unitCapVariable}
            Only Deposit Core: ${rules.onlyDepositCore}
            """.trimIndent()
        })
    }
}