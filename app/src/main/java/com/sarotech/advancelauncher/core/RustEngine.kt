package com.sarotech.advancelauncher.core

object RustEngine {
    @Volatile
    var isNativeLoaded: Boolean = false
        private set

    init {
        isNativeLoaded = runCatching {
            System.loadLibrary("advance_core")
            true
        }.getOrDefault(false)
    }

    @JvmStatic external fun getEngineVersion(): String
    @JvmStatic external fun normalizeConfig(configJson: String): String
    @JvmStatic external fun fuzzySearchApps(appsJson: String, query: String): String
    @JvmStatic external fun evaluateMath(expression: String): String
    @JvmStatic external fun sortAndCategorizeApps(appsJson: String, sortMode: String): String
    @JvmStatic external fun findNextFreeRect(
        occupiedJson: String,
        cols: Int,
        rows: Int,
        colSpan: Int,
        rowSpan: Int
    ): String
    @JvmStatic external fun validateBackup(backupJson: String): String

    fun safeEngineVersion(): String = if (isNativeLoaded) {
        runCatching { getEngineVersion() }.getOrDefault("Advance Core v2.0.0 (Fallback)")
    } else {
        "Advance Core v2.0.0 (Fallback)"
    }

    fun safeNormalizeConfig(configJson: String): String = if (isNativeLoaded) {
        runCatching { normalizeConfig(configJson) }.getOrDefault(configJson)
    } else configJson

    fun safeFuzzySearch(appsJson: String, query: String): String = if (isNativeLoaded) {
        runCatching { fuzzySearchApps(appsJson, query) }.getOrDefault("[]")
    } else "[]"

    fun safeEvaluateMath(expression: String): String = if (isNativeLoaded) {
        runCatching { evaluateMath(expression) }.getOrDefault("")
    } else ""

    fun safeSortAndCategorize(appsJson: String, sortMode: String): String = if (isNativeLoaded) {
        runCatching { sortAndCategorizeApps(appsJson, sortMode) }.getOrDefault("[]")
    } else "[]"

    fun safeFindNextFreeRect(
        occupiedJson: String,
        cols: Int,
        rows: Int,
        colSpan: Int = 1,
        rowSpan: Int = 1
    ): String = if (isNativeLoaded) {
        runCatching { findNextFreeRect(occupiedJson, cols, rows, colSpan, rowSpan) }
            .getOrDefault("{\"page\":0,\"col\":0,\"row\":0,\"col_span\":$colSpan,\"row_span\":$rowSpan}")
    } else {
        "{\"page\":0,\"col\":0,\"row\":0,\"col_span\":$colSpan,\"row_span\":$rowSpan}"
    }

    fun safeValidateBackup(backupJson: String): String = if (isNativeLoaded) {
        runCatching { validateBackup(backupJson) }
            .getOrDefault("{\"ok\":false,\"error\":\"Native core unavailable\"}")
    } else "{\"ok\":false,\"error\":\"Native core unavailable\"}"
}
