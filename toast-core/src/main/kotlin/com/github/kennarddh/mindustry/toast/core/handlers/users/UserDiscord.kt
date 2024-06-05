package com.github.kennarddh.mindustry.toast.core.handlers.users

import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.commands.senders.PlayerCommandSender
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.common.database.Database
import com.github.kennarddh.mindustry.toast.common.database.tables.Users
import com.github.kennarddh.mindustry.toast.common.verify.discord.VerifyDiscordRedis
import com.github.kennarddh.mindustry.toast.core.commands.validations.LoggedIn
import com.github.kennarddh.mindustry.toast.core.commons.getUser
import org.jetbrains.exposed.sql.update
import java.security.SecureRandom

class UserDiscord : Handler {
    private val pinSecureRandom = SecureRandom()

    @Command(["verify"])
    @LoggedIn
    @Description("Verify account with discord account.")
    suspend fun verify(sender: PlayerCommandSender) {
        val user = Database.newTransaction { sender.player.getUser()!! }

        if (user[Users.discordID] != null)
            return sender.sendError(
                "You have already verified your discord account.",
            )

        // 6 digits pin
        val pin = pinSecureRandom.nextInt(100000, 999999)

        VerifyDiscordRedis.set(user[Users.id].value, pin)

        sender.sendSuccess(
            """
            Your pin is $pin.
            Do not share your pin to anyone.
            Do /verify in Toast Mindustry Discord and enter your pin to be verified.
            Your pin will expire in 5 minutes.
            Do /discord to join Toast Mindustry Discord.
            """.trimIndent()
        )
    }


    @Command(["unlink", "unverify"])
    @LoggedIn
    @Description("Unlink this account from the discord account.")
    suspend fun unlink(sender: PlayerCommandSender) {
        Database.newTransaction {
            val user = sender.player.getUser()!!

            if (user[Users.discordID] == null)
                return@newTransaction sender.sendError(
                    "You haven't verified your discord account.",
                )

            Users.update({ Users.id eq user[Users.id] }) {
                it[this.discordID] = null
            }

            sender.sendSuccess("You have unlinked this account with <@${user[Users.discordID]}> discord account.")
        }
    }
}