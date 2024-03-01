package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ClientSide
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.commands.result.CommandResult
import com.github.kennarddh.mindustry.genesis.core.commands.result.CommandResultStatus
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.standard.commands.parameters.validations.numbers.GTE
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.distanceFrom
import com.github.kennarddh.mindustry.toast.core.commons.entities.Entities
import mindustry.Vars
import mindustry.gen.Player

class MessageHandler : Handler {
    @Command(["whisper", "w"])
    @ClientSide
    @Description("Send a message to a player. Won't be logged in Discord.")
    fun whisper(player: Player, target: Player, message: String): CommandResult? {
        if (player == target) return CommandResult("Cannot whisper to yourself.", CommandResultStatus.Failed)

        val computedMessage = Vars.netServer.chatFormatter.format(player, "[accent]<W> [white]$message")

        Logger.info("${player.name} whisper(${target.name}): $computedMessage")

        player.sendMessage(computedMessage)
        target.sendMessage(computedMessage)

        return null
    }

    @Command(["broadcast", "b"])
    @ClientSide
    @Description("Send a message to nearby player. Won't be logged in Discord.")
    fun broadcast(player: Player, @GTE(1) range: Float, message: String) {
        val filteredMessage = Vars.netServer.admins.filterMessage(player, message) ?: return

        val computedMessage = Vars.netServer.chatFormatter.format(player, "[accent]<B> [white]$message")
        val tileRange = Vars.tilesize * range

        Logger.info("${player.name} broadcast(${tileRange}): $filteredMessage")

        Entities.players.keys.forEach {
            if (it.distanceFrom(player) <= tileRange) {
                it.sendMessage(computedMessage)
            }
        }
    }
}