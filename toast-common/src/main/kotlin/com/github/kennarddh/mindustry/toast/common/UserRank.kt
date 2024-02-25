package com.github.kennarddh.mindustry.toast.common

enum class UserRank(val displayName: String, val minXP: Int) {
    BeyondBad("Beyond Bad", -1),
    Duo1("Duo 1", 0),
    Duo2("Duo 2", 1000),
    Duo3("Duo 3", 2000),
    Scatter1("Scatter 1", 3000),
    Scatter2("Scatter 2", 5000),
    Scatter3("Scatter 3", 7000),
    Scorch1("Scorch 1", 10000),
    Scorch2("Scorch 2", 13000),
    Scorch3("Scorch 3", 16000),
    Hail1("Hail 1", 19000),
    Hail2("Hail 2", 23000),
    Hail3("Hail 3", 27000),
    Wave1("Wave 1", 31000),
    Wave2("Wave 2", 36000),
    Wave3("Wave 3", 41000),
    Lancer1("Lancer 1", 47000),
    Lancer2("Lancer 2", 53000),
    Lancer3("Lancer 3", 59000);

    companion object {
        val sortedDescending: List<UserRank> = entries.sortedByDescending { it.minXP }

        fun getRank(xp: Int): UserRank {
            for (userRank in sortedDescending) {
                if (userRank == BeyondBad) continue

                if (xp < userRank.minXP) continue

                return userRank
            }

            return BeyondBad
        }
    }
}