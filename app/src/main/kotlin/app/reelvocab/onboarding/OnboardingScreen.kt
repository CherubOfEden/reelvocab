package app.reelvocab.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.reelvocab.R
import app.reelvocab.core.designsystem.component.CardSurface
import app.reelvocab.core.designsystem.component.ContextSentence
import app.reelvocab.core.designsystem.component.ReelButton
import app.reelvocab.core.designsystem.component.ReviewProgressDots
import app.reelvocab.core.designsystem.theme.ReelTheme
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 3

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReelTheme.colors
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == PAGE_COUNT - 1
    val pageOf = stringResource(
        R.string.onboarding_page_of,
        pagerState.currentPage + 1,
        PAGE_COUNT,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.base)
            .systemBarsPadding(),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            when (page) {
                0 -> WatchTapLearnPage()
                1 -> MorningQuizPage()
                else -> PermissionsPage()
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = ReelTheme.spacing.screenH,
                    end = ReelTheme.spacing.screenH,
                    top = ReelTheme.spacing.m,
                    bottom = ReelTheme.spacing.screenBottom,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.semantics { contentDescription = pageOf },
            ) {
                ReviewProgressDots(total = PAGE_COUNT, current = pagerState.currentPage)
            }
            Spacer(Modifier.weight(1f))
            if (pagerState.currentPage > 0) {
                TextButton(
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    },
                    modifier = Modifier.padding(end = ReelTheme.spacing.s),
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_back),
                        style = ReelTheme.type.bodyM,
                        color = colors.textSecondary,
                    )
                }
            }
            ReelButton(
                text = stringResource(
                    if (isLast) R.string.onboarding_start else R.string.onboarding_next,
                ),
                onClick = {
                    if (isLast) {
                        onFinished()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier.width(150.dp),
            )
        }
    }
}

@Composable
private fun PageColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ReelTheme.spacing.screenH)
            .padding(top = ReelTheme.spacing.huge, bottom = ReelTheme.spacing.xl),
    ) {
        content()
    }
}

