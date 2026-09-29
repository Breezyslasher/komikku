package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.util.system.toast
import exh.pref.MangaBrainPreferences
import kotlinx.collections.immutable.persistentListOf
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
        )
    }

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
