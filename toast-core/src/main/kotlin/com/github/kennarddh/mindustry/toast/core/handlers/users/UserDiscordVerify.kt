package com.github.kennarddh.mindustry.toast.core.handlers.users

import com.github.kennarddh.mindustry.genesis.core.commands.annotations.ClientSide
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Command
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.Description
import com.github.kennarddh.mindustry.genesis.core.commands.result.CommandResult
import com.github.kennarddh.mindustry.genesis.core.commands.result.CommandResultStatus
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.common.database.Database
import com.github.kennarddh.mindustry.toast.common.database.tables.Users
import com.github.kennarddh.mindustry.toast.common.verify.discord.VerifyDiscordRedis
import com.github.kennarddh.mindustry.toast.core.commands.validations.LoggedIn
import com.github.kennarddh.mindustry.toast.core.commons.getUser
import mindustry.gen.Player
import java.security.SecureRandom

class UserDiscordVerify : Handler() {
    private val pinSecureRandom = SecureRandom()

    @Command(["verify"])
    @ClientSide
    @LoggedIn
    @Description("Verify account with discord account.")
    suspend fun verify(player: Player): CommandResult {
        val user = Database.newTransaction { player.getUser()!! }

        if (user[Users.discordID] != null) return CommandResult(
            "You have already verified your discord account",
            CommandResultStatus.Failed
        )

        // 6 digits pin
        val pin = pinSecureRandom.nextInt(100000, 999999)

        VerifyDiscordRedis.set(user[Users.id].value, pin)

        return CommandResult(
            """
            Your pin is $pin.
            Do not share your pin to anyone.
            Do /verify in Toast Mindustry discord and enter your pin to be verified.
            Your pin will expire in 5 minutes.
            Do /discord to join Toast Mindustry discord.
            """.trimIndent()
        )
    }
}