package com.github.kennarddh.mindustry.toast.discord.listeners

import com.github.kennarddh.mindustry.toast.common.database.Database
import com.github.kennarddh.mindustry.toast.common.database.tables.Users
import com.github.kennarddh.mindustry.toast.common.selectOne
import com.github.kennarddh.mindustry.toast.common.verify.discord.VerifyDiscordRedis
import com.github.kennarddh.mindustry.toast.discord.CoroutineScopes
import com.github.kennarddh.mindustry.toast.discord.jda
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.jetbrains.exposed.sql.update


object VerifyDiscordHandler : ListenerAdapter() {
    override fun onReady(event: ReadyEvent) {
        jda.updateCommands()
            .addCommands(
                Commands.slash("verify", "Verify your mindustry account with discord")
                    .addOption(OptionType.STRING, "username", "Mindustry account username.", true)
                    .addOption(OptionType.INTEGER, "pin", "Pin.", true)
            ).queue()
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name == "verify") {
            val username = event.getOption("username")!!.asString
            val pin = event.getOption("pin")!!.asInt

            CoroutineScopes.Main.launch {
                Database.newTransaction {
                    val userWithCurrentDiscord = Users.selectOne { Users.discordID eq event.user.id }

                    if (userWithCurrentDiscord != null) {
                        event.reply("This discord account has already been used to verify ${userWithCurrentDiscord[Users.username]} account.")
                            .queue()

                        return@newTransaction
                    }

                    val user = Users.selectOne { Users.username eq username }

                    if (user == null) {
                        event.reply("Cannot find user with the username $username.").queue()

                        return@newTransaction
                    }

                    if (user[Users.discordID] != null) {
                        event.reply("User ${user[Users.username]} has already verified with other discord account.")
                            .queue()

                        return@newTransaction
                    }

                    val correctPin = VerifyDiscordRedis.get(user[Users.id].value)

                    if (correctPin == null) {
                        event.reply("Cannot find pin generated for the user. Join Toast Mindustry Server and do /verify to generate new pin.")
                            .queue()

                        return@newTransaction
                    }

                    if (correctPin != pin) {
                        event.reply("Wrong pin.").queue()

                        return@newTransaction
                    }

                    VerifyDiscordRedis.del(user[Users.id].value)

                    Users.update({
                        Users.id eq user[Users.id]
                    }) {
                        it[discordID] = event.user.id
                    }

                    event.reply("Successfully verified user $username with this discord account.").queue()
                }
            }
        }
    }
}
