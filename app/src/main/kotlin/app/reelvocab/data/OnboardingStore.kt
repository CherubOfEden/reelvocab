package app.reelvocab.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.reelDataStore by preferencesDataStore(
    name = "reelvocab_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

@Singleton
class OnboardingStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val completed: Flow<Boolean> = context.reelDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[KEY_COMPLETED] ?: false }

    suspend fun markCompleted() {
        try {
            context.reelDataStore.edit { prefs ->
                prefs[KEY_COMPLETED] = true
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w(TAG, "Could not persist onboarding completion", error)
        }
    }

    private companion object {
        const val TAG = "OnboardingStore"
        val KEY_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }
}
