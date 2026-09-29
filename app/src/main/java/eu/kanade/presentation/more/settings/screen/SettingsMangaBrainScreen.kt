package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.util.system.toast
import exh.pref.MangaBrainPreferences
import exh.recs.sources.MangaBrainFilters
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.launch
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsMangaBrainScreen : SearchableSettings {
    @Suppress("unused")
    private fun readResolve(): Any = SettingsMangaBrainScreen

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = SYMR.strings.pref_category_mangabrain

    @Composable
    override fun getPreferences(): List<Preference> {
        val prefs = remember { Injekt.get<MangaBrainPreferences>() }

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(SYMR.strings.pref_category_mangabrain),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.EditTextPreference(
                        preference = prefs.baseUrl(),
                        title = stringResource(SYMR.strings.pref_mangabrain_base_url),
                        subtitle = stringResource(SYMR.strings.pref_mangabrain_base_url_summary),
                        onValueChanged = { newValue ->
                            // Normalize on save: trim trailing slash so path joining stays clean.
                            prefs.baseUrl().set(newValue.trim().trimEnd('/'))
                            true
                        },
                    ),
                    Preference.PreferenceItem.EditTextPreference(
                        preference = prefs.apiToken(),
                        title = stringResource(SYMR.strings.pref_mangabrain_api_token),
                        subtitle = stringResource(SYMR.strings.pref_mangabrain_api_token_summary),
                    ),
                    testConnectionPreference(prefs),
                ),
            ),
            defaultsGroup(prefs),
        )
    }

    @Composable
    private fun defaultsGroup(prefs: MangaBrainPreferences): Preference.PreferenceGroup {
        val weightSemantic by prefs.weightSemantic().collectAsState()
        val weightTags by prefs.weightTags().collectAsState()
        val weightGenres by prefs.weightGenres().collectAsState()
        val minScore by prefs.minScore().collectAsState()
        val yearMin by prefs.yearMin().collectAsState()
        val yearMax by prefs.yearMax().collectAsState()
        val maxPopularity by prefs.maxPopularity().collectAsState()
        val excludeMal by prefs.excludeMalUser().collectAsState()
        val excludeAnilist by prefs.excludeAnilistUser().collectAsState()
        val any = stringResource(SYMR.strings.mangabrain_any)
        val noLimit = stringResource(SYMR.strings.mangabrain_no_limit)
        val off = stringResource(SYMR.strings.mangabrain_off)

        return Preference.PreferenceGroup(
            title = stringResource(SYMR.strings.pref_mangabrain_defaults),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.InfoPreference(stringResource(SYMR.strings.pref_mangabrain_defaults_summary)),
                weightSlider(SYMR.strings.mangabrain_weight_semantic, weightSemantic) { prefs.weightSemantic().set(it) },
                weightSlider(SYMR.strings.mangabrain_weight_tags, weightTags) { prefs.weightTags().set(it) },
                weightSlider(SYMR.strings.mangabrain_weight_genres, weightGenres) { prefs.weightGenres().set(it) },
                Preference.PreferenceItem.SliderPreference(
                    value = minScore,
                    valueRange = 0..100 step 5,
                    steps = 19,
                    title = stringResource(SYMR.strings.mangabrain_min_score),
                    valueString = if (minScore > 0) "$minScore" else off,
                    onValueChanged = { prefs.minScore().set(it) },
                ),
                multiSelect(prefs.statuses(), MangaBrainFilters.STATUSES, SYMR.strings.mangabrain_status, any),
                multiSelect(prefs.countries(), MangaBrainFilters.COUNTRIES, SYMR.strings.mangabrain_country, any),
                multiSelect(prefs.formats(), MangaBrainFilters.FORMATS, SYMR.strings.mangabrain_format, any),
                numberField(prefs.yearMin(), SYMR.strings.mangabrain_year_min, yearMin, noLimit),
                numberField(prefs.yearMax(), SYMR.strings.mangabrain_year_max, yearMax, noLimit),
                numberField(
                    prefs.maxPopularity(),
                    SYMR.strings.mangabrain_max_popularity,
                    maxPopularity,
                    stringResource(SYMR.strings.mangabrain_max_popularity_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = prefs.includeAdult(),
                    title = stringResource(SYMR.strings.mangabrain_include_adult),
                ),
                Preference.PreferenceItem.EditTextPreference(
                    preference = prefs.excludeMalUser(),
                    title = stringResource(SYMR.strings.mangabrain_exclude_mal),
                    subtitle = excludeMal.ifBlank { off },
                ),
                Preference.PreferenceItem.EditTextPreference(
                    preference = prefs.excludeAnilistUser(),
                    title = stringResource(SYMR.strings.mangabrain_exclude_anilist),
                    subtitle = excludeAnilist.ifBlank { off },
                ),
            ),
        )
    }

    @Composable
    private fun weightSlider(
        title: StringResource,
        value: Int,
        onChange: (Int) -> Unit,
    ) = Preference.PreferenceItem.SliderPreference(
        value = value,
        valueRange = 0..100 step 5,
        steps = 19,
        title = stringResource(title),
        valueString = "$value",
        onValueChanged = { onChange(it) },
    )

    @Composable
    private fun multiSelect(
        preference: tachiyomi.core.common.preference.Preference<Set<String>>,
        entries: Map<String, String>,
        title: StringResource,
        emptyLabel: String,
    ) = Preference.PreferenceItem.MultiSelectListPreference(
        preference = preference,
        entries = entries.toImmutableMap(),
        title = stringResource(title),
        subtitleProvider = { value, all ->
            value.mapNotNull { all[it] }.joinToString().ifBlank { emptyLabel }
        },
    )

    @Composable
    private fun numberField(
        preference: tachiyomi.core.common.preference.Preference<String>,
        title: StringResource,
        value: String,
        emptyLabel: String,
    ) = Preference.PreferenceItem.EditTextPreference(
        preference = preference,
        title = stringResource(title),
        subtitle = value.ifBlank { emptyLabel },
        // Only accept whole numbers (or blank to clear the limit).
        onValueChanged = { it.isBlank() || it.trim().toIntOrNull() != null },
    )

    @Composable
    private fun testConnectionPreference(prefs: MangaBrainPreferences): Preference.PreferenceItem.TextPreference {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val baseUrl by prefs.baseUrl().collectAsState()
        return Preference.PreferenceItem.TextPreference(
            title = stringResource(SYMR.strings.pref_mangabrain_test_connection),
            enabled = baseUrl.isNotBlank(),
            onClick = {
                scope.launch {
                    val trimmed = prefs.baseUrl().get().trimEnd('/')
                    val token = prefs.apiToken().get().trim()
                    val result = runCatching {
                        withIOContext {
                            val client = Injekt.get<NetworkHelper>().client
                            val request = GET("$trimmed/healthz").let { req ->
                                if (token.isEmpty()) {
                                    req
                                } else {
                                    req.newBuilder()
                                        .header("Authorization", "Bearer $token")
                                        .build()
                                }
                            }
                            client.newCall(request).awaitSuccess().close()
                        }
                    }
                    val message = if (result.isSuccess) {
                        context.stringResource(SYMR.strings.mangabrain_test_success)
                    } else {
                        context.stringResource(
                            SYMR.strings.mangabrain_test_failure,
                            result.exceptionOrNull()?.message ?: "unknown error",
                        )
                    }
                    context.toast(message)
                }
            },
        )
    }
}
