package app.reelvocab.feature.settings

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.reelvocab.core.designsystem.component.CardSurface
import app.reelvocab.core.designsystem.theme.ReelTheme
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

@Composable
fun SettingsScreen(
    onOpenLicenses: () -> Unit,
    onOpenDesignSystem: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onOpenDictionaryProbe: (() -> Unit)? = null,
    onEnableFloatingCapture: (() -> Unit)? = null,
) {
    val colors = ReelTheme.colors

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
            Text(
                text = stringResource(R.string.settings_title),
                style = ReelTheme.type.titleL,
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = ReelTheme.spacing.s),
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
                value = stringResource(R.string.settings_appearance_value),
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
            SettingsSectionHeader(text = stringResource(R.string.settings_section_about))
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
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        style = ReelTheme.type.label,
        color = ReelTheme.colors.textTertiary,
        modifier = Modifier.padding(top = ReelTheme.spacing.l, bottom = ReelTheme.spacing.xxs),
    )
}

@Composable
private fun SettingsRow(
    label: String,
    value: String?,
    onClick: (() -> Unit)? = null,
) {
    val colors = ReelTheme.colors
    CardSurface(onClick = onClick, contentPadding = PaddingValues(ReelTheme.spacing.l)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = ReelTheme.type.bodyL,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            if (value != null) {
                Column(modifier = Modifier.padding(start = ReelTheme.spacing.m)) {
                    Text(
                        text = value,
                        style = ReelTheme.type.bodyM,
                        color = colors.textSecondary,
                    )
                }
            }
            if (onClick != null) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.settings_open_item),
                    tint = colors.textTertiary,
                )
            }
        }
    }
}
