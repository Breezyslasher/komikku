package exh.recs.sources

import androidx.compose.runtime.Immutable
import exh.pref.MangaBrainPreferences
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

@Immutable
data class MangaBrainFilters(
    val weightSemantic: Int = 50,
    val weightTags: Int = 30,
    val weightGenres: Int = 20,
    val minScore: Int = 0,
    val statuses: Set<String> = emptySet(),
    val countries: Set<String> = emptySet(),
    val formats: Set<String> = emptySet(),
    val yearMin: Int? = null,
    val yearMax: Int? = null,
    val maxPopularity: Int? = null,
    val includeAdult: Boolean = false,
    val excludeMalUser: String = "",
    val excludeAnilistUser: String = "",
) {

    fun applyTo(url: HttpUrl.Builder): HttpUrl.Builder = url.apply {
        // All-zero weights would make every candidate score 0; fall back to MangaBrain's defaults.
        if (weightSemantic + weightTags + weightGenres > 0) {
            addQueryParameter("w_semantic", (weightSemantic / 100.0).toString())
            addQueryParameter("w_tags", (weightTags / 100.0).toString())
            addQueryParameter("w_genres", (weightGenres / 100.0).toString())
        }
        if (minScore > 0) addQueryParameter("min_score", minScore.toString())
        statuses.forEach { addQueryParameter("status", it) }
        countries.forEach { addQueryParameter("country", it) }
        formats.forEach { addQueryParameter("format", it) }
        yearMin?.let { addQueryParameter("year_min", it.toString()) }
        yearMax?.let { addQueryParameter("year_max", it.toString()) }
        maxPopularity?.let { addQueryParameter("max_popularity", it.toString()) }
        if (includeAdult) addQueryParameter("adult", "true")
        excludeMalUser.trim().takeIf { it.isNotEmpty() }?.let { addQueryParameter("mal_user", it) }
        excludeAnilistUser.trim().takeIf { it.isNotEmpty() }?.let { addQueryParameter("anilist_user", it) }
    }

    fun saveTo(prefs: MangaBrainPreferences) {
        prefs.weightSemantic().set(weightSemantic)
        prefs.weightTags().set(weightTags)
        prefs.weightGenres().set(weightGenres)
        prefs.minScore().set(minScore)
        prefs.statuses().set(statuses)
        prefs.countries().set(countries)
        prefs.formats().set(formats)
        prefs.yearMin().set(yearMin?.toString().orEmpty())
        prefs.yearMax().set(yearMax?.toString().orEmpty())
        prefs.maxPopularity().set(maxPopularity?.toString().orEmpty())
        prefs.includeAdult().set(includeAdult)
        prefs.excludeMalUser().set(excludeMalUser.trim())
        prefs.excludeAnilistUser().set(excludeAnilistUser.trim())
    }

    companion object {
        // Values are AniList enums, which MangaBrain filters on directly.
        val STATUSES = linkedMapOf(
            "RELEASING" to "Releasing",
            "FINISHED" to "Finished",
            "HIATUS" to "Hiatus",
            "CANCELLED" to "Cancelled",
            "NOT_YET_RELEASED" to "Not yet released",
        )
        val COUNTRIES = linkedMapOf(
            "JP" to "Manga (Japan)",
            "KR" to "Manhwa (Korea)",
            "CN" to "Manhua (China)",
            "TW" to "Taiwan",
        )
        val FORMATS = linkedMapOf(
            "MANGA" to "Manga",
            "ONE_SHOT" to "One shot",
            "NOVEL" to "Light novel",
        )

        fun fromPreferences(prefs: MangaBrainPreferences) = MangaBrainFilters(
            weightSemantic = prefs.weightSemantic().get(),
            weightTags = prefs.weightTags().get(),
            weightGenres = prefs.weightGenres().get(),
            minScore = prefs.minScore().get(),
            statuses = prefs.statuses().get(),
            countries = prefs.countries().get(),
            formats = prefs.formats().get(),
            yearMin = prefs.yearMin().get().trim().toIntOrNull(),
            yearMax = prefs.yearMax().get().trim().toIntOrNull(),
            maxPopularity = prefs.maxPopularity().get().trim().toIntOrNull(),
            includeAdult = prefs.includeAdult().get(),
            excludeMalUser = prefs.excludeMalUser().get(),
            excludeAnilistUser = prefs.excludeAnilistUser().get(),
        )
    }
}

/**
 * Per-manga filter overrides set from the Recommends screen. Kept in memory only and cleared
 * when that screen closes, so "Browse" pages opened from it reuse the same overrides.
 */
object MangaBrainSessionFilters {
    private val overrides = ConcurrentHashMap<Long, MangaBrainFilters>()

    operator fun get(mangaId: Long): MangaBrainFilters? = overrides[mangaId]

    operator fun set(mangaId: Long, filters: MangaBrainFilters) {
        overrides[mangaId] = filters
    }

    fun clear(mangaId: Long) {
        overrides.remove(mangaId)
    }
}
