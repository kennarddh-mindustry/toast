package com.github.kennarddh.mindustry.toast.core.commands.validations

import com.github.kennarddh.mindustry.genesis.core.commands.annotations.validations.CommandValidation
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.validations.CommandValidationDescription
import com.github.kennarddh.mindustry.toast.common.UserRank
import com.github.kennarddh.mindustry.toast.core.commons.safeGetPlayerData
import mindustry.gen.Player


fun validateMinimumRank(annotation: Annotation, player: Player?): Boolean {
    if (player == null) {
        // On server console
        return true
    }

    val minimumRank = (annotation as MinimumRank).minimumRank

    val playerData = player.safeGetPlayerData() ?: return false

    val rank = UserRank.getRank(playerData.xp)

    return rank >= minimumRank
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@CommandValidation
@CommandValidationDescription("Your rank must be equal or greater than :minimumRank: to use :commandName:.")
annotation class MinimumRank(val minimumRank: UserRank)
