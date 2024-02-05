package com.github.kennarddh.mindustry.toast.core.commons

import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUser
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUserServerData
import com.github.kennarddh.mindustry.toast.common.selectOne
import mindustry.gen.Player
import org.jetbrains.exposed.sql.JoinType

fun Player.getMindustryUserServerData() =
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
