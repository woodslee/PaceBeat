package com.wl.pacebeat.data

enum class Timbre {
    ELECTRONIC,
    SINE,
    WOOD
}

enum class SoundCategory(val label: String) {
    CLEAN("纯净 Click"),
    PERCUSSION("打击乐组合")
}

/**
 * Three clean Click profiles come from CC0 Freesound one-shots. The eleven
 * percussion pairs come from the user-provided 1/2-time, 8-second MP3 loops:
 * beat 1 is used as accent and beat 2 as the normal click.
 */
enum class SoundProfile(
    val id: String,
    val title: String,
    val detail: String,
    val timbre: Timbre,
    val accentHz: Double,
    val normalHz: Double,
    val category: SoundCategory
) {
    CC0_MPC(
        id = "cc0_mpc",
        title = "纯净 Click · MPC",
        detail = "CC0 · trader_one",
        timbre = Timbre.SINE,
        accentHz = 1000.00,
        normalHz = 820.00,
        category = SoundCategory.CLEAN
    ),
    CC0_ATONAL(
        id = "cc0_atonal",
        title = "纯净 Click · 高频",
        detail = "CC0 · lennartgreen",
        timbre = Timbre.SINE,
        accentHz = 1600.00,
        normalHz = 1300.00,
        category = SoundCategory.CLEAN
    ),
    CC0_STUDIO(
        id = "cc0_studio",
        title = "纯净 Click · Studio",
        detail = "CC0 · edgyboi1",
        timbre = Timbre.SINE,
        accentHz = 1000.00,
        normalHz = 820.00,
        category = SoundCategory.CLEAN
    ),
    PERC_WOODBLOCK(
        id = "perc_woodblock",
        title = "木鱼",
        detail = "高 / 低木鱼 · 1/2 拍",
        timbre = Timbre.WOOD,
        accentHz = 1200.00,
        normalHz = 900.00,
        category = SoundCategory.PERCUSSION
    ),
    PERC_CLAVES_WOOD(
        id = "perc_claves_wood",
        title = "古巴响棒 & 木鱼",
        detail = "Claves / Wood Block · 1/2 拍",
        timbre = Timbre.WOOD,
        accentHz = 2000.00,
        normalHz = 900.00,
        category = SoundCategory.PERCUSSION
    ),
    PERC_BONGO(
        id = "perc_bongo",
        title = "邦戈鼓",
        detail = "高 / 低邦戈 · 1/2 拍",
        timbre = Timbre.SINE,
        accentHz = 420.00,
        normalHz = 320.00,
        category = SoundCategory.PERCUSSION
    ),
    PERC_AGOGO(
        id = "perc_agogo",
        title = "双音钟",
        detail = "高 / 低音钟 · 1/2 拍",
        timbre = Timbre.SINE,
        accentHz = 1200.00,
        normalHz = 950.00,
        category = SoundCategory.PERCUSSION
    ),
    PERC_KICK_SNARE(
        id = "perc_kick_snare",
        title = "底鼓 & 军鼓",
        detail = "Kick / Snare · 1/2 拍",
        timbre = Timbre.SINE,
        accentHz = 80.00,
        normalHz = 200.00,
        category = SoundCategory.PERCUSSION
    ),
    PERC_KICK_WOOD(
        id = "perc_kick_wood",
        title = "底鼓 & 木鱼",
        detail = "Kick / Wood Block · 1/2 拍",
        timbre = Timbre.SINE,
        accentHz = 80.00,
        normalHz = 900.00,
        category = SoundCategory.PERCUSSION
    ),
    PERC_COWBELL_RIM(
        id = "perc_cowbell_rim",
        title = "牛铃 & 鼓边敲击",
        detail = "Cowbell / Rim · 1/2 拍",
        timbre = Timbre.SINE,
        accentHz = 800.00,
        normalHz = 1500.00,
        category = SoundCategory.PERCUSSION
    ),
    PERC_KICK_RIM(
        id = "perc_kick_rim",
        title = "底鼓 & 鼓边敲击",
        detail = "Kick / Rim · 1/2 拍",
        timbre = Timbre.SINE,
        accentHz = 80.00,
        normalHz = 1500.00,
        category = SoundCategory.PERCUSSION
    ),
    PERC_COWBELL_WOOD(
        id = "perc_cowbell_wood",
        title = "牛铃 & 木鱼",
        detail = "Cowbell / Wood Block · 1/2 拍",
        timbre = Timbre.SINE,
        accentHz = 800.00,
        normalHz = 900.00,
        category = SoundCategory.PERCUSSION
    ),
    PERC_AGOGO_WOOD(
        id = "perc_agogo_wood",
        title = "双音钟 & 木鱼",
        detail = "Agogo / Wood Block · 1/2 拍",
        timbre = Timbre.SINE,
        accentHz = 1200.00,
        normalHz = 900.00,
        category = SoundCategory.PERCUSSION
    ),
    PERC_KICK_CLAP(
        id = "perc_kick_clap",
        title = "底鼓 & 击掌",
        detail = "Kick / Clap · 1/2 拍",
        timbre = Timbre.SINE,
        accentHz = 80.00,
        normalHz = 1000.00,
        category = SoundCategory.PERCUSSION
    );

    companion object {
        fun fromId(id: String?): SoundProfile = entries.firstOrNull { it.id == id } ?: CC0_MPC
    }
}
