package com.decibel.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LibraryScope {
    Videos,
    Music,
    Favourites,
}

enum class LibrarySort {
    Custom,
    Az,
    ;

    val label: String
        get() = when (this) {
            Custom -> "Custom order"
            Az -> "A–Z"
        }
}

/** Persists Library tab scope/sort so they survive process death. */
class LibraryPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _scope = MutableStateFlow(loadScope())
    val scope: StateFlow<LibraryScope> = _scope.asStateFlow()

    private val _sort = MutableStateFlow(loadSort())
    val sort: StateFlow<LibrarySort> = _sort.asStateFlow()

    fun setScope(scope: LibraryScope) {
        prefs.edit().putString(KEY_SCOPE, scope.name).apply()
        _scope.value = scope
    }

    fun setSort(sort: LibrarySort) {
        prefs.edit().putString(KEY_SORT, sort.name).apply()
        _sort.value = sort
    }

    private fun loadScope(): LibraryScope =
        LibraryScope.entries.find { it.name == prefs.getString(KEY_SCOPE, null) }
            ?: LibraryScope.Videos

    private fun loadSort(): LibrarySort =
        LibrarySort.entries.find { it.name == prefs.getString(KEY_SORT, null) }
            ?: LibrarySort.Custom

    companion object {
        private const val PREFS = "decibel_library_ui"
        private const val KEY_SCOPE = "scope"
        private const val KEY_SORT = "sort"
    }
}
