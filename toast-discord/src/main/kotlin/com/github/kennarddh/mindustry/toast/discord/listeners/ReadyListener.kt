package com.github.kennarddh.mindustry.toast.discord.listeners

import com.github.kennarddh.mindustry.toast.common.Server
import com.github.kennarddh.mindustry.toast.discord.*
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData

object ReadyListener : ListenerAdapter() {
    override fun onReady(event: ReadyEvent) {
        Logger.info("Bot Ready")

        toastMindustryGuild = jda.getGuildById(DiscordConstant.TOAST_MINDUSTRY_GUILD_ID)!!
        notificationChannel = toastMindustryGuild.getTextChannelById(DiscordConstant.NOTIFICATIONS_CHANNEL_ID)!!
        reportsChannel = toastMindustryGuild.getTextChannelById(DiscordConstant.REPORTS_CHANNEL_ID)!!
        serverListChannel = toastMindustryGuild.getTextChannelById(DiscordConstant.SERVER_LIST_CHANNEL_ID)!!
        roleChangesChannel = toastMindustryGuild.getTextChannelById(DiscordConstant.ROLE_CHANGES_CHANNEL_ID)!!

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
            )
            .addCommands(
                Commands.slash("send-chat", "Send chat message to a mindustry server")
                    .addOptions(serverOptionData)
                    .addOption(OptionType.STRING, "message", "Message to send.", true)
            )
            .addCommands(
                Commands.slash("verify", "Verify your mindustry account with discord")
                    .addOption(OptionType.STRING, "username", "Mindustry account username.", true)
                    .addOption(OptionType.INTEGER, "pin", "Pin.", true)
            )
            .queue()
    }
}