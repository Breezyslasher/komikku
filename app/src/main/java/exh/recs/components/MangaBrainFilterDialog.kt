package exh.recs.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import exh.recs.sources.MangaBrainFilters
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt

@Composable
fun MangaBrainFilterDialog(
    initial: MangaBrainFilters,
    defaults: MangaBrainFilters,
    onDismissRequest: () -> Unit,
    onApply: (MangaBrainFilters) -> Unit,
    onSaveAsDefault: (MangaBrainFilters) -> Unit,
) {
    var filters by remember(initial) { mutableStateOf(initial) }
    // Number fields keep their raw text so the user can clear or retype them freely.
    var yearMin by remember(initial) { mutableStateOf(initial.yearMin?.toString().orEmpty()) }
    var yearMax by remember(initial) { mutableStateOf(initial.yearMax?.toString().orEmpty()) }
    var maxPopularity by remember(initial) { mutableStateOf(initial.maxPopularity?.toString().orEmpty()) }

    fun result() = filters.copy(
        yearMin = yearMin.trim().toIntOrNull(),
        yearMax = yearMax.trim().toIntOrNull(),
        maxPopularity = maxPopularity.trim().toIntOrNull(),
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(SYMR.strings.mangabrain_filters)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(SYMR.strings.mangabrain_filters_this_book),
                    style = MaterialTheme.typography.bodySmall,
                )
                PercentSlider(stringResource(SYMR.strings.mangabrain_weight_semantic), filters.weightSemantic) {
                    filters = filters.copy(weightSemantic = it)
                }
                PercentSlider(stringResource(SYMR.strings.mangabrain_weight_tags), filters.weightTags) {
                    filters = filters.copy(weightTags = it)
                }
                PercentSlider(stringResource(SYMR.strings.mangabrain_weight_genres), filters.weightGenres) {
                    filters = filters.copy(weightGenres = it)
                }
                PercentSlider(
                    label = stringResource(SYMR.strings.mangabrain_min_score),
                    value = filters.minScore,
                    valueText = if (filters.minScore > 0) "${filters.minScore}" else stringResource(SYMR.strings.mangabrain_off),
                ) { filters = filters.copy(minScore = it) }

                ChipGroup(stringResource(SYMR.strings.mangabrain_status), MangaBrainFilters.STATUSES, filters.statuses) {
                    filters = filters.copy(statuses = it)
                }
                ChipGroup(stringResource(SYMR.strings.mangabrain_country), MangaBrainFilters.COUNTRIES, filters.countries) {
                    filters = filters.copy(countries = it)
                }
                ChipGroup(stringResource(SYMR.strings.mangabrain_format), MangaBrainFilters.FORMATS, filters.formats) {
                    filters = filters.copy(formats = it)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(stringResource(SYMR.strings.mangabrain_year_min), yearMin, Modifier.weight(1f)) { yearMin = it }
                    NumberField(stringResource(SYMR.strings.mangabrain_year_max), yearMax, Modifier.weight(1f)) { yearMax = it }
                }
                NumberField(
                    stringResource(SYMR.strings.mangabrain_max_popularity),
                    maxPopularity,
                    Modifier.fillMaxWidth(),
                ) { maxPopularity = it }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(SYMR.strings.mangabrain_include_adult), modifier = Modifier.weight(1f))
                    Switch(
                        checked = filters.includeAdult,
                        onCheckedChange = { filters = filters.copy(includeAdult = it) },
                    )
                }
                OutlinedTextField(
                    value = filters.excludeMalUser,
                    onValueChange = { filters = filters.copy(excludeMalUser = it) },
                    label = { Text(stringResource(SYMR.strings.mangabrain_exclude_mal)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = filters.excludeAnilistUser,
                    onValueChange = { filters = filters.copy(excludeAnilistUser = it) },
                    label = { Text(stringResource(SYMR.strings.mangabrain_exclude_anilist)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            filters = defaults
                            yearMin = defaults.yearMin?.toString().orEmpty()
                            yearMax = defaults.yearMax?.toString().orEmpty()
                            maxPopularity = defaults.maxPopularity?.toString().orEmpty()
                        },
                    ) { Text(stringResource(MR.strings.action_reset)) }
                    TextButton(onClick = { onSaveAsDefault(result()) }) {
                        Text(stringResource(SYMR.strings.mangabrain_save_as_default))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(result()) }) { Text(stringResource(MR.strings.action_apply)) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(MR.strings.action_cancel)) }
        },
    )
}

@Composable
private fun PercentSlider(
    label: String,
    value: Int,
    valueText: String = "$value",
    onChange: (Int) -> Unit,
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(valueText, style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange((it / 5).roundToInt() * 5) },
            valueRange = 0f..100f,
            steps = 19,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGroup(
    label: String,
    entries: Map<String, String>,
    selected: Set<String>,
    onChange: (Set<String>) -> Unit,
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            entries.forEach { (key, name) ->
                FilterChip(
                    selected = key in selected,
                    onClick = { onChange(if (key in selected) selected - key else selected + key) },
                    label = { Text(name) },
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    modifier: Modifier,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { new -> if (new.all(Char::isDigit)) onChange(new) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}
