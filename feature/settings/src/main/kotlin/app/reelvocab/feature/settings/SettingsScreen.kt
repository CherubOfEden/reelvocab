package app.reelvocab.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.reelvocab.core.designsystem.component.CardSurface
import app.reelvocab.core.designsystem.component.ReelListRow
import app.reelvocab.core.designsystem.component.ReelScreenHeader
import app.reelvocab.core.designsystem.component.ReelSectionLabel
import app.reelvocab.core.designsystem.theme.ReelTheme
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

enum class PipMode(val labelRes: Int) {
    WordsOnly(R.string.settings_pip_words),
    Video(R.string.settings_pip_video),
    Audio(R.string.settings_pip_audio),
    All(R.string.settings_pip_all),
}

enum class ThemeMode(val labelRes: Int) {
    System(R.string.settings_theme_system),
    Dark(R.string.settings_theme_dark),
    Light(R.string.settings_theme_light),
}

@Immutable
data class CloudSettingsUi(
    val enabled: Boolean = true,
)

@Composable
fun SettingsScreen(
    onOpenLicenses: () -> Unit,
    onOpenDesignSystem: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onOpenDictionaryProbe: (() -> Unit)? = null,
    onEnableFloatingCapture: (() -> Unit)? = null,
    pipMode: PipMode = PipMode.WordsOnly,
    onPipModeChange: ((PipMode) -> Unit)? = null,
    cropOverlayEnabled: Boolean = true,
    onToggleCropOverlay: (() -> Unit)? = null,
    overlayAvailable: Boolean = true,
    onOpenOverlaySettings: (() -> Unit)? = null,
    themeMode: ThemeMode = ThemeMode.System,
    onThemeModeChange: ((ThemeMode) -> Unit)? = null,
    reminderEnabled: Boolean = false,
    reminderHour: Int = 9,
    reminderMinute: Int = 0,
    reminderIs24Hour: Boolean = false,
    onToggleReminder: (() -> Unit)? = null,
    onReminderTimeChange: ((Int, Int, Boolean) -> Unit)? = null,
    fullScreenAlarm: Boolean = true,
    fullScreenAlarmAvailable: Boolean = true,
    onToggleFullScreenAlarm: (() -> Unit)? = null,
    onOpenFullScreenAlarmSettings: (() -> Unit)? = null,
    appVersion: String = "",
    cloud: CloudSettingsUi = CloudSettingsUi(),
    onCloudChange: ((CloudSettingsUi) -> Unit)? = null,
) {
    val colors = ReelTheme.colors
    var showTimePicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showPipPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = ReelTheme.spacing.screenH,
            end = ReelTheme.spacing.screenH,
            top = ReelTheme.spacing.xl,
            bottom = ReelTheme.spacing.screenBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.m),
    ) {
        item {
            ReelScreenHeader(
                title = stringResource(R.string.settings_title),
                modifier = Modifier.padding(bottom = ReelTheme.spacing.s),
            )
        }

        item {
            SettingsSectionHeader(text = stringResource(R.string.settings_section_cloud))
        }
        item {
            CardSurface {
                Text(
                    text = stringResource(R.string.settings_ai_title),
                    style = ReelTheme.type.titleM,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(ReelTheme.spacing.xs))
                Text(
                    text = stringResource(R.string.settings_ai_summary),
                    style = ReelTheme.type.bodyM,
                    color = colors.textSecondary,
                )
            }
        }
        item {
            SettingsRow(
                label = stringResource(R.string.settings_cloud_toggle),
                value = stringResource(
                    if (cloud.enabled) {
                        R.string.settings_cloud_on
                    } else {
                        R.string.settings_cloud_off
                    },
                ),
                onClick = onCloudChange?.let { change ->
                    { change(cloud.copy(enabled = !cloud.enabled)) }
                },
            )
        }

        item {
            SettingsSectionHeader(text = stringResource(R.string.settings_section_plan))
        }
        item {
            SettingsRow(
                label = stringResource(R.string.settings_plan_label),
                value = stringResource(R.string.settings_plan_value),
            )
        }

        item {
            SettingsSectionHeader(text = stringResource(R.string.settings_section_appearance))
        }
        item {
            SettingsRow(
                label = stringResource(R.string.settings_appearance_label),
                value = stringResource(themeMode.labelRes),
                onClick = if (onThemeModeChange != null) {
                    { showThemePicker = true }
                } else {
                    null
                },
            )
        }

        item {
            SettingsSectionHeader(text = stringResource(R.string.settings_section_capture))
        }
        item {
            SettingsRow(
                label = stringResource(R.string.settings_floating_capture_label),
                value = stringResource(R.string.settings_floating_capture_value),
                onClick = onEnableFloatingCapture,
            )
        }
        item {
            SettingsRow(
                label = stringResource(R.string.settings_overlay_label),
                value = stringResource(
                    if (overlayAvailable) {
                        R.string.settings_overlay_allowed
                    } else {
                        R.string.settings_overlay_blocked
                    },
                ),
                onClick = onOpenOverlaySettings,
            )
        }
        item {
            SettingsRow(
                label = stringResource(R.string.settings_pip_mode_label),
                value = stringResource(pipMode.labelRes),
                onClick = if (onPipModeChange != null) {
                    { showPipPicker = true }
                } else {
                    null
                },
            )
        }
        item {
            SettingsRow(
                label = stringResource(R.string.settings_crop_overlay_label),
                value = stringResource(
                    if (cropOverlayEnabled) {
                        R.string.settings_crop_overlay_on
                    } else {
                        R.string.settings_crop_overlay_off
                    },
                ),
                onClick = onToggleCropOverlay,
            )
        }

        item {
            SettingsSectionHeader(text = stringResource(R.string.settings_section_reminders))
        }
        item {
            SettingsRow(
                label = stringResource(R.string.settings_reminder_label),
                value = stringResource(
                    if (reminderEnabled) {
                        R.string.settings_reminder_on
                    } else {
                        R.string.settings_reminder_off
                    },
                ),
                onClick = onToggleReminder,
            )
        }
        item {
            SettingsRow(
                label = stringResource(R.string.settings_reminder_time_label),
                value = reminderTimeLabel(reminderHour, reminderMinute, reminderIs24Hour),
                onClick = if (onReminderTimeChange != null) {
                    { showTimePicker = true }
                } else {
                    null
                },
            )
        }
        item {
            SettingsRow(
                label = stringResource(R.string.settings_full_screen_label),
                value = stringResource(
                    if (fullScreenAlarm) {
                        R.string.settings_reminder_on
                    } else {
                        R.string.settings_reminder_off
                    },
                ),
                onClick = onToggleFullScreenAlarm,
            )
        }
        if (fullScreenAlarm && !fullScreenAlarmAvailable) {
            item {
                SettingsRow(
                    label = stringResource(R.string.settings_full_screen_allow_label),
                    value = stringResource(R.string.settings_full_screen_blocked),
                    onClick = onOpenFullScreenAlarmSettings,
                )
            }
        }
        if (fullScreenAlarm) {
            item {
                CardSurface {
                    Text(
                        text = stringResource(R.string.settings_full_screen_summary),
                        style = ReelTheme.type.bodyM,
                        color = colors.textSecondary,
                    )
                }
            }
        }

        item {
            SettingsSectionHeader(text = stringResource(R.string.settings_section_about))
        }
        if (appVersion.isNotEmpty()) {
            item {
                SettingsRow(
                    label = stringResource(R.string.settings_version_label),
                    value = appVersion,
                )
            }
        }
        item {
            SettingsRow(
                label = stringResource(R.string.settings_licenses_label),
                value = null,
                onClick = onOpenLicenses,
            )
        }
        if (onOpenDesignSystem != null) {
            item {
                SettingsRow(
                    label = stringResource(R.string.settings_design_system_label),
                    value = null,
                    onClick = onOpenDesignSystem,
                )
            }
        }
        if (onOpenDictionaryProbe != null) {
            item {
                SettingsRow(
                    label = stringResource(R.string.settings_dictionary_probe_label),
                    value = null,
                    onClick = onOpenDictionaryProbe,
                )
            }
        }

        item {
            CardSurface(modifier = Modifier.padding(top = ReelTheme.spacing.s)) {
                Text(
                    text = stringResource(R.string.settings_privacy_title),
                    style = ReelTheme.type.titleM,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(ReelTheme.spacing.s))
                Text(
                    text = stringResource(R.string.settings_privacy_body),
                    style = ReelTheme.type.bodyM,
                    color = colors.textSecondary,
                )
            }
        }
    }

    if (showTimePicker && onReminderTimeChange != null) {
        ReminderTimePickerDialog(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            initialIs24Hour = reminderIs24Hour,
            onConfirm = { hour, minute, is24Hour ->
                onReminderTimeChange(hour, minute, is24Hour)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }

    if (showPipPicker && onPipModeChange != null) {
        PipModePickerDialog(
            selected = pipMode,
            onSelect = { mode ->
                onPipModeChange(mode)
                showPipPicker = false
            },
            onDismiss = { showPipPicker = false },
        )
    }

    if (showThemePicker && onThemeModeChange != null) {
        ThemePickerDialog(
            selected = themeMode,
            onSelect = { mode ->
                onThemeModeChange(mode)
                showThemePicker = false
            },
            onDismiss = { showThemePicker = false },
        )
    }
}

@Composable
private fun PipModePickerDialog(
    selected: PipMode,
    onSelect: (PipMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ReelTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.elevated,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = {
            Text(
                text = stringResource(R.string.settings_pip_mode_title),
                style = ReelTheme.type.titleM,
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_pip_mode_summary),
                    style = ReelTheme.type.bodyM,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(ReelTheme.spacing.s))
                PipMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) }
                            .padding(vertical = ReelTheme.spacing.s),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = mode == selected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.accent,
                                unselectedColor = colors.textTertiary,
                            ),
                        )
                        Text(
                            text = stringResource(mode.labelRes),
                            style = ReelTheme.type.bodyL,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(start = ReelTheme.spacing.s),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.settings_close),
                    style = ReelTheme.type.label,
                    color = colors.textSecondary,
                )
            }
        },
    )
}

