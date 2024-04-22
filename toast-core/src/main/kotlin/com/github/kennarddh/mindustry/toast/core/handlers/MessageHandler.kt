package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.commands.senders.CommandSender
import com.github.kennarddh.mindustry.genesis.core.commands.senders.PlayerCommandSender
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.standard.commands.parameters.validations.numbers.GTE
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.distanceFrom
import com.github.kennarddh.mindustry.toast.core.commons.entities.Entities
import com.github.kennarddh.mindustry.toast.core.commons.extensions.getName
import mindustry.Vars
import mindustry.gen.Player

class MessageHandler : Handler {
    @Command(["whisper", "w"])
    @Description("Send a message to a player. Won't be logged in Discord.")
    fun whisper(sender: CommandSender, target: Player, message: String) {
        if (sender is PlayerCommandSender && sender.player == target) return sender.sendError("Cannot whisper to yourself.")

        val filteredMessage =
            if (sender is PlayerCommandSender)
                Vars.netServer.admins.filterMessage(sender.player, message) ?: return
            else message

        val computedMessage =
            Vars.netServer.chatFormatter.format(
                if (sender is PlayerCommandSender) sender.player else null,
                "[accent]<W> [white]$filteredMessage"
            )

        Logger.info("${sender.getName()} whisper(${target.name}): $computedMessage")

        sender.sendMessage(computedMessage)
        target.sendMessage(computedMessage)
    }

    @Command(["broadcast", "b"])
    @Description("Send a message to nearby player. Won't be logged in Discord.")
    fun broadcast(sender: PlayerCommandSender, @GTE(1) range: Float, message: String) {
        val filteredMessage = Vars.netServer.admins.filterMessage(sender.player, message) ?: return

        val computedMessage = Vars.netServer.chatFormatter.format(sender.player, "[accent]<B> [white]$message")
        val tileRange = Vars.tilesize * range

        Logger.info("${sender.getName()} broadcast(${tileRange}): $filteredMessage")

        Entities.players.keys.forEach {
            if (it.distanceFrom(sender.player) <= tileRange) {
                it.sendMessage(computedMessage)
            }
        }
    }
}