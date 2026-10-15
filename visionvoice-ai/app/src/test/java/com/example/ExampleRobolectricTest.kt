package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AiDetailLevel
import com.example.data.local.ContrastTheme
import com.example.data.local.PreferencesManager
import com.example.data.local.ScanRecord
import com.example.data.local.VisionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Drishti", appName)
    }

    @Test
    fun preferencesManagerLoadsDefaults() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager(context)
        val settings = prefs.settings.value
        assertEquals(1.0f, settings.speechRate)
        assertEquals(1.0f, settings.speechPitch)
        assertTrue(settings.autoSpeakResults)
        assertEquals(ContrastTheme.DARK_CYAN, settings.contrastTheme)
        assertEquals(AiDetailLevel.BALANCED, settings.detailLevel)
    }

    @Test
    fun preferencesManagerUpdatesContrastTheme() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager(context)
        prefs.updateContrastTheme(ContrastTheme.YELLOW_ON_BLACK)
        assertEquals(ContrastTheme.YELLOW_ON_BLACK, prefs.settings.value.contrastTheme)
    }

    @Test
    fun createScanRecordModel() {
        val scan = ScanRecord(
            mode = VisionMode.SCENE,
            title = "Office Space",
            speechText = "Desk directly ahead with chair to your left",
            fullDescription = "Detailed scene with lighting and furniture layout.",
            isBookmarked = true
        )
        assertNotNull(scan)
        assertEquals(VisionMode.SCENE, scan.mode)
        assertTrue(scan.isBookmarked)
    }
}
