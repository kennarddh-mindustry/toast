package com.github.kennarddh.mindustry.toast.core.commands.validations

import com.github.kennarddh.mindustry.genesis.core.commands.annotations.validations.CommandValidation
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.validations.CommandValidationDescription
import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.toast.common.UserRank
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUserServerData
import com.github.kennarddh.mindustry.toast.core.commons.getMindustryUserAndUserServerData
import mindustry.gen.Player
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction


suspend fun validateMinimumRank(annotation: Annotation, player: Player?): Boolean {
    if (player == null) {
        // On server console
        return true
    }

    val minimumRank = (annotation as MinimumRank).minimumRank

    return newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
        // If user is null it means the user is not logged in
        val mindustryUserAndUserServerData = player.getMindustryUserAndUserServerData()!!

        val xp = mindustryUserAndUserServerData[MindustryUserServerData.xp]

        val rank = UserRank.getRank(xp)

        return@newSuspendedTransaction rank >= minimumRank
    }
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@CommandValidation
@CommandValidationDescription("Your rank must be equal or greater than :minimumRank: to use :commandName:.")
annotation class MinimumRank(val minimumRank: UserRank)
