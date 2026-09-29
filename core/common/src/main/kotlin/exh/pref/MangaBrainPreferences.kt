package exh.pref

import tachiyomi.core.common.preference.PreferenceStore

class MangaBrainPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun baseUrl() = preferenceStore.getString("mangabrain_base_url", "")

    fun apiToken() = preferenceStore.getString("mangabrain_api_token", "")

    /**
     * When true and a base URL is configured, [MangaBrainPagingSource] replaces the
     * default tracker-backed recommenders (AniList / MangaUpdates / MyAnimeList) on the
     * per-manga Recommends screen. Source-native recommenders (Comick, MangaDex-similar)
     * remain in the list either way.
     */
    fun preferOverFallback() = preferenceStore.getBoolean("mangabrain_prefer_over_fallback", false)
}
