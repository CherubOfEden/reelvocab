package app.reelvocab.core.designsystem.component

import app.reelvocab.core.designsystem.icon.ReelIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.reelvocab.core.designsystem.theme.ReelTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = ReelTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.elevated,
        contentColor = colors.textPrimary,
        scrimColor = colors.scrim,
        shape = RoundedCornerShape(
            topStart = ReelTheme.radius.sheetTop,
            topEnd = ReelTheme.radius.sheetTop,
        ),
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.strokeSubtle) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ReelTheme.spacing.xl)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(bottom = ReelTheme.spacing.xxl),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = ReelTheme.type.titleL,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(ReelTheme.spacing.m))
            }
            content()
        }
    }
}

@Composable
fun PermissionSheet(
    icon: ImageVector,
    title: String,
    body: String,
    why: String?,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ReelTheme.colors
    ReelSheet(onDismiss = onDismiss) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accent,
            )
        }
        Spacer(Modifier.height(ReelTheme.spacing.xl))
        Text(
            text = title,
            style = ReelTheme.type.titleL,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(ReelTheme.spacing.s))
        Text(
            text = body,
            style = ReelTheme.type.bodyM,
            color = colors.textSecondary,
        )
        if (why != null) {
            Spacer(Modifier.height(ReelTheme.spacing.l))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = ReelIcons.Info,
                    contentDescription = null,
                    tint = colors.secondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(ReelTheme.spacing.s))
                Text(
                    text = why,
                    style = ReelTheme.type.bodyM,
                    color = colors.textTertiary,
                )
            }
        }
        Spacer(Modifier.height(ReelTheme.spacing.xxl))
        ReelButton(text = confirmLabel, onClick = onConfirm)
    }
}

@Composable
fun ConfirmDestructive(
    title: String,
    body: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ReelTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.elevated,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        shape = RoundedCornerShape(ReelTheme.radius.sheet),
        title = {
            Text(text = title, style = ReelTheme.type.titleM)
        },
        text = {
            Text(text = body, style = ReelTheme.type.bodyM)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    style = ReelTheme.type.label,
                    color = colors.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = dismissLabel,
                    style = ReelTheme.type.label,
                    color = colors.textSecondary,
                )
            }
        },
    )
}
