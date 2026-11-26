package app.reelvocab.core.designsystem.icon

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Thin-line Lucide icon set vendored for ReelVocab. Lucide is ISC licensed.
 * Icons are tinted by [Icon]; the source strokes are intentionally neutral.
 */
object ReelIcons {
    val House: ImageVector by lazy {
        lucide("House", autoMirror = false,
            "M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8",
            "M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z",
        )
    }
    val Layers: ImageVector by lazy {
        lucide("Layers", autoMirror = false,
            "M12.83 2.18a2 2 0 0 0-1.66 0L2.6 6.08a1 1 0 0 0 0 1.83l8.58 3.91a2 2 0 0 0 1.66 0l8.58-3.9a1 1 0 0 0 0-1.83z",
            "M2 12a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 12",
            "M2 17a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 17",
        )
    }
    val BookA: ImageVector by lazy {
        lucide("BookA", autoMirror = false,
            "M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H19a1 1 0 0 1 1 1v18a1 1 0 0 1-1 1H6.5a1 1 0 0 1 0-5H20",
            "m8 13 4-7 4 7",
            "M9.1 11h5.7",
        )
    }
    val Library: ImageVector by lazy {
        lucide("Library", autoMirror = false,
            "m16 6 4 14",
            "M12 6v14",
            "M8 8v12",
            "M4 4v16",
        )
    }
    val NotebookPen: ImageVector by lazy {
        lucide("NotebookPen", autoMirror = false,
            "M13.4 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-7.4",
            "M2 6h4",
            "M2 10h4",
            "M2 14h4",
            "M2 18h4",
            "M21.378 5.626a1 1 0 1 0-3.004-3.004l-5.01 5.012a2 2 0 0 0-.506.854l-.837 2.87a.5.5 0 0 0 .62.62l2.87-.837a2 2 0 0 0 .854-.506z",
        )
    }
    val Search: ImageVector by lazy {
        lucide("Search", autoMirror = false,
            "m21 21-4.34-4.34",
            "M3 11a8 8 0 1 0 16 0a8 8 0 1 0 -16 0Z",
        )
    }
    val AlarmClock: ImageVector by lazy {
        lucide("AlarmClock", autoMirror = false,
            "M4 13a8 8 0 1 0 16 0a8 8 0 1 0 -16 0Z",
            "M12 9v4l2 2",
            "M5 3 2 6",
            "m22 6-3-3",
            "M6.38 18.7 4 21",
            "M17.64 18.67 20 21",
        )
    }
    val Settings2: ImageVector by lazy {
        lucide("Settings2", autoMirror = false,
            "M14 17H5",
            "M19 7h-9",
            "M14 17a3 3 0 1 0 6 0a3 3 0 1 0 -6 0Z",
            "M4 7a3 3 0 1 0 6 0a3 3 0 1 0 -6 0Z",
        )
    }
    val ScanText: ImageVector by lazy {
        lucide("ScanText", autoMirror = false,
            "M3 7V5a2 2 0 0 1 2-2h2",
            "M17 3h2a2 2 0 0 1 2 2v2",
            "M21 17v2a2 2 0 0 1-2 2h-2",
            "M7 21H5a2 2 0 0 1-2-2v-2",
            "M7 8h8",
            "M7 12h10",
            "M7 16h6",
        )
    }
    val BookOpenText: ImageVector by lazy {
        lucide("BookOpenText", autoMirror = true,
            "M12 5v16",
            "M16 13h2",
            "M16 9h2",
            "M20.001 19A2 2 0 0022 17V5a2 2 0 00-1.999-2L16 3.002A5 5 0 0012 5a5 5 0 00-4-2H4a2 2 0 00-2 2v12a2 2 0 001.999 2H8a5 5 0 014 2 5 5 0 014-2z",
            "M6 13h2",
            "M6 9h2",
        )
    }
    val Video: ImageVector by lazy {
        lucide("Video", autoMirror = false,
            "m16 13 5.223 3.482a.5.5 0 0 0 .777-.416V7.87a.5.5 0 0 0-.752-.432L16 10.5",
            "M4 6h10a2 2 0 0 1 2 2v8a2 2 0 0 1 -2 2h-10a2 2 0 0 1 -2 -2v-8a2 2 0 0 1 2 -2Z",
        )
    }
    val AudioLines: ImageVector by lazy {
        lucide("AudioLines", autoMirror = false,
            "M2 10v3",
            "M6 6v11",
            "M10 3v18",
            "M14 8v7",
            "M18 5v13",
            "M22 10v3",
        )
    }
    val Play: ImageVector by lazy {
        lucide("Play", autoMirror = false,
            "M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z",
        )
    }
    val Pause: ImageVector by lazy {
        lucide("Pause", autoMirror = false,
            "M15 3h3a1 1 0 0 1 1 1v16a1 1 0 0 1 -1 1h-3a1 1 0 0 1 -1 -1v-16a1 1 0 0 1 1 -1Z",
            "M6 3h3a1 1 0 0 1 1 1v16a1 1 0 0 1 -1 1h-3a1 1 0 0 1 -1 -1v-16a1 1 0 0 1 1 -1Z",
        )
    }
    val Square: ImageVector by lazy {
        lucide("Square", autoMirror = false,
            "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2Z",
        )
    }
    val Bookmark: ImageVector by lazy {
        lucide("Bookmark", autoMirror = false,
            "M17 3a2 2 0 0 1 2 2v15a1 1 0 0 1-1.496.868l-4.512-2.578a2 2 0 0 0-1.984 0l-4.512 2.578A1 1 0 0 1 5 20V5a2 2 0 0 1 2-2z",
        )
    }
    val Heart: ImageVector by lazy {
        lucide("Heart", autoMirror = false,
            "M2 9.5a5.5 5.5 0 0 1 9.591-3.676.56.56 0 0 0 .818 0A5.49 5.49 0 0 1 22 9.5c0 2.29-1.5 4-3 5.5l-5.492 5.313a2 2 0 0 1-3 .019L5 15c-1.5-1.5-3-3.2-3-5.5",
        )
    }
    val Trash2: ImageVector by lazy {
        lucide("Trash2", autoMirror = false,
            "M10 11v6",
            "M14 11v6",
            "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6",
            "M3 6h18",
            "M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2",
        )
    }
    val ArrowLeft: ImageVector by lazy {
        lucide("ArrowLeft", autoMirror = true,
            "m12 19-7-7 7-7",
            "M19 12H5",
        )
    }
    val X: ImageVector by lazy {
        lucide("X", autoMirror = false,
            "M18 6 6 18",
            "m6 6 12 12",
        )
    }
    val Ellipsis: ImageVector by lazy {
        lucide("Ellipsis", autoMirror = false,
            "M11 12a1 1 0 1 0 2 0a1 1 0 1 0 -2 0Z",
            "M18 12a1 1 0 1 0 2 0a1 1 0 1 0 -2 0Z",
            "M4 12a1 1 0 1 0 2 0a1 1 0 1 0 -2 0Z",
        )
    }
    val EllipsisVertical: ImageVector by lazy {
        lucide("EllipsisVertical", autoMirror = false,
            "M11 12a1 1 0 1 0 2 0a1 1 0 1 0 -2 0Z",
            "M11 5a1 1 0 1 0 2 0a1 1 0 1 0 -2 0Z",
            "M11 19a1 1 0 1 0 2 0a1 1 0 1 0 -2 0Z",
        )
    }
    val ListFilter: ImageVector by lazy {
        lucide("ListFilter", autoMirror = false,
            "M2 5h20",
            "M6 12h12",
            "M9 19h6",
        )
    }
    val Clapperboard: ImageVector by lazy {
        lucide("Clapperboard", autoMirror = false,
            "m12.296 3.464 3.02 3.956",
            "M20.2 6 3 11l-.9-2.4c-.3-1.1.3-2.2 1.3-2.5l13.5-4c1.1-.3 2.2.3 2.5 1.3z",
            "M3 11h18v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z",
            "m6.18 5.276 3.1 3.899",
        )
    }
    val Image: ImageVector by lazy {
        lucide("Image", autoMirror = false,
            "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2Z",
            "M7 9a2 2 0 1 0 4 0a2 2 0 1 0 -4 0Z",
            "m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21",
        )
    }
    val PictureInPicture2: ImageVector by lazy {
        lucide("PictureInPicture2", autoMirror = false,
            "M21 9V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v10c0 1.1.9 2 2 2h4",
            "M14 13h6a2 2 0 0 1 2 2v3a2 2 0 0 1 -2 2h-6a2 2 0 0 1 -2 -2v-3a2 2 0 0 1 2 -2Z",
        )
    }
    val Volume2: ImageVector by lazy {
        lucide("Volume2", autoMirror = false,
            "M11 4.702a.705.705 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.705.705 0 0 0 11 19.298z",
            "M16 9a5 5 0 0 1 0 6",
            "M19.364 18.364a9 9 0 0 0 0-12.728",
        )
    }
    val VolumeX: ImageVector by lazy {
        lucide("VolumeX", autoMirror = false,
            "M11 4.702a.7.7 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.7.7 0 0 0 11 19.298z",
            "m16.5 14.5 5-5",
            "m16.5 9.5 5 5",
        )
    }
    val SkipBack: ImageVector by lazy {
        lucide("SkipBack", autoMirror = false,
            "M17.971 4.285A2 2 0 0 1 21 6v12a2 2 0 0 1-3.029 1.715l-9.997-5.998a2 2 0 0 1-.003-3.432z",
            "M3 20V4",
        )
    }
    val SkipForward: ImageVector by lazy {
        lucide("SkipForward", autoMirror = false,
            "M21 4v16",
            "M6.029 4.285A2 2 0 0 0 3 6v12a2 2 0 0 0 3.029 1.715l9.997-5.998a2 2 0 0 0 .003-3.432z",
        )
    }
    val Sun: ImageVector by lazy {
        lucide("Sun", autoMirror = false,
            "M8 12a4 4 0 1 0 8 0a4 4 0 1 0 -8 0Z",
            "M12 2v2",
            "M12 20v2",
            "m4.93 4.93 1.41 1.41",
            "m17.66 17.66 1.41 1.41",
            "M2 12h2",
            "M20 12h2",
            "m6.34 17.66-1.41 1.41",
            "m19.07 4.93-1.41 1.41",
        )
    }
    val Check: ImageVector by lazy {
        lucide("Check", autoMirror = false,
            "M20 6 9 17l-5-5",
        )
    }
    val Pencil: ImageVector by lazy {
        lucide("Pencil", autoMirror = false,
            "M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z",
            "m15 5 4 4",
        )
    }
    val FastForward: ImageVector by lazy {
        lucide("FastForward", autoMirror = false,
            "M12 6a2 2 0 0 1 3.414-1.414l6 6a2 2 0 0 1 0 2.828l-6 6A2 2 0 0 1 12 18z",
            "M2 6a2 2 0 0 1 3.414-1.414l6 6a2 2 0 0 1 0 2.828l-6 6A2 2 0 0 1 2 18z",
        )
    }
    val Rewind: ImageVector by lazy {
        lucide("Rewind", autoMirror = false,
            "M12 6a2 2 0 0 0-3.414-1.414l-6 6a2 2 0 0 0 0 2.828l6 6A2 2 0 0 0 12 18z",
            "M22 6a2 2 0 0 0-3.414-1.414l-6 6a2 2 0 0 0 0 2.828l6 6A2 2 0 0 0 22 18z",
        )
    }
    val TriangleAlert: ImageVector by lazy {
        lucide("TriangleAlert", autoMirror = false,
            "m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3",
            "M12 9v4",
            "M12 17h.01",
        )
    }
    val CloudOff: ImageVector by lazy {
        lucide("CloudOff", autoMirror = false,
            "M10.94 5.274A7 7 0 0 1 15.71 10h1.79a4.5 4.5 0 0 1 4.222 6.057",
            "M18.796 18.81A4.5 4.5 0 0 1 17.5 19H9A7 7 0 0 1 5.79 5.78",
            "m2 2 20 20",
        )
    }
    val Tag: ImageVector by lazy {
        lucide("Tag", autoMirror = false,
            "M12.586 2.586A2 2 0 0 0 11.172 2H4a2 2 0 0 0-2 2v7.172a2 2 0 0 0 .586 1.414l8.704 8.704a2.426 2.426 0 0 0 3.42 0l6.58-6.58a2.426 2.426 0 0 0 0-3.42z",
            "M7 7.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0Z",
        )
    }
    val ChevronRight: ImageVector by lazy {
        lucide("ChevronRight", autoMirror = true,
            "m9 18 6-6-6-6",
        )
    }
    val Info: ImageVector by lazy {
        lucide("Info", autoMirror = false,
            "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0Z",
            "M12 16v-4",
            "M12 8h.01",
        )
    }
    val Share2: ImageVector by lazy {
        lucide("Share2", autoMirror = false,
            "M15 5a3 3 0 1 0 6 0a3 3 0 1 0 -6 0Z",
            "M3 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0Z",
            "M15 19a3 3 0 1 0 6 0a3 3 0 1 0 -6 0Z",
            "M8.59 13.51L15.42 17.49",
            "M15.41 6.51L8.59 10.49",
        )
    }
    val Mic: ImageVector by lazy {
        lucide("Mic", autoMirror = false,
            "M12 19v3",
            "M19 10v2a7 7 0 0 1-14 0v-2",
            "M12 2h0a3 3 0 0 1 3 3v7a3 3 0 0 1 -3 3h-0a3 3 0 0 1 -3 -3v-7a3 3 0 0 1 3 -3Z",
        )
    }
    val Bell: ImageVector by lazy {
        lucide("Bell", autoMirror = false,
            "M10.268 21a2 2 0 0 0 3.464 0",
            "M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326",
        )
    }
    val Music: ImageVector by lazy {
        lucide("Music", autoMirror = false,
            "M21 16V3a1 1 0 0 0-1.28-.96l-10 2.917a1 1 0 0 0-.72.96V19",
            "M15 16a3 3 0 1 0 6 0a3 3 0 1 0 -6 0Z",
            "M3 19a3 3 0 1 0 6 0a3 3 0 1 0 -6 0Z",
        )
    }
}

@Composable
fun ReelIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
    )
}

private fun lucide(
    name: String,
    autoMirror: Boolean = false,
    vararg paths: String,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
    autoMirror = autoMirror,
).apply {
    paths.forEach { data ->
        addPath(
            pathData = PathParser().parsePathString(data).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
}.build()
