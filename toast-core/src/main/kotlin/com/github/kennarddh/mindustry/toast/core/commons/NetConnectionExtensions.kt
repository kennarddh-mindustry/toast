package com.github.kennarddh.mindustry.toast.core.commons

import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUser
import com.github.kennarddh.mindustry.toast.common.database.tables.MindustryUserServerData
import mindustry.net.NetConnection
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and


val NetConnection.mindustryServerUserDataWhereClause: Op<Boolean>
    get() = (MindustryUser.mindustryUUID eq uuid) and (MindustryUserServerData.server eq ToastVars.server)
