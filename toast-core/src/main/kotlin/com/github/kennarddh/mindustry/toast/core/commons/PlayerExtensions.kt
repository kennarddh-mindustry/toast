package com.github.kennarddh.mindustry.toast.core.commons

import arc.math.Mathf
import com.github.kennarddh.mindustry.genesis.core.GenesisAPI
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUser
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUserServerData
import com.github.kennarddh.mindustry.toast.common.database.tables.Users
import com.github.kennarddh.mindustry.toast.common.selectOne
import com.github.kennarddh.mindustry.toast.core.handlers.users.User
import com.github.kennarddh.mindustry.toast.core.handlers.users.UserAccountHandler
import mindustry.gen.Player
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow

fun Player.distanceFrom(other: Player): Float =
    Mathf.sqrt(Mathf.pow(x - other.x, 2f) + Mathf.pow(y - other.y, 2f))

val Player.mindustryServerUserDataWhereClause: Op<Boolean>
    get() = con.mindustryServerUserDataWhereClause

/**
 * Depends on UserAccountHandler stored user id
 * Cannot be used before user is added to the users map
 */
fun Player.getUser(): ResultRow? {
    val userID = getStoredUser()?.userID ?: return null

    return Users.selectOne {
        Users.id eq userID
    }
}

fun Player.getStoredUser(): User? {
    return GenesisAPI.getHandler<UserAccountHandler>()!!.users[this]
}

fun Player.applyName(role: UserRole?): Player {
    val user = getStoredUser()!!

    name = if (role == null) {
        user.originalName
    } else {
        "[accent]<${role.displayName}> [#${color}]${user.originalName}"
    }

    return this
}

fun Player.getMindustryUserAndUserServerData() =
    MindustryUserServerData
        .join(
            MindustryUser,
            JoinType.INNER,
            onColumn = MindustryUserServerData.mindustryUserID,
            otherColumn = MindustryUser.id
        )
        .selectOne { mindustryServerUserDataWhereClause }

fun Player.getUserAndMindustryUserAndUserServerData() =
    Users.join(
        MindustryUserServerData,
        JoinType.INNER,
        onColumn = Users.id,
        otherColumn = MindustryUserServerData.userID
    ).join(
        MindustryUser,
        JoinType.INNER,
        onColumn = MindustryUserServerData.mindustryUserID,
        otherColumn = MindustryUser.id
    ).selectOne { mindustryServerUserDataWhereClause }

fun Player.getUserOptionalAndMindustryUserAndUserServerData() =
    MindustryUser.join(
        MindustryUserServerData,
        JoinType.INNER,
        onColumn = MindustryUser.id,
        otherColumn = MindustryUserServerData.mindustryUserID
    ).join(
        Users,
        JoinType.LEFT,
        onColumn = MindustryUserServerData.userID,
        otherColumn = Users.id
    ).selectOne { mindustryServerUserDataWhereClause }

fun Player.getMindustryUser() =
    MindustryUser.selectOne { MindustryUser.mindustryUUID eq uuid() }