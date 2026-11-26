package app.reelvocab.ui

import app.reelvocab.core.designsystem.icon.ReelIcons

import android.content.Context
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.reelvocab.BuildConfig
import app.reelvocab.R
import app.reelvocab.core.designsystem.component.CardSurface
import app.reelvocab.core.designsystem.theme.ReelTheme
import kotlinx.serialization.Serializable

@Serializable
data object LicensesRoute

private data class LicenseEntry(val title: String, val asset: String)

private val LicenseEntries = listOf(
    LicenseEntry("Fraunces", "licenses/OFL-Fraunces.txt"),
    LicenseEntry("Inter", "licenses/OFL-Inter.txt"),
    LicenseEntry("JetBrains Mono", "licenses/OFL-JetBrainsMono.txt"),
)

@Composable
fun LicensesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReelTheme.colors
    val context = LocalContext.current
    var expandedAsset by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = ReelTheme.spacing.screenH,
            end = ReelTheme.spacing.screenH,
            top = ReelTheme.spacing.s,
            bottom = ReelTheme.spacing.screenBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(ReelTheme.spacing.m),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = ReelIcons.ArrowLeft,
                        contentDescription = stringResource(R.string.common_back),
                        tint = colors.textSecondary,
                    )
                }
                Text(
                    text = stringResource(R.string.licenses_title),
                    style = ReelTheme.type.titleL,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(start = ReelTheme.spacing.xs),
                )
            }
        }

        item {
            Text(
                text = stringResource(R.string.licenses_intro),
                style = ReelTheme.type.bodyM,
                color = colors.textSecondary,
            )
        }

        item {
            Text(
                text = stringResource(R.string.licenses_version, BuildConfig.VERSION_NAME),
                style = ReelTheme.type.label,
                color = colors.textTertiary,
            )
        }

        items(LicenseEntries, key = { it.asset }) { entry ->
            val open = expandedAsset == entry.asset
            val licenseText = remember(entry.asset) { readAsset(context, entry.asset) }
            CardSurface {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = entry.title,
                        style = ReelTheme.type.titleM,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            expandedAsset = if (open) null else entry.asset
                        },
                    ) {
                        Text(
                            text = stringResource(
                                if (open) R.string.licenses_hide else R.string.licenses_show,
                            ),
                            style = ReelTheme.type.label,
                            color = colors.accent,
                        )
                    }
                }
                if (open) {
                    Spacer(Modifier.height(ReelTheme.spacing.s))
                    Text(
                        text = licenseText ?: stringResource(R.string.licenses_unreadable),
                        style = ReelTheme.type.bodyM,
                        color = colors.textTertiary,
                    )
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.licenses_attribution),
                style = ReelTheme.type.label,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = ReelTheme.spacing.s),
            )
        }
    }
}

private fun readAsset(context: Context, path: String): String? = runCatching {
    context.assets.open(path).bufferedReader().use { it.readText() }
}.getOrNull()
