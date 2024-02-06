package com.github.kennarddh.mindustry.toast.core.commands.validations

import com.github.kennarddh.mindustry.genesis.core.commands.annotations.validations.CommandValidation
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.validations.CommandValidationDescription
import com.github.kennarddh.mindustry.toast.common.CoroutineScopes
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.common.database.tables.Users
import com.github.kennarddh.mindustry.toast.core.commons.getUser
import kotlinx.coroutines.runBlocking
import mindustry.gen.Player
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction


fun validateMinimumRole(annotation: Annotation, player: Player?): Boolean = runBlocking {
    if (player == null) {
        // On server console
        return@runBlocking true
    }

    val minimumRole = (annotation as MinimumRole).minimumRole

    newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
        // If user is null it means the user is not logged in
        val user = player.getUser() ?: return@newSuspendedTransaction false

        val userRole = user[Users.role]

        return@newSuspendedTransaction userRole >= minimumRole
    }
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@CommandValidation
@CommandValidationDescription("Command your role must be equal or greater than :minimumRole: to use :commandName:.")
annotation class MinimumRole(val minimumRole: UserRole)
