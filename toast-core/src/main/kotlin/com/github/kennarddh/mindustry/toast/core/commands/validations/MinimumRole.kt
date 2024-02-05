package com.github.kennarddh.mindustry.toast.core.commands.validations

import com.github.kennarddh.mindustry.genesis.core.commands.annotations.validations.CommandValidation
import com.github.kennarddh.mindustry.genesis.core.commands.annotations.validations.CommandValidationDescription
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUser
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUserServerData
import com.github.kennarddh.mindustry.toast.common.database.tables.Users
import com.github.kennarddh.mindustry.toast.common.selectOne
import mindustry.gen.Player
import org.jetbrains.exposed.sql.JoinType


fun validateMinimumRole(annotation: Annotation, player: Player?): Boolean {
    if (player == null) {
        // On server console
        return true
    }

    val minimumRole = (annotation as MinimumRole).minimumRole

    val user = Users.join(
        MindustryUserServerData,
        JoinType.INNER,
        onColumn = Users.id,
        otherColumn = MindustryUserServerData.userID
    ).join(
        MindustryUser,
        JoinType.INNER,
        onColumn = MindustryUserServerData.mindustryUserID,
        otherColumn = MindustryUser.id
    ).selectOne { MindustryUser.mindustryUUID eq player.uuid() }

    // Non registered user
    if (user == null) return false

    val userRole = user[Users.role]

    return userRole >= minimumRole
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@CommandValidation
@CommandValidationDescription("Command your role must be equal or greater than :minimumRole: to use :commandName:.")
annotation class MinimumRole(val minimumRole: UserRole)
