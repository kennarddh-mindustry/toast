package com.github.kennarddh.mindustry.toast.discord.listeners

import com.github.kennarddh.mindustry.toast.discord.*
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

object ReadyListener : ListenerAdapter() {
    override fun onReady(event: ReadyEvent) {
        Logger.info("Bot Ready")

        toastMindustryGuild = jda.getGuildById(DiscordConstant.TOAST_MINDUSTRY_GUILD_ID)!!
        notificationChannel = toastMindustryGuild.getTextChannelById(DiscordConstant.NOTIFICATIONS_CHANNEL_ID)!!
        reportsChannel = toastMindustryGuild.getTextChannelById(DiscordConstant.REPORTS_CHANNEL_ID)!!
        serverListChannel = toastMindustryGuild.getTextChannelById(DiscordConstant.SERVER_LIST_CHANNEL_ID)!!
    }
}