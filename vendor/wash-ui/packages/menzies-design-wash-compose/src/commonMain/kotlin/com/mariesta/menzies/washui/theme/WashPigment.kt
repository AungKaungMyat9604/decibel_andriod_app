package com.mariesta.menzies.washui.theme

enum class WashMode {
    Light,
    Dark,
}

enum class WashPigment {
    mineral,
    indigo,
    celadon,
    vermilion,
    sepia,
    cobalt,
    moss,
    saffron,
    slate,
    lake,
    ultramarine,
    viridian,
    madder,
    ochre,
    umber,
    ivory,
    cerulean,
    crimson,
    olive,
    sienna,
    turquoise,
    lavender,
    charcoal,
    coral,
    pine,
    bronze,
    mist,
    rust,
    jade,
    ink,
    ;

    val id: String get() = name

    companion object {
        fun fromId(value: String): WashPigment =
            entries.find { it.name == value } ?: mineral
    }
}

data class WashPigmentMeta(
    val id: WashPigment,
    val label: String,
    val note: String,
    val swatchHex: String,
)

val washPigmentCatalog: List<WashPigmentMeta> = listOf(
    WashPigmentMeta(WashPigment.mineral, "Mineral", "Blue · ochre · rose", "#276C8E"),
    WashPigmentMeta(WashPigment.indigo, "Indigo", "Deep lake violet", "#3D4F8F"),
    WashPigmentMeta(WashPigment.celadon, "Celadon", "Sage glaze", "#3D7A5F"),
    WashPigmentMeta(WashPigment.vermilion, "Vermilion", "Warm lake red", "#B8432F"),
    WashPigmentMeta(WashPigment.sepia, "Sepia", "Archival ink", "#6B4E32"),
    WashPigmentMeta(WashPigment.cobalt, "Cobalt", "Bright mineral blue", "#1F5F9E"),
    WashPigmentMeta(WashPigment.moss, "Moss", "Botanical green", "#4A6B3A"),
    WashPigmentMeta(WashPigment.saffron, "Saffron", "Gold ochre", "#C48A28"),
    WashPigmentMeta(WashPigment.slate, "Slate", "Cool pigment gray", "#5A6573"),
    WashPigmentMeta(WashPigment.lake, "Lake", "Viridian teal", "#2A7A72"),
    WashPigmentMeta(WashPigment.ultramarine, "Ultramarine", "Deep lapis blue", "#2F4A9B"),
    WashPigmentMeta(WashPigment.viridian, "Viridian", "Cool chrome green", "#2F7A68"),
    WashPigmentMeta(WashPigment.madder, "Madder", "Rose madder lake", "#A63D52"),
    WashPigmentMeta(WashPigment.ochre, "Ochre", "Yellow earth", "#B8892E"),
    WashPigmentMeta(WashPigment.umber, "Umber", "Burnt earth brown", "#6B4A32"),
    WashPigmentMeta(WashPigment.ivory, "Ivory", "Ivory black gray", "#4A4842"),
    WashPigmentMeta(WashPigment.cerulean, "Cerulean", "Sky mineral blue", "#3A7CA8"),
    WashPigmentMeta(WashPigment.crimson, "Crimson", "Deep carmine", "#9E2F3E"),
    WashPigmentMeta(WashPigment.olive, "Olive", "Muted leaf green", "#6A6B3A"),
    WashPigmentMeta(WashPigment.sienna, "Sienna", "Raw earth orange", "#A65A32"),
    WashPigmentMeta(WashPigment.turquoise, "Turquoise", "Copper blue-green", "#2A8A8E"),
    WashPigmentMeta(WashPigment.lavender, "Lavender", "Soft mineral violet", "#6A5A8E"),
    WashPigmentMeta(WashPigment.charcoal, "Charcoal", "Graphite gray", "#3E4248"),
    WashPigmentMeta(WashPigment.coral, "Coral", "Warm shell pink", "#C45A4A"),
    WashPigmentMeta(WashPigment.pine, "Pine", "Forest needle green", "#3A5E42"),
    WashPigmentMeta(WashPigment.bronze, "Bronze", "Metallic ochre", "#8E6A32"),
    WashPigmentMeta(WashPigment.mist, "Mist", "Cool vapor gray", "#6A7A88"),
    WashPigmentMeta(WashPigment.rust, "Rust", "Iron oxide", "#A04828"),
    WashPigmentMeta(WashPigment.jade, "Jade", "Soft stone green", "#3A8A6A"),
    WashPigmentMeta(WashPigment.ink, "Ink", "Sumi black-blue", "#2A3548"),
)

const val THEME_STORAGE_KEY = "design-web-menzies-theme"
const val MODE_STORAGE_KEY = "design-web-menzies-mode"
