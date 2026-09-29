package exh.recs.sources

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.source.model.SManga
import exh.pref.MangaBrainPreferences
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.sy.SYMR
import uy.kohesive.injekt.injectLazy

class MangaBrainPagingSource(manga: Manga) : TrackerRecommendationPagingSource(
    // endpoint is the user-configured base URL; resolved lazily so preference reads happen off the ctor.
    endpoint = "",
    manga = manga,
) {
    private val prefs: MangaBrainPreferences by injectLazy()

    override val name: String
        get() = "MangaBrain"

    override val category: StringResource
        get() = SYMR.strings.similar_titles

    override val associatedTrackerId: Long
        get() = trackerManager.myAnimeList.id

    private fun baseUrl(): String = prefs.baseUrl().get().trimEnd('/')

    private fun authedRequest(url: String): Request {
        val request = GET(url)
        val token = prefs.apiToken().get().trim()
        if (token.isEmpty()) return request
        return request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
    }

    private fun currentFilters(): MangaBrainFilters =
        MangaBrainSessionFilters[manga.id] ?: MangaBrainFilters.fromPreferences(prefs)

    override suspend fun getRecsById(id: String): List<SManga> {
        val base = baseUrl().ifEmpty { return emptyList() }
        val filters = currentFilters()
        val resolveUrl = "$base/search/by-mal/$id".toHttpUrl().newBuilder()
            .addQueryParameter("type", "manga")
            // Seed lookup hides adult entries by default too; without this an adult seed can't resolve.
            .apply { if (filters.includeAdult) addQueryParameter("adult", "true") }
            .build()
        val resolved = with(json) {
            client.newCall(authedRequest(resolveUrl.toString())).awaitSuccess().parseAs<MBMedia>()
        }
        return fetchRecommendations(base, resolved.id, filters)
    }

    override suspend fun getRecsBySearch(search: String): List<SManga> {
        val base = baseUrl().ifEmpty { return emptyList() }
        val filters = currentFilters()
        val searchUrl = "$base/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", search)
            .addQueryParameter("medium", "manga")
            .apply { if (filters.includeAdult) addQueryParameter("adult", "true") }
            .build()
        val response = with(json) {
            client.newCall(authedRequest(searchUrl.toString())).awaitSuccess().parseAs<MBSearchResponse>()
        }
        val first = response.results.firstOrNull() ?: return emptyList()
        return fetchRecommendations(base, first.id, filters)
    }

    private suspend fun fetchRecommendations(base: String, brainId: Long, filters: MangaBrainFilters): List<SManga> {
        val recommendUrl = filters.applyTo("$base/recommend/$brainId".toHttpUrl().newBuilder()).build()
        val response = with(json) {
            client.newCall(authedRequest(recommendUrl.toString())).awaitSuccess().parseAs<MBRecommendResponse>()
        }
        return response.results.map { it.media.toSManga() }
    }

    private fun MBMedia.toSManga(): SManga {
        val displayTitle = titleEnglish ?: title ?: titleNative ?: "Untitled"
        // Recommendations get piped through SmartSearch; the url here is a stable identifier
        // and — when a MAL id is known — a link the user can open outside komikku.
        val url = idMal?.let { "https://myanimelist.net/manga/$it" }
            ?: "mangabrain://media/$id"
        return SManga(
            title = displayTitle,
            url = url,
            thumbnail_url = coverImageLarge ?: coverImage,
            description = description,
            genre = (genres + tags).joinToString(", ").takeIf { it.isNotBlank() },
            status = mangaStatusFromMangaBrain(status),
            initialized = true,
        )
    }

    private fun mangaStatusFromMangaBrain(raw: String?): Int = when (raw?.lowercase()) {
        "finished", "completed" -> SManga.COMPLETED
        "releasing", "ongoing", "in_progress" -> SManga.ONGOING
        "hiatus" -> SManga.ON_HIATUS
        "cancelled", "canceled" -> SManga.CANCELLED
        else -> SManga.UNKNOWN
    }
}

@Serializable
internal data class MBMedia(
    val id: Long,
    @SerialName("id_mal") val idMal: Long? = null,
    val medium: String? = null,
    val title: String? = null,
    @SerialName("title_english") val titleEnglish: String? = null,
    @SerialName("title_native") val titleNative: String? = null,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val format: String? = null,
    val status: String? = null,
    @SerialName("cover_image") val coverImage: String? = null,
    @SerialName("cover_image_large") val coverImageLarge: String? = null,
    @SerialName("average_score") val averageScore: Int? = null,
)

@Serializable
internal data class MBRecommendItem(
    val media: MBMedia,
    val similarity: Double? = null,
)

@Serializable
internal data class MBRecommendResponse(
    val seed: MBMedia? = null,
    val results: List<MBRecommendItem> = emptyList(),
)

@Serializable
internal data class MBSearchResponse(
    val results: List<MBMedia> = emptyList(),
)