@Composable
private fun WatchTapLearnPage() {
    val colors = ReelTheme.colors
    val type = ReelTheme.type
    val haptics = ReelTheme.haptics
    val word = stringResource(R.string.onboarding_p1_word)
    var revealed by remember { mutableStateOf(false) }

    PageColumn {
        Text(
            text = stringResource(R.string.onboarding_p1_title),
            style = type.titleL,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(ReelTheme.spacing.m))
        Text(
            text = stringResource(R.string.onboarding_p1_body),
            style = type.bodyM,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(ReelTheme.spacing.xxl))
        CardSurface {
            Text(
                text = stringResource(R.string.onboarding_p1_hint),
                style = type.label,
                color = colors.textTertiary,
            )
            Spacer(Modifier.height(ReelTheme.spacing.m))
            ContextSentence(
                sentence = stringResource(R.string.onboarding_p1_clip),
                target = word,
                onTapTarget = {
                    if (!revealed) {
                        haptics.tick()
                    }
                    revealed = true
                },
            )
            AnimatedVisibility(
                visible = revealed,
                enter = fadeIn(
                    tween(
                        durationMillis = ReelTheme.motion.standardMs,
                        easing = ReelTheme.motion.standardEasing,
                    ),
                ) + expandVertically(
                    tween(
                        durationMillis = ReelTheme.motion.standardMs,
                        easing = ReelTheme.motion.standardEasing,
                    ),
                ),
            ) {
                Column {
                    Spacer(Modifier.height(ReelTheme.spacing.l))
                    Text(
                        text = word,
                        style = type.hero,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = stringResource(R.string.onboarding_p1_ipa),
                        style = type.ipa,
                        color = colors.textSecondary,
                    )
                    Spacer(Modifier.height(ReelTheme.spacing.s))
                    Text(
                        text = stringResource(R.string.onboarding_p1_meaning),
                        style = type.bodyM,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun MorningQuizPage() {
    val colors = ReelTheme.colors
    val type = ReelTheme.type
    val haptics = ReelTheme.haptics
    val options = listOf(
        stringResource(R.string.onboarding_p2_option_a),
        stringResource(R.string.onboarding_p2_option_b),
        stringResource(R.string.onboarding_p2_option_c),
        stringResource(R.string.onboarding_p2_option_d),
    )
    val correctIndex = 1
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    PageColumn {
        Text(
            text = stringResource(R.string.onboarding_p2_title),
            style = type.titleL,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(ReelTheme.spacing.m))
        Text(
            text = stringResource(R.string.onboarding_p2_body),
            style = type.bodyM,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(ReelTheme.spacing.xxl))
        CardSurface {
            Text(
                text = stringResource(R.string.onboarding_p2_question),
                style = type.titleM,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(ReelTheme.spacing.l))
            options.forEachIndexed { index, option ->
                val isSelected = selectedIndex == index
                val isCorrect = index == correctIndex
                val container = when {
                    selectedIndex == null -> colors.surface
                    isCorrect -> colors.green.copy(alpha = 0.18f)
                    isSelected -> colors.rose.copy(alpha = 0.18f)
                    else -> colors.surface
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(ReelTheme.radius.input))
                        .background(container)
                        .selectable(
                            selected = isSelected,
                            enabled = selectedIndex == null,
                            role = Role.RadioButton,
                            onClick = {
                                selectedIndex = index
                                if (isCorrect) haptics.success() else haptics.error()
                            },
                        )
                        .padding(ReelTheme.spacing.l),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = option,
                        style = type.bodyL,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    if (selectedIndex != null && (isCorrect || isSelected)) {
                        Icon(
                            imageVector = if (isCorrect) Icons.Filled.Check else Icons.Filled.Close,
                            contentDescription = null,
                            tint = if (isCorrect) colors.green else colors.rose,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.height(ReelTheme.spacing.s))
            }
            val result = selectedIndex
            if (result != null) {
                Text(
                    text = stringResource(
                        if (result == correctIndex) {
                            R.string.onboarding_p2_correct
                        } else {
                            R.string.onboarding_p2_wrong
                        },
                    ),
                    style = type.bodyM,
                    color = if (result == correctIndex) colors.green else colors.rose,
                )
            }
        }
    }
}

@Composable
private fun PermissionsPage() {
    val colors = ReelTheme.colors
    val type = ReelTheme.type

    PageColumn {
        Text(
            text = stringResource(R.string.onboarding_p3_title),
            style = type.titleL,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(ReelTheme.spacing.m))
        Text(
            text = stringResource(R.string.onboarding_p3_body),
            style = type.bodyM,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(ReelTheme.spacing.xxl))
        PermissionRow(
            icon = Icons.Filled.Screenshot,
            tint = colors.gold,
            title = stringResource(R.string.onboarding_p3_screen_title),
            body = stringResource(R.string.onboarding_p3_screen_body),
        )
        Spacer(Modifier.height(ReelTheme.spacing.l))
        PermissionRow(
            icon = Icons.Filled.Mic,
            tint = colors.teal,
            title = stringResource(R.string.onboarding_p3_mic_title),
            body = stringResource(R.string.onboarding_p3_mic_body),
        )
        Spacer(Modifier.height(ReelTheme.spacing.l))
        PermissionRow(
            icon = Icons.Filled.Notifications,
            tint = colors.rose,
            title = stringResource(R.string.onboarding_p3_notif_title),
            body = stringResource(R.string.onboarding_p3_notif_body),
        )
        Spacer(Modifier.height(ReelTheme.spacing.xl))
        Text(
            text = stringResource(R.string.onboarding_p3_footnote),
            style = type.label,
            color = colors.textTertiary,
        )
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    body: String,
) {
    val colors = ReelTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(ReelTheme.spacing.m))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = ReelTheme.type.bodyL,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(ReelTheme.spacing.xxs))
            Text(
                text = body,
                style = ReelTheme.type.bodyM,
                color = colors.textSecondary,
            )
        }
    }
}
