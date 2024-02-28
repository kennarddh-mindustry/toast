package com.github.kennarddh.mindustry.toast.core.commands.validations

import com.github.kennarddh.mindustry.genesis.core.commands.annotations.validations.CommandValidation
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.validations.CommandValidationDescription
import com.github.kennarddh.mindustry.toast.core.commons.entities.Entities
import mindustry.gen.Player


fun validateLoggedIn(annotation: Annotation, player: Player?): Boolean {
    if (player == null) {
        // On server console
        return true
    }

    return Entities.players[player]?.userID != null
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@CommandValidation
@CommandValidationDescription("You must be registered to use :commandName:.")
annotation class LoggedIn
