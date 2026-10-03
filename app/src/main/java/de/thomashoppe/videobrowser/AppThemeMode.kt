package de.thomashoppe.videobrowser

enum class AppThemeMode(val preferenceValue: String, val label: String) {
    System("system", "Systemeinstellung"),
    Light("light", "Hell"),
    Dark("dark", "Dunkel");

    companion object {
        fun fromPreference(value: String?): AppThemeMode = entries.firstOrNull { it.preferenceValue == value } ?: System
    }
}
