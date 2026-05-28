package com.rafaelswitala.mediguard.ui.screens

/**
 * Formular zum Erstellen/Bearbeiten von Medikamenten mit erweiterbarem Zeitplan-Builder.
 * Unterstützt exakte Zeit, Zeiträume, Intervalle, wöchentliche und Tageszeiten-Pläne.
 */

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rafaelswitala.mediguard.domain.model.DayOfWeekEnum
import com.rafaelswitala.mediguard.domain.model.DayPeriod
import com.rafaelswitala.mediguard.domain.model.FrequencyType
import com.rafaelswitala.mediguard.domain.model.Medication
import com.rafaelswitala.mediguard.domain.model.MedicationFormType
import com.rafaelswitala.mediguard.domain.model.MedicationSchedule
import com.rafaelswitala.mediguard.domain.model.ScheduleDataCodec
import com.rafaelswitala.mediguard.domain.model.TreatmentType
import com.rafaelswitala.mediguard.domain.model.formatMedicationAmount
import com.rafaelswitala.mediguard.ui.localization.LocalAppLanguage
import com.rafaelswitala.mediguard.ui.localization.LocalAppStrings
import com.rafaelswitala.mediguard.viewmodel.MedicationViewModel

/**
 * Datei für die Maske zum Erstellen und Bearbeiten einer Medikation.
 * Wichtig sind valide Eingaben für Bestand, Dosierung und Erinnerungszeit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationScreen(
    viewModel: MedicationViewModel,
    medicationId: Long? = null,
    onNavigateBack: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val language = LocalAppLanguage.current
    val editingMedication by viewModel.editingMedication
    val editingSchedules by viewModel.editingSchedules
    val isEditing = medicationId != null

    var medicationName by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var dosageUnit by remember { mutableStateOf("mg") }
    var description by remember { mutableStateOf("") }
    var treatmentType by remember { mutableStateOf(TreatmentType.ONGOING) }
    var treatmentLimitMode by remember { mutableStateOf(TreatmentLimitMode.DAYS) }
    var durationDays by remember { mutableStateOf("") }
    var treatmentLimitDoses by remember { mutableStateOf("") }
    var remainingDoses by remember { mutableStateOf("") }
    var remainingVolumeMl by remember { mutableStateOf("") }
    var dosePerIntakeMl by remember { mutableStateOf("") }
    var doseQuantity by remember { mutableStateOf("1") }
    var medicationFormType by remember { mutableStateOf(MedicationFormType.TABLET) }
    var supplyAlertEnabled by remember { mutableStateOf(true) }
    var supplyAlertThreshold by remember { mutableStateOf("7") }
    var selectedType by remember { mutableStateOf(FrequencyType.EXACT_TIME) }
    var exactTimes by remember { mutableStateOf("08:00") }
    var rangeStart by remember { mutableStateOf("08:00") }
    var rangeEnd by remember { mutableStateOf("10:00") }
    var intervalHours by remember { mutableStateOf("6") }
    var intervalStart by remember { mutableStateOf("08:00") }
    var weeklyTime by remember { mutableStateOf("08:00") }
    var initialized by remember(medicationId) { mutableStateOf(false) }
    val selectedPeriods = remember { mutableStateListOf(DayPeriod.MORNING) }
    val selectedDays = remember { mutableStateListOf(DayOfWeekEnum.MONDAY) }

    LaunchedEffect(medicationId) {
        viewModel.loadMedicationForEdit(medicationId)
    }

    LaunchedEffect(isEditing, editingMedication, editingSchedules) {
        if (initialized) return@LaunchedEffect
        if (isEditing && editingMedication == null) return@LaunchedEffect

        val medication = editingMedication
        if (medication != null) {
            medicationName = medication.name
            dosage = medication.dosage
            dosageUnit = medication.dosageUnit
            description = medication.description
            treatmentType = medication.treatmentType
            treatmentLimitMode = if (medication.treatmentLimitDoses != null && medication.durationDays == null) {
                TreatmentLimitMode.DOSES
            } else {
                TreatmentLimitMode.DAYS
            }
            durationDays = medication.durationDays?.toString().orEmpty()
            treatmentLimitDoses = medication.treatmentLimitDoses?.toString().orEmpty()
            remainingDoses = medication.remainingDoses?.let(::formatMedicationAmount).orEmpty()
            remainingVolumeMl = medication.remainingVolumeMl?.let(::formatMedicationAmount).orEmpty()
            dosePerIntakeMl = medication.dosePerIntakeMl?.let(::formatMedicationAmount).orEmpty()
            doseQuantity = formatMedicationAmount(medication.doseQuantity)
            medicationFormType = medication.medicationFormType
            supplyAlertEnabled = medication.supplyAlertEnabled
            supplyAlertThreshold = medication.supplyAlertThreshold.toString()
            applyScheduleState(
                schedules = editingSchedules,
                setSelectedType = { selectedType = it },
                setExactTimes = { exactTimes = it },
                setIntervalHours = { intervalHours = it },
                setIntervalStart = { intervalStart = it },
                setWeeklyTime = { weeklyTime = it },
                selectedPeriods = selectedPeriods,
                selectedDays = selectedDays
            )
        }
        initialized = true
    }

    val scheduleDrafts = buildScheduleDrafts(
        selectedType = selectedType,
        exactTimes = exactTimes,
        rangeStart = rangeStart,
        rangeEnd = rangeEnd,
        selectedPeriods = selectedPeriods,
        intervalHours = intervalHours,
        intervalStart = intervalStart,
        weeklyTime = weeklyTime,
        selectedDays = selectedDays
    )
    val doseQuantityValue = parseDecimalAmount(doseQuantity)
    val stockInputIsValid = if (medicationFormType.usesVolume()) {
        remainingVolumeMl.isBlank() || parseDecimalAmount(remainingVolumeMl) != null
    } else {
        remainingDoses.isBlank() || parseDecimalAmount(remainingDoses) != null
    }
    val canSave = medicationName.isNotBlank() &&
        dosage.isNotBlank() &&
        doseQuantityValue != null &&
        doseQuantityValue >= MINIMUM_DOSE &&
        stockInputIsValid &&
        scheduleDrafts.isNotEmpty() &&
        hasValidTreatmentLimit(treatmentType, treatmentLimitMode, durationDays, treatmentLimitDoses)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) strings.editMedication else strings.addMedication) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    titleContentColor = MaterialTheme.colorScheme.onSecondary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSecondary
                )
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    val baseMedication = editingMedication
                    val medication = Medication(
                        id = baseMedication?.id ?: 0,
                        name = medicationName.trim(),
                        dosage = dosage.trim(),
                        dosageUnit = dosageUnit.trim().ifBlank { medicationFormType.defaultUnit() },
                        description = description.trim(),
                        treatmentType = treatmentType,
                        durationDays = if (
                            treatmentType == TreatmentType.LIMITED &&
                            treatmentLimitMode == TreatmentLimitMode.DAYS
                        ) {
                            durationDays.toIntOrNull()
                        } else {
                            null
                        },
                        treatmentLimitDoses = if (
                            treatmentType == TreatmentType.LIMITED &&
                            treatmentLimitMode == TreatmentLimitMode.DOSES
                        ) {
                            treatmentLimitDoses.toIntOrNull()
                        } else null,
                        remainingDoses = if (medicationFormType.usesVolume()) {
                            null
                        } else {
                            parseDecimalAmount(remainingDoses)
                        },
                        remainingVolumeMl = if (medicationFormType.usesVolume()) {
                            parseDecimalAmount(remainingVolumeMl)
                        } else {
                            null
                        },
                        dosePerIntakeMl = if (medicationFormType.usesVolume()) {
                            parseDecimalAmount(dosePerIntakeMl)
                        } else {
                            null
                        },
                        doseQuantity = parseDecimalAmount(doseQuantity)?.coerceAtLeast(MINIMUM_DOSE) ?: 1.0,
                        medicationFormType = medicationFormType,
                        intakeGroupId = baseMedication?.intakeGroupId,
                        supplyAlertEnabled = supplyAlertEnabled,
                        supplyAlertThreshold = supplyAlertThreshold.toIntOrNull()
                            ?: medicationFormType.defaultSupplyThreshold(),
                        createdAt = baseMedication?.createdAt ?: System.currentTimeMillis(),
                        isActive = baseMedication?.isActive ?: true
                    )

                    if (isEditing) {
                        viewModel.updateMedicationWithSchedules(medication, scheduleDrafts)
                    } else {
                        viewModel.addMedicationWithSchedules(medication, scheduleDrafts)
                    }
                    onNavigateBack()
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Text(if (isEditing) strings.update else strings.save)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionCard {
                OutlinedTextField(
                    value = medicationName,
                    onValueChange = { medicationName = it },
                    label = { Text(strings.medicationName) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = dosage,
                        onValueChange = { dosage = it },
                        label = { Text(strings.dosage) },
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = dosageUnit,
                        onValueChange = { dosageUnit = it },
                        label = { Text(strings.unit) },
                        modifier = Modifier.width(104.dp),
                        maxLines = 1
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(strings.notes) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Text(strings.medicationForm, style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MedicationFormType.entries.forEach { form ->
                        if (form == medicationFormType) {
                            Button(onClick = {
                                medicationFormType = form
                                if (dosageUnit == "mg" || dosageUnit == "ml") {
                                    dosageUnit = form.defaultUnit()
                                }
                                if (supplyAlertThreshold == "7" || supplyAlertThreshold == "50") {
                                    supplyAlertThreshold = form.defaultSupplyThreshold().toString()
                                }
                            }) { Text(form.label(language)) }
                        } else {
                            OutlinedButton(onClick = {
                                medicationFormType = form
                                dosageUnit = form.defaultUnit()
                                supplyAlertThreshold = form.defaultSupplyThreshold().toString()
                            }) { Text(form.label(language)) }
                        }
                    }
                }

                OutlinedTextField(
                    value = doseQuantity,
                    onValueChange = { doseQuantity = filterDecimalInput(it) },
                    label = { Text(strings.doseQuantity) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            SectionCard {
                Text(strings.treatment, style = MaterialTheme.typography.titleMedium)
                SegmentedButtons(
                    items = listOf(TreatmentType.ONGOING to strings.ongoing, TreatmentType.LIMITED to strings.limited),
                    selected = treatmentType,
                    onSelected = { treatmentType = it }
                )
                if (treatmentType == TreatmentType.LIMITED) {
                    Text(strings.limitedByDaysOrDoses, style = MaterialTheme.typography.bodySmall)
                    SegmentedButtons(
                        items = listOf(
                            TreatmentLimitMode.DAYS to strings.limitByDays,
                            TreatmentLimitMode.DOSES to strings.limitByDoses
                        ),
                        selected = treatmentLimitMode,
                        onSelected = { treatmentLimitMode = it }
                    )
                    if (treatmentLimitMode == TreatmentLimitMode.DAYS) {
                        OutlinedTextField(
                            value = durationDays,
                            onValueChange = { durationDays = it.filter(Char::isDigit) },
                            label = { Text(strings.durationDays) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    } else {
                        OutlinedTextField(
                            value = treatmentLimitDoses,
                            onValueChange = { treatmentLimitDoses = it.filter(Char::isDigit) },
                            label = { Text(strings.treatmentLimitDoses) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                } else {
                    Text(
                        strings.durationOngoingHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SectionCard {
                Text(strings.stock, style = MaterialTheme.typography.titleMedium)
                if (medicationFormType.usesVolume()) {
                    OutlinedTextField(
                        value = remainingVolumeMl,
                        onValueChange = { remainingVolumeMl = filterDecimalInput(it) },
                        label = { Text(strings.remainingVolume) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = dosePerIntakeMl,
                        onValueChange = { dosePerIntakeMl = filterDecimalInput(it) },
                        label = { Text(strings.dosePerIntake) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                } else {
                    OutlinedTextField(
                        value = remainingDoses,
                        onValueChange = { remainingDoses = filterDecimalInput(it) },
                        label = { Text(strings.tabletsLeft) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.supplyAlert, style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = supplyAlertEnabled,
                        onCheckedChange = { supplyAlertEnabled = it }
                    )
                }
                if (supplyAlertEnabled) {
                    OutlinedTextField(
                        value = supplyAlertThreshold,
                        onValueChange = { supplyAlertThreshold = it.filter(Char::isDigit) },
                        label = { Text(strings.alertAt) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            SectionCard {
                Text(strings.reminderType, style = MaterialTheme.typography.titleMedium)
                ScheduleTypeSelector(
                    selectedType = selectedType,
                    onSelected = { selectedType = it }
                )

                when (selectedType) {
                    FrequencyType.EXACT_TIME -> OutlinedTextField(
                        value = exactTimes,
                        onValueChange = { exactTimes = it },
                        label = { Text(strings.timesExample) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1
                    )

                    FrequencyType.TIME_RANGE -> Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = rangeStart,
                            onValueChange = { rangeStart = it },
                            label = { Text(strings.from) },
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        OutlinedTextField(
                            value = rangeEnd,
                            onValueChange = { rangeEnd = it },
                            label = { Text(strings.to) },
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                    }

                    FrequencyType.DAY_PERIOD -> PeriodSelector(
                        language = language,
                        selectedPeriods = selectedPeriods,
                        onToggle = { period ->
                            if (period in selectedPeriods) {
                                if (selectedPeriods.size > 1) selectedPeriods.remove(period)
                            } else {
                                selectedPeriods.add(period)
                            }
                        }
                    )

                    FrequencyType.INTERVAL -> {
                        OutlinedTextField(
                            value = intervalHours,
                            onValueChange = { intervalHours = it.filter(Char::isDigit) },
                            label = { Text(strings.intervalHours) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = intervalStart,
                            onValueChange = { intervalStart = it },
                            label = { Text(strings.startTime) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1
                        )
                    }

                    FrequencyType.WEEKLY -> {
                        DaySelector(
                            language = language,
                            selectedDays = selectedDays,
                            onToggle = { day ->
                                if (day in selectedDays) {
                                    if (selectedDays.size > 1) selectedDays.remove(day)
                                } else {
                                    selectedDays.add(day)
                                }
                            }
                        )
                        OutlinedTextField(
                            value = weeklyTime,
                            onValueChange = { weeklyTime = it },
                            label = { Text(strings.weeklyTime) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1
                        )
                    }
                }

                if (!canSave) {
                    Text(
                        strings.validationHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun ScheduleTypeSelector(
    selectedType: FrequencyType,
    onSelected: (FrequencyType) -> Unit
) {
    val strings = LocalAppStrings.current
    val availableTypes = listOf(
        FrequencyType.EXACT_TIME,
        FrequencyType.DAY_PERIOD,
        FrequencyType.INTERVAL,
        FrequencyType.WEEKLY
    )
    val labels = mapOf(
        FrequencyType.EXACT_TIME to strings.exactTime,
        FrequencyType.TIME_RANGE to strings.timeRange,
        FrequencyType.DAY_PERIOD to strings.dayPeriod,
        FrequencyType.INTERVAL to strings.interval,
        FrequencyType.WEEKLY to strings.weekly
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        availableTypes.forEach { type ->
            if (type == selectedType) {
                Button(onClick = { onSelected(type) }) {
                    Text(labels.getValue(type))
                }
            } else {
                OutlinedButton(onClick = { onSelected(type) }) {
                    Text(labels.getValue(type))
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    language: com.rafaelswitala.mediguard.data.settings.AppLanguage,
    selectedPeriods: List<DayPeriod>,
    onToggle: (DayPeriod) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DayPeriod.entries.forEach { period ->
            val label = period.label(language)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Checkbox(
                    checked = period in selectedPeriods,
                    onCheckedChange = { onToggle(period) }
                )
            }
        }
    }
}

@Composable
private fun DaySelector(
    language: com.rafaelswitala.mediguard.data.settings.AppLanguage,
    selectedDays: List<DayOfWeekEnum>,
    onToggle: (DayOfWeekEnum) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DayOfWeekEnum.entries.forEach { day ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(day.label(language), style = MaterialTheme.typography.bodyLarge)
                Checkbox(
                    checked = day in selectedDays,
                    onCheckedChange = { onToggle(day) }
                )
            }
        }
    }
}

@Composable
private fun <T> SegmentedButtons(
    items: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (value, label) ->
            if (value == selected) {
                Button(onClick = { onSelected(value) }) {
                    Text(label)
                }
            } else {
                OutlinedButton(onClick = { onSelected(value) }) {
                    Text(label)
                }
            }
        }
    }
}

private fun buildScheduleDrafts(
    selectedType: FrequencyType,
    exactTimes: String,
    rangeStart: String,
    rangeEnd: String,
    selectedPeriods: List<DayPeriod>,
    intervalHours: String,
    intervalStart: String,
    weeklyTime: String,
    selectedDays: List<DayOfWeekEnum>
): List<MedicationSchedule> = when (selectedType) {
    FrequencyType.EXACT_TIME -> parseTimes(exactTimes).map { (hour, minute) ->
        MedicationSchedule(
            medicationId = 0,
            scheduleType = FrequencyType.EXACT_TIME,
            scheduleData = ScheduleDataCodec.exactTime(hour, minute)
        )
    }

    FrequencyType.TIME_RANGE -> {
        val start = parseTime(rangeStart)
        val end = parseTime(rangeEnd)
        if (start != null && end != null) {
            listOf(
                MedicationSchedule(
                    medicationId = 0,
                    scheduleType = FrequencyType.TIME_RANGE,
                    scheduleData = ScheduleDataCodec.timeRange(start.first, start.second, end.first, end.second)
                )
            )
        } else {
            emptyList()
        }
    }

    FrequencyType.DAY_PERIOD -> selectedPeriods.map { period ->
        MedicationSchedule(
            medicationId = 0,
            scheduleType = FrequencyType.DAY_PERIOD,
            scheduleData = ScheduleDataCodec.dayPeriod(period)
        )
    }

    FrequencyType.INTERVAL -> {
        val start = parseTime(intervalStart)
        val hours = intervalHours.toIntOrNull()
        if (start != null && hours != null && hours > 0) {
            listOf(
                MedicationSchedule(
                    medicationId = 0,
                    scheduleType = FrequencyType.INTERVAL,
                    scheduleData = ScheduleDataCodec.interval(hours, start.first, start.second)
                )
            )
        } else {
            emptyList()
        }
    }

    FrequencyType.WEEKLY -> {
        val time = parseTime(weeklyTime)
        if (time != null && selectedDays.isNotEmpty()) {
            listOf(
                MedicationSchedule(
                    medicationId = 0,
                    scheduleType = FrequencyType.WEEKLY,
                    scheduleData = ScheduleDataCodec.weekly(selectedDays, time.first, time.second)
                )
            )
        } else {
            emptyList()
        }
    }
}

private enum class TreatmentLimitMode {
    DAYS,
    DOSES
}

private fun hasValidTreatmentLimit(
    treatmentType: TreatmentType,
    treatmentLimitMode: TreatmentLimitMode,
    durationDays: String,
    treatmentLimitDoses: String
): Boolean {
    if (treatmentType != TreatmentType.LIMITED) return true
    return when (treatmentLimitMode) {
        TreatmentLimitMode.DAYS -> durationDays.toIntOrNull()?.let { it > 0 } == true
        TreatmentLimitMode.DOSES -> treatmentLimitDoses.toIntOrNull()?.let { it > 0 } == true
    }
}

private fun applyScheduleState(
    schedules: List<MedicationSchedule>,
    setSelectedType: (FrequencyType) -> Unit,
    setExactTimes: (String) -> Unit,
    setIntervalHours: (String) -> Unit,
    setIntervalStart: (String) -> Unit,
    setWeeklyTime: (String) -> Unit,
    selectedPeriods: MutableList<DayPeriod>,
    selectedDays: MutableList<DayOfWeekEnum>
) {
    val first = schedules.firstOrNull() ?: return
    setSelectedType(first.scheduleType)

    when (first.scheduleType) {
        FrequencyType.EXACT_TIME -> setExactTimes(
            schedules
                .filter { it.scheduleType == FrequencyType.EXACT_TIME }
                .joinToString(", ") { schedule ->
                    val hour = ScheduleDataCodec.readInt(schedule.scheduleData, "hour", 8)
                    val minute = ScheduleDataCodec.readInt(schedule.scheduleData, "minute", 0)
                    formatTime(hour, minute)
                }
        )

        FrequencyType.TIME_RANGE -> {
            val startHour = ScheduleDataCodec.readInt(first.scheduleData, "startHour", 8)
            val startMinute = ScheduleDataCodec.readInt(first.scheduleData, "startMinute", 0)
            setSelectedType(FrequencyType.EXACT_TIME)
            setExactTimes(formatTime(startHour, startMinute))
        }

        FrequencyType.DAY_PERIOD -> {
            selectedPeriods.clear()
            selectedPeriods.addAll(
                schedules
                    .filter { it.scheduleType == FrequencyType.DAY_PERIOD }
                    .map { ScheduleDataCodec.readPeriod(it.scheduleData) }
                    .ifEmpty { listOf(DayPeriod.MORNING) }
            )
        }

        FrequencyType.INTERVAL -> {
            val startHour = ScheduleDataCodec.readInt(first.scheduleData, "startHour", 8)
            val startMinute = ScheduleDataCodec.readInt(first.scheduleData, "startMinute", 0)
            setIntervalHours(ScheduleDataCodec.readInt(first.scheduleData, "intervalHours", 6).toString())
            setIntervalStart(formatTime(startHour, startMinute))
        }

        FrequencyType.WEEKLY -> {
            val hour = ScheduleDataCodec.readInt(first.scheduleData, "hour", 8)
            val minute = ScheduleDataCodec.readInt(first.scheduleData, "minute", 0)
            selectedDays.clear()
            selectedDays.addAll(ScheduleDataCodec.readDays(first.scheduleData))
            setWeeklyTime(formatTime(hour, minute))
        }
    }
}

private fun parseTimes(value: String): List<Pair<Int, Int>> =
    value.split(",", ";").mapNotNull { parseTime(it) }

private fun parseTime(value: String): Pair<Int, Int>? {
    val parts = value.trim().split(":")
    if (parts.size != 2) return null

    val hour = parts[0].toIntOrNull()
    val minute = parts[1].toIntOrNull()

    return if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
        hour to minute
    } else {
        null
    }
}

private fun formatTime(hour: Int, minute: Int): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

private fun filterDecimalInput(value: String): String {
    var hasSeparator = false
    return value.filter { char ->
        when {
            char.isDigit() -> true
            (char == ',' || char == '.') && !hasSeparator -> {
                hasSeparator = true
                true
            }
            else -> false
        }
    }
}

private fun parseDecimalAmount(value: String): Double? =
    value.trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it >= 0.0 }

private const val MINIMUM_DOSE = 0.01
