package com.github.kennarddh.mindustry.toast.discord.listeners

import com.github.kennarddh.mindustry.toast.discord.CoroutineScopes
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter


object MapListener : ListenerAdapter() {
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name == "map") {
            if (event.subcommandName == "submit") {
                CoroutineScopes.IO.launch {
                    event.reply("Processing").setEphemeral(true).queue();
                }
            }
        }
    }
}
