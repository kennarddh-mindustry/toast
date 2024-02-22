package com.github.kennarddh.mindustry.toast.common.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object UserVoteKickVotes : IntIdTable() {
    val userPunishmentID = reference("userPunishmentID", UserPunishments)
    val mindustryUserID = reference("mindustryUserID", MindustryUser)
    val vote = bool("vote")
}