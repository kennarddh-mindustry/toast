package com.github.kennarddh.mindustry.toast.discord.listeners

import com.github.kennarddh.mindustry.toast.common.Server
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.common.database.Database
import com.github.kennarddh.mindustry.toast.common.database.tables.Users
import com.github.kennarddh.mindustry.toast.common.extensions.preventDiscordPings
import com.github.kennarddh.mindustry.toast.common.extensions.selectOne
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.ChatServerControl
import com.github.kennarddh.mindustry.toast.common.messaging.messages.ServerCommandServerControl
import com.github.kennarddh.mindustry.toast.common.messaging.messages.ServerControl
import com.github.kennarddh.mindustry.toast.discord.CoroutineScopes
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter


object ServerControlListener : ListenerAdapter() {
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name == "send-server-command") {
            CoroutineScopes.IO.launch {
                val user = Database.newTransaction {
                    Users.selectOne { Users.discordID eq event.user.id }
                }

                if (user == null) {
                    return@launch event.reply("Your must verify your discord account before using this.")
                        .setEphemeral(true)
                        .queue()
                }

                if (user[Users.role] < UserRole.CoOwner) {
                    return@launch event.reply("Your role must be greater than or equal to CoOwner.")
                        .setEphemeral(true)
                        .queue()
                }

                val serverString = event.getOption("server")!!.asString
                val command = event.getOption("command")!!.asString

                val server = try {
                    Server.valueOf(serverString)
                } catch (error: IllegalArgumentException) {
                    event.reply("$serverString is not a valid server").queue()

                    return@launch
                }

                Messenger.publishServerControl(
                    "${server.name}.server-command",
                    ServerControl(
                        Clock.System.now().toEpochMilliseconds(),
                        ServerCommandServerControl(command)
                    )
                )

                event.reply("Done").queue()
            }
        } else if (event.name == "send-chat") {
            val serverString = event.getOption("server")!!.asString
            val message = event.getOption("message")!!.asString

            val server = try {
                Server.valueOf(serverString)
            } catch (error: IllegalArgumentException) {
                event.reply("$serverString is not a valid server").queue()

                return
            }

            if (message == "") {
                event.reply("Message cannot be empty").queue()

                return
            }

            CoroutineScopes.Main.launch {
                Messenger.publishServerControl(
                    "${server.name}.chat",
                    ServerControl(
                        Clock.System.now().toEpochMilliseconds(),
                        ChatServerControl(event.user.effectiveName, message.preventDiscordPings())
                    )
                )
            }

            event.reply("Done").queue()
        }
    }
}
