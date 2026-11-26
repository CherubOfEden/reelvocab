package app.reelvocab.core.designsystem.component

import app.reelvocab.core.designsystem.icon.ReelIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.reelvocab.core.designsystem.illustration.ReelIllustration
import app.reelvocab.core.designsystem.illustration.ReelIllustrationArt
import app.reelvocab.core.designsystem.theme.ReelTheme

@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    val colors = ReelTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(ReelTheme.spacing.huge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = colors.accent,
            strokeWidth = 2.dp,
        )
        if (message != null) {
            Spacer(Modifier.height(ReelTheme.spacing.m))
            Text(
                text = message,
                style = ReelTheme.type.bodyM,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    ctaLabel: String,
    onCta: () -> Unit,
    modifier: Modifier = Modifier,
    illustration: ReelIllustration = ReelIllustration.Poster,
) {
    val colors = ReelTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ReelTheme.spacing.xxl, vertical = ReelTheme.spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ReelIllustrationArt(illustration = illustration)
        Spacer(Modifier.height(ReelTheme.spacing.xxl))
        Text(
            text = title,
            style = ReelTheme.type.titleL,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(ReelTheme.spacing.s))
        Text(
            text = body,
            style = ReelTheme.type.bodyM,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(ReelTheme.spacing.xxl))
        ReelButton(
            text = ctaLabel,
            onClick = onCta,
            modifier = Modifier.widthIn(max = 280.dp),
        )
    }
}

@Composable
fun ErrorState(
    title: String,
    body: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReelTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ReelTheme.spacing.xxl, vertical = ReelTheme.spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(colors.error.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ReelIcons.TriangleAlert,
                contentDescription = null,
                tint = colors.error,
            )
        }
        Spacer(Modifier.height(ReelTheme.spacing.xl))
        Text(
            text = title,
            style = ReelTheme.type.titleL,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(ReelTheme.spacing.s))
        Text(
            text = body,
            style = ReelTheme.type.bodyM,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(ReelTheme.spacing.xxl))
        ReelButton(
            text = retryLabel,
            onClick = onRetry,
            style = ReelButtonStyle.Secondary,
            modifier = Modifier.widthIn(max = 280.dp),
        )
    }
}

@Composable
fun OfflineBanner(
    message: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReelTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ReelTheme.radius.input))
            .background(colors.secondary.copy(alpha = 0.12f))
            .padding(horizontal = ReelTheme.spacing.l, vertical = ReelTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = ReelIcons.CloudOff,
            contentDescription = null,
            tint = colors.secondary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(ReelTheme.spacing.s))
        Text(
            text = message,
            style = ReelTheme.type.bodyM,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry) {
            Text(
                text = retryLabel,
                style = ReelTheme.type.label,
                color = colors.secondary,
            )
        }
    }
}

@Composable
fun InlineError(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = ReelTheme.type.bodyM,
        color = ReelTheme.colors.error,
        modifier = modifier,
    )
}
