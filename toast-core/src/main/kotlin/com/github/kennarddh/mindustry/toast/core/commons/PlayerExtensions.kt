package com.github.kennarddh.mindustry.toast.core.commons

import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUser
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUserServerData
import com.github.kennarddh.mindustry.toast.common.database.tables.Users
import com.github.kennarddh.mindustry.toast.common.selectOne
import mindustry.gen.Player
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Op

val Player.mindustryServerUserDataWhereClause: Op<Boolean>
    get() = con.mindustryServerUserDataWhereClause

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