@Composable
private fun ThemePickerDialog(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ReelTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.elevated,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = {
            Text(
                text = stringResource(R.string.settings_theme_title),
                style = ReelTheme.type.titleM,
            )
        },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) }
                            .padding(vertical = ReelTheme.spacing.s),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = mode == selected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.accent,
                                unselectedColor = colors.textTertiary,
                            ),
                        )
                        Text(
                            text = stringResource(mode.labelRes),
                            style = ReelTheme.type.bodyL,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(start = ReelTheme.spacing.s),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.settings_close),
                    style = ReelTheme.type.label,
                    color = colors.textSecondary,
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    initialIs24Hour: Boolean,
    onConfirm: (Int, Int, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ReelTheme.colors
    var is24Hour by remember { mutableStateOf(initialIs24Hour) }
    var hour by remember { mutableIntStateOf(initialHour) }
    var minute by remember { mutableIntStateOf(initialMinute) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.elevated,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = {
            Text(
                text = stringResource(R.string.settings_reminder_time_title),
                style = ReelTheme.type.titleM,
            )
        },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.s),
                ) {
                    TextButton(onClick = { is24Hour = false }) {
                        Text(
                            text = stringResource(R.string.settings_time_format_12),
                            style = ReelTheme.type.label,
                            color = if (!is24Hour) colors.accent else colors.textSecondary,
                        )
                    }
                    TextButton(onClick = { is24Hour = true }) {
                        Text(
                            text = stringResource(R.string.settings_time_format_24),
                            style = ReelTheme.type.label,
                            color = if (is24Hour) colors.accent else colors.textSecondary,
                        )
                    }
                }
                key(is24Hour) {
                    val picker = rememberTimePickerState(
                        initialHour = hour,
                        initialMinute = minute,
                        is24Hour = is24Hour,
                    )
                    LaunchedEffect(picker.hour, picker.minute) {
                        hour = picker.hour
                        minute = picker.minute
                    }
                    TimePicker(state = picker)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hour, minute, is24Hour) }) {
                Text(
                    text = stringResource(R.string.settings_time_save),
                    style = ReelTheme.type.label,
                    color = colors.accent,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.settings_time_cancel),
                    style = ReelTheme.type.label,
                    color = colors.textSecondary,
                )
            }
        },
    )
}

private fun reminderTimeLabel(hour: Int, minute: Int, is24Hour: Boolean): String {
    val safeHour = hour.coerceIn(0, 23)
    val safeMinute = minute.coerceIn(0, 59)
    if (is24Hour) return String.format(Locale.US, "%02d:%02d", safeHour, safeMinute)
    val suffix = if (safeHour < 12) "AM" else "PM"
    val displayHour = when (val twelve = safeHour % 12) {
        0 -> 12
        else -> twelve
    }
    return String.format(Locale.US, "%d:%02d %s", displayHour, safeMinute, suffix)
}

@Composable
private fun SettingsSectionHeader(text: String) {
    ReelSectionLabel(
        text = text,
        modifier = Modifier.padding(top = ReelTheme.spacing.xl),
    )
}

@Composable
private fun SettingsRow(
    label: String,
    value: String?,
    onClick: (() -> Unit)? = null,
) {
    ReelListRow(
        label = label,
        value = value,
        showChevron = onClick != null,
        onClick = onClick,
    )
}
