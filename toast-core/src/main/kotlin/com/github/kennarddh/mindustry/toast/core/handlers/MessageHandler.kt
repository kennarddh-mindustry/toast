package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ClientSide
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.standard.commands.parameters.validations.numbers.GTE
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.distanceFrom
import mindustry.Vars
import mindustry.gen.Groups
import mindustry.gen.Player

class MessageHandler : Handler() {
    @Command(["whisper", "w"])
    @ClientSide
    @Description("Send a message to a player. Won't be logged in Discord.")
    fun whisper(player: Player, target: Player, message: String) {
        val computedMessage = Vars.netServer.chatFormatter.format(player, message)

        Logger.info("${player.name} whisper(${target.name}): $computedMessage")

        player.sendMessage(computedMessage)
        target.sendMessage(computedMessage)
    }

    @Command(["broadcast", "b"])
    @ClientSide
    @Description("Send a message to nearby player. Won't be logged in Discord.")
    fun broadcast(player: Player, @GTE(1) range: Float, message: String) {
        val filteredMessage = Vars.netServer.admins.filterMessage(player, message) ?: return

        val computedMessage = Vars.netServer.chatFormatter.format(player, filteredMessage)
        val tileRange = Vars.tilesize * range

        Logger.info("${player.name} broadcast(${tileRange}): $filteredMessage")

        Groups.player.each {
            if (it.distanceFrom(player) <= tileRange) {
                it.sendMessage(computedMessage)
            }
        }
    }
}