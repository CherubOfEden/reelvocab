package app.reelvocab.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.reelvocab.core.designsystem.tts.LocalReelSpeaker

@Composable
fun ReelSpeakButton(
    text: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val speaker = LocalReelSpeaker.current
    val speakingText by speaker.speaking
    val term = text.trim()
    TtsButton(
        onClick = { speaker.speak(term) },
        speaking = term.isNotEmpty() && speakingText == term,
        contentDescription = contentDescription,
        modifier = modifier,
    )
}
