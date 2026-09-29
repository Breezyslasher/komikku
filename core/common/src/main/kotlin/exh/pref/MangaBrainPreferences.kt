package exh.pref

import tachiyomi.core.common.preference.PreferenceStore

class MangaBrainPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun baseUrl() = preferenceStore.getString("mangabrain_base_url", "")

    fun apiToken() = preferenceStore.getString("mangabrain_api_token", "")

    // Similarity weights, 0-100. MangaBrain normalizes them, so only their ratio matters.
    fun weightSemantic() = preferenceStore.getInt("mangabrain_w_semantic", 50)

    fun weightTags() = preferenceStore.getInt("mangabrain_w_tags", 30)

    fun weightGenres() = preferenceStore.getInt("mangabrain_w_genres", 20)

    // AniList average score, 0-100. 0 disables the filter.
    fun minScore() = preferenceStore.getInt("mangabrain_min_score", 0)

    // Empty set means "any".
    fun statuses() = preferenceStore.getStringSet("mangabrain_statuses", emptySet())

    fun countries() = preferenceStore.getStringSet("mangabrain_countries", emptySet())

    fun formats() = preferenceStore.getStringSet("mangabrain_formats", emptySet())

    // Free-text numbers so they can use the EditText widget; blank means "no limit".
    fun yearMin() = preferenceStore.getString("mangabrain_year_min", "")

    fun yearMax() = preferenceStore.getString("mangabrain_year_max", "")

    fun maxPopularity() = preferenceStore.getString("mangabrain_max_popularity", "")

    fun includeAdult() = preferenceStore.getBoolean("mangabrain_adult", false)

    // Hide titles already on these users' lists (MangaBrain must have synced them).
    fun excludeMalUser() = preferenceStore.getString("mangabrain_exclude_mal_user", "")

    fun excludeAnilistUser() = preferenceStore.getString("mangabrain_exclude_anilist_user", "")
}
