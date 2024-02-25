package com.github.kennarddh.mindustry.toast.discord.listeners

import com.github.kennarddh.mindustry.toast.common.Server
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.ChatServerControl
import com.github.kennarddh.mindustry.toast.common.messaging.messages.ServerCommandServerControl
import com.github.kennarddh.mindustry.toast.common.messaging.messages.ServerControl
import com.github.kennarddh.mindustry.toast.discord.CoroutineScopes
import com.github.kennarddh.mindustry.toast.discord.Logger
import com.github.kennarddh.mindustry.toast.discord.jda
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData


object ServerControlListener : ListenerAdapter() {
    override fun onReady(event: ReadyEvent) {
        val serverOptionData = OptionData(OptionType.STRING, "server", "Server").setRequired(true)

        Server.entries.forEach {
            serverOptionData.addChoice(it.displayName, it.name)
        }

        jda.updateCommands()
            .addCommands(
                Commands.slash("send-server-command", "Send server command to a mindustry server")
                    .addOptions(serverOptionData)
                    .addOption(OptionType.STRING, "command", "Command to send.", true)
                    .setDefaultPermissions(
                        DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)
                    )
            ).addCommands(
                Commands.slash("send-chat", "Send chat message to a mindustry server")
                    .addOptions(serverOptionData)
                    .addOption(OptionType.STRING, "message", "Message to send.", true)
            ).queue()
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name == "send-server-command") {
            val serverString = event.getOption("server")!!.asString
            val command = event.getOption("command")!!.asString

            val server = try {
                Server.valueOf(serverString)
            } catch (error: IllegalArgumentException) {
                event.reply("$serverString is not a valid server").queue()

                return
            }

            Logger.info("Routing key \"${server.name}.server-command\"")

            CoroutineScopes.Main.launch {
                Messenger.publishServerControl(
                    "${server.name}.server-command",
                    ServerControl(
                        Clock.System.now().toEpochMilliseconds(),
                        ServerCommandServerControl(command)
                    )
                )
            }

            event.reply("Done").queue()
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
                        ChatServerControl(event.user.effectiveName, message)
                    )
                )
            }
            
            event.reply("Done").queue()
        }
    }
}
