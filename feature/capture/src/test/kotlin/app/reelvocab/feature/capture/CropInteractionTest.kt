package app.reelvocab.feature.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CropInteractionTest {

    @Test
    fun `corners win over edge hit zones`() {
        assertEquals(
            CropHandle.TopLeft,
            cropHandleAt(
                x = 10f,
                y = 10f,
                left = 10f,
                top = 10f,
                right = 200f,
                bottom = 200f,
                edgeTouch = 28f,
                cornerTouch = 38f,
            ),
        )
        assertEquals(
            CropHandle.BottomRight,
            cropHandleAt(
                x = 200f,
                y = 200f,
                left = 10f,
                top = 10f,
                right = 200f,
                bottom = 200f,
                edgeTouch = 28f,
                cornerTouch = 38f,
            ),
        )
    }

    @Test
    fun `every edge can be grabbed away from corners`() {
        val args = listOf(
            Triple(100f, 10f, CropHandle.Top),
            Triple(100f, 200f, CropHandle.Bottom),
            Triple(10f, 100f, CropHandle.Left),
            Triple(200f, 100f, CropHandle.Right),
        )
        args.forEach { (x, y, expected) ->
            assertEquals(
                expected,
                cropHandleAt(
                    x = x,
                    y = y,
                    left = 10f,
                    top = 10f,
                    right = 200f,
                    bottom = 200f,
                    edgeTouch = 28f,
                    cornerTouch = 38f,
                ),
            )
        }
    }

    @Test
    fun `center and outside touch do not grab a handle`() {
        assertNull(
            cropHandleAt(
                x = 100f,
                y = 100f,
                left = 10f,
                top = 10f,
                right = 200f,
                bottom = 200f,
                edgeTouch = 28f,
                cornerTouch = 38f,
            ),
        )
        assertNull(
            cropHandleAt(
                x = 240f,
                y = 240f,
                left = 10f,
                top = 10f,
                right = 200f,
                bottom = 200f,
                edgeTouch = 28f,
                cornerTouch = 38f,
            ),
        )
    }

    @Test
    fun `top drag moves only the top edge`() {
        val moved = dragCropFrame(
            crop = CropFrame.Full,
            handle = CropHandle.Top,
            dragX = 0.5f,
            dragY = 0.2f,
            minWidth = 0.1f,
            minHeight = 0.1f,
        )
        assertEquals(0f, moved.left, 0.0001f)
        assertEquals(1f, moved.right, 0.0001f)
        assertEquals(1f, moved.bottom, 0.0001f)
        assertEquals(0.2f, moved.top, 0.0001f)
    }

    @Test
    fun `bottom right drag moves outward and clamps to the frame`() {
        val moved = dragCropFrame(
            crop = CropFrame(left = 0.2f, top = 0.2f, right = 0.8f, bottom = 0.8f),
            handle = CropHandle.BottomRight,
            dragX = 0.5f,
            dragY = 0.5f,
            minWidth = 0.1f,
            minHeight = 0.1f,
        )
        assertEquals(0.2f, moved.left, 0.0001f)
        assertEquals(0.2f, moved.top, 0.0001f)
        assertEquals(1f, moved.right, 0.0001f)
        assertEquals(1f, moved.bottom, 0.0001f)
    }

    @Test
    fun `minimum size prevents inverted rectangles`() {
        val moved = dragCropFrame(
            crop = CropFrame.Full,
            handle = CropHandle.Left,
            dragX = 2f,
            dragY = 0f,
            minWidth = 0.2f,
            minHeight = 0.2f,
        )
        assertEquals(0.8f, moved.left, 0.0001f)
        assertEquals(1f, moved.right, 0.0001f)
    }

    @Test
    fun `full work area maps back to the work bounds`() {
        val screen = CropFrame.Full.toScreen(
            workLeft = 18f,
            workTop = 80f,
            workWidth = 684f,
            workHeight = 1_400f,
            screenWidth = 720f,
            screenHeight = 1_560f,
        )
        assertEquals(18f / 720f, screen.left, 0.0001f)
        assertEquals(80f / 1_560f, screen.top, 0.0001f)
        assertEquals(702f / 720f, screen.right, 0.0001f)
        assertEquals(1_480f / 1_560f, screen.bottom, 0.0001f)
    }
}
