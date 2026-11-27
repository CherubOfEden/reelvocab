package app.reelvocab

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reelvocab.core.designsystem.component.LoadingState
import app.reelvocab.core.designsystem.theme.ReelTheme
import app.reelvocab.core.designsystem.tts.ProvideReelSpeaker
import app.reelvocab.data.AppearancePrefs
import app.reelvocab.feature.settings.ThemeMode
import app.reelvocab.navigation.MainScaffold
import app.reelvocab.onboarding.OnboardingScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var pendingReview by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingReview = intent?.getBooleanExtra(EXTRA_OPEN_REVIEW, false) == true
        intent?.removeExtra(EXTRA_OPEN_REVIEW)
        setContent {
            var themeMode by remember { mutableStateOf(AppearancePrefs.themeMode(this)) }
            val darkTheme = when (themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Dark -> true
                ThemeMode.Light -> false
            }
            ReelTheme(darkTheme = darkTheme) {
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                    window.isStatusBarContrastEnforced = false
                    window.isNavigationBarContrastEnforced = false
                }
                ProvideReelSpeaker {
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = ReelTheme.colors.base,
                    ) {
                        when (state) {
                            MainUiState.Loading -> Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                LoadingState()
                            }

                            MainUiState.Onboarding -> OnboardingScreen(
                                onFinished = viewModel::completeOnboarding,
                            )

                            MainUiState.Main -> MainScaffold(
                                themeMode = themeMode,
                                onThemeModeChange = { mode ->
                                    AppearancePrefs.setThemeMode(this, mode)
                                    themeMode = mode
                                },
                                openReviewRequest = pendingReview,
                                onReviewRequestHandled = {
                                    pendingReview = false
                                    intent?.removeExtra(EXTRA_OPEN_REVIEW)
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_REVIEW, false)) {
            pendingReview = true
            intent.removeExtra(EXTRA_OPEN_REVIEW)
        }
    }

    companion object {
        const val EXTRA_OPEN_REVIEW = "app.reelvocab.extra.OPEN_REVIEW"
    }
}
