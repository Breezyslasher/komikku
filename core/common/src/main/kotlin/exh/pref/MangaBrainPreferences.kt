package exh.pref

import tachiyomi.core.common.preference.PreferenceStore

class MangaBrainPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun baseUrl() = preferenceStore.getString("mangabrain_base_url", "")

    fun apiToken() = preferenceStore.getString("mangabrain_api_token", "")
}
