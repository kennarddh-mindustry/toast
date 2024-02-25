package com.github.kennarddh.mindustry.toast.core.commands.validations

import com.github.kennarddh.mindustry.genesis.core.commands.annotations.validations.CommandValidation
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.validations.CommandValidationDescription
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.common.database.Database
import com.github.kennarddh.mindustry.toast.common.database.tables.Users
import com.github.kennarddh.mindustry.toast.core.commons.getUser
import mindustry.gen.Player


suspend fun validateMinimumRole(annotation: Annotation, player: Player?): Boolean {
    if (player == null) {
        // On server console
        return true
    }

    val minimumRole = (annotation as MinimumRole).minimumRole

    return Database.newTransaction {
        // If user is null it means the user is not logged in
        val user = player.getUser() ?: return@newTransaction false

        val userRole = user[Users.role]

        return@newTransaction userRole >= minimumRole
    }
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@CommandValidation
@CommandValidationDescription("Your role must be equal or greater than :minimumRole: to use :commandName:.")
annotation class MinimumRole(val minimumRole: UserRole)
