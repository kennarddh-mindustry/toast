package com.github.kennarddh.mindustry.toast.common.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object UserVoteKick : IntIdTable() {
    val starterUserID = reference("starterUserID", Users)
    val userKickID = reference("userKickID", UserKick)
    val requiredVotes = short("requiredVotes")
}