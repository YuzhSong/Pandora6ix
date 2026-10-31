package com.pandora6ix.app.data

/** Per-account theme selection over the app's private persistent preference store. */
class ThemePreferences(
    private val read: (String) -> String?,
    private val write: (String, String) -> Unit,
    private val supported: Set<String>,
    private val default: String = "warm"
) {
    fun theme(ownerId: String?): String = ownerId?.let { read("theme_$it") }
        ?.takeIf { it in supported } ?: default

    fun select(ownerId: String, theme: String) {
        require(ownerId.isNotBlank() && theme in supported)
        write("theme_$ownerId", theme)
    }
}
