package com.github.kennarddh.mindustry.toast.core.commands.validations

import com.github.kennarddh.mindustry.genesis.core.commands.annotations.validations.CommandValidation
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.validations.CommandValidationDescription
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.core.commons.safeGetPlayerData
import mindustry.gen.Player


fun validateMinimumRole(annotation: Annotation, player: Player?): Boolean {
    if (player == null) {
        // On server console
        return true
    }

    val minimumRole = (annotation as MinimumRole).minimumRole

    val playerData = player.safeGetPlayerData() ?: return false

    val role = playerData.role ?: return false

    return role >= minimumRole
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@CommandValidation
@CommandValidationDescription("Your role must be equal or greater than :minimumRole: to use :commandName:.")
annotation class MinimumRole(val minimumRole: UserRole)
