package com.github.kennarddh.mindustry.toast.core.commons

import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUser
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUserServerData
import com.github.kennarddh.mindustry.toast.common.database.tables.Users
import com.github.kennarddh.mindustry.toast.common.selectOne
import mindustry.gen.Player
import org.jetbrains.exposed.sql.JoinType

fun Player.getMindustryUserAndUserServerData() =
    MindustryUserServerData
        .join(
            MindustryUser,
            JoinType.INNER,
            onColumn = MindustryUserServerData.mindustryUserID,
            otherColumn = MindustryUser.id
        )
        .selectOne {
            MindustryUser.mindustryUUID eq uuid()
            MindustryUserServerData.server eq ToastVars.server
        }

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
    ).selectOne {
        MindustryUser.mindustryUUID eq uuid()
        MindustryUserServerData.server eq ToastVars.server
    }

fun Player.getUserAndMindustryUser() =
    Users.join(
        MindustryUser,
        JoinType.INNER,
        onColumn = MindustryUserServerData.mindustryUserID,
        otherColumn = MindustryUser.id
    ).selectOne { MindustryUser.mindustryUUID eq uuid() }

fun Player.getMindustryUser() =
    MindustryUser.selectOne { MindustryUser.mindustryUUID eq uuid() }