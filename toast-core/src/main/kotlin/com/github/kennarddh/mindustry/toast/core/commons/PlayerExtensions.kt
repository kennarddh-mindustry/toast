package com.github.kennarddh.mindustry.toast.core.commons

import arc.math.Mathf
import com.github.kennarddh.mindustry.toast.common.UserRole
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUser
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUserServerData
import com.github.kennarddh.mindustry.toast.common.database.tables.Users
import com.github.kennarddh.mindustry.toast.common.selectOne
import com.github.kennarddh.mindustry.toast.core.commons.entities.Entities
import com.github.kennarddh.mindustry.toast.core.commons.entities.PlayerData
import mindustry.gen.Player
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow

fun Player.distanceFrom(other: Player): Float =
    Mathf.sqrt(Mathf.pow(x - other.x, 2f) + Mathf.pow(y - other.y, 2f))

val Player.mindustryServerUserDataWhereClause: Op<Boolean>
    get() = con.mindustryServerUserDataWhereClause

/**
 * Cannot be used before player is added to the "Entities.players" map
 */
fun Player.getUser(): ResultRow? {
    val userID = safeGetPlayerData()?.userID ?: return null

    return Users.selectOne { Users.id eq userID }
}

fun Player.safeGetPlayerData(): PlayerData? {
    val playerData = Entities.players[this]

    if (playerData == null) {
        sendMessage("There is an error. Please report this and explain what make this happen. Error code: NULL_PLAYER_ENTITY")

        return null
    }

    return playerData
}

fun Player.applyName(role: UserRole?) {
    val user = safeGetPlayerData() ?: return

    val roleDisplayName = role?.displayName ?: "Public"

    name = "[accent]<$roleDisplayName> [#${color}]${user.originalName}"
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