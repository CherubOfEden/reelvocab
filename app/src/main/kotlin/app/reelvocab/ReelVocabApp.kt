package app.reelvocab

import android.app.Application
import app.reelvocab.reminders.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ReelVocabApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.scheduleIfEnabled(this)
    }
}
